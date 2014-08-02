package com.free.coreservices.directorycleaner;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.free.scheduling.ExecutionContext;
import com.free.scheduling.ScheduledJob;
import com.jpmc.cto.framework.concurrent.LockManager;
import com.jpmc.cto.framework.exception.SystemException;

public class TriggerFileCleanerJob extends ScheduledJob{
	private static final Log LOG = LogFactory.getLog(TriggerFileCleanerJob.class);
	private LockManager lockManager;
	private FileCleanerJob fileCleanerJob;
	private String name;

	@Override
	public String getName() {
		return "TriggerFileCleanerJob";
	}

	@Override
	public void executeService(ExecutionContext arg0) throws SystemException {
		//TODO: when we move to /dart/data, the lock name for this will have to change!
		String lockName="cleanup-"+name;

		boolean gotLock=lockManager.acquire(lockName, 20, TimeUnit.SECONDS);
		if (gotLock){
			LOG.info("publish cleanup messages for "+name);
			fileCleanerJob.publishMessages();
			try {
				TimeUnit.SECONDS.sleep(30);
			} catch(InterruptedException e){
				Thread.currentThread().interrupt();
			} finally {
				lockManager.release(lockName);
			}
		} else {
			LOG.info("didn't get lock "+lockName);
		}
	}

	public void setLockManager(LockManager lockManager) {
		this.lockManager = lockManager;
	}

	public void setFileCleanerJob(FileCleanerJob fileCleanerJob) {
		this.fileCleanerJob = fileCleanerJob;
	}

	public void setName(String name) {
		this.name = name;
	}
}
