package com.tejas.chanak.monitoring;

import static com.tejas.chanak.types.AlarmThreshold.getExecutionTimeThreshold;
import static com.tejas.core.TejasEventHandler.Severity.ERROR;
import static com.tejas.core.enums.PlatformComponents.DAG_MANAGER;

import java.sql.Timestamp;
import java.util.List;

import com.tejas.chanak.monitoring.DAGMonitor.Mapper;
import com.tejas.chanak.scheduler.ContractRunner;
import com.tejas.chanak.types.AlarmThreshold;
import com.tejas.chanak.types.DAGContract;
import com.tejas.chanak.types.DAGContract.RevivalCondition;
import com.tejas.chanak.types.LogEntry;
import com.tejas.chanak.types.orm.ContractDetails;
import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasBackgroundJob;
import com.tejas.core.TejasContext;
import com.tejas.core.enums.TejasAlarms;
import com.tejas.types.exceptions.DBLayerException;

class SlowContractsMonitor extends TejasBackgroundJob
{
    static class SlowContractsMonitorTask extends AbstractTejasTask
    {
        /**
         * Keeping the in-memory and in-DB state in sync has never been too pleasant. There is an edge case in our way of doing things too. ContractEnqueuer
         * picks up the 'Ready' contracts from DB, marks them as 'InProgress' and assigns them to ContractRunner. ContractRunner executes the contract and marks
         * it as 'Complete' or 'Ready' again. (Depending upon the execution result) But the step of updating the contract status can fail. Which would leave the
         * system stalled. (The DB would think that the contract is InProgress and the in-memory job-queue would have forgotten about the contract) The
         * following function detects this situation and revives any such contract.
         * 
         * @throws DBLayerException
         */
        private void reviveContract(TejasContext self, String dagID, String contractID, Timestamp lastUpdationTime)
        {
            /**
             * The contract has been running for a long time. Check if the job-queue has it. Return if the job queue knows about it (which means that the
             * contract is actually running)
             */
            if (ContractRunner.isContractActive(contractID))
            {
                return;
            }

            /**
             * ContractRunner does not know about it. This could mean two things 1) ContractRunner had finished the contract long time back but failed to update
             * the DB state 2) ContractRunner finished the execution of the contract between a) The time we ran "select" to find the slow contracts AND b) Now
             * Update the contract staus to 'Ready' if it is case 1). (The way to detect that is by comparing it's last_updated timestamp with the timestamp
             * that we got when we selected it)
             */
            com.tejas.chanak.types.DAGContract.Mapper mapper = self.dbl.getMybatisMapper(DAGContract.Mapper.class);
            long numRowsUpdated = mapper.revive(new RevivalCondition(contractID, lastUpdationTime)).longValue();

            long timeSinceLastUpdate = System.currentTimeMillis() - lastUpdationTime.getTime();
            if (numRowsUpdated == 1)
            {
                String msg = "Contract [" + contractID + "] was stuck for " + timeSinceLastUpdate + " millis. It got revived";
                self.logger.fatal(msg);
                new LogEntry(dagID, contractID, msg).insertIntoDB(self);
            }
        }

        @Override
        public void runIteration(TejasContext self, TejasBackgroundJob parent) throws Exception
        {
            Timestamp slowThresholdTimestamp = new Timestamp(System.currentTimeMillis() - SLOW_CONTRACT_THRESHOLD);

            Mapper mapper = self.dbl.getMybatisMapper(DAGMonitor.Mapper.class);
            List<ContractDetails> slowContracts = mapper.selectSlowContracts(slowThresholdTimestamp);

            for (ContractDetails contract : slowContracts)
            {
                long timeSpentOnContract = System.currentTimeMillis() - contract.start_time.getTime();
                String msg = "Contract seems to be running too slow (" + (timeSpentOnContract / 60000L) + " minutes)";

                AlarmThreshold defaultAlarmThreshold = getExecutionTimeThreshold(SLOW_CONTRACT_THRESHOLD, TejasAlarms.SLOW_RUNNING_CONTRACT, "", ERROR);
                MonitoringUtils.raiseAlarm(self, contract.dag_id, contract.contract_id, SLOW_CONTRACT_THRESHOLD, defaultAlarmThreshold, msg);

                reviveContract(self, contract.dag_id, contract.contract_id, contract.last_updated);
            }
        }

    }

    private static final int NAP_INTERVAL = ApplicationConfig.findInteger("dagmanager.monitoring.slowContractMonior.napInterval.seconds", 120) * 1000;
    static final long SLOW_CONTRACT_THRESHOLD =
            ApplicationConfig.findInteger("dagmanager.monitoring.slowContractMonior.defaultSlowContractThreshold.minutes", 40) * 60L * 1000L;

    public SlowContractsMonitor()
    {
        super(new SlowContractsMonitorTask(), new Configuration.Builder("SlowContractsMonitor", DAG_MANAGER, NAP_INTERVAL).build());
    }

}
