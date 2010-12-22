package com.tejas.profiler;

import com.tejas.core.TejasMetricsHandler;
import com.tejas.utils.misc.Assert;

public class ProfilerMetricsHandler implements TejasMetricsHandler
{
    public static class ProfilerStopWatch implements StopWatch
    {
        private ProfilerScope innerScope;

        ProfilerStopWatch(String name, boolean nestedModeOnly)
        {
            Assert.notEmpty(name);
            this.innerScope = nestedModeOnly ? Profiler.startInNestedMode(name) : Profiler.start(name);
        }

        @Override
        public void stop()
        {
            if (this.innerScope != null)
            {
                this.innerScope.end();
                this.innerScope = null;
            }
        }

        @Override
        public void newLap(String name)
        {
            stop();
            this.innerScope = Profiler.start(name);
        }
    }

    @Override
    public void recordLatency(String apiName, long timeInMillis)
    {
        Profiler.recordLatency(apiName, timeInMillis);
    }

    @Override
    public void recordCount(String counterName)
    {
        Profiler.recordEvent(counterName);
    }

    @Override
    public void recordCount(String counterName, long countValue)
    {
        Profiler.recordEvent(counterName, countValue);
    }

    @Override
    public StopWatch startTick(String name)
    {
        return startTick(name, false);
    }

    @Override
    public StopWatch startTick(String name, boolean nestedMode)
    {
        return new ProfilerStopWatch(name, nestedMode);
    }

    @Override
    public void recordAdditionalData(String key, String value)
    {
        if (this.profilerScope != null)
        {
            this.profilerScope.insertAdditionalEntry(key, value);
        }
    }

    private ProfilerScope profilerScope;
}
