package com.tejas.core;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasContext.ExitStatus;
import com.tejas.core.TejasEventHandler.Severity;
import com.tejas.core.enums.TejasAlarms;
import com.tejas.utils.misc.StringUtils;

/**
 * Base class for all the background jobs. <br>
 * This class is preferred over
 * <ul>
 * <li><b> {@link Timer} : </b> Because is possible to schedule multiple {@link TimerTask} on a single timer object and Timer is known to stall when any one of
 * the {@link TimerTask} scheduled on it starts misbehaving</li>
 * <li><b> {@link Thread} : </b> Because it provides a clean shutdown method. (which, I agree, is not a big deal. But, 90% of the Threads that I have seen here
 * are not being shutdown cleanly. So .....)</li>
 * </ul>
 * <br>
 * In addition to that, this class integrates with Tejas, which means that
 * <ul>
 * <li>Alarms are raised on n (configurable) consecutive failures</li>
 * <li>Background job iterations are profiled by default</li>
 * </ul>
 * The background jobs iterates over the following steps indefinitely (Or until someone calls "signalShutdown()")
 * <ol>
 * <li>Take a nap for Configuration.napIntervalMillis milliseconds</li>
 * <li>Execute the code in "runIteration()" method of the {@link Task}</li>
 * </ol>
 * It is possible to cut the nap short for the current iteration by calling "expediteExecution()"
 * 
 * @author alokgore
 */
public class TejasBackgroundJob
{
    class Worker extends Thread
    {
        private int _exceptionCount;
        private final Configuration configuration;
        private final Task task;
        private ExceptionDetails lastSeenException;
        private Semaphore expeditionTrigger = new Semaphore(0);

        Worker(Configuration configuration, Task task)
        {
            this.configuration = configuration;
            this.task = task;
            try
            {
                this.task.init(new TejasContext());
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            this.setDaemon(true);
        }

        private void iterate(TejasContext self)
        {
            String exceptionMessage = "";
            try
            {
                self.entry(this.configuration.jobName.replace(' ', '_'));
                iterateInner();
                this._exceptionCount = -1;
            }
            catch (InterruptedException e)
            {
                self.logger.info(TejasBackgroundJob.this, "] got InterruptedException. Looks like we are going down");
            }
            catch (Throwable t)
            {
                exceptionMessage = StringUtils.serializeToString(t);
                setLastSeenException(t);
                self.logger.error(getName(), t, TejasBackgroundJob.this, " Got exception");
            }
            finally
            {
                this._exceptionCount = ((this._exceptionCount + 1) % (Integer.MAX_VALUE - 1));
                if (this._exceptionCount > this.configuration.exceptionThreshold)
                {
                    self.alarm(this.configuration.alarmSeverity, this.configuration.componentName, this.configuration.alarmName, this.configuration.deduplicationString,
                            "Name = ("
                                    + getName() + ")"
                                    + TejasBackgroundJob.this.getClass().getSimpleName() + exceptionMessage);
                }

                self.exit(this._exceptionCount == 0 ? ExitStatus.Success : ExitStatus.Failure);
            }
        }

        private void iterateInner() throws Exception
        {
            TejasContext self = new TejasContext();
            try
            {

                /*
                 * This call will make this thread sleep for napIntervalMillis if no-one calls "expediteExecution()" We take a nap before calling
                 * "runIteration()", so we don't go into a tight loop if "runIteration()" misbehaves.
                 */
                getExpeditionTrigger().tryAcquire(this.configuration.napIntervalMillis, TimeUnit.MILLISECONDS);

                this.task.runIteration(self, TejasBackgroundJob.this);
            }
            finally
            {
                /*
                 * Drain the expedition-request queue after executing the job, so, the expedition requests that came when the job was in progress do not disturb
                 * our nap again.
                 */
                setExpeditionTrigger(new Semaphore(0));

                setLastUpdated(System.currentTimeMillis());
            }
        }

        synchronized final void setLastSeenException(Throwable t)
        {
            this.lastSeenException = new ExceptionDetails(t);
        }

        public synchronized final void expediteExecution()
        {
            this.expeditionTrigger.release();
        }

        public final Semaphore getExpeditionTrigger()
        {
            return this.expeditionTrigger;
        }

        public final ExceptionDetails getLastSeenException()
        {
            return this.lastSeenException;
        }

        @Override
        public void run()
        {
            this.setName(this.configuration.jobName);
            TejasContext self = new TejasContext();
            self.logger.info("Starting background job [", this.configuration.jobName, "] wtih a napTime of [" + this.configuration.napIntervalMillis + "] millis");

            while (!isShuttingDown())
            {
                iterate(self.clone());
            }

            try
            {
                this.task.shutdown(self.clone());
            }
            catch (Exception e)
            {
                self.logger.warn("Task cleanup failed ", e);
            }

            setActive(false);
            setShuttingDown(false);
            self.logger.info("Job [", this.configuration.jobName, "] down.");
        }

        public final void setExpeditionTrigger(Semaphore expeditionTrigger)
        {
            this.expeditionTrigger = expeditionTrigger;
        }
    }

    @SuppressWarnings("rawtypes")
    public static class Configuration
    {
        public static class Builder
        {
            String jobName;
            long napIntervalMillis;
            int _exceptionThreshold = ApplicationConfig.findInteger("Tejas.backgroundjobs.exceptionThreshold", 3);
            Severity _alarmSeverity = Severity.valueOf(ApplicationConfig.findString("Tejas.backgroundjobs.alarmSeverity", Severity.FATAL.name()));
            Enum _alarmName = TejasAlarms.FOX_BACKGROUND_JOB_FAILED;
            Enum componentName;
            String _deduplicationString;

