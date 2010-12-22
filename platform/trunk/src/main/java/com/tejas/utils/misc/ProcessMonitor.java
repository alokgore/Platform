package com.tejas.utils.misc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import com.tejas.core.TejasLogger;
import com.tejas.logging.TejasLog4jWrapper;


public class ProcessMonitor extends Thread
{
	public static class Builder
	{
		private String[] command;
		
		private File _stderrRedirect;
		private File _stdoutRedirect;
		private File _workingDir;

		public Builder(String... command)
		{
			this.command = command;
		}
		
		public ProcessMonitor build()
		{
			ProcessMonitor processMonitor = new ProcessMonitor(_workingDir, _stdoutRedirect, _stderrRedirect, command);
			return processMonitor;
		}

		public Builder withStderrRedirect(File stderrRedirect)
		{
			this._stderrRedirect = stderrRedirect;
			return this;
		}

		public Builder withStdoutRedirect(File stdoutRedirect)
		{
			this._stdoutRedirect = stdoutRedirect;
			return this;
		}

		public Builder withWorkingDir(File workingDir)
		{
			this._workingDir = workingDir;
			return this;
		}
	}

	private static final TejasLogger logger = TejasLog4jWrapper.getLogger(ProcessMonitor.class);

	private final String[] commandArray;
	private final StringBuffer errorBuffer = new StringBuffer("");
	private String exception = "";
	private int exitValue = -1;
	private boolean isSuccessful = false;
	private final StringBuffer outputBuffer = new StringBuffer("");
	private final String pid = "ProcessMonitor_" + System.currentTimeMillis();

	private Process _process;
	private Date processEndTime;
	private Date processStartTime;
	private FileOutputStream stderrRedirect;
	private FileOutputStream stdoutRedirect;
	private File workingDir;
	
	public ProcessMonitor(String... commandArray)
	{
		this.commandArray = commandArray;
	}

	ProcessMonitor(File workingDir, File stdoutRedirect, File stderrRedirect, String... commandArray)
	{
		this.commandArray = commandArray;
		this.workingDir = workingDir;
		this.stdoutRedirect = getRedirectStream(stdoutRedirect);
		this.stderrRedirect = getRedirectStream(stderrRedirect);

		super.setName(pid);
	}

	/**
	 * The command (with parameters) this monitor is running
	 */
	public synchronized String getCommand()
	{
		StringBuilder buf = new StringBuilder();
		for (String commandLineArg : commandArray)
		{
			buf.append(commandLineArg).append(" ");
		}
		return buf.toString().trim();
	}

	/**
	 * Returns a dump of all that has been written to the error stream 'so far'
	 */
	public synchronized String getErrorStreamAsString()
	{
		return errorBuffer.toString();
	}

	/**
	 * Returns a dump of all that has been written to the error stream 'so far'
	 */
	public synchronized String getException()
	{
		return exception;
	}

	/**
	 * The method blocks until the process is finished. 
	 */
	public synchronized int getExitValue()
	{
		blockTillDone();
		return exitValue;
	}

	/**
	 * Returns a dump of all that has been written to the output stream 'so far'
	 */
	public synchronized String getOutputStreamAsString()
	{
		return outputBuffer.toString();
	}

	/**
	 * The method blocks until the process is finished. 
	 */
	public synchronized Date getProcessEndTime()
	{
		blockTillDone();
		return processEndTime;
	}

	public synchronized Date getProcessStartTime()
	{
		return processStartTime;
	}

	/**
	 * Blocks until the process is finished
	 */
	public synchronized boolean isSuccessful()
	{
		blockTillDone();
		return isSuccessful;
	}

	@Override
	public void run()
	{
		InputStream outputStream = null;
		InputStream errorStream = null;

		try
		{
			logger.info("Starting the process and the process-monitor thread for command ["+ getCommand() + "]");

			processStartTime = new Date();

			ProcessBuilder processBuilder = new ProcessBuilder(commandArray);
			if (workingDir != null)
			{
				processBuilder.directory(workingDir);
			}
			_process = processBuilder.start();

			outputStream = _process.getInputStream();
			errorStream = _process.getErrorStream();
			_process.getOutputStream().close();

			do
			{
				Thread.sleep(100);
				copyOutput(outputStream, errorStream);
			}
			while (isProcessAlive(_process));

			copyOutput(outputStream, errorStream);

			exitValue = _process.exitValue();
			isSuccessful = (exitValue == 0);

			logger.info("Process finished with exit-value = [" + exitValue+ "]");
			logger.info("Output-stream was [" + outputBuffer.toString() + "]");
			logger.info("Output-stream was [" + errorBuffer.toString() + "]");
		}
		catch (Throwable t)
		{
			exception = StringUtils.serializeToString(t);
			logger.info("Process failed", t);
		}
		finally
		{
			processEndTime = new Date();

			IOUtils.closeQuietly(outputStream);
			IOUtils.closeQuietly(errorStream);
			IOUtils.closeQuietly(stdoutRedirect);
			IOUtils.closeQuietly(stderrRedirect);
		}
	}

	@Override
	public String toString()
	{
		return "" + pid + " for [" + getCommand() + "]";
	}

	private void blockTillDone()
    {
	    try
        {
	        this.join();
        }
        catch (InterruptedException e)
        {
        		throw new RuntimeException(e);
        }
    }

	private void copyOutput(InputStream inputStream, InputStream errorStream)
	        throws Exception
	{
		copyOutput("SYSOUT", inputStream, outputBuffer, stdoutRedirect);
		copyOutput("SYSERR", errorStream, errorBuffer, stderrRedirect);
	}

	private void copyOutput(String streamName, InputStream inputStream, StringBuffer buffer, FileOutputStream fileRedirect) throws Exception, IOException
	{
		byte bytes[] = new byte[inputStream.available()];
		inputStream.read(bytes);
		if (bytes.length > 0)
		{
			String output = new String(bytes);
			buffer.append(output);
			logger.info("[" + streamName + "] : " + output);
			if (fileRedirect != null)
			{
				fileRedirect.write(bytes);
			}
		}
	}

	private FileOutputStream getRedirectStream(File redirectFile)
	{
		FileOutputStream stream = null;
		if (redirectFile != null)
		{
			redirectFile.getParentFile().mkdirs();
			try
            {
	            stream = new FileOutputStream(redirectFile);
            }
            catch (FileNotFoundException e)
            {
            		throw new IllegalArgumentException(e);
            }
		}
		return stream;
	}
	
	private boolean isProcessAlive(Process process)
	{
		try
		{
			process.exitValue();
			return false;
		}
		catch (IllegalThreadStateException e)
		{
			return true;
		}
	}
}
