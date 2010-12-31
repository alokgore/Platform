package com.tejas.chanak.console;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.tejas.chanak.types.DAGContract.CompletionStatus;
import com.tejas.chanak.types.LogEntry;
import com.tejas.chanak.types.orm.ContractDetails;
import com.tejas.chanak.types.orm.ContractSummary;
import com.tejas.core.TejasContext;
import com.tejas.utils.console.ConsoleUtils;
import com.tejas.utils.console.TejasServletBase;
import com.tejas.utils.misc.StringUtils;

@SuppressWarnings("serial")
public class ContractDetailsServlet extends TejasServletBase
{	
	@Override
    protected void execute(HttpServletRequest request, PrintWriter out) throws Throwable
	{
	    TejasContext self = new TejasContext();
	    
		ContractDetails contractDetails = DAGManagerConsoleUtils.selectContract(self, request.getParameter("cid"));

		out.println("<html>");
		
		String servletURL = ConsoleUtils.getServletURL(ContractOperationsServlet.class);
		String theRedServletURL = ConsoleUtils.getServletURL(TreeRestartServlet.class);
		
		out.println("<head>");
			out.println("<script language='javascript'>");
				
				out.println("function forceComplete()");
				out.println("{");
				out.println("	if(confirm(\"This will fake job-success. \\nDependent contracts may fail if they depend on the output of this contract.\\n Are you sure ?\"))");
				out.println("	{");
				out.println("		self.location = '" + servletURL + "?operation=forceComplete&cid=" + contractDetails.contract_id + "';");
				out.println("	}");
				out.println("}");
				
				out.println("function suspend()");
				out.println("{");
				out.println("	if(confirm('The future runs of this contract will be suspended.(The currently running instance of the contract will, obviously, be left untouched). \\nAre you sure you want to do this?'))");
				out.println("	{");
				out.println("		self.location = '" + servletURL + "?operation=suspend&cid=" + contractDetails.contract_id + "';");
				out.println("	}");
				out.println("}");
				
				out.println("function restartTree()");
				out.println("{");
				out.println("	if(confirm('This step is SO NOT recommended, that I am not even going to tell ya what it does. Do you want to continue anyway?'))");
				out.println("	{");
				out.println("		self.location = '" + theRedServletURL + "?cid=" + contractDetails.contract_id + "';");
				out.println("	}");
				out.println("}");
				
				out.println("function resume()");
				out.println("{");
				out.println("	if(confirm('This contract will be resumed.\\nYou might want to double check the dirty-little-fix that you did just now before you resume. \\nAre you sure you want to do this?'))");
				out.println("	{");
				out.println("		self.location = '" + servletURL + "?operation=resume&cid=" + contractDetails.contract_id + "';");
				out.println("	}");
				out.println("}");
				
				out.println("function restartNow()");
				out.println("{");
				out.println("	self.location = '" + servletURL + "?operation=restartNow&cid=" + contractDetails.contract_id + "';");
				out.println("}");
				
				out.println("function refresh()");
				out.println("{");
				out.println("	self.location = '" + servletURL + "?operation=refresh&cid=" + contractDetails.contract_id + "';");
				out.println("}");
				
				
			out.println("</script>");
		out.println("</head>");
		
		out.println("<body>");
		
		
		printHeader(out, contractDetails);
		
		//Print Contract details
		out.println("<ul>");
			String queryUrl = "<a href='" + ConsoleUtils.getServletURL(DAGSummaryServlet.class) + "?did=" + contractDetails.dag_id + "' >" + contractDetails.dag_id + "</a>";
			printBullet(out, "DAG-ID", queryUrl);
			printBullet(out, "ContractID", contractDetails.contract_id);
			printBullet(out, "Description", contractDetails.description);
			printBullet(out, "Status", contractDetails.status);
			printBullet(out, "Start-Time", contractDetails.start_time);
			printBullet(out, "Last-updated", contractDetails.last_updated);
			
			long executionTime = contractDetails.status==CompletionStatus.Complete ? contractDetails.last_updated.getTime() - contractDetails.start_time.getTime()
			        : System.currentTimeMillis() - contractDetails.start_time.getTime();
			printBullet(out, "Execution Time", StringUtils.millisToPrintableString(executionTime));

			if(contractDetails.status.equals(CompletionStatus.InProgress) && contractDetails.retry)
			{
				long timeRemainingForRestart = (contractDetails.next_retry_time.getTime() - System.currentTimeMillis())/1000;
				timeRemainingForRestart = timeRemainingForRestart < 0 ? 0 : timeRemainingForRestart;
				printBullet(out, "<font color='red'>" + "Next-retry time</font>", "<font color='red'>" 
						+ contractDetails.next_retry_time+ " (" + timeRemainingForRestart + " seconds from now) " +
						"</font>");
			}
		out.println("</ul>");

		
		out.println("<table align='center'><tr cellspacing=10>");
		out.println("<th><a href='javascript:refresh()'>Refresh</a></th>");
		
		if(contractDetails.status == CompletionStatus.Dormant)
		{
			out.println("<th><a href='javascript:resume()'>Resume</a></th>");
		}
		
		if(contractDetails.status == CompletionStatus.InProgress)
		{
			out.println("<th><a href='javascript:suspend()'>Suspend</a></th>");
		}

		if(contractDetails.status == CompletionStatus.InProgress || contractDetails.status == CompletionStatus.Dormant)
		{
			out.println("<th><a href='javascript:forceComplete()'><font color='red'>Force Completion</font></a></th>");
		}
		
		if(contractDetails.status  == CompletionStatus.InProgress && contractDetails.retry)
		{
			out.println("<th><a href='javascript:restartNow()'>Restart Now</a></th>");
		}
		
		out.println("</tr>");

		out.println("<tr>");
		out.println("<th align='center'><a href='javascript:restartTree()'><font color='red'>Restart Ancestor Contracts</font></a></th>");
		out.println("</tr></table>");
		
		//Print Log Entries
		List<String> headers = new ArrayList<String>();
		headers.add("TimeStamp");
		headers.add("Log");
		headers.add("Error_reason");
		headers.add("Detailed description");
		List<List<Object>> data = new ArrayList<List<Object>>();
		
		for (LogEntry logEntry : contractDetails.logs)
        {
			List<Object> row = new ArrayList<Object>();
			row.add(logEntry.log_time);
			row.add(logEntry.log_message);
			row.add(StringUtils.makeNullSafe(logEntry.error_reason));
			row.add(StringUtils.makeNullSafe(logEntry.detailed_description));

			data.add(row);
		}
		if (data.size() != 0)
		{
			ConsoleUtils.printTable(out, "", headers, data);
		}
		
		printTable(out, contractDetails.completedPrerequisites, "Completed Prerequisites");
		
		printTable(out, contractDetails.pendingPrerequisites, "Pending Prerequisites");
		
		printTable(out, contractDetails.dependents, "Dependents");
		
		out.println("</body>");
		out.println("</html>");
	}

