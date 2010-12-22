package com.tejas.types.exceptions;

public class TejasException extends RuntimeException
{
    public TejasException(String message)
    {
        super(message);
    }

    public TejasException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public TejasException(Throwable cause)
    {
        super(cause);
    }
}
