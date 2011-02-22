package com.tejas.utils;

import java.io.File;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import com.tejas.core.TejasContext;
import com.tejas.utils.io.FileTailer;
import com.tejas.utils.io.FileTailer.DatabaseMapper;
import com.tejas.utils.io.FileTailer.FilePositionData;
import com.tejas.utils.io.FileTailer.TailStatus;
import com.tejas.utils.misc.Assert;
import com.tejas.utils.misc.StringUtils;

/**
 * Furji test. One should verify things manually until this becomes better.
 */
public class FileTailerTest extends FileTailerTestBase
{
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

        System.out.println(mapper.selectAllData());
    }

    @Test
    public void testStaticFile() throws Exception
    {
        TejasContext self = new TejasContext();
        FileTailer tailer = new FileTailer(self, new File("/etc/passwd"), new FileTailerTestBase.CallbackHook(), true);
        tailer.start();
        Thread.sleep(1000);
        Assert.isFalse(tailer.isActive());
    }

    @Test
    public void testGrowingFile() throws InterruptedException
    {
        TejasContext self = new TejasContext();
        FileTailer tailer = new FileTailer(self, new File("/var/log/tejas/tejas.log"), new FileTailerTestBase.CallbackHook(), false);
        tailer.start();
        System.out.println("Main thread taking a 5 second nap");
        tailer.join(5000);
        tailer.stop(self);
    }

    @Test
    public void testBigFile() throws Exception
    {
        TejasContext self = new TejasContext();

        File file = new File("/tmp/alok/big_file.txt");
        FileTailer tailer = new FileTailer(self, file, new FileTailerTestBase.SilentCallbackHook(), true);

        long start = System.currentTimeMillis();
        tailer.start();
        Assert.isTrue(tailer.isActive());

        System.err.println("Waiting for the tailer to complete");
        tailer.join(0);
        System.err.println("Tailer done! Took " + StringUtils.millisToPrintableString(System.currentTimeMillis() - start) + " for file of length "
                + ((float) file.length()) / (1024 * 1024) + " MB");
    }
}
