package com.tejas.chanak.types.orm;

import com.tejas.chanak.types.DAGContract.CompletionStatus;

public class ContractSummary
{
    public String dag_id;
    public String contract_id;
    public String description;
    public CompletionStatus status;
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ContractSummary)
        {
            ContractSummary that = (ContractSummary) obj;
            return this.contract_id.equals(that.contract_id);
        }
        if (obj instanceof String)
        {
            String that = (String) obj;
            return this.contract_id.equals(that);
        }
        return false;
    }
    
    public String getContract_id()
    {
        return this.contract_id;
    }
    
    public String getDag_id()
    {
        return this.dag_id;
    }
    
    public String getDescription()
    {
        return this.description;
    }
    
    public CompletionStatus getStatus()
    {
        return this.status;
    }
    
    @Override
    public int hashCode()
    {
        return this.contract_id.hashCode();
    }
    
    public void setContract_id(String contract_id)
    {
        this.contract_id = contract_id;
    }
    
    public void setDag_id(String dag_id)
    {
        this.dag_id = dag_id;
    }
    
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    public void setStatus(CompletionStatus status)
    {
        this.status = status;
    }
}
