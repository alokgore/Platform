package com.tejas.config;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

public class ApplicationConfig
{
    private static ConfigTree myTree = null;

    public static String getApplicationName()
    {
        return instance().getApplicationName();
    }

    public static String getApplicationGroup()
    {
        return instance().getApplicationGroup();
    }

    public static String getDomain()
    {
        return instance().getDomain();
    }

    public static boolean isProduction()
    {
        return instance().isProduction();
    }

    public static String dumpConfig()
    {
        return instance().dumpConfig();
    }

    public static String dumpConfig(String prefix)
    {
        return instance().dumpConfig(prefix);
    }

    public static String findString(String key)
    {
        return instance().findString(key);
    }

    public static String findString(String key, String defaultValue)
    {
        String value = findString(key);
        if (value == null)
        {
            insertString(key, defaultValue);
            value = defaultValue;
        }
        return value;
    }

    /**
     * @return A map where values can be of type (String, List, Map). The thing to be noted here is that the config values are not decoded to their primitive
     *         types (int, boolean, double) in the map.
     */
    public static Map<String, Object> findMap(String key)
    {
        return instance().findMap(key);
    }

    /**
     * @return A list consisting of objects of type (String, List, Map). The thing to be noted here is that the config values are not decoded to their primitive
     *         types (int, boolean, double) in the list.
     */
    public static List<Object> findList(String key)
    {
        return instance().findList(key);
    }

    public static Object findObject(String key)
    {
        return instance().findObject(key);
    }

    /**
     * Construct Map of configuration values from multiple values that match a key prefix. Prefix should consist of entire segments, segments are separated by
     * periods. For the following example config:
     * 
     * <pre>
     * *.foo.a = &quot;a&quot;;
     * *.foo.b = &quot;b&quot;;
     * *.foo.c.d = &quot;c.d&quot;;
     * *.foo.c.e = &quot;c.e&quot;;
     * </pre>
     * 
     * call <code> findObjectsByPrefix("foo") </code> returns the following map: <code> {foo.a="a", foo.b="b", foo.c.d="c.d", foo.c.e="c.e"} </code> call
     * <code>findObjectByPrefix("fo")</code> returns empty map as "fo" doesn't match entire segment
     * 
     * @param keyPrefix
     *            the prefix of the key values to lookup. Should consist of entire segments.
     * @return Map with configuration values as map values and config keys as map keys.
     */
    public static Map<String, Object> findObjectsByPrefix(String keyPrefix)
    {
        return instance().findObjectsByPrefix(keyPrefix);
    }

    public static Properties findPropertiesByPrefix(String keyPrefix)
    {
        Map<String, Object> map = instance().findObjectsByPrefix(keyPrefix);
        Properties prop = new Properties();
        for (Entry<String, Object> entry : map.entrySet())
        {
            prop.put(entry.getKey(), entry.getValue().toString());
        }
        return prop;
    }

    public static Integer findInteger(String key)
    {
        return instance().findInteger(key);
    }

    public static int findInteger(String key, int defaultVal)
    {
        Integer val = instance().findInteger(key);
        if (val != null)
        {
            return val.intValue();
        }

        insertInteger(key, defaultVal);
        return defaultVal;
    }

    public static Double findDouble(String key)
    {
        return instance().findDouble(key);
    }

    public static double findDouble(String key, double defaultVal)
    {
        Double val = instance().findDouble(key);
        if (val != null)
        {
            return val.doubleValue();
        }

        insertDouble(key, defaultVal);
        return defaultVal;
    }

    public static Boolean findBoolean(String key)
    {
        return instance().findBoolean(key);
    }

    public static boolean findBoolean(String key, boolean defaultVal)
    {
        Boolean val = instance().findBoolean(key);
        if (val != null)
        {
            return val.booleanValue();
        }

        insertBoolean(key, defaultVal);
        return defaultVal;
    }

    public static void insertString(String key, String value)
    {
        instance().insertString(key, value);
    }

    public static void insertMap(String key, Map<String, Object> value)
    {
        instance().insertMap(key, value);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void insertList(String key, List value)
    {
        instance().insertList(key, value);
    }

    public static void insertInteger(String key, int value)
    {
        instance().insertInteger(key, value);
    }

    public static void insertDouble(String key, double value)
    {
        instance().insertDouble(key, value);
    }

    public static void insertBoolean(String key, boolean value)
    {
        instance().insertBoolean(key, value);
    }

    private static ConfigTree instance()
    {
        if (myTree != null)
        {
            return myTree;
        }
        String error = "ApplicationConfig not initialized yet";
        System.err.println(error);
        throw new IllegalStateException(error);
    }

    public static boolean isInitialized()
    {
        return (myTree != null);
    }

    /**
     * Initialize the config tree.
     * 
     * @param configRoot
     *            File system root of the configuration tree.
     * @param domainName
     *            Domain name for this ApplicationConfig initialization
     * @param applicationName
     *            the name of the logical running application
     * @param applicationGroup
     *            the name of the application's appgroup
     * @param overrideFiles
     *            List of override files to be picked up by the application config
     */
    public static void initialize(String configRoot, String domainName, String applicationGroup, String applicationName, String... overrideFiles)
    {
        synchronized (ApplicationConfig.class)
        {
            if (myTree == null)
            {
                myTree = new ConfigTree(configRoot, domainName, applicationGroup, applicationName, overrideFiles);
            }
            else
            {
                throw new ConfigSyntaxException("ConfigTree is already initialized");
            }

            System.out.println("******************** Initialized ApplicationConfig ********************");
            System.out.println("ApplicationConfig : Application Name = " + getApplicationName());
            System.out.println("ApplicationConfig : Application Group = " + getApplicationGroup());
            System.out.println("ApplicationConfig : Domain = " + getDomain());
            System.out.println("ApplicationConfig : Override Files = " + instance().getOverrideFileNames());
            System.out.println("********************************************************************\n\n");

            configureLog4j();

        }
    }

    private static void configureLog4j()
    {
        Properties log4jProps = findPropertiesByPrefix("log4j");
        PropertyConfigurator.configure(log4jProps);
    }

    public static synchronized void destroy()
    {
        myTree = null;
    }

    public static void loadEntriesFromFile(String path)
    {
        myTree.loadEntriesFromFile(path, true);
    }

    public static String getWebappName()
    {
        // FIXME: Distinguish web-app from application
        return getApplicationName();
    }
}
