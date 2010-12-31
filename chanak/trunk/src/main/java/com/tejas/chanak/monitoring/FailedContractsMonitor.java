package com.tejas.chanak.monitoring;

import static com.tejas.chanak.types.AlarmThreshold.getFailureThreshold;
import static com.tejas.core.TejasEventHandler.Severity.ERROR;
import static com.tejas.core.enums.PlatformComponents.DAG_MANAGER;
import static com.tejas.core.enums.TejasAlarms.TOO_MANY_CONTRACT_FAILURES;

import java.util.List;

import com.tejas.chanak.monitoring.DAGMonitor.Mapper;
import com.tejas.chanak.types.AlarmThreshold;
import com.tejas.chanak.types.orm.ContractDetails;
import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasBackgroundJob;
import com.tejas.core.TejasContext;

class FailedContractsMonitor extends TejasBackgroundJob
{
    static final int FAILURE_THRESHOLD = ApplicationConfig.findInteger("dagmanager.monitoring.failedContractMonior.defaultFailureThreshold", 5);
    private static final int NAP_INTERVAL = ApplicationConfig.findInteger("dagmanager.monitoring.failedContractMonior.napInterval.seconds", 120) * 1000;
    
    static class FailedContractsMonitorTask implements Task
    {
        @Override
        public void runIteration(TejasContext self) throws Exception
        {
            Mapper mapper = self.dbl.getMybatisMapper(DAGMonitor.Mapper.class);
            List<ContractDetails> contracts = mapper.selectFailedContracts(FAILURE_THRESHOLD);
            for (ContractDetails contract : contracts)
            {
                String msg = "Detected too many failures (" + contract.num_restarts + ") on the contract";
                AlarmThreshold defaultAlarmThreshold = getFailureThreshold(FAILURE_THRESHOLD, TOO_MANY_CONTRACT_FAILURES, "", ERROR);
                MonitoringUtils.raiseAlarm(self, contract.dag_id, contract.contract_id, FAILURE_THRESHOLD, defaultAlarmThreshold, msg);
            }
        }
    }
    
    public FailedContractsMonitor()
    {
        super(new FailedContractsMonitorTask(), new Configuration.Builder("FailedContractsMonitor", DAG_MANAGER, NAP_INTERVAL).build());
    }
}
