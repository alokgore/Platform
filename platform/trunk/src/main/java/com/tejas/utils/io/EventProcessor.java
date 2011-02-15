package com.tejas.utils.io;

import java.util.List;

public class EventProcessor<T extends TimeseriesEvent>
{
    public enum ProcessingFailureAction
    {
            Ignore,
            Retry,
            ThrowUp,
    }

    public interface ProcessingFailureHandler<T>
    {
        ProcessingFailureAction handleFailure(List<T> events, long numFailures);
    }
}
