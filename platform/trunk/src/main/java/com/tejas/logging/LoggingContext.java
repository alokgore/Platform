package com.tejas.logging;

import java.util.List;
import java.util.Vector;

import com.tejas.utils.misc.StringUtils;


public class LoggingContext
{
	private final String name;
	private final List<String> data = new Vector<String>();
	
	public LoggingContext(String name, String... data)
	{
		this.name = name;
		addContexInformation(data);
	}
	
	public LoggingContext()
	{
		//Unnamed Logging context. People! Tolerate!
		name = "";
	}
	
	@Override
	public String toString()
	{
		String response = name() + data();
		if(response.length() > 0)
		{
			response +=": ";
		}
		return response;
	}
	
	public void addContexInformation(String... contextInfo)
	{
		for (String context : contextInfo)
		{
			if(context != null && context.trim().equals("") == false)
			{
				this.data.add(context.trim());
			}
		}
	}

	private String name()
	{
		return name==null || name.trim().equals("") ? "" : "[Context:" + name + "]";
	}

	private String data()
	{
		String dataStr = "";
		if(data.size() > 0)
		{
			dataStr = "[" + StringUtils.getCSVString(data, true) + "]";
		}
		return dataStr;
	}
}

