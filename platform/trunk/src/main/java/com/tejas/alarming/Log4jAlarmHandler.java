package com.tejas.alarming;

import org.apache.log4j.Logger;

import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasEventHandler;

public class Log4jAlarmHandler implements TejasEventHandler
{
    Logger logger = Logger.getLogger(Log4jAlarmHandler.class);

    @SuppressWarnings("rawtypes")
    @Override
    public void alarm(Severity severity, Enum component, Enum alarm, String deduplicationString, String description)
    {
        logger.info("Alarm:" + severity + ":" + ApplicationConfig.getApplicationName() + ":" + component.name() + ":" + alarm.name() + deduplicationString + ":" + description);
    }

}
