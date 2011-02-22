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
            return (fileTailer != null) && fileTailer.isActive();
        }

        public synchronized void startFileTailer(TejasContext self)
        {
            if ((fileTailer == null) && hasFile())
            {
                fileTailer = new FileTailer(self, getFile(), dataListener, isOld());
                fileTailer.start();
            }
        }

        public boolean hasFile()
        {
            return getFile().exists();
        }

        public Slot(Date startTime)
        {
            this.startTime = startTime;
            endTime = DateTimeUtils.getNext(this.startTime, rolloverSchedule);
        }

        public Slot getNextSlot()
        {
            return new Slot(endTime);
        }

        public Slot getPreviousSlot()
        {
            return new Slot(DateTimeUtils.getPrevious(startTime, rolloverSchedule));
        }

        public String getFileName()
        {
            return baseFileName + new SimpleDateFormat(dateTimeSuffix).format(startTime);
        }

        public File getFile()
        {
            return new File(directory.getAbsolutePath() + File.separator + getFileName());
        }

        @Override
        public int hashCode()
        {
            return startTime.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof Slot)
            {
                Slot slot = (Slot) obj;

                return startTime.equals(slot.startTime);
            }
            return false;
        }

        /**
         * The slot is considered old if the slot time-window lies entirely in the past
         */
        public boolean isOld()
        {
            return new Date().getTime() > endTime.getTime();
        }

        public void stopTailer(TejasContext self, TailStatus status)
        {
            if (fileTailer != null)
            {
                fileTailer.stop(self, status);
            }
        }

        public void stopTailingAfterEOF(TejasContext self)
        {
            if (fileTailer != null)
            {
                fileTailer.stopTailingAfterEOF(self);
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
        return currentSlot;
    }

    public synchronized Date getSessionStartTime()
    {
        return sessionStartTime;
    }

    void rollOver(TejasContext self)
    {
        /*
         * Doing it in each iteration to cover the edge cases around delays in the file-creation etc
         */
        currentSlot.startFileTailer(self);

        if (currentSlot.isOld())
        {
            currentSlot.stopTailingAfterEOF(self);

            if (currentSlot.isActive() == false)
            {
                Slot nextSlot = currentSlot.getNextSlot();
                if ((nextSlot.startTime.getTime() < sessionEndTime.getTime()))
                {
                    self.logger.info("Switching to the slot ", nextSlot.getFileName());
                    currentSlot = nextSlot;
                    currentSlot.startFileTailer(self);
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
        self.logger.info("Stopping the rolling-tailer for [" + baseFileName + "]");
        currentSlot.stopTailer(self, TailStatus.InProgress);
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

        directory = builder.directory;
        baseFileName = builder.baseFileName;
        rolloverSchedule = builder.rolloverSchedule;
        dateTimeSuffix = builder.dateTimeSuffix;
        dataListener = builder.dataListener;

        // Pick-up last 3 files by default
        sessionStartTime =
                (builder.sessionStartTime == null ? new Date(System.currentTimeMillis() - rolloverSchedule.getIntervalInMillis() * 3) : builder.sessionStartTime);

        // Set end-time to 10 years from now, if not specified
        sessionEndTime = builder.sessionEndTime != null ? builder.sessionEndTime : new Date(System.currentTimeMillis() + 10 * 365L * 24 * 3600L * 1000L);

        this.setTask(self, new FileRolloverTask());

        currentSlot = new Slot(DateTimeUtils.getNormalizedTime(sessionStartTime, rolloverSchedule));

        self.logger.info("RollingFileTailer for [" + baseFileName + "] from [" + sessionStartTime + "] to [" + sessionEndTime + "]");
    }
}
