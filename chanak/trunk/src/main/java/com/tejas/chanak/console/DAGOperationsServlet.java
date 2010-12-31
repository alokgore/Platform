package com.tejas.chanak.console;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import com.tejas.chanak.core.DAGManager;
import com.tejas.core.TejasContext;
import com.tejas.utils.console.TejasServletBase;

public class DAGOperationsServlet extends TejasServletBase
{
	@Override
    protected void execute(HttpServletRequest request, PrintWriter out) throws Throwable
	{
		String queryID= request.getParameter("did");
		String operation = request.getParameter("operation");
		if (operation.equals("clear"))
		{
			DAGManager.cleanupDAG(new TejasContext(), queryID);
		}
		
		if (operation.equals("suspend"))
		{
			DAGManager.suspendDAG(new TejasContext(), queryID);
		}
		
		if (operation.equals("resume"))
		{
			DAGManager.resumeDAG(new TejasContext(), queryID);
		}
		
		out.println("DONE");
	}
}
