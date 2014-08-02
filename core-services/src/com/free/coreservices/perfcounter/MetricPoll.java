package com.free.coreservices.perfcounter;

public class MetricPoll {
	public void pollMetrics() {
		// for each live JVM:
		// - connect via JMX.
		// - drain the metric values
		// - put into parallel array
		// - sort by type 
		// - combine & calculate values
		// - write result to each RRD file
	}
}
