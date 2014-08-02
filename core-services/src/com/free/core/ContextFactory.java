package com.free.core;

import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.context.ApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * <code>ContextFactory</code> allows a hosted service to gain access to the
 * <code>ApplicationContext</code>. No other <code>ApplicationContext</code>
 * should be created by a service.
 * 
 * @author Sean Thornton
 */
public final class ContextFactory {
	private static ApplicationContext ctx;
	private static boolean initialized = false;

	/**
	 * Default constructor
	 */
	private ContextFactory() {

	}
	
	private static void initContext() {
		if (!initialized) {
			initialized = true;
			BeanFactoryLocator locator = ContextSingletonBeanFactoryLocator
					.getInstance();
			BeanFactoryReference reference = locator
					.useBeanFactory("applicationContext");
			ctx = (ApplicationContext) reference.getFactory();
		}
	}

	/**
	 * Sets the ApplicationContext. Intended for unit testing purporses.
	 * 
	 * @param context
	 *            Application context to use
	 */
	public synchronized static void setContext(final ApplicationContext context) {
		ctx = context;
		if (ctx != null) {
			initialized = true;
		}
	}

	/**
	 * @return The <code>ApplicationContext</code> for this service
	 */
	public synchronized static ApplicationContext getContext() {
		initContext();
		if (ctx != null) {
			return ctx;
		} else {
			throw new IllegalAccessError(
					"Circular reference in accessing application context. "
							+ "Do not assign application context in the "
							+ "constructor or class level variables.");
		}
	}

	/**
	 * @param beanName
	 *            bean to get
	 * @return the bean
	 */
	public static Object getObject(final String beanName) {
		return getContext().getBean(beanName);
	}

	/**
	 * closes the underlying context
	 */
	public synchronized static void closeContext() {
		if (ctx != null) {
			if (AbstractApplicationContext.class.isAssignableFrom(ctx.getClass())) {
				((AbstractApplicationContext) ctx).close();
				initialized = false;
			}
		}
	}
}
