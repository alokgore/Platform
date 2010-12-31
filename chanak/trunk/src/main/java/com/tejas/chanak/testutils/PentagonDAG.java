package com.tejas.chanak.testutils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;

import com.tejas.chanak.types.DAGContract;
import com.tejas.chanak.types.Dependency;
import com.tejas.chanak.types.DependencyGraph;
import com.tejas.chanak.types.contracts.SimpleContract;
import com.tejas.core.TejasContext;

public class PentagonDAG extends DependencyGraph
{

    public static class MapPutContract extends SimpleContract
	{
	    public MapPutContract()
	    {
	        super("DummyContractForPutInMap");
	    }
	
	    @Override
        public Object execute(TejasContext self) throws Exception
	    {
	        System.err.println("Putting a key in the map for verification");
	        String key=null;
	        
	        //Just put a string in the map.
	        key = RandomStringUtils.randomAlphabetic(10);
	        long value = RandomUtils.nextInt(100) + 1;
	        PentagonDAG.values.add(value);
	        System.err.println("Putting "+value + " in the map");
	        PentagonDAG._map.put(key, value);
	
	        return key;
	    }
	}

	public static class SquaringContract extends SimpleContract
	{
	    public SquaringContract()
	    {
	        super("DummyContractForSquaring");
	    }
	
	    @Override
        public Object execute(TejasContext self) throws Exception
	    {
	        List<Object> outputs = getParentContractsOutput(self);
	        System.err.println("Putting a key in the map for verification");
	        String key=outputs.get(0).toString();
	        Long number = PentagonDAG._map.get(key);
	        PentagonDAG._map.put(key, number*number);System.err.println("**************i am executing squaring for "+number+". the result is "+number*number);
	        return key;
	    }
	}

	public static class SummingContract extends SimpleContract
	{
	    public SummingContract()
	    {
	        super("DummyContractForSumming");
	    }
	
	    @Override
        public Object execute(TejasContext self) throws Exception
	    {
	        List<Object> outputs = getParentContractsOutput(self);
	        System.err.println("Putting a key in the map for verification");
	        String output0 = outputs.get(0).toString();
	        String output1 = outputs.get(1).toString();
	        Long number0 = PentagonDAG._map.get(output0);
	        Long number1 = PentagonDAG._map.get(output1);
	        String key=RandomStringUtils.randomAlphabetic(10);
	        PentagonDAG.outputKey = key;
	        PentagonDAG._map.put(key, number0+number1);
	        return null;
	    }
	}

	public static final Map<String, Long> _map = new Hashtable<String, Long>();
	public static ArrayList<Long> values = new ArrayList<Long>();
	public static String outputKey = "";

	public PentagonDAG()
    {
    	super("Dummy-Query");

    	DAGContract[] contracts;
        contracts = new SimpleContract[5];
        addContract(contracts[0] = new PentagonDAG.MapPutContract());
        addContract(contracts[1] = new PentagonDAG.MapPutContract()); 
        addContract(contracts[2] = new PentagonDAG.SquaringContract());
        addContract(contracts[3] = new PentagonDAG.SquaringContract());
        addContract(contracts[4] = new PentagonDAG.SummingContract());

        addDependency(new Dependency(contracts[0], contracts[2]));
        addDependency(new Dependency(contracts[1], contracts[3]));
        addDependency(new Dependency(contracts[2], contracts[4]));
        addDependency(new Dependency(contracts[3], contracts[4]));
    }
}
