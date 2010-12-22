package com.tejas.utils.soa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tejas.utils.misc.StringUtils;


public class ComponentHealth
{
	public static class Builder
	{
		private final String componentName;
		private final Status status;
		private String errorSummary = "";
		private String errorDetails = "";
		private List<ComponentHealth> subcomponents;
		private final Map<String, Object> metaData = new Hashtable<String, Object>();
		
		public Builder(String componentName, Status status)
		{
			this.componentName = componentName;
			this.status = status;
		}
		
		public Builder withError(String error)
		{
			this.errorSummary = error;
			return this;
		}
		
		public Builder withError(String summary, String details)
		{
			this.errorSummary = summary;
			this.errorDetails = details;
			return this;
		}
		
		public Builder withException(Exception e)
		{
			this.errorSummary = e.toString();
			this.errorDetails = StringUtils.serializeToString(e);
			return this;
		}
		
		public Builder withMetadata(Map<String, Object> metadata)
		{
			this.metaData.putAll(metadata);
			return this;
		}
		
		public Builder withMetadata(String key, Object value)
		{
			this.metaData.put(key, value);
			return this;
		}
		
		public Builder withSubcomponentDetails(List<ComponentHealth> subcomponentDetails)
		{
			this.subcomponents = subcomponentDetails;
			return this;
		}
		
		public ComponentHealth build()
		{
			return new ComponentHealth(componentName, status, errorSummary, errorDetails, subcomponents, metaData);
		}
	}
	
	public static enum Status
	{
		GREEN(0), YELLOW(1), RED(2), ;

		private int id;

		private Status(int id)
		{
			this.id = id;
		}

		public Status add(Status health)
		{
			return (health.id > id) ? health : this;
		}
	}

	private final String componentName;
	private final Status status;
	private String errorSummary = "";
	private String errorDetails = "";
	private final List<ComponentHealth> subcomponents = new Vector<ComponentHealth>();
	private Map<String, Object> metaData;

	ComponentHealth(String componentName, Status health, String errorSummary, String errorDetails, List<ComponentHealth> subcomponents, Map<String, Object> data)
	{
		this.componentName = componentName;
		this.status = health;
		this.errorSummary = (errorSummary == null ? "" : errorSummary);
		this.errorDetails = (errorDetails == null ? "" : errorDetails);
		this.metaData = data;
		if(subcomponents != null)
		{
			this.subcomponents.addAll(subcomponents);
		}
	}

	@Override
	public String toString()
	{
		return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	public String getComponentName()
	{
		return componentName;
	}

	public Status getHealth()
	{
		Status health = this.status == null ? Status.GREEN : this.status;
		List<ComponentHealth> children = getSubcomponents();
		for (ComponentHealth child : children)
		{
			health = health.add(child.getHealth());
		}
		return health;
	}

	public String getError()
	{
		String summary = this.errorSummary;
		List<ComponentHealth> children = getSubcomponents();
		for (ComponentHealth child : children)
		{
			summary = concatenateStrings(summary, child.getError());
		}
		return summary;
	}

	public String getErrorDescription()
	{
		String details = this.errorDetails == null ? null : this.errorDetails;
		List<ComponentHealth> children = getSubcomponents();
		for (ComponentHealth child : children)
		{
			details = concatenateStrings(details, child.getErrorDescription());
		}
		return details;
	}

	public List<ComponentHealth> getSubcomponents()
	{
		return subcomponents == null ? new ArrayList<ComponentHealth>() : subcomponents;
	}

	public Map<String, Object> getMetaData()
	{
		metaData = metaData == null ? new HashMap<String, Object>() : metaData;
		return metaData;
	}

	private static String concatenateStrings(String str1, String str2)
	{
		String string1 = StringUtils.makeNullSafe(str1);
		String string2 = StringUtils.makeNullSafe(str2);
		return (string1.length() == 0 || string2.length() == 0 ? string1 + string2 : "[" + string1 + "], [" + string2 + "]");
	}
	
	public void addSubcomponentHealth(ComponentHealth subcomponentHealth)
	{
		this.subcomponents.add(subcomponentHealth);
	}
}
