package com.tejas.utils.misc;

import static com.tejas.core.enums.PlatformComponents.PLATFORM_UTIL_LIB;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import com.tejas.core.TejasBackgroundJob;
import com.tejas.core.TejasBackgroundJob.Configuration;
import com.tejas.core.TejasContext;

public class FileTailer
{
    private static final int CHANNEL_SIZE = 64 * 1024;

    public class Task implements com.tejas.core.TejasBackgroundJob.Task
    {
        private static final int AVERAGE_LINE_LENGTH = 2048;

        List<Byte> prefix = new ArrayList<Byte>(AVERAGE_LINE_LENGTH);

        @Override
        public void runIteration(TejasContext self) throws Exception
        {
            List<String> lines = new ArrayList<String>();

            if (FileTailer.this.channel.isOpen() == false)
            {
                return;
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(CHANNEL_SIZE);

            int numCharsRead = FileTailer.this.channel.read(byteBuffer);

            self.logger.debug("Read ", numCharsRead, " bytes");

            if (numCharsRead < 1)
            {
                // End of File
                return;
            }

            byteBuffer.flip();

            List<Byte> currentLine = new ArrayList<Byte>(AVERAGE_LINE_LENGTH);

            while (byteBuffer.hasRemaining())
            {
                byte b = byteBuffer.get();

                if ((b == Character.LINE_SEPARATOR) || (b == Character.LETTER_NUMBER))
                {
                    Byte[] bytes = currentLine.toArray(new Byte[0]);
                    byte[] bits = new byte[bytes.length];
                    for (int i = 0; i < bytes.length; i++)
                    {
                        bits[i] = bytes[i];
                    }
                    lines.add(new String(bits));
                    currentLine = new ArrayList<Byte>(AVERAGE_LINE_LENGTH);
                }
                else
                {
                    currentLine.add(b);
                }
            }

            this.prefix = currentLine;

            FileTailer.this.listener.processNewData(lines, FileTailer.this.channel.position());
        }
    }

    public static interface DataListener
    {
        void processNewData(List<String> lines, long currentFilePosition);
    }

    File file;
    DataListener listener;
    TejasBackgroundJob worker;
    long currentFilePosition;
    FileChannel channel;

    public FileTailer(File file, DataListener listener) throws IOException
    {
        Assert.notNull(file);
        Assert.isTrue(file.exists(), "File " + file + " does not exist");
        Assert.isTrue(file.canRead());
        this.file = file;

        this.channel = new FileInputStream(file).getChannel();

        Assert.notNull(listener);
        this.listener = listener;
    }

    public void start()
    {
        String jobName = "File Tailer - " + this.file.getAbsolutePath();
        Configuration configuration = new Configuration.Builder(jobName, PLATFORM_UTIL_LIB, 100).build();

        this.worker = new TejasBackgroundJob(new Task(), configuration);

        this.worker.start();
    }

    public void stop()
    {
        this.worker.signalShutdown();

        try
        {
            this.channel.close();
        }
        catch (IOException e)
        {
            // Ignore. We are shutting down in any case!
        }
    }

}
