package com.tejas.utils;

import static com.tejas.core.enums.DatabaseEndpoints.LOCAL_MYSQL;

import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;

import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasContext;
import com.tejas.dbl.MySQLEndpoint;
import com.tejas.dbl.TejasDBLRegistry;
import com.tejas.utils.io.FileTailer;
import com.tejas.utils.io.FileTailer.DataListener;
import com.tejas.utils.io.FileTailer.DatabaseMapper;

public class FileTailerTestBase
{
    public static class CallbackHook implements DataListener
    {
        @Override
        public void processNewData(TejasContext self, List<String> lines, long currentFilePosition)
        {
            System.err.println("currentFilePosition= " + currentFilePosition);
            for (String line : lines)
            {
                System.out.println(line);
            }
        }
    }

    public static class SilentCallbackHook implements DataListener
    {
        @Override
        public void processNewData(TejasContext self, List<String> lines, long currentFilePosition)
        {
            // Silence!
        }
    }

    @BeforeClass
    public static void setup() throws Exception
    {
        ApplicationConfig.initialize(null, null, "platform", "platform-test");
        TejasDBLRegistry.registerEndpoint(new MySQLEndpoint.Builder(LOCAL_MYSQL).withDatabaseName("platform").build(), true);
    }

    @Before
    public void cleanDatabase()
    {
        DatabaseMapper mapper = new TejasContext().dbl.getMybatisMapper(FileTailer.DatabaseMapper.class);
        mapper.clearAllData();
    }

}
