package com.tejas.chanak.testutils;

import com.tejas.chanak.types.DependencyGraph;
import com.tejas.chanak.types.contracts.DummyContract;

public class StraightLineDAG extends DependencyGraph
{
	public StraightLineDAG()
	{
		super("Straight Line DAG");
		
		addContract(new DummyContract("Contract-1"));
		attachContractToCurrentStrand(new DummyContract("Contract-2"));
		attachContractToCurrentStrand(new DummyContract("Contract-3"));
		attachContractToCurrentStrand(new DummyContract("Contract-4"));
		attachContractToCurrentStrand(new DummyContract("Contract-5"));
		attachContractToCurrentStrand(new DummyContract("Contract-6"));
		attachContractToCurrentStrand(new DummyContract("Contract-7"));
		attachContractToCurrentStrand(new DummyContract("Contract-8"));
		attachContractToCurrentStrand(new DummyContract("Contract-9"));
		attachContractToCurrentStrand(new DummyContract("Contract-10"));
	}
}
