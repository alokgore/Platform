package com.tejas.utils.console;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tejas.utils.misc.StringUtils;

public abstract class TejasServletBase extends HttpServlet
{
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        PrintWriter out = new PrintWriter(response.getOutputStream());
        try
        {
            execute(request, out);
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

    public abstract void execute(HttpServletRequest request, PrintWriter out) throws Throwable;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        doGet(request, response);
    }
}
