package com.tejas.utils.console.system;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TopServlet extends CommandInvokerServlet
{
	@Override
	public final void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String[] setenv = { "/bin/zsh", "setenv COLUMNS 400" };
		String cmd = "/usr/bin/top bcn 1";
		invokeCommand(response, setenv, cmd, false);
	}
}
