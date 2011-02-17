package com.tejas.utils.io;

import java.util.ArrayList;
import java.util.List;

import com.tejas.core.TejasContext;
import com.tejas.utils.io.FileTailer.DataListener;
import com.tejas.utils.io.LineParser.ParsingFailureHandler;

public class EventProcessor<T extends TimeseriesEvent> implements DataListener
{
    public enum ProcessingFailureAction
    {
            Ignore,
            Retry,
            ThrowUp,
    }

    public interface TransportExceptionHandler<T>
    {
        ProcessingFailureAction handleTransportException(TejasContext self, List<T> events, Exception failure, long numTries);
    }

    public static class TryForeverPolicy<T> implements TransportExceptionHandler<T>
    {
        @Override
        public ProcessingFailureAction handleTransportException(TejasContext self, List<T> events, Exception failure, long numTries)
        {
            return ProcessingFailureAction.Retry;
        }
    }

    public interface EventTransporter<T>
    {
        void sendEvent(TejasContext self, List<T> events) throws Exception;
    }

    public static class ParsingFailureLogger implements ParsingFailureHandler
    {
        @Override
        public void handleParsingFailure(TejasContext self, String line, Exception failure)
        {
            self.logger.error(failure.toString() + ":" + line);
        }
    }

    private LineParser<T> lineParser;
    private ParsingFailureHandler parsingFailureHandler;
    private EventTransporter<T> eventTransporter;
    private TransportExceptionHandler<T> transportExceptionHandler;

    public EventProcessor(LineParser<T> lineParser, ParsingFailureHandler parsingFailureHandler,
            EventTransporter<T> eventTransporter, TransportExceptionHandler<T> transportExceptionHandler)
    {
        this.lineParser = lineParser;
        this.parsingFailureHandler = parsingFailureHandler;
        this.eventTransporter = eventTransporter;
        this.transportExceptionHandler = transportExceptionHandler;
    }

    @Override
    public void processNewData(TejasContext self, List<String> lines, long currentFilePosition) throws Exception
    {
        List<T> events = createEvents(self, lines);
        transportEvents(self, events);
    }

    protected void transportEvents(TejasContext self, List<T> events) throws Exception
    {
        boolean tryAgain = false;
        long numTries = 1;
        do
        {
            try
            {
                this.eventTransporter.sendEvent(self, events);
            }
            catch (Exception e)
            {
                self.logger.error("Event Sending Failed numTries = [" + numTries + "]'", e);
                tryAgain = false;
                ProcessingFailureAction action = this.transportExceptionHandler.handleTransportException(self, events, e, numTries++);

                switch (action)
                {
                    case Ignore:
                        // Do nothing. Move on
                        break;

                    case Retry:
                        tryAgain = true;
                        break;

                    case ThrowUp:
                        throw e;
                }
            }
        }
        while (tryAgain);
    }

    protected List<T> createEvents(TejasContext self, List<String> lines)
    {
        List<T> events = new ArrayList<T>();

        for (String line : lines)
        {
            try
            {
                events.add(this.lineParser.parseLine(line));
            }
            catch (Exception e)
            {
                this.parsingFailureHandler.handleParsingFailure(self, line, e);
            }
        }

        return events;
    }
}
