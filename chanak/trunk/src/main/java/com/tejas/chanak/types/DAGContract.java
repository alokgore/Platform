package com.tejas.chanak.types;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.tejas.chanak.core.DAGConcurrencyController;
import com.tejas.chanak.types.orm.ContractDetails;
import com.tejas.core.TejasContext;
import com.tejas.core.TejasDBLayer;
import com.tejas.dbl.TejasDBLTransaction;
import com.thoughtworks.xstream.XStream;

public abstract class DAGContract
{
    public enum CompletionStatus
    {
            /**
             * This state indicates that the Contract is waiting for dependencies on other Contracts to get cleared.
             */
            Waiting(0),
            
            /**
             * This indicates that the Contract is ready for execution.
             */
            Ready(1),
            
            /**
             * Indicates that the execution has begun.
             */
            InProgress(2),
            
            /**
             * This indicates that the task has been delegated to someone on a remote-host and this machine is just supposed to wait for result now
             */
            Enqueued(3),
            
            /**
             * This state indicates that the contract was 'Ready' or 'InProgress' and it got suspended from the console. Scheduler ignores Dormant Contracts.
             */
            Dormant(4),
            
            /**
             * This state indicates that the contract was 'Waiting' and it got suspended from the console. Scheduler ignores Dormant Contracts.
             */
            DormantWaiting(5),
            
            /**
             * And... Our job is done here!
             */
            Complete(6), ;
        
        private int order;
        
        private CompletionStatus(int order)
        {
            this.order = order;
        }
        
        public int getOrder()
        {
            return this.order;
        }
    }
    
    public static class ContractOutput
    {
        public String contract_id;
        
        public String dag_id;
        
        public String output_data;
        
        /**
         * @deprecated only for mybatis
         */
        @Deprecated
        public ContractOutput()
        {
            // only for mybatis
        }
        
        public ContractOutput(String dag_id, String contract_id, String output_data)
        {
            this.dag_id = dag_id;
            this.contract_id = contract_id;
            this.output_data = output_data;
        }
        
        public String getContract_id()
        {
            return this.contract_id;
        }
        
        public String getDag_id()
        {
            return this.dag_id;
        }
        
        public String getOutput_data()
        {
            return this.output_data;
        }
        
        public void setContract_id(String contract_id)
        {
            this.contract_id = contract_id;
        }
        
        public void setDag_id(String dag_id)
        {
            this.dag_id = dag_id;
        }
        
        public void setOutput_data(String output_data)
        {
            this.output_data = output_data;
        }
    }
    
    public interface Mapper
    {
        @Update("update dag_deps set dep_status = 1 where contract_id = #{id}")
        public void clearDependencies(String contractID);
        
        @Select("select num_restarts from dag_contracts where contract_id = #{id}")
        public Number countNumRestarts(String contractID);
        
        @Select("select output_data from dag_deps join dag_outputs using (contract_id) " +
                "where dependent_contract_id = #{id}")
        public List<String> getParentsOutput(String childContractID);
        
        @Insert("insert into " +
                " dag_contracts(dag_id, contract_id, description, status, contract, start_time, last_updated, next_retry_time) " +
                " values (#{dag_id}, #{contract_id}, #{description}, #{status}, #{contract}, now(), now(), now())")
        public void insert(ContractDetails contract);
        
        @Update("replace into dag_outputs(dag_id, contract_id, output_data) " +
                "values (#{dag_id}, #{contract_id}, #{output_data})")
        public void insertOutput(ContractOutput output);
        
        @Update("update dag_contracts set status = 'Complete', last_updated = now() where contract_id = #{id} " +
                // Need this extra clause to guard against the breaches of tree-restarts
                "and status in ('InProgress', 'Dormant') ")
        public void markComplete(String contractID);
        
        @Update("update dag_contracts set status = 'Enqueued', last_updated = now() where contract_id = #{id} " +
                // Need this extra clause to guard against the breaches of tree-restarts
                "and status in ('InProgress', 'Dormant') ")
        public void markEnqueued(String contractID);
        
        @Update("update dag_contracts set status = 'InProgress', last_updated = now(), retry = 0 where contract_id = #{id}")
        public void markInProgress(String contractID);
        
        @Update("update dag_contracts set last_updated = now(), retry = 1, num_restarts =  num_restarts + 1, " +
                        "next_retry_time = #{next_retry_time}  where contract_id = #{contract_id} and status = 'InProgress' ")
        public Number restart(RestartData restartData);
        
        @Update("update dag_contracts set last_updated = now(), next_retry_time = now()," +
                "status = 'Ready' where contract_id = #{id} and  status = 'Dormant' ")
        public void resume(String contractID);
        
        @Update("update dag_contracts set last_updated = now(), retry = 1, num_restarts =  num_restarts + 1, " +
                "next_retry_time = now() where contract_id = #{contract_id} and last_updated = #{last_updated} and status = 'InProgress' ")
        public Number revive(RevivalCondition condition);
        