            public Builder(String jobName, Enum componentName, long napIntervalMillis)
            {
                this.jobName = jobName;
                this.componentName = componentName;
                this.napIntervalMillis = napIntervalMillis;
            }

            public Builder alarmName(Enum alarmName)
            {
                this._alarmName = alarmName;
                return this;
            }

            public Builder alarmSeverity(Severity alarmSeverity)
            {
                this._alarmSeverity = alarmSeverity;
                return this;
            }

            public Configuration build()
            {
                return new Configuration(this);
            }

            public Builder deduplicationString(String deduplicationString)
            {
                this._deduplicationString = deduplicationString;
                return this;
            }

            public Builder exceptionThreshold(int exceptionThreshold)
            {
                this._exceptionThreshold = exceptionThreshold;
                return this;
            }

        }

        public final String jobName;
        public final long napIntervalMillis;
        public final int exceptionThreshold;
        public final Severity alarmSeverity;
        public final Enum alarmName;
        public final Enum componentName;
        public final String deduplicationString;

        Configuration(Builder builder)
        {
            this.jobName = builder.jobName;
            this.napIntervalMillis = builder.napIntervalMillis;
            this.exceptionThreshold = builder._exceptionThreshold;
            this.alarmSeverity = builder._alarmSeverity;
            this.alarmName = builder._alarmName;
            this.componentName = builder.componentName;
            this.deduplicationString = builder._deduplicationString;
        }
    }

    public static class ExceptionDetails
    {
        public final Throwable exception;
        public final long exceptionTime;

        ExceptionDetails(Throwable exception)
        {
            this.exception = exception;
            this.exceptionTime = System.currentTimeMillis();
        }
    }

    public interface Task
    {
        /**
         * Called once in the lifecycle of the {@link TejasBackgroundJob}. Can be used to initialize resources required by
         * {@link #runIteration(TejasContext, TejasBackgroundJob)}
         * 
         * @throws Exception
         */
        void init(TejasContext self) throws Exception;

        /**
         * Called for each iteration with a reference to the parent {@link TejasBackgroundJob} and a new copy of {@link TejasContext})
         */
        void runIteration(TejasContext self, TejasBackgroundJob parent) throws Exception;

        /**
         * Called once during the shutdown. The {@link #runIteration(TejasContext, TejasBackgroundJob)} method is not supposed to behave after this method has
         * been called
         */
        void shutdown(TejasContext self) throws Exception;
    }

    public static abstract class AbstractTejasTask implements Task
    {
        @Override
        public void init(TejasContext self) throws Exception
        {
            // NOOP
        }

        @Override
        public void shutdown(TejasContext self)
        {
            // NOOP
        }
    }

    private static final int WORKER_SHUTDOWN_WAIT = ApplicationConfig.findInteger("Tejas.backgroundjobs.workerShutdownWait", 2000);

    private boolean shuttingDown = false;

    private boolean isActive = false;

    private final Worker worker;

    protected long lastUpdated;

    public TejasBackgroundJob(Task task, Configuration configuration)
    {
        this.worker = new Worker(configuration, task);
        setLastUpdated(System.currentTimeMillis());
    }

    synchronized void setActive(boolean isActive)
    {
        this.isActive = isActive;
    }

    synchronized void setLastUpdated(long lastUpdated)
    {
        this.lastUpdated = lastUpdated;
    }

    synchronized void setShuttingDown(boolean shuttingDown)
    {
        this.shuttingDown = shuttingDown;
    }

    /**
     * The TejasBackgroundJob takes a nap between two successive iterations. This methods cuts the nap short for the current iteration.
     */
    public synchronized final void expediteExecution()
    {
        this.worker.getExpeditionTrigger().release();
    }

    public final synchronized long getLastJobRunTime()
    {
        return this.lastUpdated;
    }

    /**
     * The return value of this function is a deterministic indicator of whether the job is actually finished. (Remember, that the signalShutdown() call is only
     * a shutdown hint to the job. The job is not finished until it acknowledges the signal and shuts itself down).
     */
    public final synchronized boolean isActive()
    {
        return this.isActive;
    }

    public final synchronized boolean isShuttingDown()
    {
        return this.shuttingDown;
    }

    /**
     * Signal the job "enough is enough! Die now !".
     * <p>
     * But, this method does not kill the job. The job continues with whatever it was doing. It gets the shutdown signal only after it completes the in-flight
     * iteration of "runIteration()" method (unless, of-course, the runIteration() method itself responds to the "interrupt()" signal and bails out, In which
     * case the job terminated immediately.
     */
    public final synchronized void signalShutdown()
    {
        setShuttingDown(true);
        this.worker.interrupt();
        try
        {
            /*
             * The reason to wait here, is to give the worker a chance to go down with dignity, before the JVM start killing people
             */
            this.worker.join(WORKER_SHUTDOWN_WAIT);
        }
        catch (InterruptedException interruptedException)
        {
            // Ignore
        }
    }

    public final synchronized void start()
    {
        if (isActive())
        {
            // start() should be a NOOP second time around
            return;
        }

        this.worker.start();
        setActive(true);
    }

    public synchronized long timeElapsedSinceLastJobRun()
    {
        long lastUpdationTimestamp = getLastJobRunTime();
        long now = System.currentTimeMillis();
        long timeElapsedSinceLastUpdate = now - lastUpdationTimestamp;
        return timeElapsedSinceLastUpdate;
    }

    @Override
    public final String toString()
    {
        return this.worker.toString();
    }
}
