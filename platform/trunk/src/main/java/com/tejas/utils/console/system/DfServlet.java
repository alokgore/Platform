package com.tejas.utils.console.system;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class DfServlet extends CommandInvokerServlet
{
	public void printDfTrace(HttpServletResponse response) throws IOException
	{
		String[] setenv = { "/bin/zsh", "setenv COLUMNS 400" };
		String cmd = "/bin/df -h";
		invokeCommand(response, setenv, cmd, true);
	}

	@Override
	public final void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		printDfTrace(response);
	}
}
