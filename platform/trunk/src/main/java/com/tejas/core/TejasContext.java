package com.tejas.core;

import static java.lang.Character.MAX_RADIX;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;

import com.tejas.core.TejasEventHandler.Severity;
import com.tejas.core.TejasMetricsHandler.StopWatch;
import com.tejas.dbl.TejasDBLRegistry;
import com.tejas.utils.misc.Assert;

/**
 * The Context defines the domain (boundaries) within which a service operates. The context defines the DBLayer to use, the logger to log to, metrics client and
 * alarm handler. Every service call will take the context as an input and get a reference to the above mentioned resources from the context
 */
public final class TejasContext implements Cloneable
{
    private static class LatencyData
    {
        public String apiName;
        public long latency;

        public LatencyData(String apiName, long latency)
        {
            this.apiName = apiName;
            this.latency = latency;
        }
    }

    public static enum ExitStatus
    {
            Success,
            Failure;
    }

    private static String generateId(String prefix)
    {
        return (prefix + "_" + Long.toString(System.currentTimeMillis(), MAX_RADIX) + "_" + Long.toString(RandomUtils.nextInt(100000),
                MAX_RADIX)).toUpperCase();
    }

    /**
     * Failures for the API that are called asynchronously We can not use self.metrics to do that because ProfilerScope works on thread-locals
     */
    transient private List<String> _failureList = new ArrayList<String>();

    /**
     * Latencies for the API that are called asynchronously We can not use self.metrics to do that because ProfilerScope works on thread-locals
     */
    transient private List<LatencyData> _latencyData = new ArrayList<LatencyData>();

    private String _serviceName = null;

    /**
     * Global Request ID. This ID is passed across service calls
     */
    private final String grID;

    /**
     * Local Request ID. Scope of this ID is the server side code of a service call.
     */
    private final String lrID;

    private StopWatch stopWatch;

    private final TejasEventHandler alarm = TejasResourceFactory.getAlarmHandler();

    public final TejasLogger logger = TejasResourceFactory.getLogger();

    public final TejasMetricsHandler metrics = TejasResourceFactory.getMetricsHandler();

    public final TejasDBLayer dbl;

    public TejasContext()
    {
        this(TejasDBLRegistry.getDBLayer());
    }

    public TejasContext(TejasDBLayer dbl)
    {
        this.grID = generateId("G");
        this.lrID = generateId("L");

        this.dbl = dbl;

        this.logger.addContextInformation(this.grID);
        this.logger.addContextInformation(this.lrID);
    }

    private synchronized void flushMetricsData()
    {
        for (LatencyData latencyData : this._latencyData)
        {
            this.metrics.recordLatency(latencyData.apiName, latencyData.latency);
        }

        for (String api : this._failureList)
        {
            this.metrics.recordCount(api + "." + ExitStatus.Failure);
        }

        this._latencyData.clear();
        this._failureList.clear();
    }

    private synchronized String getServiceName()
    {
        return (this._serviceName == null ? "" : this._serviceName);
    }

    private synchronized void resetServiceName()
    {
        this._serviceName = "";
    }

    private synchronized void setServiceName(String name)
    {
        Assert.notEmpty(name, "Can not use null or empty as service-name");
        this._serviceName = name.trim();
    }

    @SuppressWarnings("rawtypes")
    public void alarm(Severity severity, Enum componentName, Enum alarmName, String deduplicationString, String description)
    {
        this.alarm.alarm(severity, componentName, alarmName, deduplicationString, description);
    }

    @Override
    public TejasContext clone()
    {
        return new TejasContext();
    }

    /**
     * Should be called ONCE as soon as the request enters a service call on a particular component. If the TejasContext is being used in a different Context
     * (e.g. a background job) this method should be called as soon
     */
    public synchronized TejasContext entry(String serviceName)
    {
        Assert.empty(getServiceName(), "A service [" + getServiceName()
                + "] has already been started for this context. This call is not re-entrant.");
        Assert.notEmpty(serviceName, "Service name not specified");

        setServiceName(serviceName);

        this.stopWatch = this.metrics.startTick(serviceName);
        this.metrics.recordAdditionalData("FoxGRID", this.grID);
        this.metrics.recordAdditionalData("FoxLRID", this.lrID);

        this.logger.trace("Request entered");

        return this;
    }

    /**
     * Should be called ONCE just before returning back the response for a service call on a particular component. If the TejasContext is being used in a
     * different context (e.g. A Background Job), exit() should be called after each iteration of the Job.
     * 
     * @param status
     *            Exit status of the request.
     */
    public synchronized void exit(ExitStatus status)
    {
        Assert.notEmpty(getServiceName(), "ServiceName should not have been empty here");

        this.metrics.recordCount(getServiceName() + "." + status);
        flushMetricsData();

        Assert.notNull(this.stopWatch, "'stopWatch' is null. Someone forgot to call TejasContext.entry()");
        this.stopWatch.stop();

        switch (status)
        {
            case Success:
                this.logger.trace(getServiceName() + " successful!");
                break;

            case Failure:
                this.logger.error(getServiceName() + " failed!");
                break;
        }

        resetServiceName();
    }

    /**
     * @deprecated : Will be replaced by an API on {@link TejasMetricsHandler} soon. Interface to be used by the calls that are happening in a thread parallel
     *             to the main thread (main thread is the one that created the TejasContext and ProfilerScope object). This is required because ProfilerScope
     *             works on ThreadLocal objects and can not handle metrics generation across different threads. TejasContext just caches the latency-data when
     *             this API is called. {@link #exit(ExitStatus)} method flushes this data to the main-stream.
     */
    @Deprecated
    public synchronized void recordFailureAsync(String apiName)
    {
        this._failureList.add(apiName);
    }

    /**
     * @deprecated : Will be replaced by an API on {@link TejasMetricsHandler} soon. Interface to be used by the calls that are happening in a thread parallel
     *             to the main thread (main thread is the one that created the TejasContext and ProfilerScope object). This is required because ProfilerScope
     *             works on ThreadLocal objects and can not handle metrics generation across different threads. TejasContext just caches the latency-data when
     *             this API is called. {@link #exit(ExitStatus)} method flushes this data to the main-stream.
     */
    @Deprecated
    public synchronized void recordLatencyAsync(String apiName, long latency)
    {
        this._latencyData.add(new LatencyData(apiName, latency));
    }
}
