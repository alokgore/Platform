package com.tejas.config;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.tejas.config.ConfigKey.Domain;


class ValueNode
{
	/**
	 * The location that the value came from
	 */
	private String fileFrom;
	private final ConfigKey key;

	private Object value;

	ValueNode(ConfigKey key, Object value, String fileFrom)
	{
		this.key = key;
		this.value = value;
		this.fileFrom = fileFrom;
	}

	public ConfigKey getKey()
	{
		return key;
	}

	public void insertAdditionalData(ConfigKey newKey, Object newValue, String newFileFrom)
	{
		if (!this.key.getKeyName().equals(newKey.getKeyName()))
		{
			throw new IllegalArgumentException("Trying to merge data for two different keys! Original Key = [" + key + "] New Key = [" + newKey + "]");
		}

		Domain currentDomain = key.getDomain();
		Domain newDomain = newKey.getDomain();

		boolean canOverride = newDomain.canOverride(currentDomain);

		if (canOverride)
		{
			if (newKey.isAppendFlagSet())
			{
				append(newValue, newFileFrom);
			}
			else
			{
				this.value = newValue;
				this.fileFrom = newFileFrom;
			}
		}
	}

	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		printTo(buffer);
		return buffer.toString();
	}

	public Object value()
	{
		return value;
	}

	/**
	 * Print this ValueNode into the StringBuffer 'out'. The output is suitable for use in a config
	 * file, such as an override.cfg
	 * 
	 * @param out
	 *            the StringBuffer to append our information to.
	 */
	void printTo(StringBuffer out)
	{
		Object oValue = value();
		if ((oValue instanceof String) || (oValue instanceof Map) || (oValue instanceof Vector))
		{
			out.append("# From : ").append(fileFrom).append("\n");
			out.append(key.getFullyQualifiedKeyName()).append(" = ");
			ConfigTypeUtils.printObject(oValue, out);
			out.append(";\n\n");
		}
		else
		{
			out.append("# Unsupported type in configuration for key=\"" + key.getKeyName() + "\" : " + oValue.getClass().getName());
		}
	}

    @SuppressWarnings({ "unchecked", "rawtypes" })
	private void append(Object newValue, String newFileFrom)
	{
		assertAppendability(value);
		assertAppendability(newValue);

		this.fileFrom += " AND " + newFileFrom;

		if (isMapOrList(value) && isMapOrList(newValue))
		{
			if (isMap(value) && isMap(newValue))
			{
				((Map) value).putAll((Map) newValue);
			}
			else if (isList(value) && isList(newValue))
			{
				((List) value).addAll((List) newValue);
			}
			else
			{
				throw new ConfigSyntaxException("Append attempted for distinct types: " + newValue.getClass().getName() + ", "
						+ value.getClass().getName());
			}
		}
	}

	private void assertAppendability(Object object)
	{
		if (object == null)
		{
			throw new IllegalArgumentException("Config Append attempted on null object for key [" + key + "]");
		}

		if (isMapOrList(object) == false)
		{
			throw new IllegalArgumentException("Config Append attempted on a non-Map/List object of type [" + value.getClass() + "] for key [" + key
					+ "]");
		}
	}

	private boolean isList(Object newValue)
	{
		return newValue instanceof List;
	}

	private boolean isMap(Object object)
	{
		return object instanceof Map;
	}

	private boolean isMapOrList(Object object)
	{
		return isMap(object) || isList(object);
	}
}
