package com.tejas.utils.misc;

public class TypeUtils
{
    /**
     * Null-safe equals 
     */
    public static boolean equals(Object object1, Object object2)
    {
            if(object1 == null && object2 == null) return true;

            if(object1 == null || object2 == null) return false;
            
            return object1.equals(object2);
    }
}
