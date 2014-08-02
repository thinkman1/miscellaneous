package com.free.service.web;

import javax.servlet.ServletContextEvent;

import org.springframework.web.context.ContextLoader;

import com.free.core.ContextFactory;
import com.free.core.ShutdownWrapper;
import com.free.core.StartApplication;

/**
 * Extension of Spring's context loader listener for web applications
 * 
 * @author Sean Thornton
 */
public class ContextLoaderListener extends
		org.springframework.web.context.ContextLoaderListener {

	/**
	 * 
	 */
	public static final String SERVICES_START_PARAM = "startServices";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void contextInitialized(final ServletContextEvent event) {
		super.contextInitialized(event);
		
		ContextFactory.setContext(ContextLoader.getCurrentWebApplicationContext());
		
		String startServices = event.getServletContext().getInitParameter(
				SERVICES_START_PARAM);

		if (startServices == null || startServices.toUpperCase().equals("TRUE")) {
			StartApplication.startServiceFramework();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void contextDestroyed(final ServletContextEvent event) {
		new ShutdownWrapper().doShutdown();
		super.contextDestroyed(event);
	}
}
