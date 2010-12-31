package com.tejas.chanak.scheduler;

import java.util.List;

import com.tejas.chanak.types.DAGContract;
import com.tejas.chanak.types.DependencyGraph;
import com.tejas.chanak.types.DependencyGraph.Mapper;
import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasBackgroundJob;
import com.tejas.core.TejasContext;
import com.tejas.core.enums.PlatformComponents;

class ContractEnqueuer extends TejasBackgroundJob
{
    private static final int NAP_TIME = ApplicationConfig.findInteger("dagmanager.contractEnqueuer.sleepInterval.millis", 2000);

    static class ContractEnqueuerTask implements Task
    {
        private DAGScheduler scheduler;
        public ContractEnqueuerTask(DAGScheduler scheduler)
        {
            this.scheduler = scheduler;
        }

        @Override
        public void runIteration(TejasContext self) throws Exception
        {
            Mapper mapper = self.dbl.getMybatisMapper(DependencyGraph.Mapper.class);
            
            /*
             * Mark all the contracts that have been dependency-cleared 'Ready'
             */
            mapper.processDAG();
            
            List<String> contractXMLs = mapper.selectReadyContracts();
            
            for (String xml : contractXMLs)
            {
                DAGContract contract = DAGContract.deserialize(xml);
                contract.markInProgress(self);
                scheduler.scheduleTask(new ContractRunner(contract));
            }
        }
    }
    
    public ContractEnqueuer(DAGScheduler scheduler)
    {
        super(new ContractEnqueuerTask(scheduler), new Configuration.Builder("DAGContractEnqueuer", PlatformComponents.DAG_MANAGER, NAP_TIME).build());
    }
}