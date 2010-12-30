package com.tejas.utils.misc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class StringUtils
{
	public static String serializeToString(Throwable t)
	{
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		t.printStackTrace(printWriter);
		printWriter.flush();
		return writer.toString();
	}

	@SuppressWarnings("rawtypes")
	public static String getCSVString(Collection collection)
	{
		return getCSVString(collection, false, false);
	}

	@SuppressWarnings("rawtypes")
	public static String getCSVString(Collection collection, boolean withParanteses)
	{
		return getCSVString(collection, withParanteses, false);
	}
	
	@SuppressWarnings("rawtypes")
	public static String getCSVString(Collection collection, boolean withParanteses, boolean withQuotes)
	{
		return getCSVString(collection, withParanteses, withQuotes, true);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String getCSVString(Collection collection, boolean withParanteses, boolean withQuotes, boolean withSpaces)
	{
		StringBuilder builder = new StringBuilder(withParanteses ? "(" : "");
		String spaces = withSpaces ? " ":"";
		for (Iterator<Object> i = collection.iterator(); i.hasNext();)
		{
			Object object = i.next();
			builder.append(withQuotes ? "'" : "");
			builder.append(object);
			builder.append(withQuotes ? "'" : "");

			builder.append(i.hasNext() ? ","+spaces : "");
		}
		builder.append(withParanteses ? ")" : "");
		return builder.toString();
	}

	@SuppressWarnings("rawtypes")
	public static String join(final Collection collection, final String joiningWord)
	{
		return join(collection, joiningWord, false, false);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String join(final Collection collection, final String joiningWord, final boolean withParanteses, final boolean withQuotes)
	{
		StringBuilder builder = new StringBuilder(withParanteses ? "(" : "");

		for (Iterator<Object> i = collection.iterator(); i.hasNext();)
		{
			Object object = i.next();
			builder.append(withQuotes ? "'" : "");
			builder.append(object);
			builder.append(withQuotes ? "'" : "");

			builder.append(i.hasNext() ? " " + joiningWord + " " : "");
		}
		builder.append(withParanteses ? ")" : "");
		return builder.toString();
	}

	public static String join(final Object[] array, final String joiningWord)
	{
		return join(Arrays.asList(array), joiningWord, false, false);
	}

	public static String makeNullSafe(final Object string)
	{
		return string == null ? "" : string.toString().trim();
	}

	public static String md5(final String s)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(s.getBytes("UTF8"));
			byte[] digest = md.digest();
			BigInteger bigInteger = new BigInteger(digest).abs();
			return bigInteger.toString(Character.MAX_RADIX);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
    public static String millisToPrintableString(long millis)
    {
        if(millis < 5000) return millis + " millis";
        long seconds = millis/1000;
        if(seconds < 120) return seconds + " seconds";
        long minutes = seconds/60;
        return minutes + " minutes " + ((minutes < 3) ? (seconds%60) + " seconds"  : "");
    }
	
}
