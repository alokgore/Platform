package com.tejas.chanak.test;

import static com.tejas.core.enums.DatabaseEndpoints.LOCAL_MYSQL;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.tejas.chanak.core.DAGManager;
import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasContext;
import com.tejas.dbl.MySQLEndpoint;
import com.tejas.dbl.TejasDBLRegistry;

public abstract class DAGManagerTestCaseBase
{
    @AfterClass
    public static void tearDown() throws Exception
    {
        DAGManager.shutdown(new TejasContext());
        TejasDBLRegistry.shutdown();
    }

    @BeforeClass
    public static void setUp() throws Exception
    {
        ApplicationConfig.initialize("", "", "platform", "platform");
        
        TejasDBLRegistry.registerEndpoint(new MySQLEndpoint.Builder(LOCAL_MYSQL).withDatabaseName("platform").build(), true);
        
        DAGManager.init(new TejasContext());
    }
}
