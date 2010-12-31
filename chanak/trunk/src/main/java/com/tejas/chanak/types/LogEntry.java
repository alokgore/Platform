package com.tejas.chanak.types;

import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.tejas.core.TejasContext;
import com.tejas.utils.misc.StringUtils;
import com.tejas.utils.misc.TypeUtils;

public class LogEntry
{
    public interface Mapper
    {
        @Select("select count(*) from dag_logs where dag_id = #{id}")
        public long countDAGLog(String dagID);
        
        @Update("delete from dag_logs where dag_id = #{id}")
        public void deleteDAGLog(String dagID);
        
        @Select("select * from dag_logs where contract_id = #{contractID} order by log_time desc limit 20")
        public List<LogEntry> getLogEntries(String contractID);
        
        @Insert("insert into " +
                "dag_logs (dag_id, contract_id, log_message, error_reason, detailed_description, log_time) values " +
                " (#{dag_id}, #{contract_id}, #{log_message}, #{error_reason}, #{detailed_description}, #{log_time})")
        public void insertLog(LogEntry log);
    }
    
    public String dag_id;
    public String contract_id;
    public String log_message;
    public String error_reason;
    public String detailed_description;
    public Timestamp log_time = new Timestamp(System.currentTimeMillis());
    
    @Deprecated
    /**
     * Should be used only by MyBatis 
     */
    public LogEntry()
    {
        // NO-OP
    }
    
    public LogEntry(String dagID, String contractID, ContractExecutionReport report)
    {
        this.dag_id = dagID;
        this.contract_id = contractID;
        this.log_message = report.getStatus().name();
        Throwable exception = report.getCause();
        this.error_reason = StringUtils.makeNullSafe((exception != null) ? exception.toString() : report.getReason());
        this.detailed_description = StringUtils.makeNullSafe((exception != null) ? StringUtils.serializeToString(exception) : report.getDetailedDescription());
    }
    
    public LogEntry(String dagID, String contractID, String message)
    {
        this.dag_id = dagID;
        this.contract_id = contractID;
        this.log_message = message;
        this.error_reason = "";
        this.detailed_description = "";
    }
    
    public boolean equals(LogEntry that)
    {
        return TypeUtils.equals(this.dag_id, that.dag_id) &&
                TypeUtils.equals(this.contract_id, that.contract_id) &&
                TypeUtils.equals(this.log_message, that.log_message) &&
                TypeUtils.equals(this.error_reason, that.error_reason) &&
                TypeUtils.equals(this.detailed_description, that.detailed_description) &&
                this.log_time.getTime() / 1000 == that.log_time.getTime() / 1000;
    }
    
    @Override
    public boolean equals(Object that)
    {
        if (that instanceof LogEntry)
        {
            return equals((LogEntry) that);
        }
        return false;
    }
    
    public String getContract_id()
    {
        return this.contract_id;
    }
    
    public String getDag_id()
    {
        return this.dag_id;
    }
    
    public String getDetailed_description()
    {
        return this.detailed_description;
    }
    
    public String getError_reason()
    {
        return this.error_reason;
    }
    
    public String getLog_message()
    {
        return this.log_message;
    }
    
    public Timestamp getLog_time()
    {
        return this.log_time;
    }
    
    @Override
    public int hashCode()
    {
        // Dummy
        return 0;
    }
    
    public void insertIntoDB(TejasContext self)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        mapper.insertLog(this);
    }
    
    public void setContract_id(String contract_id)
    {
        this.contract_id = contract_id;
    }
    
    public void setDag_id(String dag_id)
    {
        this.dag_id = dag_id;
    }
    
    public void setDetailed_description(String detailed_description)
    {
        this.detailed_description = detailed_description;
    }
    
    public void setError_reason(String error_reason)
    {
        this.error_reason = error_reason;
    }
    
    public void setLog_message(String log_message)
    {
        this.log_message = log_message;
    }
    
    public void setLog_time(Timestamp log_time)
    {
        this.log_time = log_time;
    }
    
    @Override
    public String toString()
    {
        return ReflectionToStringBuilder.toString(this);
    }
}
