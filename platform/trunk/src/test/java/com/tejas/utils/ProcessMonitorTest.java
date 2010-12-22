package com.tejas.utils;

import java.io.File;

import com.tejas.config.ApplicationConfig;
import com.tejas.utils.misc.ProcessMonitor;

import junit.framework.TestCase;

public class ProcessMonitorTest extends TestCase
{
	@Override
	protected void setUp() throws Exception
	{
		ApplicationConfig.initialize(null, null, "platform", "pmet-server");
	}

	public void testProcessMonitor() throws Exception
	{

		process(new ProcessMonitor("/bin/ls", "/"), true);
		process(new ProcessMonitor("/bin/ls", "/fsdfsd"), false);

		
		File home = new File(System.getProperty("user.home"));
		
		File out1 = new File("/tmp/alok/process/out1.txt");
		File err1 = new File("/tmp/alok/process/err1.txt");
		process(new ProcessMonitor.Builder("ls", ".").withWorkingDir(home).withStdoutRedirect(out1).withStderrRedirect(err1).build(), true);
		
		File out2 = new File("/tmp/alok/process/out2.txt");
		File err2 = new File("/tmp/alok/process/err2.txt");
		process(new ProcessMonitor.Builder("ls", "abra_ka_dabra").withWorkingDir(home).withStdoutRedirect(out2).withStderrRedirect(err2).build(), false);
	}

	private void process(ProcessMonitor monitor, boolean shouldSucceed) throws InterruptedException
	{
		System.out.println("\nStarting process " + monitor);
		monitor.start();
		monitor.join();
		if (monitor.isSuccessful())
		{
			System.out.println("Process " + monitor+ " finished successfully");
		}
		else
		{
			System.out.println("Process " + monitor + " FAILED");
		}
		System.out.println("Process " + monitor + " Exit value = [" + monitor.getExitValue() + "]");
		System.out.println("Process " + monitor + " Exception = [" + monitor.getException() + "]");
		System.out.println("Process " + monitor + " STDOUT = [" + monitor.getOutputStreamAsString() + "]");
		System.out.println("Process " + monitor + " STDout = [" + monitor.getErrorStreamAsString() + "]");

		assertEquals(shouldSucceed, monitor.isSuccessful());
	}
}
