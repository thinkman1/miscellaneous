package com.free.scheduling;

import java.util.Date;

import org.quartz.Calendar;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.Trigger;

/**
 * Wrapper around org.quartz.JobExecutionContext
 * 
 * @author Sean Thornton
 */
public class ExecutionContext {

	private JobExecutionContext jec;

	/**
	 * @param jec
	 *            The Quartz JobExecutionContext
	 */
	public ExecutionContext(final JobExecutionContext jec) {
		this.jec = jec;
	}

	public Object get(final Object key) {
		return jec.get(key);
	}

	public Calendar getCalendar() {
		return jec.getCalendar();
	}

	public Date getFireTime() {
		return jec.getFireTime();
	}

	public JobDetail getJobDetail() {
		return jec.getJobDetail();
	}

	public Job getJobInstance() {
		return jec.getJobInstance();
	}

	public long getJobRunTime() {
		return jec.getJobRunTime();
	}

	public JobDataMap getMergedJobDataMap() {
		return jec.getMergedJobDataMap();
	}

	public Date getNextFireTime() {
		return jec.getNextFireTime();
	}

	public Date getPreviousFireTime() {
		return jec.getPreviousFireTime();
	}

	public int getRefireCount() {
		return jec.getRefireCount();
	}

	public Object getResult() {
		return jec.getResult();
	}

	public Date getScheduledFireTime() {
		return jec.getScheduledFireTime();
	}

	public Scheduler getScheduler() {
		return jec.getScheduler();
	}

	public Trigger getTrigger() {
		return jec.getTrigger();
	}

	public void incrementRefireCount() {
		jec.incrementRefireCount();
	}

	public boolean isRecovering() {
		return jec.isRecovering();
	}

	public void put(final Object key, final Object value) {
		jec.put(key, value);
	}

	public void setJobRunTime(final long jobRunTime) {
		jec.setJobRunTime(jobRunTime);
	}

	public void setResult(final Object result) {
		jec.setResult(result);
	}

	public String toString() {
		return jec.toString();
	}
}
