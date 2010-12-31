package com.tejas.chanak.console;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;

import com.tejas.chanak.types.orm.ContractDetails;
import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasContext;
import com.tejas.utils.console.TejasServletBase;

public class ContractDisplayServlet extends TejasServletBase
{
	@Override
    protected void execute(HttpServletRequest request, PrintWriter out) throws Throwable
	{
		String action = request.getParameter("action");
		if(action!=null && action.equals("save"))
		{
		    throw new UnsupportedOperationException("'save' is not supported on contracts now");
		}
		
		TejasContext self = new TejasContext();
        ContractDetails contractDetails = DAGManagerConsoleUtils.selectContract(self, request.getParameter("cid"));
		
		String data = contractDetails.contract;
		String editStr = request.getParameter("edit");
		boolean edit = editStr != null && editStr.equals("true");
        int numRows = 0;
        for (int i = 0; i < data.length(); i++)
        {
            if(data.charAt(i) == '\n')
            {
                numRows++;
            }
        }
        
        out.println("<html>");
		out.println("<body bgcolor='FFFFFF'>");
		
		String servletURL = "/" + ApplicationConfig.getWebappName() + "/servlet/" + ContractDisplayServlet.class.getCanonicalName();
		out.println("<FORM ACTION=" + servletURL + " method='get'>");
		out.println("<INPUT TYPE='hidden' NAME='edit' VALUE='" + edit +"'>");
		out.println("<INPUT TYPE='hidden' NAME='cid' VALUE='" + StringEscapeUtils.escapeHtml(request.getParameter("cid")) + "'>");
		out.println("<INPUT TYPE='hidden' NAME='action' VALUE=''>");
		
		out.println("<table border='1'>");
		out.println("<tr><td>");
		if(!edit)
		{
			out.println("<pre>" + data + "</pre>");
			out.println("<input type='button' value='edit' onClick=document.forms[0].edit.value='true',document.forms[0].submit() >");
		}
		else
		{
			out.println("<textarea name='data' rows='" + numRows + "' cols='200'>" + data + "</textarea>");
			out.println("<input type='button' value='save' onClick=document.forms[0].edit.value='false',document.forms[0].action.value='save',document.forms[0].submit() >");
		}			
		out.println("</tr></td>");
		out.println("</table>");
		out.println("</body>");
		out.println("</html>");
		out.flush();
	}
}
