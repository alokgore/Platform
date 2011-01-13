package com.tejas.utils;

import java.io.File;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.tejas.config.ApplicationConfig;
import com.tejas.utils.misc.FileTailer;
import com.tejas.utils.misc.FileTailer.DataListener;

/**
 * Furji test. One should verify things manually until this becomes better.
 */
public class FileTrailerTest
{
    @BeforeClass
    public static void setup()
    {
        ApplicationConfig.initialize(null, null, "platform", "platform-test");
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
    public void testFileTrailer() throws Exception
    {
        FileTailer tailer = new FileTailer(new File("/var/log/tejas/tejas.log"), new CallbackHook());

        tailer.start();

        for (int i = 0; i < 30; i++)
        {
            System.err.println("Main thread waiting");
            Thread.sleep(1000);
        }

        tailer.stop();
    }
}
