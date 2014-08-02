package com.free.coreservices.perfcounter.collectors;

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

import com.free.commons.util.StringBuilderUtils;
import com.jpmc.dart.coreservices.metricreport.MetricType;
import com.jpmc.dart.coreservices.util.heartbeat.HeartBeat;
import com.jpmc.dart.coreservices.util.heartbeat.HeartBeatStatus;
import com.jpmc.dart.coreservices.util.heartbeat.ServiceBean;
import com.jpmc.dart.coreservices.util.heartbeat.ServiceGroup;

public abstract class JmxCollector extends AbstractCollector {
	private static final Log LOG = LogFactory.getLog(JmxCollector.class);

	private HeartBeat heartBeat;

	private static final Map<String, Integer> JMX_ENV_MAP = Collections.synchronizedMap(new HashMap<String, Integer>());

	static {
		// This will disable the 'heartbeat' that runs once a connection is made.  This *should* stop the thread
		// from running.  Got this from:  https://community.oracle.com/message/8315284
		JMX_ENV_MAP.put("jmx.remote.x.client.connection.check.period", Integer.valueOf(0));
	}

	public JmxCollector(String file){
		super(file);
	}

	public void setHeartBeat(HeartBeat heartBeat) {
		this.heartBeat = heartBeat;
	}


	public List<JmxStatBean> poll(MetricType type,String ...services) {
		// go through the heartbeat list & poll the servers the service is on and pull the metric value
		List<String> jmxUrls = new ArrayList<String>();

		for (ServiceGroup grp : heartBeat.getServiceGroups()){
			for (String lookFor : services){
				for (ServiceBean sb : grp.getServiceBeans()){
					if (sb.getServiceName().equals(lookFor)){
						if (sb.getStatus()==HeartBeatStatus.GREEN){
							jmxUrls.add(sb.getJmxUrl());
						}
					}
				}
			}
		}

		List<JmxStatBean> ret = new ArrayList<JmxStatBean>();

		for (String connectUrl : jmxUrls){
			LOG.debug("try service at "+connectUrl);
			JMXConnector jmxc = null;

			try {
				JMXServiceURL url =new JMXServiceURL(connectUrl);
				jmxc = JMXConnectorFactory.connect(url, JMX_ENV_MAP);
				// get a list of metrics
				MBeanServerConnection connect =jmxc.getMBeanServerConnection();
				Map<String, String> stats=(Map<String, String>)connect.invoke(new ObjectName("support:name=MetricCollector"), "getStats",new Object[]{},new String[]{});

				StringBuilder buff = new StringBuilder();

				for (String key : stats.keySet()){
					// key looks like this |LANDING_ZONE_FILES, | (no pipes, just need to show the space after the ,)
					buff.setLength(0);
					buff.append(key);
					StringBuilderUtils.trim(buff);
					buff.deleteCharAt(buff.length()-1);
					String fixkey=buff.toString();

					LOG.debug("key is |"+key+"|");

					//key will look like this: LANDING_ZONE_FILES
					if (fixkey.equals(type.toString())){

						//value looks like this: hit-count=3.0,total=3.0
						JmxStatBean bean = new JmxStatBean();
						bean.setStat(fixkey);
						String valueString=stats.get(key);

						bean.setCount(StringUtils.trim(valueString.substring(valueString.indexOf("=")+1,valueString.indexOf(","))));
						bean.setTotal(StringUtils.trim(valueString.substring(valueString.lastIndexOf("=")+1)));

						LOG.debug("value string is "+valueString+" I got "+ReflectionToStringBuilder.reflectionToString(bean));

						ret.add(bean);
					}
					IOUtils.closeQuietly(jmxc);
				}
			} catch (Exception e){
				LOG.debug("couldn't collect stats",e);
			}
			finally {
				IOUtils.closeQuietly(jmxc);
			}
		}

		return ret;
	}

}
