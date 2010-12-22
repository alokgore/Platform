package com.tejas.profiler;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tejas.config.ApplicationConfig;
import com.tejas.profiler.Profiler;
import com.tejas.profiler.ProfilerScope;

/**
 * 'Furji' test. Just to make sure that we do have NPEs in the seemingly
 * harmless codepaths. Check the profiler_logs to be absolutely sure about the
 * functionality
 */
public class ProfilerTest
{
	@Test
	public void test() throws Exception
	{
		testBasicProfilerScope();
		testNestedScope();
		testProfilerAPIs();

		/*
		 * Test the same Profiler APIs in nested mode
		 */
		ProfilerScope root = Profiler.start("MyRootScope");
		testProfilerAPIs();
		root.end();

	}

	private void testBasicProfilerScope() throws InterruptedException
	{
		ProfilerScope scope = Profiler.start("BasicProfilerTest");
		Thread.sleep(2000);
		scope.end();
	}

	private void testNestedScope() throws InterruptedException
	{
		ProfilerScope scope2 = Profiler.start("NestingTest");
		Thread.sleep(100);
		scope2.insertAdditionalEntry("FoxGRID", "XXXXXXXX");
		scope2.insertAdditionalEntry("FoxLRID", "YYYYYYYY");

		ProfilerScope nestedScope = Profiler.startInNestedMode("my_api_2_nested_call");
		Thread.sleep(100);
		nestedScope.end();
		scope2.end();
	}

	private void testProfilerAPIs()
	{
		Profiler.recordEvent("my_event_1");
		Profiler.recordEvent("my_event_2", 10);
		Profiler.recordLatency("my_profiler_api_1", 10000);
	}

	@BeforeClass
	protected void setUp() throws Exception
	{
		ApplicationConfig.initialize(null, "debug", "cms", "cms-service");
	}

	@AfterClass
	protected void tearDown() throws Exception
	{
		ApplicationConfig.destroy();
	}
}
