package com.free.coreservices.perfcounter;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import com.jpmc.cto.dart.model.system.PerformanceCounter;
import com.jpmc.cto.framework.MessageProcessor;
import com.jpmc.cto.framework.MessageType;
import com.jpmc.cto.framework.exception.ApplicationException;
import com.jpmc.dart.dao.jdbc.PerformanceCounterDao;

public class PerformanceCounterService implements MessageProcessor<PerformanceCounter>{

	// peformance stat file format:
	// record size
	// 	PerformanceCounterType name
	//  counter value
	
	private static final MessageType typez[]= new MessageType[]{new PerformanceCounter().getMessageType()};
	
	@Autowired
	private PerformanceCounterDao performanceCounterDao;
	
	@Override
	public MessageType[] getAcceptedTypes() {
		return typez;
	}
	
	@Override
	public String getName() {
		return "performance counter service";
	}

	@Override
	public void processMessage(PerformanceCounter arg0)
			throws ApplicationException {
		if (arg0.getId()==null){
			arg0.setId(UUID.randomUUID());
		}
		performanceCounterDao.save(arg0);
	}
}
