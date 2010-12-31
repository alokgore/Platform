package com.tejas.chanak.types.orm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.tejas.chanak.types.DAGContract.CompletionStatus;
import com.tejas.chanak.types.DAGStatus;
import com.tejas.utils.misc.Assert;
import com.tejas.utils.misc.StringUtils;

public class DAGDetails
{
    public DAGSummary summary;
    private Map<CompletionStatus, Long> statusCounts = new Hashtable<CompletionStatus, Long>();
    public long totalContracts = 0;
    
    public DAGDetails(DAGSummary summary)
    {
        this.summary = summary;
    }
    
    public void addContractCounts(DAGStatusCount... list)
    {
        this.totalContracts = 0;
        for (DAGStatusCount statusCount : list)
        {
            Assert.equals(this.summary.dag_id, statusCount.dag_id);
            
            this.statusCounts.put(statusCount.status, statusCount.count);
            this.totalContracts += statusCount.count;
        }
    }
    
    public long getContractCount(CompletionStatus status)
    {
        Long count = this.statusCounts.get(status);
        return count == null ? 0 : count.longValue();
    }
    
    public String getPrintableExecutionTime()
    {
        long executionTime =
                (this.summary.status == DAGStatus.Complete ? this.summary.end_time.getTime() : System.currentTimeMillis()) - this.summary.start_time.getTime();
        return StringUtils.millisToPrintableString(executionTime);
        
    }
    
    public List<StatusCount> getStatusCounts()
    {
        List<StatusCount> response = new ArrayList<StatusCount>();
        Set<Entry<CompletionStatus, Long>> set = this.statusCounts.entrySet();
        
        for (Entry<CompletionStatus, Long> entry : set)
        {
            response.add(new StatusCount(entry.getKey(), entry.getValue()));
        }
        Collections.sort(response);
        return response;
    }
    
}
