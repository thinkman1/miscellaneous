package com.jpmc.dart.commons.monitor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 *
 * This class contains wrapper methods that can be made on JMX server.
 * For e.g finding out the queue size i.e no.of messages exist in a queue etc.
 *
 * @author Satya G
 * @author jboyer
 */
public class JmxQueueInspector implements InitializingBean, DisposableBean {

	private static Log log = LogFactory.getLog(JmxQueueInspector.class);
	/**
	 * JMX service url
	 */
	private static final String JMX_SERVICE_URL = "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi";
	/**
	 * Domain for a specific Queue
	 */
	private static final String ACTIVEMQ_4_QUEUE_DOMAIN =
			"org.apache.activemq:BrokerName=%s,Type=Queue,Destination=%s";
	private static final String ACTIVEMQ_4_QUEUE_NAMES_DOMAIN =
			"org.apache.activemq:BrokerName=%s,Type=Queue,*";
	private static final String ACTIVEMQ_5_QUEUE_DOMAIN =
		"org.apache.activemq:type=Broker,brokerName=%s,destinationType=Queue,destinationName=%s";
	private static final String ACTIVEMQ_5_QUEUE_NAMES_DOMAIN =
		"org.apache.activemq:type=Broker,brokerName=%s,destinationType=Queue,destinationName=*";
	/**
	 * The attribute for Queue MBean which returns no.of messages present in a particular queue.
	 */
	private static final String QUEUE_SIZE_ATTRIBUTE = "QueueSize";


	/**
	 * Host on which RMI Registry can be reached
	 */
	private String rmiConnectorHost;
	/**
	 * The port on which RMI connector server is listening to.
	 */
	private int rmiRegistryPort;
	/**
	 * Username for logging into JMX.  Leave null if not turned on.
	 */
	private String username;
	/**
	 * User secret key for logging into JMX.  Don't use the 'P' word - static scan will kill us.
	 */
	private String bannedStaticScanWord;
	/**
	 * JMX connector
	 */
	private JMXConnector jmxc = null;

	// TODO Kind of ugly but BrokerLookup ALSO assumes that's the host you want to connect to.  Bad.
	private String brokerName;
	private boolean useBrokerLookup;
	private BrokerLookup brokerLookup;

	/**
	 * This method is used to close the connection to connector server by calling close method.
	 * @throws Exception if problems occurred in closing connection to connector server.
	 */
	@Override
	public void destroy() throws Exception {
		close();
	}

	/**
	 * This method is used to close the connection to connector server.
	 * @throws IOException if problems occurred in closing connection to connector server.
	 */
	public void close() throws IOException {
		if (jmxc != null) {
			log.debug(String.format("closing.. connection to connector server %s",
					this.rmiConnectorHost));
			jmxc.close();
			log.debug(String.format("closed connection to connector server %s successfully",
					this.rmiConnectorHost));
			jmxc = null;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Validate.notNull(this.rmiConnectorHost, "Connector Host must not be null");
		Validate.isTrue(this.rmiRegistryPort > 0,
				String.format("Input RMI Registry port %d is not valid", Integer.valueOf(this.rmiRegistryPort)));
	}

	/**
	 * This method is used to establish the connection to connector server.
	 * @throws MalformedURLException if jmx service url is not correct
	 * @throws IOException if problems occurred in establishing connection to connector server
	 */
	public void connect() throws IOException {
		Exception caused = null;
		for (int i = 0; i < 10; i++) {
			try {
				if (jmxc == null || jmxc.getConnectionId() == null || jmxc.getConnectionId().equals("")) {
					log.debug(String.format("Establishing.. connection to connector server %s at port %s",
							this.rmiConnectorHost, Integer.valueOf(this.rmiRegistryPort)));
					String host = getJmxHost();
					JMXServiceURL url = new JMXServiceURL(String.format(JMX_SERVICE_URL, host, Integer.valueOf(this.rmiRegistryPort)));
					Map<String, Object> properties = new HashMap<String, Object>();
					if (this.username != null && !this.username.equals("")) {
						String[] creds = { this.username, this.bannedStaticScanWord };
						properties.put("jmx.remote.credentials", creds);
					}
					jmxc = JMXConnectorFactory.connect(url, properties);
					log.debug(String.format("Established connection to connector server %s at port %s successfully",
							this.rmiConnectorHost, Integer.valueOf(this.rmiRegistryPort)));
				} else {
					log.debug("Connection found, using that one.");
				}
				return;
			} catch (Exception e) {
				caused = e;
				log.warn("Failed to connect after the " + i+1 + " retry.", e);
				try {
					TimeUnit.SECONDS.sleep(15);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
			}
		}

		throw new IOException("Got exception while connecting JMX", caused);
	}

	protected String getJmxHost() {
		String host = null;
		if (isUseBrokerLookup() && getBrokerLookup() != null) {
			host = getBrokerLookup().getActiveBrokerName();
		}

		if (host == null) {
			host = this.rmiConnectorHost;
		}

		if (host == null) {
			host = "localhost";
		}

		return host;
	}

	public int getQueueSize(final String queueName) throws Exception {
		int returnValue = 0;
		try {
			connect();
			Validate.notNull(queueName, "queueName must not be null or empty");
			MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
			String queueDomain = String.format(useBrokerLookup ? ACTIVEMQ_5_QUEUE_DOMAIN : ACTIVEMQ_4_QUEUE_DOMAIN, getJmxDomain(), queueName);
			log.info("queueDomain = " + queueDomain);
			ObjectName mbeanObjName = new ObjectName(queueDomain);
			Object queueCount = mbsc.getAttribute(mbeanObjName, QUEUE_SIZE_ATTRIBUTE);
			returnValue = Integer.parseInt(String.valueOf(queueCount));
		} finally {
			close();
		}
		return returnValue;
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public Map<String, Integer> getQueueNamesAndVolumes() throws Exception {
		Map<String, Integer> queueNamesAndVolumes = new HashMap<String, Integer>();
		try {
			connect();
			MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
			//Query to get the list of queue name objects
			String queueNamesDomain = String.format(useBrokerLookup ? ACTIVEMQ_5_QUEUE_NAMES_DOMAIN : ACTIVEMQ_4_QUEUE_NAMES_DOMAIN, getJmxDomain());
			log.info("queueNamesDomain = " + queueNamesDomain);
			ObjectName mbeanObjName = new ObjectName(queueNamesDomain);
			Set<ObjectName> queueNames = mbsc.queryNames(mbeanObjName, null);
			for (ObjectName objectName : queueNames) {
				int pos = objectName.toString().lastIndexOf("=");
				String queueName = objectName.toString().substring(pos+1);
				//Query to get the queue size attribute
				Object queueCountObject = mbsc.getAttribute(objectName, QUEUE_SIZE_ATTRIBUTE);
				int queueCount = Integer.parseInt(String.valueOf(queueCountObject));
				queueNamesAndVolumes.put(queueName, Integer.valueOf(queueCount));

			}
		} finally {
			close();
		}
		return queueNamesAndVolumes;
	}

	public String getJmxDomain() {
		String broker = null;
		if (isUseBrokerLookup() && getBrokerLookup() != null) {
			broker = getBrokerLookup().getActiveBrokerName();
		}

		if (broker == null) {
			if (getBrokerName() != null) {
				broker = getBrokerName();
			} else {
				broker = "localhost";
			}
		}

		return broker;
	}
}
