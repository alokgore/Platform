package com.tejas.chanak.types;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tejas.chanak.types.ContractConfiguration.Mapper;
import com.tejas.chanak.types.ContractConfiguration.SelectKey;
import com.tejas.core.TejasContext;
import com.tejas.core.TejasEventHandler.Severity;
import com.thoughtworks.xstream.XStream;

public class AlarmThreshold implements Comparable<AlarmThreshold>
{
    public static enum ThresholdType
    {
            NumFailures,
            
            ExecutionTimeMillis,
        ;
        
        public String getContractSettingID()
        {
            return "AlarmThreshold_" + name();
        }
    }
    
    public final ThresholdType thresholdType;
    public final long thresholdValue;
    @SuppressWarnings("rawtypes")
    public final Enum alarmName;
    public final String alarmDescription;
    public final Severity alarmSeverity;
    
    @SuppressWarnings("rawtypes")
    public static AlarmThreshold getFailureThreshold(int numFailures, final Enum alarmName, final String alarmDescription, final Severity alarmSeverity)
    {
        return new AlarmThreshold(ThresholdType.NumFailures, numFailures, alarmName, alarmDescription, alarmSeverity);
    }
    
    @SuppressWarnings("rawtypes")
    public static AlarmThreshold getExecutionTimeThreshold(long executionTimeMillis, final Enum alarmName, final String alarmDescription, final Severity alarmSeverity)
    {
        return new AlarmThreshold(ThresholdType.ExecutionTimeMillis, executionTimeMillis, alarmName, alarmDescription, alarmSeverity);
    }
    
    @SuppressWarnings("rawtypes")
    private AlarmThreshold(final ThresholdType type, final long thresholdValue, final Enum alarmName, final String alarmDescription, final Severity alarmSeverity)
    {
        this.thresholdType = type;
        this.thresholdValue = thresholdValue;
        this.alarmName = alarmName;
        this.alarmDescription = alarmDescription;
        this.alarmSeverity = alarmSeverity;
    }
    
    public void insertIntoDB(TejasContext self, String dagID, String contractID) 
    {
        Mapper mapper = self.dbl.getMybatisMapper(ContractConfiguration.Mapper.class);
        ContractConfiguration config = new ContractConfiguration(dagID, contractID, thresholdType.getContractSettingID(), new XStream().toXML(this));
        mapper.insert(config);
    }
    
    public static List<AlarmThreshold> getAlarmThresholds(TejasContext self, String contractID, ThresholdType thresholdType) 
    {
        List<AlarmThreshold> thresholds = new ArrayList<AlarmThreshold>();
        
        List<ContractConfiguration> response =
                self.dbl.getMybatisMapper(ContractConfiguration.Mapper.class).getConfigForKey(new SelectKey(contractID, thresholdType.getContractSettingID()));
        for (ContractConfiguration config : response)
        {
            AlarmThreshold threshold = (AlarmThreshold) new XStream().fromXML(config.config_value);
            thresholds.add(threshold);
        }
        return thresholds;
    }
    
    @Override
    public int compareTo(AlarmThreshold obj)
    {
        int typeComparision = this.thresholdType.compareTo(obj.thresholdType);
        if (typeComparision != 0)
        {
            return typeComparision;
        }
        return (-1) * ((Long) this.thresholdValue).compareTo(obj.thresholdValue);
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof AlarmThreshold)
        {
            AlarmThreshold threshold = (AlarmThreshold) obj;
            return this.thresholdType == threshold.thresholdType && this.thresholdValue == threshold.thresholdValue;
        }
        return false;
    }
    
    @Override
    public String toString()
    {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SIMPLE_STYLE);
    }
    
    @Override
    public int hashCode()
    {
        return 0;
    }
    
}
