package com.tejas.utils.io;

public interface LineParser<T extends TimeseriesEvent>
{
    public class ParsingException extends IllegalArgumentException
    {
        private final String errorCode;
        private final String sourceData;
        private final String errorDescription;

        public ParsingException(String sourceData, String errorCode, String errorDescription)
        {
            this.sourceData = sourceData;
            this.errorCode = errorCode;
            this.errorDescription = errorDescription;
        }

        public ParsingException(String errorCode, String errorDescription)
        {
            this("", errorCode, errorDescription);
        }

        public String getErrorCode()
        {
            return errorCode;
        }

        public String getSourceData()
        {
            return sourceData;
        }

        public String getErrorDescription()
        {
            return errorDescription;
        }
    }

    T parseLine(String line) throws ParsingException;
}
