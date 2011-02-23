package com.tejas.core;

import com.tejas.alarming.Log4jAlarmHandler;
import com.tejas.config.ApplicationConfig;
import com.tejas.logging.TejasLog4jWrapper;
import com.tejas.profiler.DummyMetricsHandler;
import com.tejas.profiler.ProfilerMetricsHandler;

public class TejasResourceFactory
{
    public static TejasLogger getLogger()
    {
        return TejasLog4jWrapper.getLogger(TejasResourceFactory.class);
    }

    public static TejasEventHandler getAlarmHandler()
    {
        return new Log4jAlarmHandler();
    }

    public static TejasMetricsHandler getMetricsHandler()
    {
        if (ApplicationConfig.findBoolean("tejas.metrics.dummy", true))
        {
            return new DummyMetricsHandler();
        }
        return new ProfilerMetricsHandler();
    }
}
