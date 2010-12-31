package com.tejas.chanak.types.orm;

import com.tejas.chanak.types.DAGContract.CompletionStatus;

public class StatusCount implements Comparable<StatusCount>
{
    public long count;
    public CompletionStatus status;
    
    public StatusCount(CompletionStatus status, long count)
    {
        this.status = status;
        this.count = count;
    }
    
    @Override
    public int compareTo(StatusCount o)
    {
        return this.status.getOrder() - o.status.getOrder();
    }
    
    public long getCount()
    {
        return this.count;
    }
    
    public CompletionStatus getStatus()
    {
        return this.status;
    }
    
    public void setCount(long count)
    {
        this.count = count;
    }
    
    public void setStatus(CompletionStatus status)
    {
        this.status = status;
    }
}
