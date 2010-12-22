package com.tejas.utils.soa;

import com.tejas.utils.soa.ComponentHealth.Status;

public class DeepHealthReporter
{
	IShallowHealthReporter shallowHealthReporter;

	public DeepHealthReporter(IShallowHealthReporter shallowHealthReporter)
	{
		this.shallowHealthReporter = shallowHealthReporter;
	}
	
	public ComponentHealth getComponentHealth()
	{
		return getDeepComponentHealth(this.shallowHealthReporter);
	}

	private ComponentHealth getDeepComponentHealth(IShallowHealthReporter reporter)
	{
		ComponentHealth report = getShallowComponentHealth(reporter);
		for (IShallowHealthReporter subcomp : reporter.getSubcomponents())
		{
			report.addSubcomponentHealth(getDeepComponentHealth(subcomp));
		}
		return report;
	}

	private ComponentHealth getShallowComponentHealth(IShallowHealthReporter reporter)
	{
		ComponentHealth report;
		try
		{
			report = reporter.report();
		}
		catch (Exception e)
		{
			report =  new ComponentHealth.Builder(reporter.getComponentName(), Status.RED).withException(e).build();
		}
		return report;
	}
}
