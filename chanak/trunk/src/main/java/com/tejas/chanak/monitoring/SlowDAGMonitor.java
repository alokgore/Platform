package com.tejas.chanak.monitoring;

import static com.tejas.core.enums.PlatformComponents.DAG_MANAGER;
import static com.tejas.core.enums.TejasAlarms.SLOW_RUNNING_DAG;

import java.sql.Timestamp;
import java.util.List;

import com.tejas.chanak.monitoring.DAGMonitor.Mapper;
import com.tejas.chanak.types.orm.DAGSummary;
import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasBackgroundJob;
import com.tejas.core.TejasContext;
import com.tejas.core.TejasEventHandler.Severity;

class SlowDAGMonitor extends TejasBackgroundJob
{
    private static final int NAP_INTERVAL = ApplicationConfig.findInteger("dagmanager.monitoring.slowDAGMonior.napInterval.seconds", 600) * 1000;
    static final long SLOW_DAG_THRESHOLD = ApplicationConfig.findInteger("dagmanager.monitoring.slowDAGMonior.defaultSlowDAGThreshold.minutes", 90) * 60L * 1000L;

    public SlowDAGMonitor()
    {
        super(new TejasContext(), new SlowDAGMonitorTask(), new Configuration.Builder("SlowDAGMonitor", DAG_MANAGER, NAP_INTERVAL).build());
    }

    public static class SlowDAGMonitorTask extends AbstractTejasTask
    {
        @Override
        public void runIteration(TejasContext self, TejasBackgroundJob parent) throws Exception
        {
            Timestamp slowThresholdTimestamp = new Timestamp(System.currentTimeMillis() - SLOW_DAG_THRESHOLD);

            Mapper mapper = self.dbl.getMybatisMapper(DAGMonitor.Mapper.class);
            List<DAGSummary> slowDAGs = mapper.selectSlowDAGs(slowThresholdTimestamp);

            for (DAGSummary dag : slowDAGs)
            {
                long timeSpentOnDAG = System.currentTimeMillis() - dag.start_time.getTime();
                String msg =
                        "DAG seems to be running too slow (" + (timeSpentOnDAG / 60000L) + " minutes). DAGID = [" + dag.dag_id + "]. Description = [" + dag.description
                                + "]";
                self.alarm(Severity.ERROR, DAG_MANAGER, SLOW_RUNNING_DAG, "", msg);
            }
        }
    }
}
