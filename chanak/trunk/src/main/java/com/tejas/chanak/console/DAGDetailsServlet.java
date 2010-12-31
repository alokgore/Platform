package com.tejas.chanak.console;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import com.tejas.chanak.types.orm.DAGDetails;
import com.tejas.chanak.types.orm.StatusCount;
import com.tejas.core.TejasContext;
import com.tejas.utils.console.ConsoleUtils;
import com.tejas.utils.console.TejasServletBase;

public class DAGDetailsServlet extends TejasServletBase
{
	@Override
    protected void execute(HttpServletRequest request, PrintWriter out) throws Throwable
	{
	    TejasContext self = new TejasContext();
        DAGDetails dagDetails = DAGManagerConsoleUtils.selectDAGDetails(self, request.getParameter("did"));
        
		String did = request.getParameter("did");
		out.println("<html>");
		out.println("<head><meta http-equiv='refresh' content='5'></head>");
		out.println("<body  bgcolor='FFFFFF'>");
		out.println("<table border='0'>");
		out.println("<tr> <th align='center'>Status</th></tr>");
		
		for (StatusCount statusCount : dagDetails.getStatusCounts())
        {
		    addRowEntry(out, did, statusCount);
        }

		out.println("</table>");
		out.println("</body>");
		out.println("</html>");
	}
	
	private void addRowEntry(PrintWriter out, String did, StatusCount statusCount)
	{
	    String servletURL = ConsoleUtils.getServletURL(ContractsSummaryServlet.class);
		out.println("<tr>");
		out.println("<th> <a href=" + servletURL + "?refreshInterval=60&did=" + did + "&statusChoice=" + statusCount.status 
		        + " target=\"mainFrame\">" + statusCount.status + "</a></th>");
		out.println("<th>" + statusCount.count + "</th>");
		out.println("</tr>");
	}
}