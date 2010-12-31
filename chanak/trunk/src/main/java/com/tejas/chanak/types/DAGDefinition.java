package com.tejas.chanak.types;

import java.util.ArrayList;
import java.util.List;

import com.tejas.chanak.types.contracts.SinkContract;
import com.tejas.chanak.types.contracts.StartContract;

public class DAGDefinition
{
    private List<DAGContract> _contracts;
    private List<Dependency> _dependencies;
    private StartContract _startContract;
    private SinkContract _sinkContract;
    
    public DAGDefinition()
    {
        _contracts = new ArrayList<DAGContract>();
        _dependencies = new ArrayList<Dependency>();
        _startContract = new StartContract(getClass().getSimpleName());
        _sinkContract = new SinkContract(getClass().getSimpleName());
        
        _contracts.add(_startContract);
        _contracts.add(_sinkContract);
        _dependencies.add(new Dependency(_startContract, _sinkContract));
    }
    
    public List<DAGContract> getContracts()
    {
        return _contracts;
    }
    
    public void addContract(DAGContract contract)
    {
        _contracts.add(contract);
        addDependency(new Dependency(getStartContract(), contract));
        addDependency(new Dependency(contract, getSinkContract()));
    }
    
    public List<Dependency> getDependencies()
    {
        return _dependencies;
    }
    
    public void addDependency(Dependency dependency)
    {
        _dependencies.add(dependency);
    }
    
    public void addDependency(DAGContract contract, DAGContract dependentContract)
    {
        _dependencies.add(new Dependency(contract, dependentContract));
    }
    
    public DAGContract getStartContract()
    {
        return _startContract;
    }
    
    public DAGContract getSinkContract()
    {
        return _sinkContract;
    }
    
    public void attachContractToCurrentStrand(DAGContract contract)
    {
        DAGContract currentStrandEndContract = getCurrentStrandEndContract();
        addContract(contract);
        addDependency(currentStrandEndContract, contract);
    }
    
    protected void linkStrandBarrierToCurrentStrand(String barrierContractID)
    {
        DAGContract currentStrandEndContract = getCurrentStrandEndContract();
        addDependency(new Dependency(currentStrandEndContract.getContractID(), barrierContractID));
    }
    
    private DAGContract getCurrentStrandEndContract()
    {
        List<DAGContract> contracts = getContracts();
        DAGContract currentStrandEndContract = contracts.get(contracts.size() - 1);
        return currentStrandEndContract;
    }
}
