package com.free.coreservices.util.heartbeat;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

public class JvmBean {
	private String host;
	private String jmx;
	private String serviceName;
	private String jmxUri;

	@Override
	public String toString() {
		return ReflectionToStringBuilder.reflectionToString(this);
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getJmx() {
		return jmx;
	}

	public void setJmx(String jmx) {
		this.jmx = jmx;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setJmxUri(String jmxUri) {
		this.jmxUri = jmxUri;
	}

	public String getJmxUri() {
		return jmxUri;
	}
}
