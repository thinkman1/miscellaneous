package com.free.scheduling;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.free.util.FrameworkUtils;
import com.jpmc.cto.framework.Processor;
import com.jpmc.cto.framework.concurrent.LockManager;
import com.jpmc.cto.framework.exception.SystemException;

/**
 * @author Sean Thornton
 */
public abstract class ScheduledJob extends QuartzJobBean implements
		Processor, InitializingBean, StatefulJob {

	private static Log log = LogFactory.getLog(ScheduledJob.class);

	private long lockDuration;

	private TimeUnit lockDurationUnit;

	private String lockName;

	private LockManager lockManager;

	@Override
	protected final void executeInternal(final JobExecutionContext context)
			throws JobExecutionException {
		try {
			if (log.isDebugEnabled()) {
				log.debug("Executing service '" + getName() + "' as job: " + getJobName(context));
			}

			StopWatch sw = new StopWatch();
			sw.start();
			// ScheduledTask perform their own locking
			if (lockManager == null || (this instanceof ScheduledTask)) {
				executeService(new ExecutionContext(context));
			} else {
				log.trace("Acquiring lock: " + lockName);
				if (lockManager.acquire(lockName, lockDuration, lockDurationUnit)) {
					try {
						executeService(new ExecutionContext(context));
					} finally {
						log.trace("Releasing lock: " + lockName);
						lockManager.release(lockName);
					}
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Unabe to acquire lock: " + lockName);
					}
				}
			}

			sw.stop();
			if (log.isDebugEnabled()) {
				log.debug("Finished executing service '" + getName() + "' as job '" 
						+ getJobName(context) + " in " + sw.getTime() 
						+ " ms. Next fire time: " + context.getNextFireTime());
			}
		} catch (Exception e) {
			handleThrowable(context, e);
			throw new JobExecutionException(e);
		} catch (Error e) {
			handleThrowable(context, e);
			throw e;
		}
	}

	private void handleThrowable(final JobExecutionContext context, Throwable t) {
		String description = "Exception processing scheduled job: " + getJobName(context);
		FrameworkUtils.handleException(getName(), description, null, getJobName(context), null, t, null, null);
	}
	
	private String getJobName(JobExecutionContext context) {
		String name = null;
		if (context != null && context.getJobDetail() != null) {
			name = context.getJobDetail().getName();
		}
		
		return name;
	}

	/**
	 * @param context
	 *            The Job Context
	 * @throws VpcException
	 *             If there was a known exception while executing the service
	 */
	public abstract void executeService(final ExecutionContext context) throws SystemException;

	@Override
	public void afterPropertiesSet() throws SystemException {
		// We don't care for ScheduledTask
		if (lockManager != null && !(this instanceof ScheduledTask)) {
			Validate.notNull(lockName, "name must be set to use a lock");
			Validate.notNull(lockDurationUnit,
					"durationUnit must be set to use a lock");
			Validate.isTrue(lockDuration > 0,
					"duration must be non-zero to use a lock");
		}
	}

	public final long getLockDuration() {
		return lockDuration;
	}

	public final void setLockDuration(final long lockDuration) {
		this.lockDuration = lockDuration;
	}

	public final TimeUnit getLockDurationUnit() {
		return lockDurationUnit;
	}

	public final void setLockDurationUnit(final TimeUnit lockDurationUnit) {
		this.lockDurationUnit = lockDurationUnit;
	}

	public final String getLockName() {
		return lockName;
	}

	public final void setLockName(final String lockName) {
		this.lockName = lockName;
	}

	public LockManager getLockManager() {
		return lockManager;
	}

	public void setLockManager(LockManager lockManager) {
		this.lockManager = lockManager;
	}
}
