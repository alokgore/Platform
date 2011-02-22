package com.tejas.utils.io;

import static com.tejas.core.enums.PlatformComponents.PLATFORM_UTIL_LIB;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.tejas.core.TejasBackgroundJob;
import com.tejas.core.TejasBackgroundJob.AbstractTejasTask;
import com.tejas.core.TejasBackgroundJob.Configuration;
import com.tejas.core.TejasContext;
import com.tejas.utils.misc.Assert;

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
            return fileName;
        }

        public void setFileName(String fileName)
        {
            this.fileName = fileName;
        }

        public long getFilePosition()
        {
            return filePosition;
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

    public static class FileTailInfo
    {
        private String file_name;
        private String file_position;
        private String tail_status;

        // Since SQLite does not return Date/Time java types
        private String start_time;
        private String end_time;
        private String last_updated;

        public String getTail_status()
        {
            return tail_status;
        }

        public void setTail_status(String tail_status)
        {
            this.tail_status = tail_status;
        }

        public String getFile_name()
        {
            return file_name;
        }

        public void setFile_name(String file_name)
        {
            this.file_name = file_name;
        }

        public String getFile_position()
        {
            return file_position;
        }

        public void setFile_position(String file_position)
        {
            this.file_position = file_position;
        }

        public String getStart_time()
        {
            return start_time;
        }

        public void setStart_time(String start_time)
        {
            this.start_time = start_time;
        }

        public String getEnd_time()
        {
            return end_time;
        }

        public void setEnd_time(String end_time)
        {
            this.end_time = end_time;
        }

        public String getLast_updated()
        {
            return last_updated;
        }

        public void setLast_updated(String last_updated)
        {
            this.last_updated = last_updated;
        }

        @Override
        public String toString()
        {
            return ReflectionToStringBuilder.toString(this);
        }
    }

    public interface DatabaseMapper
    {
        @Update("create table if not exists tejas_file_tailer_log (" +
                "file_name varchar(512) primary key, " +
                "file_position int, " +
                "tail_status varchar(64), " +
                "start_time datetime, " +
                "end_time datetime default null, " +
                "last_updated timestamp)")
        public void createTable();

        @Insert("insert or ignore into tejas_file_tailer_log(file_name, file_position, start_time, last_updated, tail_status) " +
                "values (#{fileName}, #{filePosition}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'InProgress')")
        public void insert(FilePositionData record);

        @Update("update tejas_file_tailer_log set file_position = #{filePosition}, last_updated = CURRENT_TIMESTAMP where file_name = #{fileName}")
        public void update(FilePositionData record);

        @Select("select file_position from tejas_file_tailer_log where file_name = #{fileName}")
        public long readPosition(String fileName);

        @Select("select tail_status from tejas_file_tailer_log where file_name = #{fileName}")
        public TailStatus readStatus(String fileName);

        @Update("update tejas_file_tailer_log set tail_status  = 'Complete', end_time = CURRENT_TIMESTAMP, last_updated = CURRENT_TIMESTAMP where file_name = #{fileName}")
        public void markTailComplete(String fileName);

        @Update("delete from tejas_file_tailer_log where file_name like '#{baseFileName}%' ")
        public void clearAllDataForFile(String baseFileName);

        @Update("truncate tejas_file_tailer_log")
        public void clearAllData();

        @Select("select * from tejas_file_tailer_log order by last_updated desc")
        public List<FileTailInfo> selectAllData();

    }

    public static interface DataListener
    {
        void processNewData(TejasContext self, List<String> lines, long currentFilePosition) throws Exception;
    }

    public class FileTailerTask extends AbstractTejasTask
    {
        private static final int MAX_FILENAME_LENGTH = 512;
        private static final int AVERAGE_LINE_LENGTH = 2048;
        private final FileChannel channel;
        private final DataListener listener;
        private final String fileName;
        private transient boolean closed = false;
        private boolean autoStop;

        public synchronized boolean isAutoStop()
        {
            return autoStop;
        }

        public synchronized void setAutoStop(boolean autoStop)
        {
            this.autoStop = autoStop;
        }

        public String getFileName()
        {
            return fileName;
        }

        public void markCompletion(TejasContext self)
        {
            self.logger.debug("Marking the tail process Complete");
            DatabaseMapper mapper = self.dbl.getMybatisMapper(DatabaseMapper.class);
            mapper.markTailComplete(fileName);
            endTime = new Date();
        }

        private List<Byte> currentLine = new ArrayList<Byte>(AVERAGE_LINE_LENGTH);

        public FileTailerTask(File file, DataListener listener, boolean autoStop)
        {
            this.listener = Assert.notNull(listener);
            this.autoStop = autoStop;
            Assert.isTrue(Assert.notNull(file).canRead(), "File [" + file + "] is not readable");

            fileName = file.getAbsolutePath();
            Assert.isTrue(fileName.length() < MAX_FILENAME_LENGTH, "File Name is too long. Limit on the file-name is " + MAX_FILENAME_LENGTH + ". Filename was ["
                    + fileName + "]");

            try
            {
                channel = new FileInputStream(file).getChannel();
            }
            catch (FileNotFoundException e)
            {
                throw new RuntimeException(e);
            }
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
            mapper.insert(new FilePositionData(fileName, 0));

            long filePosition = mapper.readPosition(fileName);

            self.logger.info("Starting a tail on [" + fileName + "] from position [", filePosition, "]");
            channel.position(filePosition);
        }

        @Override
        public void shutdown(TejasContext self)
        {
            try
            {
                if (!closed)
                {
                    self.logger.info("Stopping the tail process");
                    channel.close();
                }
            }
            catch (IOException e)
            {
                // Ignore. We are shutting down in any case!
            }

            closed = true;
        }

        private transient boolean moreDataAvailable;

        @Override
        public boolean shouldTakeANap()
        {
            return moreDataAvailable == false;
        }

        @Override
        public void runIteration(TejasContext self, TejasBackgroundJob parent) throws Exception
        {
            moreDataAvailable = false;

            if (channel.isOpen() == false)
            {
                return;
            }

            long oldPosition = channel.position();
            ByteBuffer byteBuffer = ByteBuffer.allocate(CHANNEL_READ_SIZE);
            int numBytesRead = channel.read(byteBuffer);

            self.metrics.recordCount("bytesRead", numBytesRead);

            if (numBytesRead < 1)
            {
                // End of File
                if (isAutoStop())
                {
                    self.logger.info("Auto-stopping the tail process");
                    markCompletion(self);
                    parent.signalShutdown();
                }
                return;
            }

            // This will make sure that we do not take a nap after this iteration
            moreDataAvailable = true;

            List<String> lines = readBuffer(byteBuffer);

            try
            {
                processData(self, lines, channel.position());

            }
            catch (Exception e)
            {
                self.logger.error("Exception in processing file data", e);
                self.logger.error("Resetting the file pointer back to [" + oldPosition + "]");
                channel.position(oldPosition);
            }
        }

        private List<String> readBuffer(ByteBuffer byteBuffer)
        {
            // Make it ready for reads
            byteBuffer.flip();

            List<String> lines = new ArrayList<String>();

            while (byteBuffer.hasRemaining())
            {
                byte b = byteBuffer.get();

                if ((b == Character.LINE_SEPARATOR) || (b == Character.LETTER_NUMBER))
                {
                    lines.add(new String(getBytes(currentLine)));
                    currentLine = new ArrayList<Byte>(AVERAGE_LINE_LENGTH);
                }
                else
                {
                    currentLine.add(b);
                }
            }
            return lines;
        }

        private void processData(TejasContext self, List<String> lines, long position) throws Exception
        {
            listener.processNewData(self, lines, position);

            DatabaseMapper mapper = self.dbl.getMybatisMapper(DatabaseMapper.class);
            mapper.update(new FilePositionData(fileName, position));
        }
    }

    private static final int CHANNEL_READ_SIZE = 1024 * 1024;
    private TejasBackgroundJob worker;

    /**
     * @return true if the FileTailer is still tailing the file
     */
    public final boolean isActive()
    {
        return worker.isActive();
    }

    private FileTailerTask fileTailerTask;
    private Date startTime;

    public synchronized Date getStartTime()
    {
        return startTime;
    }

    public synchronized Date getEndTime()
    {
        return endTime;
    }

    Date endTime;

    /**
     * @param autoStop
     *            Stop tailing the file after it has read all the data (tail without --follow)
     */
    public FileTailer(TejasContext self, File file, DataListener listener, boolean autoStop)
    {
        Assert.isTrue(file.exists(), "File [" + file + "] does not exist");
        String jobName = "File Tailer - " + file.getAbsolutePath();
        Configuration configuration = new Configuration.Builder(jobName, PLATFORM_UTIL_LIB, 100).build();
        fileTailerTask = new FileTailerTask(file, listener, autoStop);
        worker = new TejasBackgroundJob(self, fileTailerTask, configuration);
    }

    public void start()
    {
        startTime = new Date();
        worker.start();
    }

    /**
     * Waits at most timeout milliseconds for this background-job to terminate. A timeout of 0 means indefinite wait. <br>
     * <p>
     * <i> <font color="red">Do keep in mind that if the FileTailer "tail --follow" mode, it will never terminate (unless someone calls
     * {@link #stop(TejasContext)}). So calling join with 0 timeout will result in indefinite wait </i> </font>
     * </p>
     */
    public void join(int timeout) throws InterruptedException
    {
        worker.join(timeout);
    }

    /**
     * Stop the tail process for now. <br>
     * This leaves the {@link TailStatus} in {@link TailStatus#InProgress}, which means that the next time this a process is brought up on this file, it will
     * start from the point where this process stopped
     */
    public void stop(TejasContext self)
    {
        stop(self, TailStatus.InProgress);
    }

    /**
     * Stop the tail process.
     * 
     * @param status
     *            If the value of this parameter is {@link TailStatus#Complete}, this marks the tail process on this file as complete and any subsequent request
     *            to tail the file again will fail with {@link IllegalStateException}. <br>
     *            If the value of this parameter is {@link TailStatus#InProgress}, the next tail process on this file will start from the point we are at now
     */
    public void stop(TejasContext self, TailStatus status)
    {
        self.logger.info("Signaling the tail process to go down with TailStatus [" + status + "] ");
        worker.signalShutdown();
        if (status == TailStatus.Complete)
        {
            fileTailerTask.markCompletion(self);
        }
    }

    public void stopTailingAfterEOF(TejasContext self)
    {
        self.logger.trace("Asking the tailer to go down after EOF");
        fileTailerTask.setAutoStop(true);
    }

}
