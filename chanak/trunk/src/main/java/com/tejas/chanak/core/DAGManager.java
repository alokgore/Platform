package com.tejas.chanak.core;

import java.sql.Timestamp;
import java.util.List;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.tejas.chanak.monitoring.DAGMonitor;
import com.tejas.chanak.scheduler.DAGScheduler;
import com.tejas.chanak.types.DAGStatus;
import com.tejas.chanak.types.DependencyGraph;
import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasContext;

public class DAGManager
{
    public interface Mapper
    {
        @Update("delete from dag_contracts where dag_id = #{id}")
        void cleanupContracts(String dagID);
        
        @Update("delete from dag_deps where dag_id = #{id}")
        void cleanupDependencies(String dagID);
        
        @Update("delete from dag_outputs where dag_id = #{id}")
        void cleanupOutputs(String dagID);
        
        @Update("delete from dag_logs where dag_id = #{id}")
        void cleanupLogs(String dagID);
        
        @Update("delete from dag_contract_config where dag_id = #{id}")
        void cleanupConfig(String dagID);
        
        @Update("delete from dag_summary where dag_id = #{id}")
        void cleanupSummary(String dagID);
        
        @Update("update dag_summary set status='Complete', end_time=now() where dag_id=#{id}")
        void markDAGComplete(String dagID);
        
        @Select("select dag_id from  dag_summary where status = 'Complete' and end_time < #{time}")
        List<String> selectExpiredDAGs(Timestamp expirySince);
        
        @Select("select count(*) from dag_summary where status <> 'Complete'")
        long countActiveDAGs();
        
        
    }
    
    private static boolean _isInitialized;
    private static DAGScheduler dagScheduler;
    
    public synchronized static void init(TejasContext self) throws Exception
    {
        if (!isInitialized())
        {
            self.logger.info("DAGManager coming up!");
            if (!ApplicationConfig.findBoolean("dagmanager.consoleOnly", false))
            {
                dagScheduler = new DAGScheduler(self);
                dagScheduler.start();
                
                DAGMonitor.init(self);
            }
            _isInitialized = true;
        }
    }
    
    public synchronized static void shutdown(TejasContext self)
    {
        if (isInitialized())
        {
            self.logger.info("DAGManager going down!");
            DAGMonitor.shutdown();
            dagScheduler.signalShutDown();
            _isInitialized = false;
        }
    }
    
    public static synchronized boolean isInitialized()
    {
        return _isInitialized;
    }
    
    public static void cleanupDAG(TejasContext self, String dagID)
    {
        self.logger.info("Going to clean-up the DAG [" + dagID + "]");
        
        self.dbl.startTransaction();
        try
        {
            Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
            
            mapper.cleanupSummary(dagID);
            mapper.cleanupContracts(dagID);
            mapper.cleanupConfig(dagID);
            mapper.cleanupDependencies(dagID);
            mapper.cleanupLogs(dagID);
            mapper.cleanupOutputs(dagID);
            
            self.dbl.commit();
            
            self.logger.info("Cleaned up the DAG [" + dagID + "]");
        }
        finally
        {
            self.dbl.rollback();
        }
    }
    
    public static void markQueryCompletion(TejasContext self, String dagID)
    {
        self.logger.info("Marking DAG [" + dagID + "] as complete");
        
        self.dbl.getMybatisMapper(Mapper.class).markDAGComplete(dagID);
        
        self.logger.info("Marked the query " + dagID + " as complete");
    }
    
    public static DAGStatus getQueryStatus(TejasContext self, String dagID)
    {
        return self.dbl.getMybatisMapper(com.tejas.chanak.types.DependencyGraph.Mapper.class).selectDAGSummary(dagID).status;
    }
    
    public static void suspendDAG(TejasContext self, String dagID)
    {
        self.logger.info("Going to suspend the DAG [" + dagID + "]");
        
        self.dbl.startTransaction();
        try
        {
            com.tejas.chanak.types.DependencyGraph.Mapper mapper = self.dbl.getMybatisMapper(DependencyGraph.Mapper.class);
            
            mapper.suspendDAG(dagID);
            mapper.suspendRunnableContracts(dagID);
            mapper.suspendWaitingContracts(dagID);
            
            self.dbl.commit();
            
            self.logger.info("Suspended the DAG [" + dagID + "]");
        }
        finally
        {
            self.dbl.rollback();
        }
    }
    
    public static void resumeDAG(TejasContext self, String dagID)
    {
        self.logger.info("Going to resume the DAG [" + dagID + "]");
        
        self.dbl.startTransaction();
        try
        {
            com.tejas.chanak.types.DependencyGraph.Mapper mapper = self.dbl.getMybatisMapper(DependencyGraph.Mapper.class);

            mapper.resumeDAG(dagID);
            mapper.resumeRunnableContracts(dagID);
            mapper.resumeWaitingContracts(dagID);
            
            self.dbl.commit();
            
            self.logger.info("Resumed the DAG [" + dagID + "]");
        }
        finally
        {
            self.dbl.rollback();
        }
    }
    
    public static boolean isDAGManagerIdle(TejasContext self)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        return mapper.countActiveDAGs() == 0;
    }
    
    static final long DAG_EXPIRY_INTERVAL_MILLIS = ApplicationConfig.findInteger("dagmanager.scavenger.dag_expiry.seconds", 4 * 24 * 3600) * 1000;
    
    public static void cleanExpiredDAGs(TejasContext self)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        List<String> expiredDAGs = mapper.selectExpiredDAGs(new Timestamp(System.currentTimeMillis() - DAG_EXPIRY_INTERVAL_MILLIS));
        for (String dag : expiredDAGs)
        {
            cleanupDAG(self, dag);
        }
    }
}
