package com.tejas.chanak.console;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.tejas.chanak.types.orm.DAGDetails;
import com.tejas.chanak.types.orm.StatusCount;
import com.tejas.core.TejasContext;
import com.tejas.utils.console.ConsoleUtils;
import com.tejas.utils.console.TejasServletBase;

@SuppressWarnings("serial")
public class ListDAGsServlet extends TejasServletBase
{
    @Override
    protected void execute(HttpServletRequest request, PrintWriter out) throws Throwable
    {
        out.println("<html>");
        out.println("<body>");
        out.println("<h4 align='center'>" + new Date() + "</h4>");
        
        List<String> headers = new ArrayList<String>();
        headers.add("DAG-ID");
        headers.add("Start-Time");
        headers.add("End-Time");
        headers.add("Execution Time");
        headers.add("Status");
        headers.add("Details");
        headers.add("Description");
        
        List<List<Object>> graphs = new ArrayList<List<Object>>();
        
        List<DAGDetails> dags = DAGManagerConsoleUtils.getDAGDetails(new TejasContext());
        
        for (DAGDetails dag : dags)
        {
            List<Object> row = new ArrayList<Object>();
            
            String servletURL = ConsoleUtils.getServletURL(DAGSummaryServlet.class);
            String didHref = "<a href=" + servletURL + "?did=" + dag.summary.dag_id + ">" + dag.summary.dag_id + "</a>";
            row.add(didHref);
            
            row.add(dag.summary.start_time);
            row.add(dag.summary.end_time);
            row.add(dag.getPrintableExecutionTime());
            row.add(dag.summary.status);
            
            StringBuffer status = new StringBuffer();
            status.append("<table border='1' align='center'>");
            for (StatusCount statusCount : dag.getStatusCounts())
            {
                status.append("<tr><td>");
                status.append(statusCount.status);
                status.append("</td><td>");
                status.append(statusCount.count);
                status.append("</td></tr>");
            }
            status.append("</table>");
            row.add(status.toString());
            
            row.add(dag.summary.description);
            
            graphs.add(row);
        }
        
        if (graphs.size() > 0)
        {
            ConsoleUtils.printTable(out, "Dependency Graphs", headers, graphs);
        }
        
        out.println("</body>");
        out.println("</html>");
    }
}
