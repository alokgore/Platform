package com.tejas.utils.soa;

import java.util.List;
import java.util.Vector;

import com.tejas.utils.soa.ComponentHealth.Status;


public abstract class ShallowHealthReporterBase implements IShallowHealthReporter
{
	private final String componentName;
	private final List<IShallowHealthReporter> subcomponents = new Vector<IShallowHealthReporter>();
	
	public ShallowHealthReporterBase(String componentName)
	{
		this.componentName = componentName;
	}

	@Override
	public String getComponentName()
	{
		return componentName;
	}

	@Override
	public List<? extends IShallowHealthReporter> getSubcomponents()
	{
		return subcomponents;
	}
	
	protected void addSubcomponent(IShallowHealthReporter subcomponent)
	{
		subcomponents.add(subcomponent);
	}

	/**
	 * ;-)
	 */
	protected ComponentHealth ALL_IZZ_WELL()
	{
		return new ComponentHealth.Builder(getComponentName(), Status.GREEN).build();
	}
}
