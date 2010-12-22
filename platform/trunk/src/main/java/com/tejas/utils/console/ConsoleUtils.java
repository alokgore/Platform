package com.tejas.utils.console;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.tejas.config.ApplicationConfig;
import com.tejas.utils.console.ListOptionsServlet.ConsoleEntry;


public class ConsoleUtils
{

    @SuppressWarnings({ "rawtypes" })
	public static String getServletURL(Class clazz)
	{
		return getServletURL(clazz.getCanonicalName());
	}

	public static String getServletURL(String fqClassName)
	{
		return "/" + ApplicationConfig.getApplicationName() + "/servlet/" + fqClassName;
	}

	public static void printHomePage(PrintWriter out, ListOptionsServlet indexServlet, String options)
	{
		out.println("<html>");
			out.println("<frameset cols='10%,*'>");
				out.println("<frame src='" + getServletURL(indexServlet.getClass()) + (options == null ? "" : "?" + options) +"'/>");
				ConsoleEntry defaultEntry = indexServlet.getDefaultEntry();
				out.println("<frame name='mainFrame'  src='" +  (defaultEntry==null ? "" : defaultEntry.getServletURL()) + "' />");
			out.println("</frameset>");
		out.println("</html>");
	}
	
	public static void printHomePage(PrintWriter out, ListOptionsServlet indexServlet)
	{
		printHomePage(out, indexServlet, null);
	}
	
	public static void printTable(PrintWriter out, String heading, List<String> columnNames, List<List<Object>> dataRows)
	{
		printTable(out, heading, columnNames, dataRows, true);
	}
	
	public static void printTable(PrintWriter out, String heading, List<String> columnNames, List<List<Object>> dataRows, boolean centerAlignData)
	{
		out.println("<h3 " + (centerAlignData ? "align='center'": "") + ">" + heading+ "</h3>");
		out.println("<table " + (centerAlignData ? "align='center'": "")  + " border='1'>");

		out.println("<tr>");

		String[] columnNamesArray = columnNames.toArray(
				new String[0]);

		for (int i = 0; i < columnNamesArray.length; i++)
		{
			String columnName = columnNamesArray[i];
			if (!columnName.startsWith("_"))
			{
				out.println("<th rowspan='2'> " + columnName + "</th>");
			}
			else
			{
				String[] slices = columnName.split("_");
				String sectionName = slices[1];
				int j = i + 1;
				while ((j < columnNamesArray.length) && columnNamesArray[j].startsWith("_" + sectionName))
				{
					j++;
				}
				out.println("<th colspan='" + (j - i) + "'> " + sectionName + "</th>");
				i = j - 1;
			}
		}
		out.println("</tr>");
		
		out.println("");
		
		out.println("<tr>");
		for (int i = 0; i < columnNamesArray.length; i++)
		{
			String columnName = columnNamesArray[i];
			if (columnName.startsWith("_"))
			{
				String[] slices = columnName.split("_");
				String subSectionName = slices[2];
				out.println("<th> " + subSectionName + "</th>");
			}
		}
		out.println("</tr>");

		for (List<Object> row : dataRows)
		{
			out.println("<tr>");
			for (Object column : row)
			{
				out.println("<td " + (centerAlignData ? "align='center'": "") + ">" + column + "</td>");
			}
			out.println("</tr>");
		}
		out.println("</table>");
	}

		public static void printRefreshTag(PrintWriter out, int refreshInterval)
	{
		out.println("<head>");
		out.println("<meta http-equiv=\"refresh\" content=\"" + refreshInterval
				+ "\" </meta>");
		out.println("</head>");
	}
	
	public static void printTableHeaders(PrintWriter out, Object... headerNames)
	{
		printTableRow(out, "th", headerNames);
	}

	public static void printTwoColumnTable(PrintWriter out, String tableHeader, Object... values)
	{
		out.println("<h4 align='center' >" + tableHeader + "</h4>");
		if(values.length % 2 != 0)
		{
			throw new IllegalArgumentException("NumEntries in a two column table should be even");
		}
		out.println("<table align='center' >");
			for (int i = 0; i < values.length/2; i++)
			{
				printTableRow(out, "<b>" + values[i*2] + "</b>", values[i*2+1]);
			}
		out.println("</table>");
	}
	
	public static void printTableRow(PrintWriter out, Object... headerNames)
	{
		printTableRow(out, "td", headerNames);
	}
	
	private static void printTableRow(PrintWriter out, String columnHeader, Object... headerNames)
	{
		out.println("<tr>");
		for (Object header : headerNames)
		{
			out.println("<" +columnHeader +">");
				out.println(header);
			out.println("</" +columnHeader +">");
		}
		out.println("</tr>");
	}
	public static void printTableWithLinks(PrintWriter out, String heading, List<String> columnNames,boolean centerAlignData, List<List<Object>> dataRows, Map<String, String> links) {
		if((links==null) || links.isEmpty())
        {
            ConsoleUtils.printTable(out, heading, columnNames, dataRows, centerAlignData);
        }
        else {
			out.println("<h3 " + (centerAlignData ? "align='center'": "") + ">" + heading+ "</h3>");
			out.println("<table " + (centerAlignData ? "align='center'": "")  + " border='1'>");

			out.println("<tr>");

			String[] columnNamesArray = columnNames.toArray(
					new String[0]);

			for (int i = 0; i < columnNamesArray.length; i++)
			{
				String columnName = columnNamesArray[i];
				if (!columnName.startsWith("_"))
				{
					out.println("<th rowspan='2'> " + columnName + "</th>");
				}
				else
				{
					String[] slices = columnName.split("_");
					String sectionName = slices[1];
					int j = i + 1;
					while ((j < columnNamesArray.length) && columnNamesArray[j].startsWith("_" + sectionName))
					{
						j++;
					}
					out.println("<th colspan='" + (j - i) + "'> " + sectionName + "</th>");
					i = j - 1;
				}
			}
			out.println("</tr>");
			
			out.println("");
			
			out.println("<tr>");
			for (int i = 0; i < columnNamesArray.length; i++)
			{
				String columnName = columnNamesArray[i];
				if (columnName.startsWith("_"))
				{
					String[] slices = columnName.split("_");
					String subSectionName = slices[2];
					out.println("<th> " + subSectionName + "</th>");
				}
			}
			out.println("</tr>");
			
			for (List<Object> row : dataRows)
			{	
				int index=0;
				out.println("<tr>");
				for (Object column : row)
				{
					if(links.keySet().contains(columnNamesArray[index]))
                    {
                        out.println("<td " + (centerAlignData ? "align='center'": "") + "><a target='_blank' href=\"" + links.get(columnNamesArray[index])+"?"+columnNamesArray[index]+"="+column + "\">"+column+"</a></td>");
                    }
                    else
                    {
                        out.println("<td " + (centerAlignData ? "align='center'": "") + ">" + column + "</td>");
                    }
				index++;
				}
				out.println("</tr>");
			}
			out.println("</table>");
		}
		
	}
}
