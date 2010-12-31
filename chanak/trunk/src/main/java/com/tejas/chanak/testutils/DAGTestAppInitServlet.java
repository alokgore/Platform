package com.tejas.chanak.testutils;

import static com.tejas.core.enums.DatabaseEndpoints.LOCAL_MYSQL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.tejas.chanak.core.DAGManager;
import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasContext;
import com.tejas.dbl.MySQLEndpoint;
import com.tejas.dbl.TejasDBLRegistry;

public class DAGTestAppInitServlet extends HttpServlet
{
    @Override
    public void init() throws ServletException
    {
        try
        {
            ApplicationConfig.initialize("", "test", "platform", "dag-test");
            
            TejasDBLRegistry.registerEndpoint(new MySQLEndpoint.Builder(LOCAL_MYSQL).withDatabaseName("platform").build(), true);
            
			DAGManager.init(new TejasContext());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }
    
    @Override
    public void destroy()
    {
        try
        {
            TejasContext self = new TejasContext();
            DAGManager.shutdown(self);
            
            TejasDBLRegistry.shutdown();
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
