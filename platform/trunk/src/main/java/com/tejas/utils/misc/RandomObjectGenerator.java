package com.tejas.utils.misc;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;

public class RandomObjectGenerator
{
    private static class Property
    {
        @SuppressWarnings("rawtypes")
        private Class clazz;
        private String propertyName;
        
        @SuppressWarnings("rawtypes")
        public Property(Class clazz, String propertyName)
        {
            this.clazz = clazz;
            this.propertyName = propertyName;
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof Property)
            {
                Property that = (Property) obj;
                return that.clazz.getCanonicalName().equals(this.clazz.getCanonicalName())
                        && that.propertyName.equals(this.propertyName);
            }
            return false;
        }
        
        @Override
        public int hashCode()
        {
            return this.clazz.getCanonicalName().hashCode() + this.propertyName.hashCode();
        }
    }
    
    private interface TypeGenerator<T>
    {
        T generateRandomObject(Class<? extends T> clazz) throws Exception;
    }
    
    private static class TypeGeneratorFactory
    {
        public static TypeGenerator<BigDecimal> bigDecimalGenerator()
        {
            return new TypeGenerator<BigDecimal>() {
                @Override
                public BigDecimal generateRandomObject(Class<? extends BigDecimal> clazz)
                {
                    return new BigDecimal(RandomUtils.nextInt(100));
                }
            };
        }
        
        public static TypeGenerator<BigInteger> bigIntegerGenerator()
        {
            return new TypeGenerator<BigInteger>() {
                @Override
                public BigInteger generateRandomObject(Class<? extends BigInteger> clazz)
                {
                    return BigInteger.valueOf(RandomUtils.nextInt(100));
                }
            };
        }
        
        public static TypeGenerator<Boolean> boolGenerator()
        {
            return new TypeGenerator<Boolean>() {
                @Override
                public Boolean generateRandomObject(Class<? extends Boolean> clazz)
                {
                    return RandomUtils.nextBoolean();
                }
            };
        }
        
        public static TypeGenerator<Date> dateGenerator()
        {
            return new TypeGenerator<Date>() {
                @Override
                public Date generateRandomObject(Class<? extends Date> clazz)
                {
                    return new Date();
                }
            };
        }
        
        @SuppressWarnings("rawtypes")
        public static TypeGenerator<Enum> enumGenerator()
        {
            return new TypeGenerator<Enum>() {
                @Override
                public Enum generateRandomObject(Class<? extends Enum> clazz) throws Exception
                {
                    Enum[] values = (Enum[]) clazz.getMethod("values").invoke(null);
                    return values[RandomUtils.nextInt(values.length)];
                }
            };
        }
        
        public static TypeGenerator<Integer> intGenerator()
        {
            return new TypeGenerator<Integer>() {
                @Override
                public Integer generateRandomObject(Class<? extends Integer> clazz)
                {
                    return RandomUtils.nextInt(100);
                }
            };
        }
        
        public static TypeGenerator<Long> longGenerator()
        {
            return new TypeGenerator<Long>() {
                @Override
                public Long generateRandomObject(Class<? extends Long> clazz)
                {
                    return RandomUtils.nextLong();
                }
            };
        }
        
        public static TypeGenerator<String> stringGenerator()
        {
            return new TypeGenerator<String>() {
                @Override
                public String generateRandomObject(Class<? extends String> clazz)
                {
                    return RandomStringUtils.randomAlphanumeric(10);
                }
            };
        }
        
        public static TypeGenerator<Timestamp> timestampGenerator()
        {
            return new TypeGenerator<Timestamp>() {
                @Override
                public Timestamp generateRandomObject(Class<? extends Timestamp> clazz)
                {
                    return new Timestamp(System.currentTimeMillis());
                }
            };
        }
        
        public static TypeGenerator<XMLGregorianCalendar> xmlCalendarGenerator()
        {
            return new TypeGenerator<XMLGregorianCalendar>() {
                @Override
                public XMLGregorianCalendar generateRandomObject(Class<? extends XMLGregorianCalendar> clazz)
                {
                    GregorianCalendar cal = new GregorianCalendar();
                    cal.setTime(new Date());
                    return new XMLGregorianCalendarImpl(cal);
                }
            };
        }
    }
    
    public static RandomObjectGenerator getInstance()
    {
        return new RandomObjectGenerator();
    }
    
    @SuppressWarnings("rawtypes")
    private Map<Class, TypeGenerator> typeRegistry = new ConcurrentHashMap<Class, TypeGenerator>();
    
    @SuppressWarnings("rawtypes")
    private Map<Property, Class> propertyRegistry = new ConcurrentHashMap<Property, Class>();
    
    private RandomObjectGenerator()
    {
        register(Long.class, TypeGeneratorFactory.longGenerator());
        register(Long.TYPE, TypeGeneratorFactory.longGenerator());
        
        register(Integer.class, TypeGeneratorFactory.intGenerator());
        register(Integer.TYPE, TypeGeneratorFactory.intGenerator());
        
        register(Boolean.class, TypeGeneratorFactory.boolGenerator());
        register(Boolean.TYPE, TypeGeneratorFactory.boolGenerator());
        
        register(BigDecimal.class, TypeGeneratorFactory.bigDecimalGenerator());
        register(BigInteger.class, TypeGeneratorFactory.bigIntegerGenerator());
        
        register(String.class, TypeGeneratorFactory.stringGenerator());
        register(Date.class, TypeGeneratorFactory.dateGenerator());
        register(Timestamp.class, TypeGeneratorFactory.timestampGenerator());
        register(XMLGregorianCalendar.class, TypeGeneratorFactory.xmlCalendarGenerator());
        register(Enum.class, TypeGeneratorFactory.enumGenerator());
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ArrayList getRandomCollection(Class clazz, PropertyDescriptor property, boolean tolerateHoles) throws Exception
    {
        Class collectionType = getCollectionType(clazz, property.getName());
        ArrayList list = new ArrayList();
        if (collectionType == null)
        {
            if (tolerateHoles)
            {
                return list;
            }
            throw new IllegalStateException("No mapping found for class:" + clazz + " property:" + property.getName());
        }
        for (int i = 0; i < 5; i++)
        {
            list.add(random(collectionType, tolerateHoles));
        }
        return list;
    }
    
    @SuppressWarnings("rawtypes")
    private TypeGenerator getTypeGenerator(Class clazz)
    {
        TypeGenerator typeGenerator = null;
        while (clazz != null && !clazz.equals(Object.class))
        {
            typeGenerator = this.typeRegistry.get(clazz);
            if (typeGenerator != null)
            {
                break;
            }
            clazz = clazz.getSuperclass();
        }
        return typeGenerator;
    }
    
    <T> void register(Class<T> clazz, TypeGenerator<T> typeGenerator)
    {
        this.typeRegistry.put(clazz, typeGenerator);
    }
    
    void registerCollectionType(Class<?> clazz, String propertyName, Class<?> collectionType)
    {
        this.propertyRegistry.put(new Property(clazz, propertyName), collectionType);
    }
    
    @SuppressWarnings("rawtypes")
    public Class getCollectionType(Class clazz, String propertyName)
    {
        return this.propertyRegistry.get(new Property(clazz, propertyName));
    }
    
    public <T> T random(Class<T> clazz) throws Exception
    {
        return random(clazz, false);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T random(Class<T> clazz, boolean tolerateHoles) throws Exception
    {
        if (clazz.equals(Object.class))
        {
            if (tolerateHoles)
            {
                return null;
            }
            throw new IllegalStateException("Reached Object!");
        }
        
        TypeGenerator typeGenerator;
        if ((typeGenerator = getTypeGenerator(clazz)) != null)
        {
            return (T) typeGenerator.generateRandomObject(clazz);
        }
        
        System.out.println("Trying to generate an object of type " + clazz.getCanonicalName());
        T response = clazz.newInstance();
        
        for (PropertyDescriptor property : PropertyUtils.getPropertyDescriptors(clazz))
        {
            if (List.class.isAssignableFrom(property.getPropertyType()))
            {
                ArrayList list = getRandomCollection(clazz, property, tolerateHoles);
                
                if (property.getWriteMethod() != null)
                {
                    property.getWriteMethod().invoke(response, new Vector());
                }
                
                ((List) property.getReadMethod().invoke(response)).addAll(list);
            }
            else
            {
                Method writeMethod = property.getWriteMethod();
                if (writeMethod != null)
                {
                    Object propertyValue = random(property.getPropertyType(), tolerateHoles);
                    writeMethod.invoke(response, propertyValue);
                }
            }
        }
        
        return response;
    }
}
