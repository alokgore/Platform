package com.tejas.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import com.tejas.core.TejasContext;
import com.tejas.utils.io.RollingFileTailer;
import com.tejas.utils.io.RollingFileTailer.Slot;
import com.tejas.utils.misc.Assert;

public class RollingFileTailerTest extends FileTailerTestBase
{
    /**
     * Start from a {Tnow - 7 hours} and stop @ {Tnow - 2 hours}
     */
    @Test
    public void testSimpleLogReplay() throws Exception
    {
        File tmpDir = createTmpDir();
        String baseFileName = "test.log";

        TejasContext self = new TejasContext();

        RollingFileTailer tailer = new RollingFileTailer
                    .Builder(tmpDir, baseFileName, new SilentCallbackHook())
                            .sessionEndTime(new Date(System.currentTimeMillis() - 2 * 3600 * 1000L))
                            .sessionStartTime(new Date(System.currentTimeMillis() - 7 * 3600 * 1000L))
                            .build(self);

        createFiles(tailer, 15);

        tailer.start();
        tailer.join(0);
    }

    /**
     * Start from a {Tnow - 7 hours} and stop @ {Tnow - 2 hours}. A few files in the middle can be missing
     */
    @Test
    public void testMissingFiles() throws Exception
    {
        File tmpDir = createTmpDir();
        String baseFileName = "test.log";

        TejasContext self = new TejasContext();

        RollingFileTailer tailer = new RollingFileTailer
                    .Builder(tmpDir, baseFileName, new SilentCallbackHook())
                            .sessionEndTime(new Date(System.currentTimeMillis() - 2 * 3600 * 1000L))
                            .sessionStartTime(new Date(System.currentTimeMillis() - 7 * 3600 * 1000L))
                            .build(self);

        createFiles(tailer, 20);

        Slot slot = tailer.getCurrentSlot();
        for (int i = 0; i < 5; i++)
        {
            Assert.isTrue(slot.getFile().exists() && slot.getFile().delete());

            slot = slot.getNextSlot().getNextSlot();
        }

        tailer.start();
        tailer.join(0);
    }

    public List<Slot> createFiles(RollingFileTailer tailer, int numFiles) throws IOException
    {
        List<Slot> response = new ArrayList<RollingFileTailer.Slot>();
        Slot slot = tailer.new Slot(tailer.getSessionStartTime()).getPreviousSlot().getPreviousSlot().getPreviousSlot();
        for (int i = 0; i < numFiles; i++)
        {
            File file = slot.getFile();
            System.out.println("Creating file [" + file.getAbsolutePath() + "]");
            Collection<String> lines = new ArrayList<String>();
            for (int j = 0; j < 10; j++)
            {
                lines.add(RandomStringUtils.randomAlphanumeric(50));
            }
            FileUtils.writeLines(file, lines);

            response.add(slot);

            slot = slot.getNextSlot();
        }

        return response;
    }

    public File createTmpDir() throws IOException
    {
        File tmpDir = new File("/tmp/tejas/file_tailer");
        tmpDir.mkdirs();
        Assert.isTrue(tmpDir.exists() && tmpDir.isDirectory());
        FileUtils.cleanDirectory(tmpDir);
        return tmpDir;
    }
}
