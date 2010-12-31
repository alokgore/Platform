package com.tejas.chanak.types;

import static com.tejas.chanak.types.DAGContract.CompletionStatus.Ready;
import static com.tejas.chanak.types.DAGContract.CompletionStatus.Waiting;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.tejas.chanak.types.contracts.SinkContract;
import com.tejas.chanak.types.contracts.StartContract;
import com.tejas.chanak.types.orm.DAGSummary;
import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasContext;

public class DependencyGraph
{
    public interface Mapper
    {
        @Select("select count(*) from dag_contracts where dag_id=#{id} and status != 'Complete'")
        public long countUnfinishedContracts(String dagID);
        
        @Insert("insert into dag_summary(dag_id, description, start_time) " +
                            " values (#{dag_id}, #{description}, now()) ")
        public void insert(DAGSummary summary);
        
        @Update("update dag_contracts set status = 'Ready', start_time = now() where status = 'Waiting' and contract_id in " +
                    "(select dependent_contract_id as cid from dag_deps group by dependent_contract_id having count(*)=sum(dep_status))")
        public Number processDAG();
        
        @Update("update dag_summary set status = 'InProgress' where dag_id = #{id} and status = 'Dormant'")
        public Number resumeDAG(String dagID);
        
        @Update("update dag_contracts set status = 'Ready', next_retry_time = now() where dag_id = #{id} and status = 'Dormant'")
        public Number resumeRunnableContracts(String dagID);
        
        @Update("update dag_contracts set status = 'Waiting' where dag_id = #{id} and status = 'DormantWaiting'")
        public Number resumeWaitingContracts(String dagID);
        
        @Select("select * from dag_summary where dag_id=#{id}")
        public DAGSummary selectDAGSummary(String dagID);
        
        @Select("select contract from dag_contracts where (status = 'Ready' or (status = 'InProgress' and retry = 1 and next_retry_time < now()))")
        public List<String> selectReadyContracts();
        
        @Update("update dag_summary set status = 'Dormant' where dag_id = #{id} and status = 'InProgress' ")
        public Number suspendDAG(String dagID);
        
        @Update("update dag_contracts set status = 'Dormant' where dag_id = #{id} and status in ('InProgress', 'Ready')")
        public Number suspendRunnableContracts(String dagID);
        
        @Update("update dag_contracts set status = 'DormantWaiting' where dag_id = #{id} and status = 'Waiting'")
        public Number suspendWaitingContracts(String dagID);
        
    }
    
    private static final int RANDOM_PART_LENGTH = 8;
    
    /**
     * Generate a random-id.
     */
    public static String getNewID()
    {
        long timestamp = System.currentTimeMillis();
        
        /*
         * Calling randomAlphabetic for the first char to prevent mysql from parsing the id as a number (hint: try executing 'create database 23e34')
         */
        String newID = RandomStringUtils.randomAlphabetic(1) + RandomStringUtils.randomAlphanumeric(RANDOM_PART_LENGTH);
        
        if (ApplicationConfig.isProduction())
        {
            newID += "_" + Long.toString(timestamp, 32);
        }
        
        return newID;
    }
    
    private String dagID;
    private DAGDefinition defaultDagDefinition;
    
    private String description;
    private List<Dependency> globalDependencies = new ArrayList<Dependency>();
    private SinkContract globalSinkContract;
    private StartContract globalStartContract;
    
    private List<DAGDefinition> subDagDefinitions = new ArrayList<DAGDefinition>();
    
    public DependencyGraph(String description)
    {
        this(description, new DAGDefinition());
    }
    
    public DependencyGraph(String description, DAGDefinition dagDefinition)
    {
        this.dagID = getNewID();
        this.defaultDagDefinition = dagDefinition;
        this.description = description;
    }
    
    public void addContract(DAGContract contract)
    {
        this.defaultDagDefinition.addContract(contract);
    }
    
    public void addDependency(DAGDefinition dagDefinition)
    {
        this.globalDependencies.add(new Dependency(this.defaultDagDefinition.getSinkContract(), dagDefinition.getStartContract()));
    }
    
    public void addDependency(DAGDefinition dagDefinition, DAGDefinition dependentDAGDefinition)
    {
        this.globalDependencies.add(new Dependency(dagDefinition.getSinkContract(), dependentDAGDefinition.getStartContract()));
    }
    
