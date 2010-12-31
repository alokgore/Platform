package com.tejas.chanak.console;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import com.tejas.config.ApplicationConfig;
import com.tejas.utils.console.TejasServletBase;

@SuppressWarnings("serial")
public class DAGSummaryServlet extends TejasServletBase
{
	@Override
    protected void execute(HttpServletRequest request, PrintWriter out) throws Throwable
	{
		out.println("<html>");
			out.println("<frameset cols='14%,*'>");
				out.println("<frameset rows='40%,*'>");
					String servletURL = "/" + ApplicationConfig.getWebappName() + "/servlet/" + DAGDetailsServlet.class.getCanonicalName();
					out.println("<frame src='" + servletURL + "?did=" + request.getParameter("did") + "'/>");
					servletURL = "/" + ApplicationConfig.getWebappName() + "/servlet/" + DAGControlsServlet.class.getCanonicalName();
					out.println("<frame src='" + servletURL + "?did=" + request.getParameter("did") + "'/>");
				out.println("</frameset>");
				out.println("<frame name='mainFrame'  src=''/>");
			out.println("</frameset>");
		out.println("</html>");
	}
}