        @Select("select contract from dag_contracts where contract_id = #{id}")
        public String selectContractXML(String contractID);
        
        @Update("update dag_contracts set last_updated = now(), retry = 0, " +
                "status = 'Dormant' where contract_id = #{id} and status = 'InProgress' ")
        public void suspend(String contractID);
        
    }
    
    public static class RestartData
    {
        public String contract_id;
        public Timestamp next_retry_time;
        
        /**
         * @deprecated only for mybatis
         */
        @Deprecated
        public RestartData()
        {
            // Only for mybatis
        }
        
        public RestartData(String contract_id, Timestamp next_retry_time)
        {
            this.contract_id = contract_id;
            this.next_retry_time = next_retry_time;
        }
        
        public String getContract_id()
        {
            return this.contract_id;
        }
        
        public Timestamp getNext_retry_time()
        {
            return this.next_retry_time;
        }
        
        public void setContract_id(String contract_id)
        {
            this.contract_id = contract_id;
        }
        
        public void setNext_retry_time(Timestamp next_retry_time)
        {
            this.next_retry_time = next_retry_time;
        }
    }
    
    public static class RevivalCondition
    {
        public String contract_id;
        public Timestamp last_updated; // Optimistic locking
        
        /**
         * @deprecated only for mybatis
         */
        @Deprecated
        public RevivalCondition()
        {
            // only for mybatis
        }
        
        public RevivalCondition(String contract_id, Timestamp last_updated)
        {
            this.contract_id = contract_id;
            this.last_updated = last_updated;
        }
        
        public String getContract_id()
        {
            return this.contract_id;
        }
        
        public Timestamp getLast_updated()
        {
            return this.last_updated;
        }
        
        public void setContract_id(String contract_id)
        {
            this.contract_id = contract_id;
        }
        
        public void setLast_updated(Timestamp last_updated)
        {
            this.last_updated = last_updated;
        }
    }
    
    private static final float CONTRACT_BACKOFF_FACTOR = 1.5f;
    
    private static final long CONTRACT_BACKOFF_TIME_CAP = 10 * 60 * 1000L;
    
    private static final long INITIAL_CONTRACT_RETRY_INTERVAL = 5000;
    
    public static XStream xstream = new XStream();
    
    public static DAGContract deserialize(String xml)
    {
        return ((DAGContract) xstream.fromXML(xml));
    }
    
    public static final DAGContract getInstance(TejasContext self, String contractID)
    {
        return deserialize(self.dbl.getMybatisMapper(Mapper.class).selectContractXML(contractID));
    }
    
    private List<AlarmThreshold> _alarmThresholds = new ArrayList<AlarmThreshold>();
    
    /**
     * ContractID of this contract. This is a uniqueue string _ACROSS_ all queries.
     */
    private String _contractID;
    
    /**
     * ID of the dependency graph that contains this contract
     */
    private String _dagID;
    
    private CompletionStatus contractCompletionStatus;
    
    private String description;
    
    /**
     * @deprecated Only for mybatis
     */
    @Deprecated
    public DAGContract()
    {
        this("DAGContract - NO DESCRIPTION");
    }
    
    public DAGContract(String description)
    {
        this._contractID = DependencyGraph.getNewID();
        this.description = (description == null || description.trim().equals("")) ? getClass().getSimpleName() : description;
        this.contractCompletionStatus = CompletionStatus.Waiting;
    }
    
    void setContractCompletionStatus(CompletionStatus completionStatus)
    {
        this.contractCompletionStatus = completionStatus;
    }
    
    void setDAGID(String queryID)
    {
        this._dagID = queryID;
    }
    
    public void addAlarmThreshold(AlarmThreshold threshold)
    {
        this._alarmThresholds.add(threshold);
    }
    
    public List<AlarmThreshold> getAlarmThresholds()
    {
        return this._alarmThresholds;
    }
    
    public CompletionStatus getContractCompletionStatus()
    {
        return this.contractCompletionStatus;
    }
    
    public String getContractID()
    {
        return this._contractID;
    }
    
    public String getDAGID()
    {
        if (this._dagID == null)
        {
            throw new IllegalStateException(
                    "DAG-ID on this contract is null at this point. This will be set at the time of DAG-Creation. You can not access it till then");
        }
        return this._dagID;
    }
    
    public String getDescription()
    {
        return this.description;
    }
    
    /**
     * All the profiling (count of calls, status, latency etc) for this Contract will be done using this metrics name. By default, the metrics name is the
     * simple name of the Contract class. People who want a different metrics name should override this method.
     */
    public String getMetricsName()
    {
        return this.getClass().getSimpleName();
    }
    
