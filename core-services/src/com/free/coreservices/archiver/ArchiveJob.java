package com.free.coreservices.archiver;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omg.CORBA.SystemException;

public class ArchiveJob extends ScheduledJob {
	private static final Log LOG = LogFactory.getLog(ArchiveJob.class);
	
	private ArchiveKickoffJob archiveKickoffJob; 
	private String targetQueue;
	private LockManager lockManager;
	private String baseDir;
	
	public ArchiveJob() {
		setLockDuration(30);
		setLockDurationUnit(TimeUnit.SECONDS);
	}
	
	@Override
	public String getName() {
		return "ArchiveJob";
	}
	
	public void setLock(LockManager lockManager) {
		this.lockManager=lockManager;
	}
	
	@Override
	public void executeService(ExecutionContext arg0) throws SystemException {
		boolean yesLock=lockManager.acquire("ARCHIVE "+baseDir , 2, TimeUnit.MINUTES);
		if (yesLock){
			try {
				LOG.info("start archive publishing");
				archiveKickoffJob.publishArchiveMessages(targetQueue);
				
				LOG.info("hang out, we're done with publishing");
				// if this runs too fast the other process can get a lock as soon as we're done
				TimeUnit.SECONDS.sleep(30);
				
				LOG.info("end archive publish");
			} catch (Exception e){
				throw new SystemException(e);
			} finally {
				lockManager.release("ARCHIVE "+baseDir);
			}
		} else {
			LOG.info("could not acquire ARCHIVE "+baseDir );
		}
	}
	
	public void setArchiveKickoffJob(ArchiveKickoffJob archiveKickoffJob) {
		this.archiveKickoffJob = archiveKickoffJob;
	}
	
	public void setTargetQueue(String targetQueue) {
		this.targetQueue = targetQueue;
	}

	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}
}
