package com.free.coreservices.util.heartbeat;

import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

public class ServiceGroup {

	private int currentTotal;
	private int expectedTotal;

	private String groupName;
	private HeartBeatStatus status;
	private List<ServiceBean> serviceBeans;

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getGroupName() {
		return groupName;
	}

	public int getExpectedTotal() {
		return expectedTotal;
	}

	public void setExpectedTotal(int expectedTotal) {
		this.expectedTotal = expectedTotal;
	}

	public HeartBeatStatus getStatus() {
		return status;
	}

	public void setStatus(HeartBeatStatus status) {
		this.status = status;
	}

	public List<ServiceBean> getServiceBeans() {
		return serviceBeans;
	}

	public void setServiceBeans(List<ServiceBean> serviceBeans) {
		this.serviceBeans = serviceBeans;
	}

	public void setCurrentTotal(int currentTotal) {
		this.currentTotal = currentTotal;
	}

	public int getCurrentTotal() {
		return currentTotal;
	}
}
