package com.tejas.utils.console.system;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PsServlet extends CommandInvokerServlet
{
	protected void printPsTrace(HttpServletResponse response) throws IOException
	{
		String[] setenv = { "/bin/zsh", "setenv COLUMNS 400" };
		String cmd = "ps -eo pid,user,pcpu,pmem,args --sort=-pcpu,-pmem";
		invokeCommand(response, setenv, cmd, false);
	}

	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		printPsTrace(response);
	}
}
