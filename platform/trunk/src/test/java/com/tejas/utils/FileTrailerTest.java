package com.tejas.utils;

import static com.tejas.core.enums.DatabaseEndpoints.LOCAL_MYSQL;

import java.io.File;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasContext;
import com.tejas.dbl.MySQLEndpoint;
import com.tejas.dbl.TejasDBLRegistry;
import com.tejas.utils.misc.Assert;
import com.tejas.utils.misc.FileTailer;
import com.tejas.utils.misc.FileTailer.DataListener;
import com.tejas.utils.misc.FileTailer.DatabaseMapper;
import com.tejas.utils.misc.FileTailer.FilePositionData;
import com.tejas.utils.misc.FileTailer.TailStatus;

/**
 * Furji test. One should verify things manually until this becomes better.
 */
public class FileTrailerTest
{
    @BeforeClass
    public static void setup() throws Exception
    {
        ApplicationConfig.initialize(null, null, "platform", "platform-test");
        TejasDBLRegistry.registerEndpoint(new MySQLEndpoint.Builder(LOCAL_MYSQL).withDatabaseName("platform").build(), true);
    }

    public static class CallbackHook implements DataListener
    {
        @Override
        public void processNewData(List<String> lines, long currentFilePosition)
        {
            System.err.println("currentFilePosition= " + currentFilePosition);
            for (String line : lines)
            {
                System.out.println(line);
            }
        }
    }

    @Test
    public void testDBMapper() throws Exception
    {
        TejasContext self = new TejasContext();
        DatabaseMapper mapper = self.dbl.getMybatisMapper(DatabaseMapper.class);

        mapper.createTable();
        String fileName = RandomStringUtils.randomAlphanumeric(10);
        mapper.insert(new FilePositionData(fileName, 0));

        mapper.update(new FilePositionData(fileName, 50));
        Assert.equals(50, mapper.readPosition(fileName));

        Assert.equals(TailStatus.InProgress, mapper.readStatus(fileName));

        mapper.markTailComplete(fileName);

        Assert.equals(TailStatus.Complete, mapper.readStatus(fileName));
    }

    @Test
    public void testFileTrailer() throws Exception
    {
        TejasContext self = new TejasContext();

        FileTailer passwdTailer = new FileTailer(self, new File("/etc/passwd"), new CallbackHook(), true);
        passwdTailer.start(self);
        Thread.sleep(1000);
        Assert.isFalse(passwdTailer.isActive());

        FileTailer tailer = new FileTailer(self, new File("/var/log/tejas/tejas.log"), new CallbackHook(), false);

        tailer.start(self);

        for (int i = 0; i < 10; i++)
        {
            System.err.println("Main thread waiting");
            Thread.sleep(1000);
        }

        tailer.stop(self);
    }
}
