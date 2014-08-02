package com.free.coreservices.util;

import java.net.InetAddress;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * borrowed from CTO framework until we can move all the code to CTO framework
 * 
 * @author E001668
 * 
 */
public class MachineInformation implements InitializingBean {
	private static Log log = LogFactory.getLog(MachineInformation.class);

	private static final String JVM_NAME_PROPERTY = "jvm.name";
	private static final String JMX_PORT_PROPERTY = "com.sun.management.jmxremote.port";

	private String machineName = "unknown";
	private String jvmName = "unknown";

	private int jmxPort;

	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			machineName = addr.getHostName();
		} catch (Exception e) {
			log.warn("Unable to determine host name", e);
		}

		String name = System.getProperty(JVM_NAME_PROPERTY);
		String port = System.getProperty(JMX_PORT_PROPERTY);

		if (!StringUtils.isEmpty(name)) {
			jvmName = name;
		}

		jvmName += ":";

		if (NumberUtils.isDigits(port)) {
			jvmName += port;
			jmxPort = Integer.parseInt(port);
		} else {
			jvmName += "0";
		}
	}

	public String getJvmName() {
		return jvmName;
	}

	public String getMachineName() {
		return machineName;
	}

	public int getJmxPort() {
		return jmxPort;
	}

}
