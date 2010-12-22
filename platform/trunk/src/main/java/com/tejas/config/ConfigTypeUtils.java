package com.tejas.config;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

class ConfigTypeUtils
{

	static String getString(Object o)
	{
		return (o != null) && (o instanceof String) ? (String) o : null;
	}

    @SuppressWarnings({ "rawtypes" })
	static Map getMap(Object o)
	{
		return (o != null) && (o instanceof Map) ? (Map) o : null;
	}

    @SuppressWarnings({ "rawtypes" })
	static List getList(Object o)
	{
		return (o != null) && (o instanceof List) ? (List) o : null;
	}

	public static Integer getInteger(Object o)
	{
		if ((o != null) && (o instanceof String))
		{
			return Integer.valueOf((String) o);
		}
		else if (o instanceof Number)
		{
			return new Integer(((Number) o).intValue());
		}
		return null;
	}

	public static Long getLong(Object o)
	{
		if ((o != null) && (o instanceof String))
		{
			return Long.valueOf((String) o);
		}
		else if (o instanceof Number)
		{
			return new Long(((Number) o).longValue());
		}
		return null;
	}

	public static Double getDouble(Object o)
	{
		if ((o != null) && (o instanceof String))
		{
			return Double.valueOf((String) o);
		}
		else if (o instanceof Number)
		{
			return new Double(((Number) o).doubleValue());
		}
		return null;
	}

	public static Boolean getBoolean(Object o)
	{
		if ((o != null) && (o instanceof String))
		{
			char first = ((String) o).toUpperCase().charAt(0);
			if ((first == 'T') || (first == 'Y') || (first == '1'))
            {
                return Boolean.valueOf(true);
            }
			if ((first == 'F') || (first == 'N') || (first == '0'))
            {
                return new Boolean(false);
            }

			throw new ConfigSyntaxException("Unable to convert string value to bool: " + (String) o);
		}
		else if (o instanceof Boolean)
		{
			return (Boolean) o;
		}
		return null;
	}

    @SuppressWarnings("rawtypes")
    public static void printObject(Object value, StringBuffer out)
	{
		if (value instanceof String)
		{
			out.append("\"").append((String) value).append("\"");
		}
		else
		{
			if (value instanceof List)
			{
				ConfigTypeUtils.printList((List) value, out);
			}
			else if (value instanceof Map)
			{
				ConfigTypeUtils.printMap((Map) value, out);
			}
			else
			{
				throw new ConfigSyntaxException("Unsupported type in config tree: " + value.getClass().getName());
			}
		}
	}

    @SuppressWarnings("rawtypes")
    static void printList(List value, StringBuffer out)
	{
		int count = value.size();
		out.append("( ");
		for (int i = 0; i < count; ++i)
		{
			printObject(value.get(i), out);
			if (i != count)
			{
				out.append(", ");
			}
		}
		out.append(" )");
	}

    @SuppressWarnings({ "unchecked", "rawtypes" })
	static void printMap(Map value, StringBuffer out)
	{
		Set<Entry<String, Object>> entrySet = value.entrySet();
		Iterator<Entry<String, Object>> iter = entrySet.iterator();
		out.append("{ ");
		while (iter.hasNext())
		{
			Map.Entry<String, Object> entry = iter.next();
			out.append(entry.getKey()).append(" = ");
			printObject(entry.getValue(), out);
			out.append("; ");
		}
		out.append(" } ");
	}
}
