package com.tejas.chanak.types;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.tejas.utils.misc.TypeUtils;

public class ContractConfiguration
{
    public interface Mapper
    {
        @Select("select count(*) From dag_contract_config where contract_id = #{id}")
        public long countConfig(String contractID);
        
        @Update("delete From dag_contract_config where dag_id = #{id}")
        public void deleteConfig(String dagID);
        
        @Select("select * From dag_contract_config where contract_id = #{id}")
        public List<ContractConfiguration> getConfigForContract(String contractID);
        
        @Select("select * From dag_contract_config where contract_id = #{contract_id} and config_key = #{config_key}")
        public List<ContractConfiguration> getConfigForKey(SelectKey key);
        
        @Insert("insert into dag_contract_config (dag_id, contract_id, config_key, config_value) " +
                "values (#{dag_id}, #{contract_id}, #{config_key}, #{config_value})")
        public void insert(ContractConfiguration config);
    }
    
    public static class SelectKey
    {
        public String contract_id;
        public String config_key;
        
        public SelectKey()
        {
            // For mybatis
        }
        
        public SelectKey(String contract_id, String config_key)
        {
            this.contract_id = contract_id;
            this.config_key = config_key;
        }
        
        public String getConfig_key()
        {
            return this.config_key;
        }
        
        public String getContract_id()
        {
            return this.contract_id;
        }
        
        public void setConfig_key(String config_key)
        {
            this.config_key = config_key;
        }
        
        public void setContract_id(String contract_id)
        {
            this.contract_id = contract_id;
        }
    }
    
    public String dag_id;
    public String contract_id;
    public String config_key;
    public String config_value;
    
    /**
     * @deprecated Only for mybatis
     */
    @Deprecated
    public ContractConfiguration()
    {
        // For MyBatis
    }
    
    public ContractConfiguration(String dag_id, String contract_id, String config_key, String config_value)
    {
        this.dag_id = dag_id;
        this.contract_id = contract_id;
        this.config_key = config_key;
        this.config_value = config_value;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ContractConfiguration)
        {
            ContractConfiguration that = (ContractConfiguration) obj;
            
            return TypeUtils.equals(this.dag_id, that.dag_id) &&
                    TypeUtils.equals(this.contract_id, that.contract_id) &&
                    TypeUtils.equals(this.config_key, that.config_key) &&
                    TypeUtils.equals(this.config_value, that.config_value);
        }
        
        return false;
    }
    
    public String getConfig_key()
    {
        return this.config_key;
    }
    
    public String getConfig_value()
    {
        return this.config_value;
    }
    
    public String getContract_id()
    {
        return this.contract_id;
    }
    
    public String getDag_id()
    {
        return this.dag_id;
    }
    
    @Override
    public int hashCode()
    {
        return 0;
    }
    
    public void setConfig_key(String config_key)
    {
        this.config_key = config_key;
    }
    
    public void setConfig_value(String config_value)
    {
        this.config_value = config_value;
    }
    
    public void setContract_id(String contract_id)
    {
        this.contract_id = contract_id;
    }
    
    public void setDag_id(String dag_id)
    {
        this.dag_id = dag_id;
    }
    
}
