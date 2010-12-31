package com.tejas.chanak.monitoring;

import java.util.Collections;
import java.util.List;

import com.tejas.chanak.console.ContractDetailsServlet;
import com.tejas.chanak.types.AlarmThreshold;
import com.tejas.core.TejasContext;
import com.tejas.core.enums.PlatformComponents;
import com.tejas.utils.console.ConsoleUtils;

public class MonitoringUtils
{
    public static void raiseAlarm(TejasContext self, String dagID, String contractID, long thresholdParameterValue, AlarmThreshold defaultAlarmThreshold, String msg)
    {
        msg += "Contract ID = [" + contractID + "]. DagID = [" + dagID + "].\n";
        msg += "Visit [" + ConsoleUtils.getServletURL(ContractDetailsServlet.class) + "cid=" + contractID + "] for details";
        
        self.logger.error(msg);
        
        List<AlarmThreshold> alarmThresholds = AlarmThreshold.getAlarmThresholds(self, contractID, defaultAlarmThreshold.thresholdType);
        if (alarmThresholds.size() == 0)
        {
            alarmThresholds.add(defaultAlarmThreshold);
        }
        
        /*
         * Sort the list in the decreasing order of the alarm-thresholds Alarm ONLY on the first match;
         */
        Collections.sort(alarmThresholds);
        
        for (AlarmThreshold threshold : alarmThresholds)
        {
            if (threshold.thresholdValue < thresholdParameterValue)
            {
                self.alarm(threshold.alarmSeverity, PlatformComponents.DAG_MANAGER, threshold.alarmName, "", threshold.alarmDescription + "\n" + msg);
                break;
            }
        }
    }
    
}
