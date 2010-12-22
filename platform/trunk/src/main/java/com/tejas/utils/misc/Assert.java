package com.tejas.utils.misc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class Assert
{
    public static void allNotEmpty(String... strings)
    {
        for (String str : strings)
        {
            notEmpty(str);
        }
    }

    /**
     * Assert that the given string is <code>null</code> or empty
     * 
     * @throws IllegalArgumentException
     *             otherwise
     */
    public static void empty(String string)
    {
        empty(string, "[Assertion failed] - this argument must be null or empty");
    }

    /**
     * Assert that the given string is <code>null</code> or empty
     * 
     * @throws IllegalArgumentException
     *             otherwise
     */
    public static void empty(String string, String message)
    {
        if ((string != null) && (string.trim().equals("") == false))
        {
            throw new IllegalArgumentException(message);
        }
    }

    public static void equals(double obj1, double obj2)
    {
        equals(obj1, obj2, new MathContext(2, RoundingMode.HALF_UP));
    }

    public static void equals(double expected, double actual, double delta)
    {
        equals(null, expected, actual, delta);
    }

    public static void equals(double obj1, double obj2, MathContext mathContext)
    {
        equals(new BigDecimal(obj1, mathContext), new BigDecimal(obj2, mathContext));
    }

    public static void equals(Object obj1, Object obj2)
    {
        isTrue(((obj1 == null) && (obj2 == null)) ||
                ((obj1 != null) && (obj2 != null)));

        if (obj1 != null)
        {
            isTrue(obj1.equals(obj2), "obj1=[" + obj1 + "], obj2=[" + obj2 + "]");
        }
    }

    public static void equals(String message, double expected, double actual, double delta)
    {
        if (Double.compare(expected, actual) == 0)
        {
            return;
        }
        if (!(Math.abs(expected - actual) <= delta))
        {
            throw new IllegalStateException(message + "<Expected>" +
                    new Double(expected) + "</Expected> But <Actual>" + new Double(actual) + "</Actual>");
        }
    }

    /**
     * Assert a boolean expression, throwing <code>IllegalArgumentException</code> if the test result is <code>true</code>.
     */
    public static void isFalse(boolean expression)
    {
        isFalse(expression, "[Assertion failed] - this expression must be false");
    }

    public static void isFalse(boolean expression, RuntimeException exception)
    {
        if (expression)
        {
            throw exception;
        }
    }

    /**
     * Assert a boolean expression, throwing <code>IllegalArgumentException</code> if the test result is <code>true</code>.
     * 
     * @param expression
     *            a boolean expression
     * @param message
     *            the exception message to use if the assertion fails
     * @throws IllegalArgumentException
     *             if expression is <code>true</code>
     */
    public static void isFalse(boolean expression, String message)
    {
        if (expression)
        {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isNull(Object object)
    {
        isNull(object, "[Assertion failed] -  the argument should have been null. It was [" + object + "]");
    }

    public static void isNull(Object object, RuntimeException exception)
    {
        if (object != null)
        {
            throw exception;
        }
    }

    public static void isNull(Object object, String message)
    {
        if (object != null)
        {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert a boolean expression, throwing <code>IllegalArgumentException</code> if the test result is <code>false</code>.
     */
    public static void isTrue(boolean expression)
    {
        isTrue(expression, "[Assertion failed] - this expression must be true");
    }

    public static void isTrue(boolean expression, RuntimeException exception)
    {
        if (!expression)
        {
            throw exception;
        }
    }

    /**
     * Assert a boolean expression, throwing <code>IllegalArgumentException</code> if the test result is <code>false</code>.
     * 
     * @param expression
     *            a boolean expression
     * @param message
     *            the exception message to use if the assertion fails
     * @throws IllegalArgumentException
     *             if expression is <code>false</code>
     */
    public static void isTrue(boolean expression, String message)
    {
        if (!expression)
        {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that the given string is not <code>null</code> or empty
     * 
     * @throws IllegalArgumentException
     *             otherwise
     */
    public static void notEmpty(String string)
    {
        notEmpty(string, "[Assertion failed] - this argument is required; it must not be null");
    }

    /**
     * Assert that the given string is not <code>null</code> or empty
     * 
     * @throws IllegalArgumentException
     *             otherwise
     */
    public static void notEmpty(String string, String message)
    {
        if ((string == null) || string.trim().equals(""))
        {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notNull(Object object)
    {
        notNull(object, "[Assertion failed] -  this argument is required; it must not be null");
    }

    public static void notNull(Object object, RuntimeException exception)
    {
        if (object == null)
        {
            throw exception;
        }
    }

    public static void notNull(Object object, String message)
    {
        if (object == null)
        {
            throw new IllegalArgumentException(message);
        }
    }

}
