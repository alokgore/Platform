package com.tejas.chanak.testutils;

import com.tejas.chanak.types.DependencyGraph;
import com.tejas.chanak.types.contracts.SimpleContract;
import com.tejas.core.TejasContext;

public class FailureDAG extends DependencyGraph
{
	public static class FailureContract extends SimpleContract
	{
		public FailureContract()
		{
			super("FailureContract");
		}

		@Override
        public Object execute(TejasContext self) throws Exception
		{
			throw new IllegalStateException("Born to fail");
		}
	}
	
	public FailureDAG()
	{
		super("FailureDAG");
		FailureContract failureContract = new FailureContract();
		addContract(failureContract);
	}
}
