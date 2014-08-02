package com.free.coreservices.perfcounter.collectors;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * for each object that extends AbstractCollector in the context, set up a thread threadpool that executes each collector on the interval sampleSeconds() asks for
 * @author E001668
 *
 */
public class StatsCollectorProcess implements BeanPostProcessor, DisposableBean{
	private static final Log LOG = LogFactory.getLog(StatsCollectorProcess.class);

	private boolean testMode=false;

	private List<AbstractCollector> collectors = new ArrayList<AbstractCollector>();


	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {

		if (bean instanceof AbstractCollector){
			AbstractCollector collect = (AbstractCollector) bean;
			collectors.add(collect);
			// make sure the collector base dir is there

			LOG.info("set up collector "+beanName);

			File baseRRDPath = new File(collect.getBaseRrdPath());
			baseRRDPath.mkdirs();

			try {
				collect.setupCollector();
				collect.startCollection();
			} catch (Exception e) {
				LOG.error("couldn't start collector "+beanName,e);
			}

		}

		return bean;
	}

	@Override
	public void destroy() throws Exception {
		for (AbstractCollector collect : collectors){
			collect.stopCollect();
		}
	}

	public void collect() throws Exception {
		Date time=new Date();

		for (AbstractCollector collect : collectors){
			collect.collect(time);
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	public void setTestMode(boolean testMode) {
		this.testMode = testMode;
	}

}
