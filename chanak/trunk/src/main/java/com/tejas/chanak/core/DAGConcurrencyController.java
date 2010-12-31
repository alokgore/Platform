package com.tejas.chanak.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import com.tejas.core.TejasContext;

/**
 * Used to control concurrency of contract execution in the DAG-Manager. <br>
 * DAG-Manager runs the contracts in parallel. But some contracts need to make sure that the resource-usage should be controlled (in terms of how many people
 * can work on something concurrently) <br>
 * The resources that need to be concurrency-controlled should be registered with this class using the API {@link #registerResource(TejasContext, String, int)}
 * This class, as the linux file-system, supports advisory locking over the resource. Contracts that need to access the resource should override getResourceID()
 * method
 */
public class DAGConcurrencyController
{
    static ConcurrentMap<String, Semaphore> workPermits = new ConcurrentHashMap<String, Semaphore>();

    private static Semaphore getResourceController(String resourceID)
    {
        /*
         * Each resource, by default, is assumed NOT to support concurrency. People who think that they can do better than that, should call registerResource()
         * before the DAG starts.
         */
        workPermits.putIfAbsent(resourceID, new Semaphore(1));
        return workPermits.get(resourceID);
    }

    public static void acquireWorkPermit(TejasContext self, String resourceID) throws InterruptedException
    {
        if ((resourceID != null) && !resourceID.trim().equals(""))
        {
            self.logger.info("Acquiring work permit for [" + resourceID + "]");
            Semaphore semaphore = DAGConcurrencyController.getResourceController(resourceID);
            semaphore.acquire();
            self.logger.info("Acquired work permit for [" + resourceID + "]");
        }
    }

    /**
     * Register a resource on which you want the concurrency control.
     * 
     * @param resourceID
     *            Unique identifier for the resource. (Could be a hostname, db-link name etc)
     * @param numWorkPermits
     *            (Number of parallel executions allowed on this resource).
     */
    public static void registerResource(TejasContext self, String resourceID, int numWorkPermits)
    {
        self.logger.info("Registering resourse " + resourceID + " with numWorkPermits = " + numWorkPermits);
        workPermits.put(resourceID, new Semaphore(numWorkPermits));
    }

    public static void releaseWorkPermit(TejasContext self, String resourceID)
    {
        if ((resourceID != null) && !resourceID.trim().equals(""))
        {
            self.logger.info("Releasing work permit for [" + resourceID + "]");
            Semaphore semaphore = getResourceController(resourceID);
            semaphore.release();
            self.logger.info("Released work permit for [" + resourceID + "]");
        }
    }

}
