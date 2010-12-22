package com.tejas.config;

public class ConfigSyntaxException extends RuntimeException
{
	public ConfigSyntaxException(String message)
	{
		super(message);
	}

	public ConfigSyntaxException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
