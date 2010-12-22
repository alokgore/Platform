package com.tejas.core;

/**
 * A thin wrapper class for collecting metrics.
 */
public interface TejasMetricsHandler
{
    /**
     * StopWatch ;-)
     * <p>
     * 
     * Holds just one timer <br>
     * It is assumed that the timer is started as soon as the StopWatch gets
     * created <br>
     * StopWatch has just two APIs
     * <li> {@link #stop()} that finishes the timer</li><br>
     * <li> {@link #split()} splits the timer, essentially stops the previous
     * timer, starts a new timer for the API name provided.
     */
    public static interface StopWatch
    {
        void stop();

        /**
         * Starts a new timer, stopping the previous timer if it was running.
         */
        void newLap(String apiName);
    }

    public void recordLatency(String apiName, long timeInMillis);

    public void recordCount(String counterName);

    public void recordCount(String counterName, long countValue);

    /**
     * Creates a {@link StopWatch} by the given name
     */
    public StopWatch startTick(String name);

    /**
     * Creates a {@link StopWatch} by the given name
     * 
     * @param name
     * @param nestedModeOnly
     *            If this is set to true, the StopWatch is created only if there
     *            is an existing enclosing StopWatch. The reason one would want
     *            to create a StopWatch conditionally is to avoid pumping out
     *            too much data
     * @return
     */
    public StopWatch startTick(String name, boolean nestedModeOnly);

    public void recordAdditionalData(String key, String value);

}
