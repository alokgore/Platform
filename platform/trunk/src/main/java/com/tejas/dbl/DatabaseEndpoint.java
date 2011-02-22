package com.tejas.dbl;

import java.math.BigInteger;
import java.sql.Driver;
import java.sql.DriverManager;
import javax.sql.DataSource;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.tejas.config.ApplicationConfig;
import com.tejas.dbl.types.BigIntegerTypeHandler;

public class DatabaseEndpoint
{
    public enum DatabaseVendor
    {
            HSQLDB,
            MySQL,
            SQLite,
            POSTGRES;
    }

    public static class EndPointDefinition
    {
        public final Driver driver;
        public final String jdbcURL;
        public final String password;
        public final String username;

        public EndPointDefinition(String jdbcURL, Driver driver)
        {
            this(jdbcURL, driver, null, null);
        }

        public EndPointDefinition(String jdbcURL, Driver driver, String username, String password)
        {
            this.jdbcURL = jdbcURL;
            this.driver = driver;
            this.username = username;
            this.password = password;
        }
    }

    public enum EndpointType
    {
            READ_ONLY("RO"),
            READ_WRITE("RW");

        private String name;

        private EndpointType(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    private final TejasDataSource dataSource;
    @SuppressWarnings("rawtypes")
    public final Enum name;
    public final SqlSessionFactory sqlSessionFactory;

    public final EndpointType type;
    public final DatabaseVendor vendor;

    protected DatabaseEndpoint(@SuppressWarnings("rawtypes") Enum name, EndpointType type, DatabaseVendor vendor, EndPointDefinition endPointDefinition) throws Exception
    {
        this.name = name;
        this.type = type;
        this.vendor = vendor;
        dataSource = initialize(endPointDefinition);

        Configuration configuration = new Configuration(new Environment(name.name(), new JdbcTransactionFactory(), dataSource));
        // XXX: Document all of the settings below and make them configurable
        configuration.setAutoMappingBehavior(AutoMappingBehavior.FULL);
        configuration.setDefaultExecutorType(ExecutorType.SIMPLE);
        configuration.setCacheEnabled(false);

        configuration.getTypeHandlerRegistry().register(BigInteger.class, new BigIntegerTypeHandler());

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    private void addPropertyDefault(ComboPooledDataSource ds)
    {
        ds.setIdleConnectionTestPeriod(600);
        ds.setMaxIdleTime(7200);
        ds.setInitialPoolSize(getPropertyValue("dblayer.pool.initialSize", 40));
        ds.setMaxPoolSize(getPropertyValue("dblayer.pool.maxSize", 40));
        ds.setPreferredTestQuery("select 1");
        ds.setCheckoutTimeout(getPropertyValue("dblayer.pool.checkoutTimeout", 5000));
    }

    private int getPropertyValue(final String primaryKey, int defaultValue)
    {
        final String secondaryKey = primaryKey + name + "." + type;
        final Integer propertyValue = ApplicationConfig.findInteger(primaryKey);
        if (propertyValue != null)
        {
            return propertyValue.intValue();
        }
        return ApplicationConfig.findInteger(secondaryKey, defaultValue);
    }

    private TejasDataSource initialize(EndPointDefinition endPointDefinition) throws Exception
    {
        DriverManager.registerDriver(endPointDefinition.driver);

        ComboPooledDataSource comboPooledDataSource = new ComboPooledDataSource();
        addPropertyDefault(comboPooledDataSource);
        comboPooledDataSource.setDriverClass(endPointDefinition.driver.getClass().getCanonicalName());
        comboPooledDataSource.setJdbcUrl(endPointDefinition.jdbcURL);

        if (endPointDefinition.username != null)
        {
            comboPooledDataSource.setUser(endPointDefinition.username);
        }

        if (endPointDefinition.password != null)
        {
            comboPooledDataSource.setPassword(endPointDefinition.password);
        }

        comboPooledDataSource.softResetDefaultUser();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run()
            {
                DatabaseEndpoint.this.close();
            }
        });
        return new TejasDataSource(comboPooledDataSource);
    }

    public synchronized <T> void addMapper(Class<T> mapper)
    {
        Configuration configuration = sqlSessionFactory.getConfiguration();
        if (configuration.hasMapper(mapper) == false)
        {
            configuration.addMapper(mapper);
        }
    }

    public void close()
    {
        try
        {
            dataSource.close();
        }
        catch (Exception e)
        {
            // Ignore
        }
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public String toString()
    {
        return ReflectionToStringBuilder.toString(this);
    }

}
