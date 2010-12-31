package com.tejas.chanak.testutils;


import java.util.ArrayList;
import java.util.List;

import com.tejas.chanak.types.DAGContract;
import com.tejas.chanak.types.Dependency;
import com.tejas.chanak.types.DependencyGraph;
import com.tejas.chanak.types.contracts.DummyContract;

public class BigDAG extends DependencyGraph
{
	public BigDAG(int numRows, int numColumns)
	{
		super("Big-Graph numRows=" + numRows + ", numColumns=" + numColumns + " ");
		
		List<DAGContract> graphContracts = new ArrayList<DAGContract>();

		for (int i = 0; i < numRows; i++)
		{
			
			List<DAGContract> rowContracts = new ArrayList<DAGContract>();
			for (int j = 0; j < numColumns; j++)
			{
				DummyContract columnContract = new DummyContract("Dummy [" + i  + "] [" + j + "]");
				rowContracts.add(columnContract);
				addContract(columnContract);
				for (DAGContract dagContract : graphContracts)
				{
					addDependency(new Dependency(columnContract, dagContract));
				}
			}
			graphContracts.addAll(rowContracts);
		}
	}
}