    public List<Object> getParentContractsOutput(TejasContext self)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        List<String> outputXMLs = mapper.getParentsOutput(this.getContractID());
        List<Object> response = new ArrayList<Object>();
        for (String xml : outputXMLs)
        {
            response.add(xstream.fromXML(xml));
        }
        return response;
    }
    
    /**
     * Used to control concurrency of contract execution in the DAG-Manager.
     * 
     * @see {@link DAGConcurrencyController} DAG-Manager runs the contracts in parallel. But some contracts (like the JDBC-pipe contract that pulls data from
     *      the Trust-DW) need to make sure that the resource-usage should be controlled (in terms of how many people can work on it concurrenly). If the
     *      contract is using a resource that is concurrency controlled, it should override this method
     */
    public String getResourceID()
    {
        return "";
    }
    
    public synchronized String getXML()
    {
        return xstream.toXML(this);
    }
    
    public void insertInToDB(TejasContext self)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        mapper.insert(new ContractDetails(this._dagID, this._contractID, this.description, this.contractCompletionStatus, getXML()));
        
        for (AlarmThreshold threshold : this._alarmThresholds)
        {
            threshold.insertIntoDB(self, getDAGID(), getContractID());
        }
    }
    
    public void log(TejasContext self, ContractExecutionReport report)
    {
        LogEntry logEntry = new LogEntry(getDAGID(), getContractID(), report);
        logEntry.insertIntoDB(self);
    }
    
    public void log(TejasContext self, String message)
    {
        LogEntry logEntry = new LogEntry(getDAGID(), getContractID(), message);
        logEntry.insertIntoDB(self);
    }
    
    /**
     * Marks the contract as complete and inserts the output of the contract in the DB.
     */
    public final void markCompletion(TejasContext self, final ContractExecutionReport report)
    {
        self.logger.info("Marking the contract ", this, " as complete");
        
        new TejasDBLTransaction(self.dbl) {
            @Override
            public void doInTransaction(TejasDBLayer dbl)
            {
                Mapper mapper = dbl.getMybatisMapper(Mapper.class);
                
                mapper.markComplete(getContractID());
                if (report.getData() != null)
                {
                    mapper.insertOutput(new ContractOutput(getDAGID(), getContractID(), xstream.toXML(report.getData())));
                }
                
                mapper.clearDependencies(getContractID());
            }
        }.execute();
        
        self.logger.info("Marked the contract ", this, " as complete");
    }
    
    /**
     * Mark the contract as 'Enqueued'. This is the state that indicates that a remote host is responsible for the execution of this contract and the
     * local-machine is just waiting for the remote-host.
     */
    public final void markEnqueued(TejasContext self)
    {
        self.logger.info("Marking the contract ", this, "  'Enqueued' ");
        
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        mapper.markEnqueued(getContractID());
        
        self.logger.info("Marked the contract ", this, "  'Enqueued' ");
    }
    
    public final void markInProgress(TejasContext self)
    {
        self.logger.debug("Marking the contract ", this, "  'Enqueued' ");
        
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        mapper.markInProgress(getContractID());
        
        self.logger.debug("Marked the contract ", this, "  'Enqueued' ");
    }
    
    /**
     * Schedules the contract for a restart (if the contract is 'InProgress'). Takes the backOff policy in consideration while calculating the next-rerty time.
     */
    public final void restart(TejasContext self)
    {
        self.logger.info("Marking the contract ", this, "  'up for a restart' ");
        
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        
        long numRestarts = mapper.countNumRestarts(getContractID()).longValue();
        long backOffTime = (long) (INITIAL_CONTRACT_RETRY_INTERVAL * Math.pow(CONTRACT_BACKOFF_FACTOR, numRestarts));
        backOffTime = Math.min(backOffTime, CONTRACT_BACKOFF_TIME_CAP);
        Timestamp nextRetryTime = new Timestamp(System.currentTimeMillis() + backOffTime);
        mapper.restart(new RestartData(getContractID(), nextRetryTime));
        
        self.logger.info("Marked the contract ", this, "  'up for a restart' ");
    }
    
    /**
     * Schedules the contract for an immediate restart (if the contract is 'InProgress').
     */
    public final void restartNow(TejasContext self)
    {
        self.logger.info("Marking the contract ", this, "  'up for a restart' NOW");
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        
        mapper.restart(new RestartData(getContractID(), new Timestamp(System.currentTimeMillis())));
        
        self.logger.info("Marked the contract ", this, "  'up for a restart' NOW");
    }
    
    public final void resume(TejasContext self)
    {
        self.logger.info("Resuming the contract ", this);
        
        self.dbl.getMybatisMapper(Mapper.class).resume(getContractID());
        
        self.logger.info("Resumed the contract ", this);
    }
    
    public abstract ContractExecutionReport run(TejasContext self) throws Exception;
    
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    /**
     * Makes the cotract 'Dormant'. This contract will not be retried untill someone calls "resume()" on the contract. The current execution (if one is in
     * progress) will not be disturbed though.
     */
    public final void suspend(TejasContext self)
    {
        self.logger.info("Suspending the contract ", this);
        
        self.dbl.getMybatisMapper(Mapper.class).suspend(getContractID());
        
        self.logger.info("Suspended the contract ", this);
    }
    
    @Override
    public final String toString()
    {
        return this.getContractID() + "@" + this.getDAGID();
    }
}
