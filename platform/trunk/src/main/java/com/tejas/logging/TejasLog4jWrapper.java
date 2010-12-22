package com.tejas.logging;

import org.apache.log4j.Logger;

import com.tejas.core.TejasLogger;
import com.tejas.utils.misc.StringUtils;


public class TejasLog4jWrapper implements TejasLogger
{
	private final Logger logger;
	
	private final static ThreadLocal<LoggingContext> contextHolder = new ThreadLocal<LoggingContext>(); 
	
	private TejasLog4jWrapper(@SuppressWarnings("rawtypes") Class clazz)
	{
		logger = Logger.getLogger(clazz);
	}
	
	public static TejasLogger getLogger(@SuppressWarnings("rawtypes") Class clazz)
	{
		return new TejasLog4jWrapper(clazz);
	}
	
	@Override
    public void createThreadLoggingContext(String contextName, String...contextData)
	{
		contextHolder.set(new LoggingContext(contextName, contextData));
	}
	
	@Override
    public void createThreadLoggingContext()
	{
		contextHolder.set(new LoggingContext());
	}
	
	@Override
    public void resetThreadLoggingContext()
	{
		contextHolder.set(null);
	}
	
	@Override
    public void addContextInformation(String...data)
	{
		LoggingContext loggingContext = contextHolder.get();
		if(loggingContext != null)
		{
			loggingContext.addContexInformation(data);
		}
	}
	
	@Override
    public void debug(Object... objects)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug(getMessage(objects));
		}
	}

	@Override
    public void error(Object... objects)
	{
			logger.error(getMessage(objects));
	}

	@Override
    public void fatal(Object... objects)
	{
		logger.fatal(getMessage(objects));
	}

	@Override
    public void info(Object... objects)
	{
		if (logger.isInfoEnabled())
		{
			logger.info(getMessage(objects));
		}
	}

	@Override
    public void warn(Object... objects)
	{
		logger.warn(getMessage(objects));
	}

	@Override
    public void trace(Object... objects)
	{
		if (logger.isTraceEnabled())
		{
			logger.trace(getMessage(objects));
		}
	}

	private String getMessage(Object[] objects)
	{
		StringBuilder builder = new StringBuilder();
		
		LoggingContext loggingContext = contextHolder.get();
		if(loggingContext != null)
		{
			builder.append(loggingContext);
		}
		
		for (Object object : objects)
		{
			if (object instanceof Throwable)
			{
				Throwable t = (Throwable) object;
				builder.append(StringUtils.serializeToString(t));
			}
			else
			{
				builder.append(object);
			}
		}
		return builder.toString();
	}
	
}
