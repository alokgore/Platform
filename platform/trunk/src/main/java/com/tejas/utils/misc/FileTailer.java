package com.tejas.utils.misc;

import static com.tejas.core.enums.PlatformComponents.PLATFORM_UTIL_LIB;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.tejas.core.TejasBackgroundJob;
import com.tejas.core.TejasBackgroundJob.AbstractTejasTask;
import com.tejas.core.TejasBackgroundJob.Configuration;
import com.tejas.core.TejasContext;

public class FileTailer
{
    public static class FilePositionData
    {
        public FilePositionData()
        {
            // For MyBatis
        }

        public String fileName;
        public long filePosition;

        public FilePositionData(String fileName, long filePosition)
        {
            this.fileName = fileName;
            this.filePosition = filePosition;
        }

        public String getFileName()
        {
            return this.fileName;
        }

        public void setFileName(String fileName)
        {
            this.fileName = fileName;
        }

        public long getFilePosition()
        {
            return this.filePosition;
        }

        public void setFilePosition(long filePosition)
        {
            this.filePosition = filePosition;
        }
    }

    public enum TailStatus
    {
            Complete,
            InProgress
    }

    public interface DatabaseMapper
    {
        @Update("create table if not exists tejas_file_tailer_log (" +
                    "id int(11) auto_increment primary key, " +
                "file_name varchar(512) unique key, " +
                "file_position int(11), " +
                "tail_status varchar(64), " +
                "last_updated timestamp)")
        public void createTable();

        @Insert("insert ignore into tejas_file_tailer_log(file_name, file_position, tail_status) values (#{fileName}, #{filePosition}, 'InProgress')")
        public void insert(FilePositionData record);

        @Update("update tejas_file_tailer_log set file_position = #{filePosition} where file_name = #{fileName}")
        public void update(FilePositionData record);

        @Select("select file_position from tejas_file_tailer_log where file_name = #{fileName}")
        public long readPosition(String fileName);

        @Select("select tail_status from tejas_file_tailer_log where file_name = #{fileName}")
        public TailStatus readStatus(String fileName);

        @Update("update tejas_file_tailer_log set tail_status  = 'Complete' where file_name = #{fileName}")
        public void markTailComplete(String fileName);
    }

    public static interface DataListener
    {
        void processNewData(List<String> lines, long currentFilePosition) throws Exception;
    }

    public class FileTailerTask extends AbstractTejasTask
    {
        private static final int MAX_FILENAME_LENGTH = 512;
        private static final int AVERAGE_LINE_LENGTH = 2048;
        private final FileChannel channel;
        private final DataListener listener;
        private final String fileName;

        public String getFileName()
        {
            return this.fileName;
        }

        public void markCompletion(TejasContext self)
        {
            self.logger.info("Marking the tail process on [" + this.fileName + "] Complete");
            DatabaseMapper mapper = self.dbl.getMybatisMapper(DatabaseMapper.class);
            mapper.markTailComplete(this.fileName);
        }

        private List<Byte> currentLine = new ArrayList<Byte>(AVERAGE_LINE_LENGTH);

        public FileTailerTask(File file, DataListener listener) throws IOException
        {
            this.listener = Assert.notNull(listener);
            Assert.isTrue(Assert.notNull(file).canRead(), "File [" + file + "] is not readable");

            this.fileName = file.getAbsolutePath();
            Assert.isTrue(this.fileName.length() < MAX_FILENAME_LENGTH, "File Name is too long. Limit on the file-name is " + MAX_FILENAME_LENGTH + ". Filename was ["
                    + this.fileName + "]");

            this.channel = new FileInputStream(file).getChannel();
        }

        private byte[] getBytes(List<Byte> line)
        {
            byte[] bits = new byte[line.size()];
            for (int i = 0; i < bits.length; i++)
            {
                bits[i] = line.get(i);
            }
            return bits;
        }

        @Override
        public void init(TejasContext self) throws Exception
        {
            DatabaseMapper mapper = self.dbl.getMybatisMapper(DatabaseMapper.class);
            mapper.createTable();
            mapper.insert(new FilePositionData(this.fileName, 0));

            long filePosition = mapper.readPosition(this.fileName);

            self.logger.info("Setting the starting file-position in [" + this.fileName + "] to [", filePosition, "]");
            this.channel.position(filePosition);
        }

        @Override
        public void shutdown(TejasContext self)
        {
            try
            {
                self.logger.info("Stopping the tail process on [" + this.fileName + "]");
                this.channel.close();
            }
            catch (IOException e)
            {
                // Ignore. We are shutting down in any case!
            }
        }

        @Override
        public void runIteration(TejasContext self, TejasBackgroundJob parent) throws Exception
        {
            if (this.channel.isOpen() == false)
            {
                return;
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(CHANNEL_SIZE);
            int numCharsRead = this.channel.read(byteBuffer);
            self.logger.debug("Read ", numCharsRead, " bytes");

            if (numCharsRead < 1)
            {
                // End of File
                return;
            }

            // Make it ready for reads
            byteBuffer.flip();

            List<String> lines = new ArrayList<String>();

            while (byteBuffer.hasRemaining())
            {
                byte b = byteBuffer.get();

                if ((b == Character.LINE_SEPARATOR) || (b == Character.LETTER_NUMBER))
                {
                    lines.add(new String(getBytes(this.currentLine)));
                    this.currentLine = new ArrayList<Byte>(AVERAGE_LINE_LENGTH);
                }
                else
                {
                    this.currentLine.add(b);
                }
            }
            processData(self, lines, this.channel.position());
        }

        private void processData(TejasContext self, List<String> lines, long position)
        {
            try
            {
                this.listener.processNewData(lines, position);
            }
            catch (Exception e)
            {
                // XXX: Need retry with exponential back-offs here
                self.logger.error("Exception in processing file data [" + lines + "]", e);
            }

            DatabaseMapper mapper = self.dbl.getMybatisMapper(DatabaseMapper.class);
            mapper.update(new FilePositionData(this.fileName, position));
        }
    }

    private static final int CHANNEL_SIZE = 64 * 1024;
    private TejasBackgroundJob worker;
    private FileTailerTask fileTailerTask;

    public FileTailer(File file, DataListener listener) throws IOException
    {
        String jobName = "File Tailer - " + file.getAbsolutePath();
        Configuration configuration = new Configuration.Builder(jobName, PLATFORM_UTIL_LIB, 100).build();
        this.fileTailerTask = new FileTailerTask(file, listener);
        this.worker = new TejasBackgroundJob(this.fileTailerTask, configuration);
    }

    public void start(TejasContext self)
    {
        self.logger.info("Starting a tail process on file [" + this.fileTailerTask.getFileName() + "]");
        this.worker.start();
    }

    /**
     * Stop the tail process for now. <br>
     * This leaves the {@link TailStatus} in {@link TailStatus#InProgress}, which means that the next time a tail process is brought up, it will start from the
     * point where we are leaving
     */
    public void stop(TejasContext self)
    {
        self.logger.info("Signaling the tail process on file [" + this.fileTailerTask.getFileName() + "] to go down");
        this.worker.signalShutdown();
    }

    /**
     * Stop the tail process and mark it Complete in the database<br>
     */
    public void markComplete(TejasContext self)
    {
        this.worker.signalShutdown();
        this.fileTailerTask.markCompletion(self);
    }
}
