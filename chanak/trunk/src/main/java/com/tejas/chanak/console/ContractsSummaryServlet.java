package com.tejas.chanak.console;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;

import com.tejas.chanak.types.DAGContract.CompletionStatus;
import com.tejas.chanak.types.orm.ContractDetails;
import com.tejas.core.TejasContext;
import com.tejas.utils.console.ConsoleUtils;
import com.tejas.utils.console.TejasServletBase;
import com.tejas.utils.misc.StringUtils;

@SuppressWarnings("serial")
public class ContractsSummaryServlet extends TejasServletBase
{
	@Override
    protected void execute(HttpServletRequest request, PrintWriter out) throws Throwable
	{
		out.println("<html>");
		String refreshInterval = StringEscapeUtils.escapeHtml(request.getParameter("refreshInterval"));
		out.println("<head><meta http-equiv='refresh' content='" + refreshInterval + "'></head>");
		out.println("<body>");
		out.println("<h4 align='center'>" + new Date()+ "</h4>");
		
		String servletURL = ConsoleUtils.getServletURL(ContractsSummaryServlet.class);
		out.println("<FORM ACTION=" + servletURL + " method='get'>");
		out.println("<INPUT TYPE='text' NAME='refreshInterval' VALUE='" + refreshInterval + "'> refreshInterval (in seconds)");
		out.println("<INPUT TYPE='hidden' NAME='did' VALUE='" + StringEscapeUtils.escapeHtml(request.getParameter("did")) + "'>");
		out.println("<INPUT TYPE='submit' VALUE='Refresh' onClick=document.forms[0].submit()>");
		out.println("<INPUT TYPE='hidden' NAME='statusChoice' VALUE='" + StringEscapeUtils.escapeHtml(request.getParameter("statusChoice")) + "'>");
		List<String> headers = new ArrayList<String>();
		headers.add("ContractID");
		headers.add("Restarts");
		headers.add("Execution Time");
		headers.add("Status");
		headers.add("Description");
		
		String dagID = request.getParameter("did");
        CompletionStatus statusChoice = CompletionStatus.valueOf(request.getParameter("statusChoice"));
        List<ContractDetails> contracts = DAGManagerConsoleUtils.selectContracts(new TejasContext(), dagID, statusChoice);
		
		List<List<Object>> data = new ArrayList<List<Object>>();
		for (ContractDetails contract : contracts)
		{
			List<Object> row = new ArrayList<Object>();

			servletURL = ConsoleUtils.getServletURL(ContractDetailsServlet.class);
			String hrefcid = "<a href=" + servletURL + "?cid=" + contract.contract_id + ">" + contract.contract_id + "</a>";
			row.add(hrefcid);
			
			row.add(contract.num_restarts);
			long executionTime = contract.status.equals(CompletionStatus.Complete) ? contract.last_updated.getTime()- contract.start_time.getTime() : System.currentTimeMillis() - contract.start_time.getTime();
			row.add(StringUtils.millisToPrintableString(executionTime));
			row.add(contract.status);
			row.add(contract.description);
			
			data.add(row);
		}
		
		ConsoleUtils.printTable(out, "Contracts Summary", headers, data);
		out.println("</FORM>");
		out.println("</body>");
		out.println("</html>");
		out.flush();
	}
}
