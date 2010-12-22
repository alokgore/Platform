package com.tejas.core;

public interface TejasLogger
{
	public void debug(Object... objects);

	public void error(Object... objects);

	public void fatal(Object... objects);

	public void info(Object... objects);

	public void warn(Object... objects);

	public void trace(Object... objects);

	public abstract void addContextInformation(String...data);

	public abstract void resetThreadLoggingContext();

	public abstract void createThreadLoggingContext();

	public abstract void createThreadLoggingContext(String contextName, String...contextData);
}
