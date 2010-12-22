package com.tejas.config;

class ConfigEntry
{
	public final ConfigKey key;

	/**
	 * Can be a primitive data type or a List or Map
	 */
	public final Object value;

	public ConfigEntry(String fullyQualifiedKeyName, Object value)
	{
		this.key = new ConfigKey(fullyQualifiedKeyName);
		this.value = value;
	}

	@Override
	public String toString()
	{
		return " " + key + " =  " + value;
	}
}
