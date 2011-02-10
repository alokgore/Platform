package com.tejas.utils.io;

import java.io.File;
import java.util.Date;
import com.tejas.utils.misc.DateTimeUtils.Schedule;

public class RollingFileTailer
{
    private File directory;
    private String baseFileName;
    private Schedule rolloverSchedule;
    private String dateTimeSuffix;
    private Date currentFileTime;

    private RollingFileTailer(File directory, String baseFileName, Schedule rolloverSchedule, String dateTimeSuffix)
    {
        this.directory = directory;
        this.baseFileName = baseFileName;
        this.rolloverSchedule = rolloverSchedule;
        this.dateTimeSuffix = dateTimeSuffix;
    }
}
