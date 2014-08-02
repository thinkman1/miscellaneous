package com.jpmc.dart.dao.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Sathya
 *
 */
public final class DartSqlContextFactory {
	private static ApplicationContext ctx = new ClassPathXmlApplicationContext("dartSqlContext.xml");

	/**
	 * Private constructor.
	 */
	private DartSqlContextFactory() {
	}


	/**
	 * @return The current application context
	 */
	public static ApplicationContext getContext() {
		if (ctx != null) {
			return ctx;
		}
		throw new IllegalAccessError("Circular reference in accessing application context. "
				+ "Do not assign application context in the constructor or class level "
				+ "variables.");
	}

	/**
	 *
	 * @param beanName the name of the sql bean
	 * @return the sql from the bean as a string
	 */
	public static String getSqlBean(final String beanName) {
		return (String) getContext().getBean(beanName);
	}

	/**
	 * Sets the ApplicationContext. Intended for unit testing purporses.
	 *
	 * @param context
	 *            Application context to use
	 */
	public static void setContext(final ApplicationContext context) {
		ctx = context;
	}
}
