package com.tejas.utils.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class ListOptionsServlet extends HttpServlet
{
	public static class ConsoleEntry
	{
		enum EntryType
		{
			Link,
			Header,
			LineBreak,
			;
		}

		public final String title;
		public final String servletClass;
		public final boolean isDefault;
		public final EntryType type;
		
		public ConsoleEntry()
		{
			this(EntryType.LineBreak, null, "", false);
		}

		public ConsoleEntry(String header)
		{
			this(EntryType.Header, header, "", false);
		}
		
        @SuppressWarnings("rawtypes")
        public ConsoleEntry(String title, Class servletClass)
		{
			this(EntryType.Link, title, servletClass, false);
		}

        @SuppressWarnings("rawtypes")
        public ConsoleEntry(String title, Class servletClass, boolean isDefault)
		{
			this(EntryType.Link, title, servletClass, isDefault);
		}

		public ConsoleEntry(String title, String servletClassFQName)
		{
			this(EntryType.Link, title, servletClassFQName, false);
		}

		public ConsoleEntry(String title, String servletClassFQName, boolean isDefault)
		{
			this(EntryType.Link, title, servletClassFQName, isDefault);
		}
		
		public String getServletURL()
		{
			return ConsoleUtils.getServletURL(servletClass);
		}
		
		public ConsoleEntry(EntryType type, String title, String servletClassFQName, boolean isDefault)
		{
			this.type = type;
			this.title = title;
			this.servletClass = servletClassFQName;
			this.isDefault = isDefault;
		}
		
        @SuppressWarnings("rawtypes")
        public ConsoleEntry(EntryType type, String title, Class servletClass, boolean isDefault)
		{
			this(type, title, servletClass.getCanonicalName(), isDefault);
		}
	}
	
	private final List<ConsoleEntry> consoleEntries;
	
	private ConsoleEntry defaultEntry;
	
	public ListOptionsServlet(ConsoleEntry... consoleEntries)
	{
		this.consoleEntries = new ArrayList<ConsoleEntry>(Arrays.asList(consoleEntries));
		for (ConsoleEntry entry : consoleEntries)
		{
			if(entry.isDefault)
			{
				defaultEntry = entry;
				break;
			}
		}
	}

	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response) 
	throws ServletException, IOException
	{
		PrintWriter out = response.getWriter();
		listOptions(out, consoleEntries.toArray(new ConsoleEntry[0]));
	}

	public static void listOptions(PrintWriter out, ConsoleEntry[] consoleEntries)
	{
		out.println("<html>");
			out.println("<body>");
				out.println("<table>");
						for (ConsoleEntry entry : consoleEntries)
						{
							out.println("<tr>");
								out.println("<td>");
									switch(entry.type)
									{
										case Link:
											out.println("<a target='mainFrame' href='" + ConsoleUtils.getServletURL(entry.servletClass) +"'>" +entry.title);
											out.println("</a>");
											break;
										case Header:
											out.println("<h4>" + entry.title + "</h4>");
											break;
										case LineBreak:
											out.println("<br>" + "</br>");
											break;
									}
								out.println("</td>");
							out.println("</tr>");
						}
				out.println("</table>");
			out.println("</body>");
		out.println("</html>");
		out.close();
	}

	public ConsoleEntry getDefaultEntry()
	{
		return defaultEntry;
	}
	
}
