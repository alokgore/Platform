package com.tejas.chanak.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.tejas.chanak.types.DAGContract.CompletionStatus;
import com.tejas.chanak.types.Dependency;
import com.tejas.chanak.types.LogEntry;
import com.tejas.chanak.types.orm.ContractDetails;
import com.tejas.chanak.types.orm.ContractSummary;
import com.tejas.chanak.types.orm.DAGDetails;
import com.tejas.chanak.types.orm.DAGStatusCount;
import com.tejas.chanak.types.orm.DAGSummary;
import com.tejas.core.TejasContext;

public class DAGManagerConsoleUtils
{
    public static class ContractFilter
    {
        public String dag_id;
        
        public CompletionStatus status;
        
        /**
         * @deprecated Only for MyBatis
         */
        @Deprecated
        public ContractFilter()
        {
            // Only for MyBatis
        }
        
        public ContractFilter(String dag_id, CompletionStatus status)
        {
            this.dag_id = dag_id;
            this.status = status;
        }
        
        public String getDag_id()
        {
            return this.dag_id;
        }
        
        public CompletionStatus getStatus()
        {
            return this.status;
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
    
    public interface Mapper
    {
        @Update("update dag_contracts set last_updated = now(), status = 'Waiting', num_restarts =  num_restarts + 1 " +
                "where contract_id = #{id} ")
        void markContractWaiting(String contract_id);
        
        @Update("update dag_contracts set last_updated = now(), status = 'Ready', num_restarts =  num_restarts + 1 " +
                        "where contract_id = #{id} ")
        void reRunContract(String contract_id);
        
        @Update("update dag_deps set dep_status = 0 where " +
                "contract_id = #{contract_id} and dependent_contract_id = #{dependent_contract_id}")
        void resetDependency(Dependency dependency);
        
        @Select("select dag_id, status, count(*) as count from dag_contracts group by dag_id, status")
        List<DAGStatusCount> selectAllDAGCounts();
        
        @Select("select D.contract_id as contract_id, C.description as description, C.dag_id as dag_id, C.status as status from dag_deps D, dag_contracts C " +
                "where D.contract_id = C.contract_id and dependent_contract_id=#{id}")
        List<ContractSummary> selectAllPrerequisites(String contract_id);
        
        @Select("select D.contract_id, C.description, C.dag_id, C.status from dag_deps D, dag_contracts C " +
                "where D.contract_id = C.contract_id and dependent_contract_id=#{id} and D.dep_status=1")
        List<ContractSummary> selectCompletedPrerequisites(String contract_id);
        
        @Select("select * from dag_contracts where contract_id = #{id}")
        ContractDetails selectContractData(String contractID);
        
        @Select("select * from dag_logs where contract_id = #{id} order by log_time desc limit 30")
        List<LogEntry> selectContractLogs(String contractID);
        
        @Select("select * from dag_contracts where dag_id = #{dag_id} and status = #{status}")
        List<ContractDetails> selectContracts(ContractFilter filter);
        
        @Select("select dag_id, status, count(*) as count from dag_contracts where dag_id = #{id} group by dag_id, status")
        List<DAGStatusCount> selectDAGCounts(String dagID);
        
        @Select("select * from dag_summary where dag_id = #{id}")
        DAGSummary selectDAGSummary(String dagID);
        
        @Select("select * from dag_summary order by start_time desc")
        List<DAGSummary> selectDAGSummaryData();
        
        @Select("select D.contract_id as contract_id, C.description as description, C.dag_id as dag_id, C.status as status from dag_deps D, dag_contracts C " +
                "where D.dependent_contract_id = C.contract_id and D.contract_id=#{id}")
        List<ContractSummary> selectDependents(String contract_id);
        
        @Select("select D.contract_id as contract_id, C.description as description, C.dag_id as dag_id, C.status as status from dag_deps D, dag_contracts C " +
                "where D.contract_id = C.contract_id and dependent_contract_id=#{id} and D.dep_status=0")
        List<ContractSummary> selectPendingPrerequisites(String contract_id);
    }
    
    private static Map<String, DAGDetails> getDAGMapByDAGID(Mapper mapper)
    {
        Map<String, DAGDetails> details = new Hashtable<String, DAGDetails>();
        List<DAGSummary> list = mapper.selectDAGSummaryData();
        for (DAGSummary summary : list)
        {
            details.put(summary.dag_id, new DAGDetails(summary));
        }
        return details;
    }
    
    public static List<DAGDetails> getDAGDetails(TejasContext self)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        
        Map<String, DAGDetails> details = getDAGMapByDAGID(mapper);
        List<DAGStatusCount> dagCounts = mapper.selectAllDAGCounts();
        
        for (DAGStatusCount dagStatusCount : dagCounts)
        {
            DAGDetails dagDetails = details.get(dagStatusCount.dag_id);
            if (dagDetails != null)
            {
                dagDetails.addContractCounts(dagStatusCount);
            }
        }
        
        List<DAGDetails> response = new ArrayList<DAGDetails>(details.values());
        
        Collections.sort(response, new Comparator<DAGDetails>() {
            @Override
            public int compare(DAGDetails a, DAGDetails b)
            {
                if (a.summary.status.code() != b.summary.status.code())
                {
                    return a.summary.status.code() - b.summary.status.code();
                }
                
                return (int) (a.summary.start_time.getTime() - b.summary.start_time.getTime());
            }
        });
        
        return response;
    }
    
    public static void markContractWaiting(TejasContext self, String contractID)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        mapper.markContractWaiting(contractID);
    }
    
    public static void resetDependency(TejasContext self, String contractID, String parentContractID)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        mapper.resetDependency(new Dependency(parentContractID, contractID));
    }
    
    public static void restartContract(TejasContext self, String contractID)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        mapper.reRunContract(contractID);
    }
    
    public static List<ContractSummary> selectAllPrerequisites(TejasContext self, String contractID)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        return mapper.selectAllPrerequisites(contractID);
    }
    
    public static List<ContractSummary> selectCompletedPrerequisites(TejasContext self, String contractID)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        return mapper.selectCompletedPrerequisites(contractID);
    }
    
    public static ContractDetails selectContract(TejasContext self, String contractID)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        ContractDetails contract = mapper.selectContractData(contractID);
        
        contract.completedPrerequisites = mapper.selectCompletedPrerequisites(contractID);
        contract.pendingPrerequisites = mapper.selectPendingPrerequisites(contractID);
        contract.dependents = mapper.selectDependents(contractID);
        
        contract.logs = mapper.selectContractLogs(contractID);
        
        return contract;
    }
    
    public static List<ContractDetails> selectContracts(TejasContext self, String dagID, CompletionStatus status)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        return mapper.selectContracts(new ContractFilter(dagID, status));
    }
    
    public static DAGDetails selectDAGDetails(TejasContext self, String dagID)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        DAGSummary dagSummary = mapper.selectDAGSummary(dagID);
        DAGDetails response = new DAGDetails(dagSummary);
        response.addContractCounts(mapper.selectDAGCounts(dagID).toArray(new DAGStatusCount[0]));
        return response;
    }
    
    public static List<ContractSummary> selectDependencies(TejasContext self, String contractID)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        return mapper.selectDependents(contractID);
    }
    
    public static List<ContractSummary> selectPendingPrerequisites(TejasContext self, String contractID)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        return mapper.selectPendingPrerequisites(contractID);
    }
    
}
