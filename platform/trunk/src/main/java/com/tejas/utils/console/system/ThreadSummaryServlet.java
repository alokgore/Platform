package com.tejas.utils.console.system;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ThreadSummaryServlet extends ThreadStatusServletBase
{
	@Override
	public final void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		printThreadDetails(request, response, "Threads List", false);
	}
}
