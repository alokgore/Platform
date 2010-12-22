package com.tejas.utils.console.system;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class IoStatServlet extends CommandInvokerServlet
{
	protected void printIostatTrace(HttpServletResponse response) throws IOException
	{
		String[] setenv = { "/bin/zsh", "setenv COLUMNS 400" };
		String cmd = "/usr/bin/iostat";
		invokeCommand(response, setenv, cmd, true);
	}

	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		printIostatTrace(response);
	}
}
