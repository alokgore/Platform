package com.tejas.chanak.scheduler;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasBackgroundJob;
import com.tejas.core.TejasContext;

public class DAGScheduler
{
    private static final int CORE_POOL_SIZE = ApplicationConfig.findInteger("dagmanager.scheduler.corePoolSize", 10);
    private static final int KEEP_ALIVE = ApplicationConfig.findInteger("dagmanager.scheduler.poolKeepAlive", 0);
    private static final int MAX_POOL_SIZE = ApplicationConfig.findInteger("dagmanager.scheduler.maxPoolSize", 20);
    private static final int WOB_QUEUE_SIZE = ApplicationConfig.findInteger("dagmanager.scheduler.queueSize", 1000);
    
    static long threadCount = 0;
    private ContractEnqueuer contractEnqueuer;
    private TejasBackgroundJob dagScavenger;
    private ThreadPoolExecutor threadPool;
    private LinkedBlockingDeque<Runnable> workQueue = new LinkedBlockingDeque<Runnable>(WOB_QUEUE_SIZE);
    
    public DAGScheduler(TejasContext self)
    {
        self.logger.info("Starting the DAGScheduler");
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable command)
            {
                Thread thread = new Thread(command);
                thread.setName("DAG-Scheduler-" + ApplicationConfig.getApplicationName() + "-" + (threadCount++));
                thread.setDaemon(true);
                return thread;
            }
        };
        
        threadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE, SECONDS, workQueue, threadFactory, new CallerRunsPolicy());
        contractEnqueuer = new ContractEnqueuer(this);
        dagScavenger = new DAGScavenger();
    }
    
    public void scheduleTask(Runnable task)
    {
        threadPool.execute(task);
    }
    
    public void signalShutDown()
    {
        dagScavenger.signalShutdown();
        contractEnqueuer.signalShutdown();
        threadPool.shutdownNow();
    }
    
    public void start()
    {
        contractEnqueuer.start();
        dagScavenger.start();
    }
    
}
