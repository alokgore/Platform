package com.tejas.profiler;

class Profiler
{
	public static void recordEvent(final String name)
	{
		recordEvent(name, 1);
	}

	public static void recordEvent(final String name, final long count)
	{
		new ProfilerScope.Builder(name).forCount(count).build().end();
	}

	public static void recordLatency(final String name, final long latencyInMillis)
	{
		new ProfilerScope.Builder(name).forTimeSpent(latencyInMillis).build().end();
	}

	/**
	 * Resets (clears) the thread-local storage for Profiling. (This is to guard
	 * against memory leaks that happen when people forget to "end()" the
	 * ProfilerScope objects. This method can be called in the beginning/end of
	 * an iteration/thread
	 */
	public static void resetProfilingData()
	{
		ProfilerScope.reset();
	}

	public static ProfilerScope start(final String name)
	{
		return new ProfilerScope.Builder(name).build();
	}

	/**
	 * Starts a Profiling session only in 'Nested' mode (i.e. It outputs the
	 * profiled data only if this profiler session is inside another profiler)
	 */
	public static ProfilerScope startInNestedMode(final String name)
	{
		return new ProfilerScope.Builder(name).nestedOnly().build();
	}
}
