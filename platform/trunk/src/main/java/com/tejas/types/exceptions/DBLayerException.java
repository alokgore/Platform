package com.tejas.types.exceptions;

public class DBLayerException extends TejasException
{
    public DBLayerException(String message)
    {
        super(message);
    }

    public DBLayerException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public DBLayerException(Throwable cause)
    {
        super(cause);
    }
}
