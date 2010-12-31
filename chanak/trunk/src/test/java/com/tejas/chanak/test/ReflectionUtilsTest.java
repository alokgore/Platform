package com.tejas.chanak.test;

import org.junit.Test;

import com.tejas.chanak.types.orm.ContractDetails;
import com.tejas.utils.misc.RandomObjectGenerator;

public class ReflectionUtilsTest
{
    @Test
    public void testReflectionUtils() throws Exception
    {
        ContractDetails random = RandomObjectGenerator.getInstance().random(ContractDetails.class, true);
        System.out.println(random);
    }
}
