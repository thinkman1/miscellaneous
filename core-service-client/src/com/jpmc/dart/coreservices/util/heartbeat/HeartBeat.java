package com.jpmc.dart.coreservices.util.heartbeat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;

import com.jpmc.dart.coreservices.util.BadStatusCodeException;
import com.jpmc.dart.coreservices.util.HttpWrapper;
import com.jpmc.vpc.commons.xstream.VPCStream;
import com.jpmc.vpc.dao.jdbc.ConfigurationDao;
import com.jpmc.vpc.model.database.Configuration;
import com.jpmc.vpc.model.type.ConfigurationType;

public class HeartBeat implements InitializingBean, DisposableBean {

	private class UpdateHeartbeat extends Thread {
		public UpdateHeartbeat() {
			setName("dart-heartbeat-poller");
		}

		@Override
		public void run() {
			while (run) {
				try {
					TimeUnit.MINUTES.sleep(1);
				} catch (InterruptedException e) {
					LOG.warn("interrrupted? ");
					Thread.currentThread().interrupt();
				}
				try {
					poll();
				} catch (Exception e) {
					LOG.warn("couldn't check heartbat", e);
				}
			}
		}
	}

	private String hosts[];
	private static final Log LOG = LogFactory.getLog(HeartBeat.class);
	private ConfigurationDao configurationDao;
	private List<ServiceGroup> serviceGroups = new ArrayList<ServiceGroup>();
	private HttpWrapper httpWrapper;
	private Thread heartBeatMonitor;
	private volatile boolean run = true;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.serviceGroups = load();

		if (this.heartBeatMonitor == null) {
			this.heartBeatMonitor = new UpdateHeartbeat();
			this.heartBeatMonitor.start();
		}
	}

	public List<ServiceGroup> load() throws Exception {
		// create the initial service list
		List<ServiceGroup> temp = new ArrayList<ServiceGroup>();

		Configuration conf = configurationDao.findById(ConfigurationType.HEARTBEAT);

		BufferedReader reader = null;

		if (conf == null) {
			try {
				reader = new BufferedReader(new InputStreamReader(new ClassPathResource(
						"heartbeat.csv").getInputStream()));
			} catch (Exception e) {
				LOG.info("no heartbeat found?, no big deal, unless you are in prod.");
				return temp;
			}
		} else {
			String heartBeat = conf.getConfiguration();
			reader = new BufferedReader(new StringReader(heartBeat));
		}

		// it's CSV, so it's easy to digest

		String line = reader.readLine();

		Map<String, ServiceGroup> groups = new HashMap<String, ServiceGroup>();

		while (line != null) {
			LOG.debug("Heartbeat Config Line:  " + line);
			if (!line.startsWith("#") && StringUtils.isNotEmpty(line)) {
				String splits[] = line.split(",");
				String groupName = splits[0];
				String serviceName = splits[1];
				String serverName = splits[2];
				int minimum = Integer.parseInt(splits[3]);
				int totalExpectedByHost = Integer.parseInt(splits[4]);

				if (!groups.containsKey(groupName)) {
					groups.put(groupName, new ServiceGroup());
					groups.get(groupName).setGroupName(groupName);
					groups.get(groupName).setServiceBeans(new ArrayList<ServiceBean>());
				}
				ServiceBean service = new ServiceBean();
				service.setExpectedByHost(totalExpectedByHost);
				service.setHost(serverName);
				service.setServiceName(serviceName);
				service.setStatus(HeartBeatStatus.RED);
				service.setMinByHost(minimum);
				groups.get(groupName).getServiceBeans().add(service);
				groups.get(groupName).setExpectedTotal(
						groups.get(groupName).getExpectedTotal() + service.getExpectedByHost());
			}
			line = reader.readLine();
		}

		reader.close();

		for (String key : groups.keySet()) {
			temp.add(groups.get(key));
		}
		return temp;
	}

	public void poll() throws Exception {

		List<ServiceGroup> heart = load();

		StringBuilder uri = new StringBuilder();
		for (String host : hosts) {
			try {
				uri.setLength(0);
				uri.append(host).append("/HeartBeatAll");

				InputStream results = httpWrapper.executeGet(uri.toString(), null);

				List<JvmBean> jvms = (List<JvmBean>) VPCStream.fromXML(results);
				IOUtils.closeQuietly(results);

				for (ServiceGroup group : heart) {
					for (ServiceBean sb : group.getServiceBeans()) {
						sb.setStatus(HeartBeatStatus.RED);
					}
				}

				for (ServiceGroup group : heart) {
					group.setStatus(HeartBeatStatus.RED);
					group.setCurrentTotal(0);

					for (JvmBean jvm : jvms) {
						for (ServiceBean sb : group.getServiceBeans()) {
							if (sb.getExpectedByHost() == 0) {
								sb.setStatus(HeartBeatStatus.GREEN);
								group.setCurrentTotal(group.getCurrentTotal() + 1);
								continue;
							}
							if (jvm.getServiceName().equals(sb.getServiceName())) {
								if (jvm.getHost().equals(sb.getHost())) {
									sb.setJmxPort(jvm.getJmx());
									sb.setJmxUrl(jvm.getJmxUri());
									sb.setLastTimestamp(System.currentTimeMillis());
									sb.setStatus(HeartBeatStatus.GREEN);
									group.setCurrentTotal(group.getCurrentTotal() + 1);
								}
							}
						}
					}
					for (ServiceGroup groupz : serviceGroups) {
						if (groupz.getCurrentTotal() == groupz.getExpectedTotal()) {
							groupz.setStatus(HeartBeatStatus.GREEN);
						}
					}
				}

				this.serviceGroups = Collections.unmodifiableList(heart);
				break;
			} catch (BadStatusCodeException e) {
				LOG.error("couldn't check heartbeat with " + host + " got response code "
						+ e.getStatusCode());
			} catch (Exception e) {
				LOG.error("couldn't check heartbeat with " + host + " " + " url " + uri + " "
						+ e.getMessage());
			}
		}
	}

	@Override
	public void destroy() throws Exception {
		this.run = false;
		this.heartBeatMonitor.interrupt();
		this.heartBeatMonitor.join(5000);
	}

	public List<ServiceGroup> getServiceGroups() {
		return serviceGroups;
	}

	public void setConfigurationDao(ConfigurationDao configurationDao) {
		this.configurationDao = configurationDao;
	}

	public void setHttpWrapper(HttpWrapper httpWrapper) {
		this.httpWrapper = httpWrapper;
	}

	public void setHosts(String[] hosts) {
		this.hosts = hosts;
	}
}
