package com.free.coreservices.metricreport;

public class Metric {
	private MetricType type;
	private double value;
	
	public Metric(MetricType type, double value){
		this.type=type;
		this.value=value;
	}
	
	public MetricType getType() {
		return type;
	}
	
	public double getValue() {
		return value;
	}
}
