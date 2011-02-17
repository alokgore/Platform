package com.tejas.utils.io;

import com.tejas.core.TejasContext;

public interface LineParser<T extends TimeseriesEvent>
{
    public static class ParsingException extends IllegalArgumentException
    {
        private final String errorCode;
        private final String errorDescription;

        public ParsingException(String errorCode, String errorDescription)
        {
            this.errorCode = errorCode;
            this.errorDescription = errorDescription;
        }

        public String getErrorCode()
        {
            return errorCode;
        }

        public String getErrorDescription()
        {
            return errorDescription;
        }

        @Override
        public String toString()
        {
            return "ParsingException:{[" + errorCode + "], [" + errorDescription + "]}";
        }
    }

    public interface ParsingFailureHandler
    {
        void handleParsingFailure(TejasContext self, String line, Exception failure);
    }

    T parseLine(String line) throws ParsingException;
}
