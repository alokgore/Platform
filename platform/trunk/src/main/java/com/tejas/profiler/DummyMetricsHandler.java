package com.tejas.profiler;

import com.tejas.core.TejasMetricsHandler;

public class DummyMetricsHandler implements TejasMetricsHandler
{

    public class DummyStopWatch implements StopWatch
    {
        @Override
        public void stop()
        {
            // Dummy
        }

        @Override
        public void newLap(String apiName)
        {
            // Dummy
        }
    }

    @Override
    public void recordLatency(String apiName, long timeInMillis)
    {
        // Dummy
    }

    @Override
    public void recordCount(String counterName)
    {
        // Dummy
    }

    @Override
    public void recordCount(String counterName, long countValue)
    {
        // Dummy
    }

    @Override
    public StopWatch startTick(String name)
    {
        return new DummyStopWatch();
    }

    @Override
    public StopWatch startTick(String name, boolean nestedModeOnly)
    {
        return new DummyStopWatch();
    }

    @Override
    public void recordAdditionalData(String key, String value)
    {
        // Dummy
    }

}
