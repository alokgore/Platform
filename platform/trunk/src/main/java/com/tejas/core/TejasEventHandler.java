package com.tejas.core;

public interface TejasEventHandler
{
    public static enum Severity
    {
            WARNING,
            ERROR,
            FATAL
    }

    @SuppressWarnings("rawtypes")
    public void alarm(Severity severity, Enum component, Enum alarm, String deduplicationString, String description);

}
