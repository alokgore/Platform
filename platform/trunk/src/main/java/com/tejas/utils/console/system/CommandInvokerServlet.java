package com.tejas.utils.console.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

/*
 * XXX: Make this less open by restricting the set of commands it can take to an enum
 */
public class CommandInvokerServlet extends HttpServlet
{
	protected void invokeCommand(HttpServletResponse response, String[] setenv, String cmd, boolean block) throws IOException
	{
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("<body>");
		out.println("<pre>");
		out.println("<h2 align='left'>" + new Date() + "</h2>");
		String s = null;

		try
		{
			Process p = Runtime.getRuntime().exec(setenv);
			p = Runtime.getRuntime().exec(cmd);
			int i = 0;
			if (block)
			{
				i = p.waitFor();
			}
			if (i == 0)
			{
				BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
				// read the output from the command
				while ((s = stdInput.readLine()) != null)
				{
					out.println(s + "<br>");
				}
			}
			else
			{
				BufferedReader stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				// read the output from the command
				while ((s = stdErr.readLine()) != null)
				{
					out.println("<font color='red'>" + s + "<br>");
				}
			}
		}
		catch (Exception e)
		{
			out.println(e);
		}
		out.println("</pre>");
		out.println("</body>");
		out.println("</html>");
		out.close();
	}
}
