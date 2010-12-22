package com.tejas.dbl;

import static com.tejas.dbl.DatabaseEndpoint.EndpointType.READ_ONLY;
import static com.tejas.dbl.DatabaseEndpoint.EndpointType.READ_WRITE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.math.RandomUtils;

import com.tejas.core.TejasDBLayer;
import com.tejas.core.TejasLogger;
import com.tejas.dbl.DatabaseEndpoint.EndpointType;
import com.tejas.logging.TejasLog4jWrapper;
import com.tejas.utils.misc.Assert;


public class TejasDBLRegistry
{
    private static class FoxEndpointStore
    {
        private final ArrayList<DatabaseEndpoint> readOnlyEndpoints = new ArrayList<DatabaseEndpoint>();
        private DatabaseEndpoint readWriteEndpoint = null;

        public FoxEndpointStore()
        {
            // NO-OP
        }

        private void validateEndpoint(DatabaseEndpoint endPoint)
        {
            if ((endPoint.type == READ_WRITE) && (readWriteEndpoint != null))
            {
                throw new IllegalArgumentException("DBLayer already has a read-write endpoint " + readWriteEndpoint);
            }

            for (DatabaseEndpoint existingEndpoint : getAllEndpoints())
            {
                Assert.isTrue(endPoint.vendor == existingEndpoint.vendor, "Incompatible endpoints [" + existingEndpoint + "] and [" + endPoint + "]");
            }
        }

        Collection<DatabaseEndpoint> getAllEndpoints()
        {
            LinkedList<DatabaseEndpoint> endpoints = new LinkedList<DatabaseEndpoint>(readOnlyEndpoints);
            if (readWriteEndpoint != null)
            {
                endpoints.add(readWriteEndpoint);
            }

            return endpoints;
        }

        public void addEndpoint(DatabaseEndpoint endPoint)
        {
            validateEndpoint(endPoint);
            if (endPoint.type == EndpointType.READ_WRITE)
            {
                readWriteEndpoint = endPoint;
            }
            else
            {
                readOnlyEndpoints.add(endPoint);
            }
        }

        public DatabaseEndpoint getEndpointForRead()
        {
            DatabaseEndpoint readEndpoint = null;
            if (readOnlyEndpoints.size() == 0)
            {
                readEndpoint = readWriteEndpoint;
            }
            else
            {
                readEndpoint = readOnlyEndpoints.get(RandomUtils.nextInt(readOnlyEndpoints.size()));
            }
            if (readEndpoint == null)
            {
                throw new IllegalStateException("Could not find any suitable database endpoint.");
            }
            return readEndpoint;
        }

        public DatabaseEndpoint getEndpointForWrite()
        {
            if (readWriteEndpoint == null)
            {
                throw new IllegalStateException("No READ-WRITE database endpoint has been registered yet.");
            }
            return readWriteEndpoint;
        }

        public boolean hasWriteEndpoint()
        {
            return (readWriteEndpoint != null);
        }
    }

    static final TejasLogger logger = TejasLog4jWrapper.getLogger(TejasDBLRegistry.class);

    @SuppressWarnings("rawtypes")
    private static ConcurrentHashMap<Enum, FoxEndpointStore> endPoints = new ConcurrentHashMap<Enum, FoxEndpointStore>();
    @SuppressWarnings("rawtypes")
    private static Enum defaultEndpoint;

    @SuppressWarnings("rawtypes")
    private static synchronized final Enum getDefaultEndpoint()
    {
        return defaultEndpoint;
    }

    @SuppressWarnings("rawtypes")
    private static FoxEndpointStore getEndpointStore(Enum name)
    {
        FoxEndpointStore store = endPoints.get(name);
        Assert.notNull(store, "Endpoint by name '" + name + "' is not registered");
        return store;
    }

    @SuppressWarnings("rawtypes")
    private static synchronized final void setDefaultEndpoint(Enum defaultEndpoint)
    {
        Assert.isTrue(((TejasDBLRegistry.defaultEndpoint == null) || (TejasDBLRegistry.defaultEndpoint == defaultEndpoint)), "[" + TejasDBLRegistry.defaultEndpoint
                + "] has already been defined as the default endpoint on this JVM");
        TejasDBLRegistry.defaultEndpoint = defaultEndpoint;
    }

    /**
     * @return The most powerful Database Endpoint (i.e. read-write endpoint if exists, read-only otherwise) of the "default" type. (Assuming a default endpoint
     *         has been defined using {@link #registerEndpoint(DatabaseEndpoint, boolean)}). <br>
     *         Returns Null if no default endpoint has been defined
     */
    @SuppressWarnings("rawtypes")
    public static TejasDBLayer getDBLayer()
    {
        Enum endpointName = getDefaultEndpoint();
        return endpointName == null ? null : getDBLayer(endpointName, null);
    }

    /**
     * Tries to get the most powerful Database Endpoint possible (i.e. If there is a read-write endpoint available it returns that. Gives you a read-only
     * endpoint otherwise)
     */
    @SuppressWarnings("rawtypes")
    public static TejasDBLayer getDBLayer(Enum endpointName)
    {
        return getDBLayer(endpointName, null);
    }

    @SuppressWarnings("rawtypes")
    public static TejasDBLayer getDBLayer(Enum name, EndpointType type)
    {
        FoxEndpointStore store = getEndpointStore(name);
        DatabaseEndpoint endpoint = (((type == null) && store.hasWriteEndpoint()) || (type == READ_WRITE) ? store.getEndpointForWrite() : store.getEndpointForRead());
        return new TejasDBLayerImpl(endpoint, type == READ_ONLY);
    }

    public static synchronized void registerEndpoint(DatabaseEndpoint endPoint) throws Exception
    {
        registerEndpoint(endPoint, false);
    }

    public static synchronized void registerEndpoint(DatabaseEndpoint endPoint, boolean isDefaultEndpoint) throws Exception
    {
        logger.info("Registering endpoint [" + endPoint + "]");
        endPoints.putIfAbsent(endPoint.name, new FoxEndpointStore());
        FoxEndpointStore endpointStore = endPoints.get(endPoint.name);
        endpointStore.addEndpoint(endPoint);

        if (isDefaultEndpoint)
        {
            setDefaultEndpoint(endPoint.name);
        }
    }

    public static void shutdown()
    {
        logger.info("Shutting down the FoxDBLayerFactory");
        for (FoxEndpointStore store : endPoints.values())
        {
            for (DatabaseEndpoint endpoint : store.getAllEndpoints())
            {
                logger.info("Closing endpoint [" + endpoint + "]");
                endpoint.close();
            }
        }
        endPoints.clear();
    }

}
