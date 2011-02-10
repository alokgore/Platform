package com.tejas.utils.io;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.tejas.core.TejasContext;
import com.tejas.utils.misc.DateTimeUtils;
import com.tejas.utils.misc.DateTimeUtils.Schedule;
import com.tejas.utils.misc.FileTailer;
import com.tejas.utils.misc.FileTailer.DataListener;

public class RollingFileTailer
{
    class Slot
    {
        public final Date startTime;
        public final Date endTime;
        private FileTailer fileTailer;

        public synchronized FileTailer getFileTailer(TejasContext self)
        {
            if ((this.fileTailer == null) && getFile().exists())
            {
                this.fileTailer = new FileTailer(self, getFile(), RollingFileTailer.this.dataListener, isOld());
            }
            return this.fileTailer;
        }

        public Slot(Date startTime, Date endTime)
        {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public Slot getNextSlot()
        {
            return new Slot(this.endTime, DateTimeUtils.getNext(this.endTime, RollingFileTailer.this.rolloverSchedule));
        }

        public Slot getPreviousSlot()
        {
            return new Slot(DateTimeUtils.getPrevious(this.startTime, RollingFileTailer.this.rolloverSchedule), this.startTime);
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
    }

    File directory;
    String baseFileName;
    Schedule rolloverSchedule;
    String dateTimeSuffix;
    DataListener dataListener;
    Date sessionStartTime;
    Date sessionEndTime;

    public RollingFileTailer()
    {
        // dummy
    }

    RollingFileTailer(File directory, String baseFileName, Schedule rolloverSchedule,
            String dateTimeSuffix, DataListener dataListener, Date sessionStartTime,
            Date sessionEndTime)
    {
        this.directory = directory;
        this.baseFileName = baseFileName;
        this.rolloverSchedule = rolloverSchedule;
        this.dateTimeSuffix = dateTimeSuffix;
        this.dataListener = dataListener;
        this.sessionStartTime = sessionStartTime;
        this.sessionEndTime = sessionEndTime;
    }

    Slot getCurrentSlot()
    {
        Date slotStart = DateTimeUtils.getNormalizedTime(new Date(), RollingFileTailer.this.rolloverSchedule);
        Timestamp slotEnd = DateTimeUtils.getNext(slotStart, RollingFileTailer.this.rolloverSchedule);
        return new Slot(slotStart, slotEnd);
    }

    @SuppressWarnings("hiding")
    public static class Builder
    {
        private File directory;
        private String baseFileName;
        private Schedule rolloverSchedule = Schedule.Hourly;
        private String dateTimeSuffix = ".YYYY-MM-DD-HH";
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

        public RollingFileTailer build()
        {
            return new RollingFileTailer(this);
        }
    }

    @SuppressWarnings("synthetic-access")
    RollingFileTailer(Builder builder)
    {
        this.directory = builder.directory;
        this.baseFileName = builder.baseFileName;
        this.rolloverSchedule = builder.rolloverSchedule;
        this.dateTimeSuffix = builder.dateTimeSuffix;
        this.dataListener = builder.dataListener;
        this.sessionStartTime = builder.sessionStartTime;
        this.sessionEndTime = builder.sessionEndTime;
    }
}
