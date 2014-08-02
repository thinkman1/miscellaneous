package com.free.coreservices.heartbeat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.jpmc.dart.coreservices.util.MachineInformation;
import com.jpmc.dart.coreservices.util.heartbeat.JvmBean;
import com.sun.tools.attach.VirtualMachine;

public class FindAliveJvmsLocal implements InitializingBean {

	private static final Log LOG = LogFactory.getLog(FindAliveJvmsLocal.class);

	boolean testMode=false;

	private List<JvmBean> aliveJvms = new ArrayList<JvmBean>();

	private MachineInformation machineInformation;

	private static final Map<String, Integer> JMX_ENV_MAP = Collections.synchronizedMap(new HashMap<String, Integer>());

	static {
		// This will disable the 'heartbeat' that runs once a connection is made.  This *should* stop the thread
		// from running.  Got this from:  https://community.oracle.com/message/8315284
		JMX_ENV_MAP.put("jmx.remote.x.client.connection.check.period", Integer.valueOf(0));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		lookForLiveJvms();
	}

	public void lookForLiveJvms() throws Exception {
		List<String> possible = new ArrayList<String>();

		if (testMode){
			return;
		}

		BufferedReader reader = null;

		try {
			LOG.debug("try to figure out what is alive");

			Process foo = Runtime.getRuntime().exec("/bin/ps aux");
			reader = new BufferedReader(new InputStreamReader(foo.getInputStream()));

			String line = reader.readLine();
			if (line==null){
				LOG.debug("uh, error? "+IOUtils.toString(foo.getErrorStream()));
			}

			while (line!=null) {
				if (line.contains("com.sun.management.jmxremote.port")){
					// grab out the PID
					String subLine = line.replaceAll(" ", ";");
					String pid=StringUtils.trim(subLine.split(";")[2]);
					if (StringUtils.trimToNull(pid)!=null){
						possible.add(pid);
					} else {
						pid=StringUtils.trim(subLine.split(";")[3]);
						if (StringUtils.trimToNull(pid)!=null){
							possible.add(pid);
						}
					}
				}
				line = reader.readLine();
			}

			IOUtils.closeQuietly(reader);
			foo.destroy();

		} catch (Exception e){
			LOG.error("couldn't find jvms ",e);
		}
		finally {
			IOUtils.closeQuietly(reader);
		}

		LOG.debug("found "+possible.size()+" jvms");

		List<JvmBean> lookFor = new ArrayList<JvmBean>();

		for (String pid : possible){
			JMXConnector c = null;
			try {
				LOG.debug("ATTACH! TO "+pid);
				VirtualMachine m = VirtualMachine.attach(pid);

				// why not just grab this out of the ps string?  well, it saves me from having to parse the string plus
				// it's a good indicator the JVM is alive and able to do stuff
				String jmxPort=m.getSystemProperties().getProperty("com.sun.management.jmxremote.port");
				m.detach();

				if (jmxPort == null) {
					LOG.warn("Process ID " + pid + " did not have a jmx port defined.  Skipping service");
				}
				else {
					// connect via JMX to it
					String jmxUri="service:jmx:rmi:///jndi/rmi://"+machineInformation.getMachineName()+":"+jmxPort+"/jmxrmi";
					JvmBean service = new JvmBean();
					service.setHost(machineInformation.getMachineName());
					service.setJmx(jmxPort);
					service.setJmxUri(jmxUri);

					// connect & get some info
					JMXServiceURL u = new JMXServiceURL(service.getJmxUri());
					c = JMXConnectorFactory.connect(u, JMX_ENV_MAP);
					MBeanServerConnection connect =c.getMBeanServerConnection();
					String name=(String)connect.invoke(new ObjectName("support:name=ServiceInformation"), "getServiceName",new Object[]{},new String[]{});
					service.setServiceName(name);

					IOUtils.closeQuietly(c);

					LOG.debug("added service "+ReflectionToStringBuilder.reflectionToString(service));

					lookFor.add(service);
				}

			} catch (Exception e){
				LOG.warn("didn't connect to JVM "+e.getMessage()+" to pid "+pid);
			}
			finally {
				IOUtils.closeQuietly(c);
			}
		}

		aliveJvms=lookFor;

	}

	public List<JvmBean> getAliveJvms() {
		return new ArrayList<JvmBean>(aliveJvms);
	}

	public void setMachineInformation(MachineInformation machineInformation) {
		this.machineInformation = machineInformation;
	}

	public void setTestMode(boolean testMode) {
		this.testMode = testMode;
	}
}
