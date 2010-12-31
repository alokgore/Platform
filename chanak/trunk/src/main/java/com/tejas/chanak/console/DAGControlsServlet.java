package com.tejas.chanak.console;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import com.tejas.chanak.core.DAGManager;
import com.tejas.chanak.types.DAGStatus;
import com.tejas.core.TejasContext;
import com.tejas.utils.console.ConsoleUtils;
import com.tejas.utils.console.TejasServletBase;

public class DAGControlsServlet extends TejasServletBase
{
	@Override
    protected void execute(HttpServletRequest request, PrintWriter out) throws Throwable
	{
		String did = request.getParameter("did");
		String servletURL = ConsoleUtils.getServletURL(DAGOperationsServlet.class);
		servletURL = servletURL + "?did=" + did + "&operation=";
		
		out.println("<html>");
		out.println("<head>");
		out.println("<script language='javascript'>");
		
		out.println("function clearDAG()");
		out.println("{");
		out.println("	if(confirm(\"This will remove the DAG from DB. Are you sure ?\"))");
		out.println("	{");
		out.println("		parent.mainFrame.location = '" + servletURL  +  "clear"+ "' ;");
		out.println("	}");
		out.println("}");
		
		
		out.println("function suspendDAG()");
		out.println("{");
		out.println("	if(confirm('This will suspend the DAG Execution. Are you sure?'))");
		out.println("	{");
		out.println("		parent.mainFrame.location = '" + servletURL  +  "suspend"+ "' ;");
		out.println("	}");
		out.println("}");
		
		out.println("function resumeDAG()");
		out.println("{");
		out.println("	if(confirm('This will resume the DAG Execution. \\nMe hoping that you have verified the dirty-little-fixes that you have made and ready for the launch. \\nAre you sure?'))");
		out.println("	{");
		out.println("		parent.mainFrame.location = '" + servletURL  +  "resume"+ "' ;");
		out.println("	}");
		out.println("}");
		
		
		out.println("</script>");
		out.println("</head>");
		
		
		out.println("<body  bgcolor='FFFFFF'>");
		out.println("<table border='0'>");
		
		out.println("<tr><th>");
		out.println("<table>");
		out.println("<tr><th align='center'>Operations</th></tr>");
		out.println("<tr><th>" + "<a href='javascript:clearDAG()'><font color='red'>Clear DAG</font></a>" + "</th></tr>");

		DAGStatus dagStatus = DAGManager.getQueryStatus(new TejasContext(), did);
		if(dagStatus == DAGStatus.Dormant)
		{
			out.println("<tr><th>" + "<a href='javascript:resumeDAG()'><font color='green'>Resume DAG</font></a>" + "</th></tr>");
		}
		
		if (dagStatus == DAGStatus.InProgress)
		{
			out.println("<tr><th>" + "<a href='javascript:suspendDAG()'><font color='brown'>Suspend DAG</font></a>" + "</th></tr>");
		}

		out.println("</table>");
		out.println("</th></tr>");
		
		out.println("</table>");
		out.println("</body>");
		out.println("</html>");
		out.flush();
	}
}
