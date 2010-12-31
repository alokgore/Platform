package com.tejas.chanak.scheduler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.tejas.chanak.core.DAGConcurrencyController;
import com.tejas.chanak.types.ContractExecutionReport;
import com.tejas.chanak.types.ContractExecutionReport.ExecutionStatus;
import com.tejas.chanak.types.DAGContract;
import com.tejas.core.TejasContext;
import com.tejas.core.TejasContext.ExitStatus;
import com.tejas.types.exceptions.DBLayerException;

public class ContractRunner implements Runnable
{
	private static ConcurrentMap<String, String> activeContracts = new ConcurrentHashMap<String, String>();
	
	DAGContract contract;
	
	public ContractRunner(DAGContract contract)
	{
		this.contract = contract;
		String contractID = contract.getContractID();
		String prevVal = activeContracts.putIfAbsent(contractID, contractID);
		if(prevVal != null)
		{
			throw new IllegalStateException("Someone tried to execute contract [" + contractID + "] concurrently");
		}
	}

	@Override
    public void run()
	{
		TejasContext self = new TejasContext();
		try
		{
			self.entry("ContractExecution");
			
			self.logger.info("Starting the contract", contract);
			
			ContractExecutionReport report = runContract(self);
			self.logger.info("Contract", contract, " finished with status ", report.getStatus());
			
			processReport(self, report);

			self.exit(report.getStatus() == ExecutionStatus.Failed? ExitStatus.Failure: ExitStatus.Success);
		}
		finally
		{
			activeContracts.remove(contract.getContractID());
		}		
	}

	public static boolean isContractActive(String contractID)
	{
		return activeContracts.containsKey(contractID);
	}
	
	private static final int INITIAL_RETRY_INTERVAL = 2000;
	private static final float BACKOFF_FACTOR = 1.5f;
	private static final long MAX_NUM_RETRIES = 5;

	private void processReport(TejasContext self, ContractExecutionReport report)
	{
		boolean success = false;
		int numRetries = 0;
		do
		{
			try
			{
				updateContractStatus(self, contract, report);
				contract.log(self, report);
				success = true;
			}
			catch (DBLayerException e)
			{
				try
				{
					Thread.sleep((long) (INITIAL_RETRY_INTERVAL*Math.pow(BACKOFF_FACTOR, numRetries++)));
				}
				catch (InterruptedException e1)
				{
				    //Ignore
				}
			}
		}
		while(!success && numRetries < MAX_NUM_RETRIES);
	}

	private static boolean simulateUpdateFailure;

	public synchronized static void simulateUpdateFailure(boolean flag)
	{
		simulateUpdateFailure = flag;
	}

	public static synchronized boolean simulateUpdateFailure()
	{
		return simulateUpdateFailure;
	}
	
	public static void updateContractStatus(TejasContext self, DAGContract contract, ContractExecutionReport report) 
	{
		/*
		 * Yes, yes. I know. I may burn in hell for this act against humanity.
		 * But I don't see any other way of testing the contract-revival code path
		 * And I am hoping that HE will understand.
		 */
		if(simulateUpdateFailure())
		{
			self.logger.fatal("Simulating the DB failure in contact-status update for contract [" + contract.getContractID() + "] of dag [" + contract.getContractID() + "]");
			throw new DBLayerException("Simulating the DB failure in contact-status update");
		}
		
		switch (report.getStatus())
		{
			case Complete:
				contract.markCompletion(self, report);
				break;

			case Failed:
				contract.restart(self);
				break;

			case Enqueued:
				contract.markEnqueued(self);
				break;
				
			default:
				throw new IllegalArgumentException("Illegal Contract Completion Status " + report.getStatus());
		}
	}

	private ContractExecutionReport runContract(TejasContext self)
	{
		ContractExecutionReport report = null;
		try
		{
			DAGConcurrencyController.acquireWorkPermit(self, contract.getResourceID());
			long startTime = System.currentTimeMillis();
			report = contract.run(self);
			long endTime = System.currentTimeMillis();
			self.metrics.recordLatency(contract.getMetricsName(), endTime-startTime);
		}
		catch (Throwable e)
		{
			self.logger.error("", e, "Contract "+ contract + " failed");
			report =  new ContractExecutionReport(e);
		}
		finally
		{
			DAGConcurrencyController.releaseWorkPermit(self, contract.getResourceID());
		}
		return report;
	}
}
