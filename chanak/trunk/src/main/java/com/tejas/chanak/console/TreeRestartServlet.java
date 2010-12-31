package com.tejas.chanak.console;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.tejas.chanak.types.orm.ContractSummary;
import com.tejas.core.TejasContext;
import com.tejas.types.exceptions.DBLayerException;
import com.tejas.utils.console.ConsoleUtils;
import com.tejas.utils.console.TejasServletBase;

@SuppressWarnings("serial")
public class TreeRestartServlet extends TejasServletBase
{
	private static final String ANCESTORS = "ANCESTORS";
	private static final String FIRE = "restartChain";

	public static class Contract
	{
		public final String contract_id;
		public final String description;
		
		public Contract(final String contractID, final String description)
		{
			this.contract_id = contractID;
			this.description = description;
		}
		
		@Override
        public int hashCode()
		{
			return contract_id.hashCode();
		}
		
		@Override
        public boolean equals(Object obj)
		{
			if (obj instanceof Contract)
			{
				Contract contract = (Contract) obj;
				return contract.contract_id.equals(this.contract_id);
			}
			if(obj instanceof String)
			{
				return contract_id.equals(obj.toString());
			}
			return false;
		}
	}

	@Override
    protected void execute(HttpServletRequest request, PrintWriter out) throws Throwable
	{
		out.println("<html>");
		out.println("<body>");

		if(request.getParameter(FIRE) != null)
		{
			restartTree(request, out);
			return;
		}

		String cid = request.getParameter("cid");
		Set<ContractSummary> ancestors = getAllAncestors(cid);
		
        String servletURL = ConsoleUtils.getServletURL(TreeRestartServlet.class);
        out.println("<FORM ACTION=" + servletURL + " >");

        out.println("<input type='hidden' id='cid' name='cid' value='" + cid + "'>");
        
        out.print("<table>");
        	printAncestorList(out, ancestors);
        printTheREDButton(out);
		out.print("</table>");
		
		out.println("</body>");
		out.println("</html>");
	}

	private void printAncestorList(PrintWriter out, Set<ContractSummary> ancestors)
	{
		out.println("<table align='center' border=1>");
		for (ContractSummary contract : ancestors)
		{
			out.print("<tr>");
				out.println("<td><INPUT TYPE='checkbox' NAME='" + ANCESTORS + "' VALUE='" + contract.contract_id + "'> </td>");
				String url = ConsoleUtils.getServletURL(ContractDetailsServlet.class) + "?cid=" + contract.contract_id;
				out.println("<td> <a href='" + url  + "' >" + contract.contract_id + "</a></td>");
				out.println("<td>"+ contract.description + "</td>");
			out.print("</tr>");
		}
		out.println("</table>");
	}

	private void restartTree(HttpServletRequest request, PrintWriter out) throws DBLayerException
	{
		out.println("<html>");
		out.println("<body>");

		TejasContext self = new TejasContext();

		String cid = request.getParameter("cid");
		String[] ancestors = request.getParameterValues(ANCESTORS);
		
		List<String> contractsToBeRestarted = new ArrayList<String>();
		contractsToBeRestarted.add(cid);
		contractsToBeRestarted.addAll(Arrays.asList(ancestors));

		self.logger.info("Going to restart the chain " + contractsToBeRestarted);
		
		self.dbl.startTransaction();
		try
		{
			for (String contract : contractsToBeRestarted)
			{
				List<ContractSummary> immediateAncestors = getImmediateAncestors(self, contract);
				List<String> parentsInTheChain = getIntersection(contractsToBeRestarted, immediateAncestors);
				
				if(parentsInTheChain.size()==0)
				{
					self.logger.info("Marking " + contract + " READY AGAIN");
					DAGManagerConsoleUtils.restartContract(self, contract);
				}
				else
				{
					self.logger.info("Marking " + contract + " as WAITING");
                    DAGManagerConsoleUtils.markContractWaiting(self, contract);
					
					for (String parent : parentsInTheChain)
					{
						self.logger.info("Resetting the dependency of " + contract + " on " + parent);
	                    DAGManagerConsoleUtils.resetDependency(self, contract, parent);
					}
				}
			}
			self.dbl.commit();
		}
		finally
		{
			self.dbl.rollback();
		}
		
		out.println("<h2>Hail Mogambo ! The tree has been restarted Best of luck !</h2>");
		
		out.println("</body>");
		out.println("</html>");
	}

	private List<String> getIntersection(List<String> contractsToBeRestarted, List<ContractSummary> immediateAncestors)
	{
		Set<String> set = new HashSet<String>();
		for (ContractSummary contract : immediateAncestors)
		{
			set.add(contract.contract_id);
		}
		List<String> response = new ArrayList<String>();
		for (String contract : contractsToBeRestarted)
		{
			if(set.contains(contract))
				response.add(contract);
		}
		return response;
	}

	private void printTheREDButton(PrintWriter out)
	{
		out.print("<tr>");
		out.print("<th colspan=2>");
		out.println("<INPUT name='" + FIRE + "' TYPE='submit' VALUE='RE-RUN THE SELECTED CONTRACTS (If you do not know what this means, you do not want this)' onClick=document.forms[0].submit() />");
		out.print("</th>");
		out.print("</tr>");
	}
	
	public static Set<ContractSummary> getAllAncestors(String cid) throws DBLayerException
	{
		HashSet<ContractSummary> response = new HashSet<ContractSummary>();
		
		LinkedList<String> bfsQueue = new LinkedList<String>();
		bfsQueue.add(cid);
		
		while(!bfsQueue.isEmpty())
		{
			String contract = bfsQueue.remove();
			List<ContractSummary> immediateAncestors = getImmediateAncestors(new TejasContext(), contract);
			for (ContractSummary ancestor : immediateAncestors)
			{
				if(response.contains(ancestor) == false)
				{
					response.add(ancestor);
					bfsQueue.add(ancestor.contract_id);
				}
			}
		}

		return response;
	}

	private static List<ContractSummary> getImmediateAncestors(TejasContext self, String cid) throws DBLayerException
	{
	    return DAGManagerConsoleUtils.selectAllPrerequisites(self, cid);
	}
}
