package com.tejas.core;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasContext.ExitStatus;
import com.tejas.core.TejasEventHandler.Severity;
import com.tejas.dbl.types.TejasAlarms;
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
 * In addition to that, this class integrates with Fox, which means that
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
public final class TejasBackgroundJob
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
        }

        private void iterate(TejasContext self)
        {
            String exceptionMessage = "";
            try
            {
                self.entry(configuration.jobName.replace(' ', '_'));
                iterateInner();
                _exceptionCount = -1;
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
                _exceptionCount = ((_exceptionCount + 1) % (Integer.MAX_VALUE - 1));
                if (_exceptionCount > configuration.exceptionThreshold)
                {
                    self.alarm(configuration.alarmSeverity, configuration.componentName, configuration.alarmName, configuration.deduplicationString, "Name = ("
                            + getName() + ")"
                            + TejasBackgroundJob.this.getClass().getSimpleName() + exceptionMessage);
                }

                self.exit(_exceptionCount == 0 ? ExitStatus.Success : ExitStatus.Failure);
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
                getExpeditionTrigger().tryAcquire(configuration.napIntervalMillis, TimeUnit.MILLISECONDS);

                task.runIteration(self);
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
            expeditionTrigger.release();
        }

        public final Semaphore getExpeditionTrigger()
        {
            return expeditionTrigger;
        }

        public final ExceptionDetails getLastSeenException()
        {
            return lastSeenException;
        }

        @Override
        public void run()
        {
            this.setName(configuration.jobName);
            this.setDaemon(true);
            TejasContext self = new TejasContext();
            self.logger.info("Starting background job [", configuration.jobName, "] wtih a napTime of [" + configuration.napIntervalMillis + "] millis");
            while (!isShuttingDown())
            {
                iterate(self.clone());
            }
            setActive(false);
            setShuttingDown(false);
            self.logger.info("Job [", configuration.jobName, "] down.");
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
            int _exceptionThreshold = ApplicationConfig.findInteger("Fox.backgroundjobs.exceptionThreshold", 3);
            Severity _alarmSeverity = Severity.valueOf(ApplicationConfig.findString("Fox.backgroundjobs.exceptionThreshold", Severity.FATAL.name()));
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
        void runIteration(TejasContext self) throws Exception;
    }

    private static final int WORKER_SHUTDOWN_WAIT = ApplicationConfig.findInteger("Fox.backgroundjobs.workerShutdownWait", 2000);

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
        worker.getExpeditionTrigger().release();
    }

    public final synchronized long getLastJobRunTime()
    {
        return lastUpdated;
    }

    /**
     * The return value of this function is a deterministic indicator of whether the job is actually finished. (Remember, that the signalShutdown() call is only
     * a shutdown hint to the job. The job is not finished until it acknowledges the signal and shuts itself down).
     */
    public final synchronized boolean isActive()
    {
        return isActive;
    }

    public final synchronized boolean isShuttingDown()
    {
        return shuttingDown;
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
        worker.interrupt();
        try
        {
            /*
             * The reason to wait here, is to give the worker a chance to go down with dignity, before the JVM start killing people
             */
            worker.join(WORKER_SHUTDOWN_WAIT);
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

        worker.start();
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
        return worker.toString();
    }
}
