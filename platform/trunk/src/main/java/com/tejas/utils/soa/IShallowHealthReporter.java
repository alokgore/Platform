package com.tejas.utils.soa;

import java.util.List;

public interface IShallowHealthReporter
{
	/**
	 * Supposed to return the ComponentHealth only for the top level component. 
	 * {@link DeepHealthReporter} will take care of extracting the report for subcomponents 
	 */
	ComponentHealth report() throws Exception;
	
	/**
	 * Should return the same name that is being used for the ComponentHealth.componentName.
	 * This function is required by ServiceHealthReportonly for cases where  report() throws up
	 */
	String getComponentName();

	List<? extends IShallowHealthReporter> getSubcomponents();
}
