package com.tejas.chanak.types.orm;

import com.tejas.chanak.types.DAGContract.CompletionStatus;

public class DAGStatusCount
{
    public String dag_id;
    public CompletionStatus status;
    public long count;
    
    public long getCount()
    {
        return this.count;
    }
    
    public String getDag_id()
    {
        return this.dag_id;
    }
    
    public CompletionStatus getStatus()
    {
        return this.status;
    }
    
    public void setCount(long count)
    {
        this.count = count;
    }
    
    public void setDag_id(String dag_id)
    {
        this.dag_id = dag_id;
    }
    
    public void setStatus(CompletionStatus status)
    {
        this.status = status;
    }
}