    public void addDependency(Dependency dependency)
    {
        this.defaultDagDefinition.addDependency(dependency);
    }
    
    public void addSubDAGDefinition(DAGDefinition dagDefinition)
    {
        if (this.subDagDefinitions.size() == 0)
        {
            this.globalStartContract = new StartContract();
            this.globalSinkContract = new SinkContract();
            this.globalDependencies.add(new Dependency(this.globalStartContract, this.defaultDagDefinition.getStartContract()));
            this.globalDependencies.add(new Dependency(this.defaultDagDefinition.getSinkContract(), this.globalSinkContract));
        }
        this.subDagDefinitions.add(dagDefinition);
        this.globalDependencies.add(new Dependency(this.globalStartContract, dagDefinition.getStartContract()));
        this.globalDependencies.add(new Dependency(dagDefinition.getSinkContract(), this.globalSinkContract));
    }
    
    /**
     * Adds the contract to the current chain (strand) of contracts. (Just a helper method that makes the dag-setup easy by automatically adding a dependency)
     */
    public void attachContractToCurrentStrand(DAGContract contract)
    {
        this.defaultDagDefinition.attachContractToCurrentStrand(contract);
    }
    
    public List<DAGContract> getContracts()
    {
        List<DAGContract> contracts = new ArrayList<DAGContract>();
        contracts.addAll(this.defaultDagDefinition.getContracts());
        if (this.subDagDefinitions.size() > 0)
        {
            contracts.add(this.globalStartContract);
            contracts.add(this.globalSinkContract);
            for (DAGDefinition dagDefinition : this.subDagDefinitions)
            {
                contracts.addAll(dagDefinition.getContracts());
            }
        }
        return contracts;
    }
    
    public synchronized String getDagID()
    {
        return this.dagID;
    }
    
    public List<Dependency> getDependencies()
    {
        List<Dependency> dependencies = new ArrayList<Dependency>();
        dependencies.addAll(this.defaultDagDefinition.getDependencies());
        if (this.subDagDefinitions.size() > 0)
        {
            dependencies.addAll(this.globalDependencies);
            for (DAGDefinition dagDefinition : this.subDagDefinitions)
            {
                dependencies.addAll(dagDefinition.getDependencies());
            }
        }
        return dependencies;
    }
    
    public synchronized String getDescription()
    {
        return this.description;
    }
    
    public DAGContract getStartContract()
    {
        if (this.subDagDefinitions.size() == 0)
        {
            return this.defaultDagDefinition.getStartContract();
        }
        return this.globalStartContract;
    }
    
    public List<DAGDefinition> getSubDAGS()
    {
        return this.subDagDefinitions;
    }
    
    /**
     * Sets up a link (dependency) between the barrier (of multiple strands) and the current strand. (Just a helper method that makes the dag-setup easy)
     */
    public void linkStrandBarrierToCurrentStrand(String barrierContractID)
    {
        this.defaultDagDefinition.linkStrandBarrierToCurrentStrand(barrierContractID);
    }
    
    public synchronized void setDescription(String description)
    {
        this.description = description;
    }
    
    public void start() throws Exception
    {
        TejasContext self = new TejasContext();
        self.dbl.startTransaction();
        try
        {
            start(self);
            self.dbl.commit();
        }
        finally
        {
            self.dbl.rollback();
        }
    }
    
    public void start(TejasContext self) throws Exception
    {
        try
        {
            if (!self.dbl.isTransactional())
            {
                throw new IllegalStateException("DAG setup function startQuery should be called only in a transactional context");
            }
            
            self.logger.info("Bootstrapping query [" + getDagID() + "]");
            
            Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
            mapper.insert(new DAGSummary(getDagID(), getDescription()));
            
            self.logger.info("Setting up contracts for [" + getDagID() + "]");
            
            DAGContract startContract = getStartContract();
            
            for (DAGContract contract : getContracts())
            {
                contract.setDAGID(getDagID());
                contract.setContractCompletionStatus(contract == startContract ? Ready : Waiting);
                contract.insertInToDB(self);
            }
            
            for (Dependency dependency : getDependencies())
            {
                dependency.insertIntoDB(self, getDagID());
            }
        }
        catch (Exception e)
        {
            self.logger.fatal(e, "Query Bootstrap for [" + getDagID() + "] failed !!");
            throw e;
        }
    }
}
