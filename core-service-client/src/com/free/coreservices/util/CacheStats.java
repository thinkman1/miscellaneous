package com.free.coreservices.util;

import java.util.HashMap;
import java.util.Map;

import com.jamonapi.MonitorFactory;


public class CacheStats {
	public Map<String, String> dumpStats() {
		Map<String, String> stats = new HashMap<String, String>();
		
		for (Object o[] : MonitorFactory.getData()){
			if ( (o[0]!=null) && (o[1]!=null)) {
				stats.put(o[0].toString(),o[1].toString());				
			}
		}
		
		if (stats.size()==0){
			stats.put("empty", "0");
		}
		
		return stats;
	}
}
