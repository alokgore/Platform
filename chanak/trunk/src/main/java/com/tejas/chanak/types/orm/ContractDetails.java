package com.tejas.chanak.types.orm;

import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tejas.chanak.types.DAGContract.CompletionStatus;
import com.tejas.chanak.types.LogEntry;

public class ContractDetails
{
    public String dag_id;
    
    public String contract_id;
    public long num_restarts;
    public boolean retry;
    public Timestamp last_updated;
    public Timestamp start_time;
    public Timestamp next_retry_time;
    public String description;
    public CompletionStatus status;
    /**
     * XStream serialized XML data
     */
    public String contract;
    
    public List<ContractSummary> completedPrerequisites;
    
    public List<ContractSummary> pendingPrerequisites;
    public List<ContractSummary> dependents;
    public List<LogEntry> logs;
    
    /**
     * @deprecated Only for mybatis
     */
    @Deprecated
    public ContractDetails()
    {
        // Only for mybatis
    }
    
    public ContractDetails(String dag_id, String contract_id, String description, CompletionStatus status, String contract)
    {
        this.dag_id = dag_id;
        this.contract_id = contract_id;
        this.description = description;
        this.status = status;
        this.contract = contract;
    }
    
    public List<ContractSummary> getCompletedPrerequisites()
    {
        return this.completedPrerequisites;
    }
    
    public String getContract()
    {
        return this.contract;
    }
    
    public String getContract_id()
    {
        return this.contract_id;
    }
    
    public String getDag_id()
    {
        return this.dag_id;
    }
    
    public List<ContractSummary> getDependents()
    {
        return this.dependents;
    }
    
    public String getDescription()
    {
        return this.description;
    }
    
    public Timestamp getLast_updated()
    {
        return this.last_updated;
    }
    
    public List<LogEntry> getLogs()
    {
        return this.logs;
    }
    
    public Timestamp getNext_retry_time()
    {
        return this.next_retry_time;
    }
    
    public long getNum_restarts()
    {
        return this.num_restarts;
    }
    
    public List<ContractSummary> getPendingPrerequisites()
    {
        return this.pendingPrerequisites;
    }
    
    public Timestamp getStart_time()
    {
        return this.start_time;
    }
    
    public CompletionStatus getStatus()
    {
        return this.status;
    }
    
    public boolean isRetry()
    {
        return this.retry;
    }
    
    public void setCompletedPrerequisites(List<ContractSummary> completedPrerequisites)
    {
        this.completedPrerequisites = completedPrerequisites;
    }
    
    public void setContract(String contract)
    {
        this.contract = contract;
    }
    
    public void setContract_id(String contract_id)
    {
        this.contract_id = contract_id;
    }
    
    public void setDag_id(String dag_id)
    {
        this.dag_id = dag_id;
    }
    
    public void setDependents(List<ContractSummary> dependents)
    {
        this.dependents = dependents;
    }
    
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    public void setLast_updated(Timestamp last_updated)
    {
        this.last_updated = last_updated;
    }
    
    public void setLogs(List<LogEntry> logs)
    {
        this.logs = logs;
    }
    
    public void setNext_retry_time(Timestamp next_retry_time)
    {
        this.next_retry_time = next_retry_time;
    }
    
    public void setNum_restarts(long num_restarts)
    {
        this.num_restarts = num_restarts;
    }
    
    public void setPendingPrerequisites(List<ContractSummary> pendingPrerequisites)
    {
        this.pendingPrerequisites = pendingPrerequisites;
    }
    
    public void setRetry(boolean retry)
    {
        this.retry = retry;
    }
    
    public void setStart_time(Timestamp start_time)
    {
        this.start_time = start_time;
    }
    
    public void setStatus(CompletionStatus status)
    {
        this.status = status;
    }
    
    @Override
    public String toString()
    {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
