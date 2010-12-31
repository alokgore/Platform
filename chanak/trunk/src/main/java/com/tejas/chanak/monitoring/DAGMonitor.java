package com.tejas.chanak.monitoring;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.tejas.chanak.types.orm.ContractDetails;
import com.tejas.chanak.types.orm.DAGSummary;
import com.tejas.core.TejasBackgroundJob;
import com.tejas.core.TejasContext;

public class DAGMonitor
{
    public static interface Mapper
    {
        @Update("update dag_contracts " +
                "set status = 'Ready', next_retry_time = now(), num_restarts =  num_restarts + 1, last_updated = now() " +
                "where status = 'InProgress'")
        void restartStuckContracts();
        
        @Select("select * from dag_contracts where status <> 'Complete' and num_restarts > #{numRestarts}")
        List<ContractDetails> selectFailedContracts(long numRestarts);
        
        @Select("select * from dag_contracts where status = 'InProgress' and start_time < #{time}")
        List<ContractDetails> selectSlowContracts(Timestamp startedBefore);
        
        @Select("select * from dag_summary where status <> 'Complete' and start_time < #{time}")
        List<DAGSummary> selectSlowDAGs(Timestamp startedBefore);
    }
    
    private static boolean initialized = false;
    
    private static List<TejasBackgroundJob> monitoringJobs;
    
    public synchronized static void init(TejasContext self) throws Exception
    {
        if (!initialized)
        {
            self.dbl.getMybatisMapper(Mapper.class).restartStuckContracts();
            
            monitoringJobs = new ArrayList<TejasBackgroundJob>();
            
            monitoringJobs.add(new SlowDAGMonitor());
            monitoringJobs.add(new SlowContractsMonitor());
            monitoringJobs.add(new FailedContractsMonitor());
            
            for (TejasBackgroundJob job : monitoringJobs)
            {
                job.start();
            }
            initialized = true;
        }
    }
    
    public synchronized static void shutdown()
    {
        if (initialized)
        {
            for (TejasBackgroundJob job : monitoringJobs)
            {
                job.signalShutdown();
            }
            initialized = false;
        }
    }
}
