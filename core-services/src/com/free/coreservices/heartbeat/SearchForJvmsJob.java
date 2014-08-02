package com.free.coreservices.heartbeat;

import org.quartz.StatefulJob;

import com.free.scheduling.ExecutionContext;
import com.free.scheduling.ScheduledJob;
import com.jpmc.cto.framework.exception.SystemException;

public class SearchForJvmsJob extends ScheduledJob implements StatefulJob {
	FindAliveJvmsLocal findAliveJvms;
	FindAliveJvmsFromOtherHosts findAliveJvmsFromOtherHosts;
	
	@Override
	public void executeService(ExecutionContext arg0) throws SystemException {
		try {
			findAliveJvms.lookForLiveJvms();
			findAliveJvmsFromOtherHosts.findRemoteJvms();
		} catch (Exception e){
			throw new SystemException(e);
		}
	}
	
	@Override
	public String getName() {
		return "FindLivingJvmsOnMachine";
	}

	public void setFindAliveJvms(FindAliveJvmsLocal findAliveJvms) {
		this.findAliveJvms = findAliveJvms;
	}

	public void setFindAliveJvmsFromOtherHosts(
			FindAliveJvmsFromOtherHosts findAliveJvmsFromOtherHosts) {
		this.findAliveJvmsFromOtherHosts = findAliveJvmsFromOtherHosts;
	}
}
