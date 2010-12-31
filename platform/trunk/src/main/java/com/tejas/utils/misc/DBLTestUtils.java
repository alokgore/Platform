package com.tejas.utils.misc;

import java.lang.reflect.Method;

import com.tejas.core.TejasContext;

public class DBLTestUtils
{
    public static <T> void testMapper(Class<T> mapperClass)
    {
        testMapper(mapperClass, false);
    }
    
    public static <T> void testMapper(Class<T> mapperClass, boolean nullifyUnknownFields)
    {
        TejasContext self = new TejasContext();
        T mapper = self.dbl.getMybatisMapper(mapperClass);
        
        Class<?>[] interfaces = mapper.getClass().getInterfaces();
        Assert.isTrue(1 == interfaces.length);
        
        for (Method method : ((Class<?>) interfaces[0]).getMethods())
        {
            Class<?>[] parameterTypes = method.getParameterTypes();
            
            try
            {
                System.out.println("Invoking [" + mapperClass.getCanonicalName() + "] method [" + method.getName() + "]");
                
                if (parameterTypes.length == 0)
                {
                    method.invoke(mapper);
                }
                else
                {
                    Object[] args = new Object[parameterTypes.length];
                    int i = 0;
                    for (Class<?> paramClass : parameterTypes)
                    {
                        args[i++] = RandomObjectGenerator.getInstance().random(paramClass, nullifyUnknownFields);
                    }
                    method.invoke(mapper, args);
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        
    }
}
