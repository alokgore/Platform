package com.tejas.utils.console.system;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tejas.config.ApplicationConfig;
import com.tejas.utils.console.ConsoleUtils;


public class ListLogsServlet extends HttpServlet
{
	@Override
	@SuppressWarnings("unchecked")
	public final void doGet(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException
	{
		List<String> logList = (List<String>)ApplicationConfig.findObject("log.console.file.list");
		ListIterator<String> iterator = logList.listIterator();
		PrintWriter out = response.getWriter();
		out.println("<html>");
			out.println("<body>");
			out.println("<h4>" + "Log Files" + "</h4>");
				out.println("<table>");
				while (iterator.hasNext())
				{
					String logFile = iterator.next();
					out.println("<tr>");
						out.println("<td>");
							out.println(logFile);
						out.println("</td>");
						out.println("<td>");
							out.println("<a target='mainFrame' href='" + ConsoleUtils.getServletURL(TailServlet.class) +
									"?filename=" + logFile + "&logtype=Full'>" + "Full");
							out.println("</a>");
						out.println("</td>");
						out.println("<td>");
						out.println("<a target='mainFrame' href='" + ConsoleUtils.getServletURL(TailServlet.class) +
								"?filename=" + logFile + "&logtype=Tail'>" + "Tail");
						out.println("</a>");
					out.println("</td>");
					out.println("</tr>");
				}
				out.println("</table>");
			out.println("</body>");
		out.println("</html>");
		out.close();
	}
}
