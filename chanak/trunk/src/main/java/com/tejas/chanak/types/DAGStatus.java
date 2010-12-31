package com.tejas.chanak.types;

public enum DAGStatus
{
        InProgress(0),
        Dormant(1),
        Complete(2), ;
    
    int code;
    
    private DAGStatus(int code)
    {
        this.code = code;
    }
    
    public int code()
    {
        return this.code;
    }
    
}
