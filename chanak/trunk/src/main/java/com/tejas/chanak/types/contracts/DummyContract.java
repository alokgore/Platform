package com.tejas.chanak.types.contracts;

import com.tejas.core.TejasContext;


public class DummyContract extends SimpleContract
{	
    public DummyContract(String description)
    {
    	super(description);
    }
    
    @Override
    public Object execute(TejasContext self) throws Exception
    {
        return null;
    }
}
