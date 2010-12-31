package com.tejas.chanak.types.contracts;

import com.tejas.chanak.types.ContractExecutionReport;
import com.tejas.chanak.types.DAGContract;
import com.tejas.core.TejasContext;


public abstract class SimpleContract extends DAGContract
{

	public SimpleContract(String description)
	{
		super(description);
	}

	@Override
    public final ContractExecutionReport run(TejasContext self) throws Exception
	{
		return new ContractExecutionReport(execute(self));
	}
	
	public abstract Object execute(TejasContext self) throws Exception;
	
}
