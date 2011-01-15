package com.tejas.chanak.scheduler;

import com.tejas.chanak.core.DAGManager;
import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasBackgroundJob;
import com.tejas.core.TejasContext;
import com.tejas.core.enums.PlatformComponents;

class DAGScavenger extends TejasBackgroundJob
{
    static class DAGScavengerTask extends AbstractTejasTask
    {
        @Override
        public void runIteration(TejasContext self, TejasBackgroundJob parent) throws Exception
        {
            DAGManager.cleanExpiredDAGs(self);
        }
    }

    static final long NAP_TIME = ApplicationConfig.findInteger("dagmanager.scavenger.sleepInterval.seconds", 60 * 60) * 1000;

    public DAGScavenger()
    {
        super(new DAGScavengerTask(), new Configuration.Builder("DAGScavenger", PlatformComponents.DAG_MANAGER, NAP_TIME).build());
    }

}
