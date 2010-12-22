package com.tejas.dbl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
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
        ClassLoader classLoader = mapperInterface.getClassLoader();
        Class[] interfaces = new Class[] { mapperInterface };
        TejasMapperProxy proxy = new TejasMapperProxy(sessionHolder, mapperInterface);
        return (T) Proxy.newProxyInstance(classLoader, interfaces, proxy);
    }

    private final Class<T> mapperInterface;
    private final SQLSessionHolder sessionHolder;

    @SuppressWarnings("rawtypes")
    private static Set<Class> updateAnnotations = Collections.synchronizedSet(new HashSet<Class>(Arrays.asList(new Class[] { Update.class, Delete.class, Insert.class,
            InsertProvider.class, UpdateProvider.class })));

    private TejasMapperProxy(SQLSessionHolder sessionHolder, Class<T> mapperInterface)
    {
        this.sessionHolder = sessionHolder;
        this.mapperInterface = mapperInterface;
    }

    // XXX: Remove
    @SuppressWarnings("unused")
    private boolean requiresWriteAccess(Method method)
    {
        Annotation[] annotations = method.getAnnotations();
        for (Annotation annotation : annotations)
        {
            if (updateAnnotations.contains(annotation.annotationType()))
            {
                return true;
            }
        }
        return false;
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
