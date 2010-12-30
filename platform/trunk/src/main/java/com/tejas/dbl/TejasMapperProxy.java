package com.tejas.dbl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.apache.ibatis.binding.MapperProxy;
import org.apache.ibatis.session.SqlSession;

import com.tejas.core.TejasLogger;
import com.tejas.dbl.TejasDBLayerImpl.SQLSessionHolder;
import com.tejas.logging.TejasLog4jWrapper;
import com.tejas.types.exceptions.DBLayerException;

public class TejasMapperProxy<T> implements InvocationHandler
{
    private static final TejasLogger logger = TejasLog4jWrapper.getLogger(TejasMapperProxy.class);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T newMapperProxy(Class<T> mapperInterface, SQLSessionHolder sessionHolder)
    {
        Class[] interfaces = new Class[] { mapperInterface };
        TejasMapperProxy proxy = new TejasMapperProxy(sessionHolder, mapperInterface);
        return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), interfaces, proxy);
    }

    private final Class<T> mapperInterface;
    private final SQLSessionHolder sessionHolder;

    private TejasMapperProxy(SQLSessionHolder sessionHolder, Class<T> mapperInterface)
    {
        this.sessionHolder = sessionHolder;
        this.mapperInterface = mapperInterface;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        logger.trace("TejasMapperProxy.invoke for method=[", method, "], args={", (args == null ? null : Arrays.asList(args)), "}");

        SqlSession session = sessionHolder.checkoutSession();
        T ibatisMapperProxy = MapperProxy.newMapperProxy(mapperInterface, session);
        try
        {
            return method.invoke(ibatisMapperProxy, args);
        }
        catch (Exception e)
        {
            throw new DBLayerException(e);
        }
        finally
        {
            sessionHolder.checkinSession(session);
        }
    }
}
