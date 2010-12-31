package com.tejas.chanak.types.orm;

import java.sql.Timestamp;

import com.tejas.chanak.types.DAGStatus;

public class DAGSummary
{
    public String dag_id;
    public String description;
    public Timestamp start_time;
    public Timestamp end_time;
    public DAGStatus status;
    
    public DAGSummary()
    {
        // For MyBatis
    }
    
    public DAGSummary(String dag_id, String description)
    {
        this.dag_id = dag_id;
        this.description = description;
    }
    
    public String getDag_id()
    {
        return this.dag_id;
    }
    
    public String getDescription()
    {
        return this.description;
    }
    
    public Timestamp getEnd_time()
    {
        return this.end_time;
    }
    
    public Timestamp getStart_time()
    {
        return this.start_time;
    }
    
    public DAGStatus getStatus()
    {
        return this.status;
    }
    
    public void setDag_id(String dag_id)
    {
        this.dag_id = dag_id;
    }
    
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    public void setEnd_time(Timestamp end_time)
    {
        this.end_time = end_time;
    }
    
    public void setStart_time(Timestamp start_time)
    {
        this.start_time = start_time;
    }
    
    public void setStatus(DAGStatus status)
    {
        this.status = status;
    }
    
}
