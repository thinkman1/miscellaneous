/**
 * 
 */
package com.jpmc.cto.web.tomcat.daemon;

import java.util.Date;
import java.util.Timer;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * This class is a container for workers
 * 
 * @author R502440
 * 
 */
public class ListenerWorker {

	private static Log LOG = LogFactory.getLog(ListenerWorker.class);

	private Timer timer;
	private Date now;
	private LogCleaner logCleaner;
	
	private long period;

	public ListenerWorker() {
		this.timer = new Timer();
		this.now = new Date();
		LOG.info("CleanLogWorker is registered at " + now);
	}

	public void cleanLog() {
		logCleaner.init();
		timer.schedule(logCleaner, now, period);
	}

	public void shutdown() {
		LOG.info("CleanLogWorker is shutting down.");
		timer.cancel();
	}
	
	public LogCleaner getLogCleaner() {
		if (logCleaner == null) {
			logCleaner = new LogCleaner();
		}
		
		return logCleaner;
	}
	
	public void setPeriod(long period) {
		this.period = period;
	}
}
