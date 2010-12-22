package com.tejas.dbl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Arrays;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;

import com.tejas.utils.misc.Assert;


public class Foo
{
    private long fooId;
    private String fooString = RandomStringUtils.randomAlphanumeric(10);
    private Date fooDate = new Date(System.currentTimeMillis());
    private Timestamp fooTime = new Timestamp(System.currentTimeMillis());
    private byte[] fooBlob = RandomStringUtils.random(100).getBytes();
    private String fooClob = RandomStringUtils.randomAlphanumeric(10000);
    private boolean fooBoolean = RandomUtils.nextBoolean();
    private BigInteger fooBigInt = BigInteger.valueOf(RandomUtils.nextLong());
    private BigDecimal fooBigDecimal = new BigDecimal(RandomUtils.nextDouble(), new MathContext(2, RoundingMode.HALF_UP));
    private double fooDouble = RandomUtils.nextDouble();

    final BigDecimal getFooBigDecimal()
    {
        return fooBigDecimal;
    }

    final BigInteger getFooBigInt()
    {
        return fooBigInt;
    }

    final byte[] getFooBlob()
    {
        return fooBlob;
    }

    final String getFooClob()
    {
        return fooClob;
    }

    final Date getFooDate()
    {
        return fooDate;
    }

    final double getFooDouble()
    {
        return fooDouble;
    }

    final long getFooId()
    {
        return fooId;
    }

    final String getFooString()
    {
        return fooString;
    }

    final Timestamp getFooTime()
    {
        return fooTime;
    }

    final boolean isFooBoolean()
    {
        return fooBoolean;
    }

    final void setFooBigDecimal(BigDecimal fooBigDecimal)
    {
        this.fooBigDecimal = fooBigDecimal;
    }

    final void setFooBigInt(BigInteger fooBigInt)
    {
        this.fooBigInt = fooBigInt;
    }

    final void setFooBlob(byte[] fooBlob)
    {
        this.fooBlob = fooBlob;
    }

    final void setFooBoolean(boolean fooBoolean)
    {
        this.fooBoolean = fooBoolean;
    }

    final void setFooClob(String fooClob)
    {
        this.fooClob = fooClob;
    }

    final void setFooDate(Date fooDate)
    {
        this.fooDate = fooDate;
    }

    final void setFooDouble(double fooDouble)
    {
        this.fooDouble = fooDouble;
    }

    final void setFooId(long fooId)
    {
        this.fooId = fooId;
    }

    final void setFooString(String fooString)
    {
        this.fooString = fooString;
    }

    final void setFooTime(Timestamp fooTime)
    {
        this.fooTime = fooTime;
    }

    public void assertEquals(Object obj)
    {
        if (this == obj)
        {
            return;
        }

        Assert.notNull(obj);
        Assert.isTrue(getClass() == obj.getClass());

        Foo that = (Foo) obj;

        Assert.equals(this.fooBigDecimal.doubleValue(), that.fooBigDecimal.doubleValue(), 0.01D);

        Assert.equals(this.fooBigInt, that.fooBigInt);
        Assert.isTrue(Arrays.equals(fooBlob, that.fooBlob));
        Assert.equals(this.fooClob, that.fooClob);

        // Since Java Date Object also contains the hour/minute/second/milli part
        Assert.equals(this.fooDate.toString(), that.fooDate.toString());

        Assert.equals(this.fooDouble, that.fooDouble, new MathContext(2));
        Assert.equals(this.fooId, that.fooId);
        Assert.equals(this.fooString, that.fooString);

        // Doing this because MySQL truncates millis from the timestamp
        Assert.equals(this.fooTime.getTime() / 1000, that.fooTime.getTime() / 1000);
    }

    @Override
    public int hashCode()
    {
        return fooString.hashCode();
    }

    @Override
    public String toString()
    {
        return "Foo [fooId=" + fooId + ", fooString=" + fooString + ", fooDate=" + fooDate + ", fooTime=" + fooTime +
                ", fooBoolean=" + fooBoolean + ", fooBigInt=" + fooBigInt + ", fooBigDecimal=" + fooBigDecimal + ", fooDouble=" + fooDouble
                + "]";
    }

}
