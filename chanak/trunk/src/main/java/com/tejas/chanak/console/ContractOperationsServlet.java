package com.tejas.chanak.console;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tejas.chanak.types.ContractExecutionReport;
import com.tejas.chanak.types.DAGContract;
import com.tejas.core.TejasContext;
import com.tejas.utils.console.ConsoleUtils;
import com.tejas.utils.misc.StringUtils;

public class ContractOperationsServlet extends HttpServlet
{
    
    public static enum ContractOperations
    {
            forceComplete,
            suspend,
            resume,
            refresh,
            restartNow, ;
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        PrintWriter out = new PrintWriter(response.getOutputStream());
        try
        {
            execute(request);
            response.sendRedirect(ConsoleUtils.getServletURL(ContractDetailsServlet.class) + "?cid=" + request.getParameter("cid"));
        }
        catch (Throwable t)
        {
            out.println(StringUtils.serializeToString(t));
        }
        finally
        {
            out.flush();
            out.close();
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        doGet(request, response);
    }
    
    protected void execute(HttpServletRequest request) throws Throwable
    {
        String contractID = request.getParameter("cid");
        ContractOperations operation = ContractOperations.valueOf(request.getParameter("operation"));
        TejasContext self = new TejasContext();
        
        DAGContract contract = DAGContract.getInstance(self, contractID);
        switch (operation)
        {
            case forceComplete:
                contract.log(self, "Force completing the contract");
                contract.markCompletion(self, ContractExecutionReport.SUCCESS);
                break;
            
            case suspend:
                contract.suspend(self);
                break;
            
            case refresh:
                // Do nothing :)
                // The sendRedirect() will do the magic
                break;
            
            case restartNow:
                contract.restartNow(self);
                break;
            
            case resume:
                contract.resume(self);
                break;
        }
    }
}
