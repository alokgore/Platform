package com.tejas.utils.io;

import static com.tejas.core.enums.PlatformComponents.PLATFORM_UTIL_LIB;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.tejas.core.TejasBackgroundJob;
import com.tejas.core.TejasContext;
import com.tejas.utils.io.FileTailer.DataListener;
import com.tejas.utils.io.FileTailer.TailStatus;
import com.tejas.utils.misc.DateTimeUtils;
import com.tejas.utils.misc.DateTimeUtils.Schedule;

public class RollingFileTailer extends TejasBackgroundJob
{
    public class FileRolloverTask extends AbstractTejasTask
    {
        @Override
        public void runIteration(TejasContext self, TejasBackgroundJob backgroundJob) throws Exception
        {
            RollingFileTailer.this.rollOver(self);
        }

        @Override
        public void shutdown(TejasContext self) throws Exception
        {
            RollingFileTailer.this.stopTailing(self);
        }
    }

    @SuppressWarnings("synthetic-access")
    public class Slot
    {
        public final Date startTime;
        public final Date endTime;
        private FileTailer fileTailer;

        /**
         * @return
         * @see com.tejas.utils.io.FileTailer#isActive()
         */
        public final boolean isActive()
        {
            return (this.fileTailer != null) && this.fileTailer.isActive();
        }

        public synchronized void startFileTailer(TejasContext self)
        {
            if ((this.fileTailer == null) && hasFile())
            {
                this.fileTailer = new FileTailer(self, getFile(), RollingFileTailer.this.dataListener, isOld());
                this.fileTailer.start();
            }
        }

        public boolean hasFile()
        {
            return getFile().exists();
        }

        public Slot(Date startTime)
        {
            this.startTime = startTime;
            this.endTime = DateTimeUtils.getNext(this.startTime, RollingFileTailer.this.rolloverSchedule);
        }

        public Slot getNextSlot()
        {
            return new Slot(this.endTime);
        }

        public Slot getPreviousSlot()
        {
            return new Slot(DateTimeUtils.getPrevious(this.startTime, RollingFileTailer.this.rolloverSchedule));
        }

        public String getFileName()
        {
            return RollingFileTailer.this.baseFileName + new SimpleDateFormat(RollingFileTailer.this.dateTimeSuffix).format(this.startTime);
        }

        public File getFile()
        {
            return new File(RollingFileTailer.this.directory.getAbsolutePath() + File.separator + getFileName());
        }

        @Override
        public int hashCode()
        {
            return this.startTime.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof Slot)
            {
                Slot slot = (Slot) obj;

                return this.startTime.equals(slot.startTime);
            }
            return false;
        }

        /**
         * The slot is considered old if the slot time-window lies entirely in the past
         */
        public boolean isOld()
        {
            return new Date().getTime() > this.endTime.getTime();
        }

        public void stopTailer(TejasContext self, TailStatus status)
        {
            if (this.fileTailer != null)
            {
                this.fileTailer.stop(self, status);
            }
        }

        public void stopTailingAfterEOF(TejasContext self)
        {
            if (this.fileTailer != null)
            {
                this.fileTailer.stopTailingAfterEOF(self);
            }
        }
    }

    private File directory;
    private String baseFileName;
    private Schedule rolloverSchedule;
    private String dateTimeSuffix;
    private DataListener dataListener;
    private Date sessionStartTime;
    private Date sessionEndTime;
    private Slot currentSlot;

    public synchronized Slot getCurrentSlot()
    {
        return this.currentSlot;
    }

    public synchronized Date getSessionStartTime()
    {
        return this.sessionStartTime;
    }

    public void rollOver(TejasContext self)
    {
        /*
         * Doing it in each iteration to cover the edge cases around delays in the file-creation etc
         */
        this.currentSlot.startFileTailer(self);

        if (this.currentSlot.isOld())
        {
            this.currentSlot.stopTailingAfterEOF(self);

            if (this.currentSlot.isActive() == false)
            {
                Slot nextSlot = this.currentSlot.getNextSlot();
                if ((nextSlot.startTime.getTime() < this.sessionEndTime.getTime()))
                {
                    self.logger.info("Switching to the slot ", nextSlot.getFileName());
                    this.currentSlot = nextSlot;
                    this.currentSlot.startFileTailer(self);
                }
                else
                {
                    /*
                     * We have reached the end of the session. Die!
                     */
                    this.signalShutdown();
                }
            }

        }
    }

    void stopTailing(TejasContext self)
    {
        self.logger.info("Stopping the rolling-tailer for [" + this.baseFileName + "]");
        this.currentSlot.stopTailer(self, TailStatus.InProgress);
    }

    @SuppressWarnings("hiding")
    public static class Builder
    {
        private File directory;
        private String baseFileName;
        private Schedule rolloverSchedule = Schedule.Hourly;
        private String dateTimeSuffix = ".yyyy-MM-dd-HH";
        private DataListener dataListener;
        private Date sessionStartTime;
        private Date sessionEndTime;

        public Builder(File directory, String baseFileName, DataListener dataListener)
        {
            this.directory = directory;
            this.baseFileName = baseFileName;
            this.dataListener = dataListener;
        }

        public Builder rolloverSchedule(Schedule rolloverSchedule)
        {
            this.rolloverSchedule = rolloverSchedule;
            return this;
        }

        public Builder dateTimeSuffix(String dateTimeSuffix)
        {
            this.dateTimeSuffix = dateTimeSuffix;
            return this;
        }

        public Builder sessionStartTime(Date sessionStartTime)
        {
            this.sessionStartTime = sessionStartTime;
            return this;
        }

        public Builder sessionEndTime(Date sessionEndTime)
        {
            this.sessionEndTime = sessionEndTime;
            return this;
        }

        public RollingFileTailer build(TejasContext self)
        {
            return new RollingFileTailer(this, self);
        }
    }

    @SuppressWarnings("synthetic-access")
    RollingFileTailer(Builder builder, TejasContext self)
    {
        super(self, null, new Configuration.Builder("FileRolloverTask - " + builder.baseFileName, PLATFORM_UTIL_LIB, 1000).build());

        this.directory = builder.directory;
        this.baseFileName = builder.baseFileName;
        this.rolloverSchedule = builder.rolloverSchedule;
        this.dateTimeSuffix = builder.dateTimeSuffix;
        this.dataListener = builder.dataListener;

        // Pick-up last 3 files by default
        this.sessionStartTime =
                (builder.sessionStartTime == null ? new Date(System.currentTimeMillis() - this.rolloverSchedule.getIntervalInMillis() * 3) : builder.sessionStartTime);

        // Set end-time to 10 years from now, if not specified
        this.sessionEndTime = builder.sessionEndTime != null ? builder.sessionEndTime : new Date(System.currentTimeMillis() + 10 * 365L * 24 * 3600L * 1000L);

        this.setTask(self, new FileRolloverTask());

        this.currentSlot = new Slot(DateTimeUtils.getNormalizedTime(this.sessionStartTime, this.rolloverSchedule));

        self.logger.info("RollingFileTailer for [" + this.baseFileName + "] from [" + this.sessionStartTime + "] to [" + this.sessionEndTime + "]");
    }
}
