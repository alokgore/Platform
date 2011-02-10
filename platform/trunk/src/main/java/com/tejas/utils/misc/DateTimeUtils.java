package com.tejas.utils.misc;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeUtils
{
    public enum Schedule
    {
            EveryMinute("5 * * * * ?", 60 * 1000L, Calendar.MINUTE), // 5th second of every minute
            Hourly("0 5 * * * ?", 60 * 60 * 1000L, Calendar.HOUR_OF_DAY), // 5th minute of every hour
            Daily("0 30 1 * * ?", 24 * 60 * 60 * 1000L, Calendar.DAY_OF_MONTH), // every day @ 1:30 AM
            Weekly("0 0 5 ? * MON", 7 * 24 * 60 * 60 * 1000L, Calendar.WEEK_OF_YEAR), // 5:00 AM on every Monday
            Monthly("0 0 7 1 * ?", 28 * 24 * 60 * 60 * 1000L, Calendar.MONTH), // 7:00 AM on 1st of every month
        ;

        private final int calendarUnit;
        private final String cronExpression;
        private final long interval;

        private Schedule(String cronExpression, long interval, int calendarUnit)
        {
            this.cronExpression = cronExpression;
            this.interval = interval;
            this.calendarUnit = calendarUnit;
        }

        public int getCalendarUnit()
        {
            return this.calendarUnit;
        }

        public String getDefaultCronExpression()
        {
            return this.cronExpression;
        }

        public long getIntervalInMillis()
        {
            return this.interval;
        }
    }

    /**
     * Adds/Subtracts delta number of {@link Schedule} units from the date <br>
     */
    public static Date addScheduleUnits(Date date, Schedule schedule, int numUnits)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(schedule.getCalendarUnit(), numUnits);
        return cal.getTime();
    }

    /**
     * @see #getNormalizedTimeInTZ(Timestamp, Schedule, TimeZone)
     */
    public static Timestamp getNormalizedTime(Timestamp time, Schedule schedule)
    {
        return getNormalizedTimeInTZ(time, schedule, TimeZone.getDefault());
    }

    /**
     * Normalization of date means doing a "floor()" on the date based on schedule, e.g.
     * <ul>
     * <li>If the schedule is {@link Schedule#Hourly}, this operation would set minutes, seconds and milliseconds part to zero</li>
     * <li>If the schedule is {@link Schedule#Monthly}, this operation would set date=1, (hour, minute, seconds and milliseconds) = 0</li>
     */
    public static Timestamp getNormalizedTimeInTZ(Timestamp time, Schedule schedule, TimeZone timeZone)
    {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTime(time);
        switch (schedule)
        {
            case Monthly:
            case Weekly:
                if (schedule == Schedule.Monthly)
                {
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                }
                else if (schedule == Schedule.Weekly)
                {
                    calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                }
                //$FALL-THROUGH$
            case Daily:
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                //$FALL-THROUGH$
            case Hourly:
                calendar.set(Calendar.MINUTE, 0);
                //$FALL-THROUGH$
            default:
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
        }
        return new Timestamp(calendar.getTime().getTime());
    }

    public static Timestamp getPreviousTime(Timestamp time, Schedule schedule)
    {
        return getTimeInTZ(time, schedule, TimeZone.getDefault(), -1);
    }

    public static Timestamp getNextTime(Timestamp time, Schedule schedule)
    {
        return getTimeInTZ(time, schedule, TimeZone.getDefault(), +1);
    }

    private static Timestamp getTimeInTZ(Timestamp time, Schedule schedule, TimeZone timeZone, int sign)
    {
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTime(time);
        switch (schedule)
        {
            case Monthly:
                cal.add(Calendar.MONTH, sign * 1);
                break;

            case Weekly:
                cal.add(Calendar.DATE, sign * 7);
                break;

            case Daily:
                cal.add(Calendar.DATE, sign * 1);
                break;

            case Hourly:
                cal.add(Calendar.HOUR, sign * 1);
                break;

            case EveryMinute:
                cal.add(Calendar.MINUTE, sign * 1);
                break;
        }
        return new Timestamp(cal.getTime().getTime());
    }

    public static Timestamp convertTime(Timestamp time, TimeZone sourceTZ, TimeZone destinationTZ)
    {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        fmt.setTimeZone(destinationTZ);
        String dateStr = fmt.format(time);
        fmt.setTimeZone(sourceTZ);
        try
        {
            Timestamp convertedTimestamp = new Timestamp(fmt.parse(dateStr).getTime());
            return convertedTimestamp;
        }
        catch (ParseException e)
        {
            throw new RuntimeException("Error parsing date string: " + dateStr, e);
        }
    }
}
