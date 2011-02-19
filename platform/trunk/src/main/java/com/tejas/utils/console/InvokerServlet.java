package com.tejas.utils.console;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class InvokerServlet extends HttpServlet
{
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String serverletName = req.getPathInfo();

        if (serverletName.startsWith("/"))
        {
            serverletName = serverletName.substring(1);
        }

        try
        {
            Class<?> clazz = Class.forName(serverletName);

            HttpServlet servlet = (HttpServlet) clazz.newInstance();

            Method method = clazz.getMethod("doGet", HttpServletRequest.class, HttpServletResponse.class);

            method.invoke(servlet, req, resp);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }
}
