package com.free.coreservices.perfcounter;

import com.free.coreservices.perfcounter.collectors.StatsCollectorProcess;
import com.free.scheduling.ExecutionContext;
import com.free.scheduling.ScheduledJob;
import com.jpmc.cto.framework.exception.SystemException;

public class SamplerJob extends ScheduledJob {
	
	private StatsCollectorProcess statsCollectorProcess;
	
	@Override
	public String getName() {
		return "SamplerJob";
	}
	
	@Override
	public void executeService(ExecutionContext arg0) throws SystemException {
		try {
			statsCollectorProcess.collect();	
		} catch (Exception e){
			throw new SystemException(e);
		}
	}
	
	public void setStatsCollectorProcess(
			StatsCollectorProcess statsCollectorProcess) {
		this.statsCollectorProcess = statsCollectorProcess;
	}
	
}
