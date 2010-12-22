package com.tejas.utils.console.system;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tejas.utils.console.ConsoleUtils;
import com.tejas.utils.misc.StringUtils;


public class ThreadStatusServletBase extends HttpServlet
{
	protected final void printThreadDetails(HttpServletRequest request, HttpServletResponse response, String tableName, boolean printStackTrace) 
	throws IOException
	{
		String nameFilter = StringUtils.makeNullSafe(request.getParameter("name"));
		String traceFilter = StringUtils.makeNullSafe(request.getParameter("trace"));
		
		ThreadGroup next = Thread.currentThread().getThreadGroup();
		ThreadGroup top = next; 
	    while (next != null) 
	    {
	    	top = next; 
	    	next = next.getParent(); 
	    }
	    Thread[] threadlist = new Thread[top.activeCount()];
	    top.enumerate(threadlist);
	    
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("<body>");
		out.println("<h3 align='left'>" + new Date()+ "</h3>");
		
		List<String> headers = new ArrayList<String>();
		headers.add("Thread-id");
		headers.add("Thread-name");
		if(printStackTrace)
		{
			headers.add("Current Stack Trace");
		}
		
		List<List<Object>> data = new ArrayList<List<Object>>();
		for (Thread thread : threadlist)
		{
			ArrayList<Object> row = new ArrayList<Object>();
			
			row.add(thread.getId());
			String threadName = thread.getName();
			
			if(threadName.contains(nameFilter) == false)
			{
				continue;
			}
			
			row.add(threadName);
			if (printStackTrace)
			{
				String stackTrace = StringUtils.join(thread.getStackTrace(), "\n");
				if(stackTrace.contains(traceFilter) == false)
				{
					continue;
				}
				row.add(stackTrace);
			}
		
			data.add(row);
		}
		
		ConsoleUtils.printTable(out, tableName, headers, data);
		
		out.println("</body>");
		out.println("</html>");
		out.close();
	}
}
