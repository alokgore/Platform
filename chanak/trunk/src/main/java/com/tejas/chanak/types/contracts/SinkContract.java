package com.tejas.chanak.types.contracts;

import com.tejas.chanak.core.DAGManager;
import com.tejas.chanak.types.DependencyGraph;
import com.tejas.core.TejasContext;

public class SinkContract extends SimpleContract
{
    public SinkContract(String dagName)
    {
        super("Sink Contract for " + dagName);
    }
    
    public SinkContract()
    {
        super("Sink contract");
    }
    
    @Override
    public Object execute(TejasContext self) throws Exception
    {
        String dagID = getDAGID();
        com.tejas.chanak.types.DependencyGraph.Mapper mapper = self.dbl.getMybatisMapper(DependencyGraph.Mapper.class);
        long numberOfUnfinishedContracts = mapper.countUnfinishedContracts(dagID);
        /*
         * Mark the query as complete only if the number of unfinished contracts for this dag is 1 (current contract is still in progress)
         */
        if (numberOfUnfinishedContracts == 1)
        {
            DAGManager.markQueryCompletion(self, dagID);
        }
        
        mapper.countUnfinishedContracts(getDAGID());
        
        return null;
    }
}
