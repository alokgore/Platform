package com.tejas.chanak.types.contracts;

public class StartContract extends DummyContract 
{
	public StartContract() 
	{
		this("DAG");
	}

	public StartContract(String description) 
	{
		super("Start contract for [" + description + "]");
	}
}
