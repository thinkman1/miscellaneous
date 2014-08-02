package com.free.coreservices.heartbeat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.URI;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.jpmc.dart.coreservices.util.BadStatusCodeException;
import com.jpmc.dart.coreservices.util.HttpWrapper;
import com.jpmc.dart.coreservices.util.MachineInformation;
import com.jpmc.dart.coreservices.util.heartbeat.JvmBean;
import com.jpmc.vpc.commons.xstream.VPCStream;

public class FindAliveJvmsFromOtherHosts implements ApplicationContextAware {
	private static final Log LOG = LogFactory.getLog(FindAliveJvmsFromOtherHosts.class);
	private ApplicationContext context;
	private String hosts[];
	private MachineInformation machineInformation;
	private HttpWrapper httpWrapper;
	private List<JvmBean> allRemoteServices = new ArrayList<JvmBean>();

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context=applicationContext;
	}

	public List<JvmBean> getAllRemoteServices() {
		return new ArrayList<JvmBean>(allRemoteServices);
	}

	public void findRemoteJvms() throws Exception{
		List<JvmBean> tempRemote = new ArrayList<JvmBean>();


		for (String host : hosts){
			//this is host:port, so look at the first part.
			HostConfiguration conf = new HostConfiguration();
			conf.setHost(new URI(host,false));

			// don't look on the local host, look on the others
			if (!StringUtils.equals(conf.getHost(), machineInformation.getMachineName())){
				StringBuilder uri=new StringBuilder();
				uri.append(host).append("/HeartBeat");

				LOG.info("check host "+uri);

				InputStream results = null;
				try {
					results=httpWrapper.executeGet(uri.toString());
					List<JvmBean> services = (List<JvmBean>)VPCStream.fromXML(results);
					IOUtils.closeQuietly(results);
					tempRemote.addAll(services);
				} catch (BadStatusCodeException e){
					LOG.warn("bad status checking heartbeat "+e.getStatusCode());
				} catch (Exception e){
					LOG.warn("caught exception checking heartbeat ",e);
				}
				finally {
					IOUtils.closeQuietly(results);
				}
			}
		}
		allRemoteServices=tempRemote;
	}

	public void setHosts(String[] hosts) {
		this.hosts = hosts;
	}

	public void setMachineInformation(MachineInformation machineInformation) {
		this.machineInformation = machineInformation;
	}

	public void setHttpWrapper(HttpWrapper httpWrapper) {
		this.httpWrapper = httpWrapper;
	}
}
