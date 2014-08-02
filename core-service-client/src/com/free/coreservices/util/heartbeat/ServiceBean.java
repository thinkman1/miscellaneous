package com.free.coreservices.util.heartbeat;

import org.apache.commons.lang.builder.ToStringBuilder;

public class ServiceBean {
	private String serviceName;
	private String jmxPort;
	private String jmxUrl;
	private String host;
	private long lastTimestamp;
	private HeartBeatStatus status;
	private int expectedByHost;
	private int minByHost;

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getJmxPort() {
		return jmxPort;
	}

	public void setJmxPort(String jmxPort) {
		this.jmxPort = jmxPort;
	}

	public String getJmxUrl() {
		return jmxUrl;
	}

	public void setJmxUrl(String jmxUrl) {
		this.jmxUrl = jmxUrl;
	}

	public long getLastTimestamp() {
		return lastTimestamp;
	}

	public void setLastTimestamp(long lastTimestamp) {
		this.lastTimestamp = lastTimestamp;
	}

	public HeartBeatStatus getStatus() {
		return status;
	}

	public void setStatus(HeartBeatStatus status) {
		this.status = status;
	}

	public void setExpectedByHost(int expectedByHost) {
		this.expectedByHost = expectedByHost;
	}

	public int getExpectedByHost() {
		return expectedByHost;
	}

	public void setMinByHost(int minByHost) {
		this.minByHost = minByHost;
	}

	public int getMinByHost() {
		return minByHost;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}
}
