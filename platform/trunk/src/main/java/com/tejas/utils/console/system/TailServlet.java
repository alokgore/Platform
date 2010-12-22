package com.tejas.utils.console.system;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

public class TailServlet extends HttpServlet
{
	public static final int TTL_SECS_DEFAULT = 15;
	private static long fos = 0;
	private final int ttl_ = TTL_SECS_DEFAULT;	
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException 
	{
		String logFile = request.getParameter("filename");
		String logType = request.getParameter("logtype");
		
		if (logFile == null)
			return;
		
		if (logType.equals("Full"))
		{
			printFullLog(response, logFile);
		}
		else
		{
			BufferedReader br = new BufferedReader(new FileReader(logFile));
			PrintWriter out = response.getWriter();
			response.setContentType("text/html");
			out.println("<pre>");
			out.flush();
			blocking(br, out);
			out.println("</pre><b><a href=\"\" onClick='window.location.reload(false);'>click to refresh</a></b>");
			out.flush();
			out.close();
		}
	}

	private void printFullLog(HttpServletResponse response, String logFile)
	throws FileNotFoundException, IOException
	{
		response.setContentType("text/html");
		BufferedReader br = new BufferedReader(new FileReader(logFile));
		String nextLine = br.readLine();
		PrintWriter out = response.getWriter();
		out.println("<pre>");
		while (nextLine != null)
		{
			out.println(StringEscapeUtils.escapeHtml(nextLine));
			nextLine = br.readLine();
		}
		br.close();
		out.flush();
		out.close();
	}
	
	private void blocking(BufferedReader br, PrintWriter out) throws IOException
	{
		br.skip(fos);
		long begin = System.currentTimeMillis();
		while ((System.currentTimeMillis() - begin) < ttl_ * 5000) 
		{
			String nextLine = br.readLine();
			while (nextLine != null)
			{
				out.println(StringEscapeUtils.escapeHtml(nextLine));
				fos = fos + nextLine.length() + 1;
				nextLine = br.readLine();
				out.flush();
			}
			try 
			{
				Thread.sleep(100);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		} // only go for 5 minutes
	}
}

