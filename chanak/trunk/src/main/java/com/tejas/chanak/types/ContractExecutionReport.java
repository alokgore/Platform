package com.tejas.chanak.types;

public class ContractExecutionReport
{
    public static enum ExecutionStatus 
    {
    		Complete,
        Enqueued,
        Failed,
    }
    
    private ExecutionStatus _status;
    private String _reason;
    private String _detailedDescription;
    private Throwable _cause;
    private Object _data;

    public ContractExecutionReport(Throwable cause)
    {
        this(ExecutionStatus.Failed, null, null);
        this._cause = cause;
    }
    
    public ContractExecutionReport(Object data)
    {
        this(ExecutionStatus.Complete, null, null);
        this._data = data;
    }
    
    public ContractExecutionReport(ExecutionStatus status, String reason, String description)
    {
        this._status = status;
        this._reason = reason;
        this._detailedDescription = description;
    }

    public synchronized String getReason()
    {
        return this._reason;
    }

    public synchronized String getDetailedDescription()
    {
        return this._detailedDescription;
    }

    public synchronized ExecutionStatus getStatus()
    {
        return this._status;
    }

    public synchronized Throwable getCause()
    {
        return this._cause;
    }

    public static final ContractExecutionReport SUCCESS = new ContractExecutionReport(ExecutionStatus.Complete, null, null);
    
    public static final ContractExecutionReport ENQUEUED = new ContractExecutionReport(ExecutionStatus.Enqueued, null, null);

    public synchronized Object getData()
    {
        return this._data;
    }
}
