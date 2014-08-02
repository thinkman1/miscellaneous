package com.free.coreservices.metricreport;

import java.util.HashMap;
import java.util.Map;

import com.jamonapi.MonitorFactory;

public class MetricCollector  {
	public void pushMetricValue(Metric value){
		MonitorFactory.add(value.getType().name(), "", value.getValue());
	}
	
	public Map<String, String> getStats() {
		Map<String, String> stats = new HashMap<String, String>();
		if (MonitorFactory.getData()==null){
			return stats;
		}
		
		for (Object o[] : MonitorFactory.getData()){
			if ( (o[0]!=null) && (o[1]!=null)) {
				// put in hit count & total 
				stats.put(o[0].toString(),"hit-count="+o[1].toString()+",total="+o[3].toString());				
			}
		}
		return stats;
	}
}