	private void printHeader(PrintWriter out, ContractDetails contractDetails)
	{
		out.println("<table align='center'> <tr>");
		
			out.println("<th>");
					String xmlServletURL = ConsoleUtils.getServletURL(ContractDisplayServlet.class);
					xmlServletURL = xmlServletURL + "?cid=" + contractDetails.contract_id;
					out.println("<a href=\"javascript:window.open('" + xmlServletURL + "')\">");
					out.println("[XML Dump]");
					out.println("</a>");
			out.println("</th>");

		out.println("</tr></table>");
	}
	
	private void printTable(PrintWriter out, List<ContractSummary> contracts,  String hdr1)
	{
		List<String> headers = null;
		
		for (ContractSummary contract : contracts)
        {
			List<Object> row = new ArrayList<Object>();
			String cid = (String)row.get(0);
			String servletURL = ConsoleUtils.getServletURL(ContractDetailsServlet.class);
			String hrefcid = "<a href=" + servletURL + "?cid=" + cid + ">" + cid + "</a>";
			row.add(hrefcid);
			row.add(contract.description);
		}
		
		if (contracts.size() != 0)
		{
			headers = new ArrayList<String>();
			headers.add(hdr1);
			headers.add("description");
			List<List<Object>> tableData = new ArrayList<List<Object>>();
            ConsoleUtils.printTable(out, "", headers, tableData);
		}
	}
	
	private void printBullet(PrintWriter out, String name, Object value)
	{
		out.println("<li>");
			out.println("<b>" + name + " : </b>" + value);
		out.println("</li>");
	}
}
