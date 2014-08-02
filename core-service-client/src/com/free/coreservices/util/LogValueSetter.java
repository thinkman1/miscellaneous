package com.free.coreservices.util;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class LogValueSetter {
	public void setLogLevel(String classOrPackage, String level) {
		Logger logger = LogManager.getLogger(classOrPackage);
		logger.setLevel(Level.toLevel(level));
	}
}
