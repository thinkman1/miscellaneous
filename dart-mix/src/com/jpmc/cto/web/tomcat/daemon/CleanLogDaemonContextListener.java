package com.jpmc.cto.web.tomcat.daemon;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Context listener of workers, now we have one worker - log cleaner
 *
 * @author R502440
 *
 */
public class CleanLogDaemonContextListener implements LifecycleListener {

	private static Log LOG = LogFactory.getLog(CleanLogDaemonContextListener.class);

	private ListenerWorker worker;

	// Initialized properties with default values
	private String logDirectory = "logs";
	private String includeFilePatterns = "*";
	private String excludeFilePatterns = "";
	private int logRetentionDays = 14;
	private int logDeleteCheckMinutes = 6 * 60;

	@Override
	public void lifecycleEvent(LifecycleEvent event) {
		if (Lifecycle.START_EVENT.equals(event.getType())) {

			this.initialize();
			worker.cleanLog();
		} else if (Lifecycle.STOP_EVENT.equals(event.getType())) {

			worker.shutdown();
		}
	}

	protected void initialize() {
		LOG.info("CleanLogDaemonContextListener: Initailizing CleanLogWorker.");
		worker = new ListenerWorker();
		worker.setPeriod(logDeleteCheckMinutes * 60 * 1000);

		worker.getLogCleaner().setExcludeFilePatterns(excludeFilePatterns);
		worker.getLogCleaner().setIncludeFilePatterns(includeFilePatterns);
		worker.getLogCleaner().setLogDeleteCheckMinutes(logDeleteCheckMinutes);
		worker.getLogCleaner().setLogRetentionDays(logRetentionDays);
		worker.getLogCleaner().setLogDirectory(logDirectory);
	}

	public void setLogDirectory(String logDirectory) {
		this.logDirectory = logDirectory;
	}

	public void setIncludeFilePatterns(String includeFilePatterns) {
		this.includeFilePatterns = includeFilePatterns;
	}

	public void setExcludeFilePatterns(String excludeFilePatterns) {
		this.excludeFilePatterns = excludeFilePatterns;
	}

	public void setLogRetentionDays(int logRetentionDays) {
		this.logRetentionDays = logRetentionDays;
	}

	public void setLogDeleteCheckMinutes(int logDeleteCheckMinutes) {
		this.logDeleteCheckMinutes = logDeleteCheckMinutes;
	}

}
