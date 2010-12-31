package com.tejas.chanak.types;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import com.tejas.core.TejasContext;

public class Dependency
{
    
    public static class DepData
    {
        public String contract_id;
        public String dag_id;
        public String dependent_contract_id;
        
        /**
         * @deprecated only for mybatis
         */
        @Deprecated
        public DepData()
        {
            // only for mybatis
        }
        
        public DepData(String dag_id, String contract_id, String dependent_contract_id)
        {
            this.dag_id = dag_id;
            this.contract_id = contract_id;
            this.dependent_contract_id = dependent_contract_id;
        }
        
        public String getContract_id()
        {
            return this.contract_id;
        }
        
        public String getDag_id()
        {
            return this.dag_id;
        }
        
        public String getDependent_contract_id()
        {
            return this.dependent_contract_id;
        }
        
        public void setContract_id(String contract_id)
        {
            this.contract_id = contract_id;
        }
        
        public void setDag_id(String dag_id)
        {
            this.dag_id = dag_id;
        }
        
        public void setDependent_contract_id(String dependent_contract_id)
        {
            this.dependent_contract_id = dependent_contract_id;
        }
    }
    
    public interface Mapper
    {
        @Update("update dag_deps set dep_status = 1 where contract_id = #{contractID}")
        public void clearDependencies(String contractID);
        
        @Update("delete from dag_deps where dag_id = #{dagID}")
        public void deleteDependencies(String dagID);
        
        @Insert("insert into " +
                "dag_deps (dag_id, contract_id, dependent_contract_id) " +
                "values (#{dag_id}, #{contract_id}, #{dependent_contract_id}) ")
        public void insert(DepData data);
    }
    
    private String contract_id;
    private String dependent_contract_id;
    
    /**
     * @deprecated Only for MyBatis
     */
    @Deprecated
    public Dependency()
    {
        // Only for MyBatis
    }
    
    public Dependency(DAGContract contract, DAGContract dependentContract)
    {
        this.contract_id = contract.getContractID();
        this.dependent_contract_id = dependentContract.getContractID();
    }
    
    public Dependency(String contractID, String dependentContractID)
    {
        this.contract_id = contractID;
        this.dependent_contract_id = dependentContractID;
    }
    
    public String getContract_id()
    {
        return this.contract_id;
    }
    
    public String getDependent_contract_id()
    {
        return this.dependent_contract_id;
    }
    
    public void insertIntoDB(TejasContext self, String dagID)
    {
        Mapper mapper = self.dbl.getMybatisMapper(Mapper.class);
        mapper.insert(new DepData(dagID, this.contract_id, this.dependent_contract_id));
    }
    
    /**
     * @deprecated Only for MyBatis
     */
    @Deprecated
    public void setContract_id(String contract_id)
    {
        this.contract_id = contract_id;
    }
    
    /**
     * @deprecated Only for MyBatis
     */
    @Deprecated
    public void setDependent_contract_id(String dependent_contract_id)
    {
        this.dependent_contract_id = dependent_contract_id;
    }
    
}
