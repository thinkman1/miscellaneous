package com.free.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.xml.transform.Source;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.scheduling.SchedulingException;

import com.free.jms.CommandType;
import com.free.jms.JmsAware;
import com.free.jms.JmsListenerContainer;
import com.free.jms.JmsTemplate;
import com.jpmc.cto.commons.helpers.ExceptionHelper;
import com.jpmc.cto.commons.xstream.XmlStream;
import com.jpmc.cto.framework.EventType;
import com.jpmc.cto.framework.MessageHeader;
import com.jpmc.cto.framework.MessageType;
import com.jpmc.cto.framework.configuration.ApplicationAware;
import com.jpmc.cto.framework.configuration.ApplicationInformation;
import com.jpmc.cto.framework.configuration.MachineInformation;
import com.jpmc.cto.framework.exception.FrameworkException;
import com.jpmc.cto.framework.exception.FrameworkIssue;
import com.jpmc.cto.framework.exception.Issue;
import com.jpmc.cto.framework.exception.SystemException;

/**
 * @author Sean Thornton
 */
public final class FrameworkUtils implements JmsAware, ApplicationAware, InitializingBean {
	private static Log log = LogFactory.getLog(FrameworkUtils.class);
	
	private static JmsTemplate template;
	
	private static String applicationName;
	private static String machineName;
	private static int jmxPort;
	
	public FrameworkUtils() {
	}
	
	public static void startContext(ApplicationContext ctx) {
		Map<String, JmsListenerContainer> listenerMap = ctx.getBeansOfType(JmsListenerContainer.class);
		for (Map.Entry<String, JmsListenerContainer> e : listenerMap.entrySet()) {
			JmsListenerContainer c = e.getValue();
			if (c.isAutoStartup()) {
				log.info(String.format("%s is configured to start automatically and listen on %s", e.getKey(), c.getDestinationName()));
			} else {
				log.info(String.format("Starting bean %s that listens on %s", e.getKey(), c.getDestinationName()));
				c.start();
			}
		}
		
		Map<String, Scheduler> schedulers = ctx.getBeansOfType(Scheduler.class);
		try {
			for (Map.Entry<String, Scheduler> e : schedulers.entrySet()) {
				Scheduler s = e.getValue();
					if (!s.isStarted() && !s.isShutdown()) {
						log.info("Starting scheduler " + e.getKey());
						s.start();
					}
			}
		} catch (SchedulerException ex) {
			throw new SchedulingException("Problem starting Scheduler", ex);
		}
	}
	
	public static com.jpmc.cto.framework.Message getMessage(Source source, Unmarshaller unmarshaller) throws FrameworkException {
		try {
			Object object = unmarshaller.unmarshal(source);
			if (object instanceof com.jpmc.cto.framework.Message) {
				return (com.jpmc.cto.framework.Message) object;
			} else {
				throw new FrameworkException("Object not instance of com.jpmc.cto.framework.Message");
			}
		} catch (XmlMappingException e) {
			throw new FrameworkException("Error mapping source to object", e);
		} catch (IOException e) {
			throw new FrameworkException("Error reading from source", e);
		}
	}

	/**
	 * @param serviceName
	 *            The name of the service
	 * @param origin
	 *            Where the message came from
	 * @param description
	 *            The exception description
	 * @param message
	 *            The jms message the had issues
	 * @param ex
	 *            The exception that occurred
	 * @param correlationId
	 *            the correlation id for the issue(s)
	 * @param linkId
	 *            the linkId for the issue
	 * @return 
	 */
	public static Issue handleException(
			final String serviceName, 
			final String description,
			final String text, 
			final String origin,
			final Object object, 
			final Throwable ex, 
			final UUID linkId,
			final UUID correlationId) {
		logException(serviceName, object, ex);
		
		Issue issue = createIssue(serviceName, description, text, origin, object, ex);
		
		if (linkId == null) {
			if (object instanceof Message) {
				Message m = (Message) object;
				try {
					String value = m.getStringProperty(MessageHeader.MESSAGE_ID);
					if (value != null) {
						UUID link = UUID.fromString(value);
						issue.setLinkId(link);
					}
				} catch (Exception e) {
					log.warn("Couldn't read header property", e);
				}
			} else if (object instanceof com.jpmc.cto.framework.Message) {
				com.jpmc.cto.framework.Message m = (com.jpmc.cto.framework.Message) object;
				issue.setLinkId(m.getId());
			}
		} else {
			issue.setLinkId(linkId);
		}

		issue.setCorrelationId(correlationId);

		if (template != null) {
			template.publishIssue(issue);
		}
		
		return issue;
	}

	/**
	 * Helper method to log exceptions
	 * 
	 * @param serviceName
	 *            The service name that caused the exception
	 * @param object
	 *            The object related to the exception
	 * @param exception
	 *            The, you know, exception
	 */
	public static void logException(final String serviceName, final Object object, final Throwable exception) {
		StringBuilder sb = new StringBuilder();

		sb.append("Service '");
		sb.append(serviceName);
		sb.append("' encountered an error while processing.\n");
		if (object != null) {
			try {
				sb.append("Message Contents:\n");
				if (object instanceof String) {
					sb.append((String) object);
				} else if (object instanceof Message) {
					sb.append(prettyMessageContents((Message) object));
				} else {
					sb.append(XmlStream.toXML(object));
				}
				sb.append("\n");
			} catch (Exception e) {
				log.warn("Error serializing message", e);
			}
		}

		if (exception == null) {
			log.error(sb.toString());
		} else {
			log.error(sb.toString(), exception);
		}
	}
	
	/**
	 * Helper method to create issue objects
	 * 
	 * @param serviceName
	 *            The name of the service causing the issue
	 * @param description
	 *            desc of the issue
	 * @param origin
	 *            the origin queue of the issue
	 * @param problemObject
	 *            The object related to the issue
	 * @param throwable
	 *            The exception
	 * @param correlationId
	 *            Correlation id of the issue if applicable
	 * @return the initialized Issue
	 */
	public static Issue createIssue(
			String serviceName, String description, String text,
			String origin, Object problemObject, Throwable throwable, Issue issue) {
		if (problemObject == null && throwable instanceof SystemException) {
			SystemException se = (SystemException) throwable;
			if (se.getLinkedMessage() != null) {
				problemObject = se.getLinkedMessage();
			}
		}
		
		if (origin == null && throwable instanceof SystemException) {
			SystemException se = (SystemException) throwable;
			origin = se.getOrigin();
		}
		
		if (text == null && throwable instanceof SystemException) {
			SystemException se = (SystemException) throwable;
			text = se.getExtendedDescription();
		}

		issue.setId(UUID.randomUUID());
		issue.setDate(new Date());
		issue.setDescription(description);
		issue.setErrorText(text);
		issue.setOrigin(origin);
		issue.setMachineName(machineName);
		if (problemObject instanceof String) {
			issue.setObject((String) problemObject);
		} else if (problemObject instanceof Message) {
			issue.setObject(prettyMessageContents((Message) problemObject));
		} else {
			issue.setObject(XmlStream.toXML(problemObject));
		}
		issue.setApplicationName(applicationName);
		issue.setServiceName(serviceName);
		
		issue.setException(XmlStream.toXML(throwable));
		if (throwable instanceof SystemException) {
			SystemException se = (SystemException) throwable;
			issue.setErrorCode(se.getErrorCode());
		}
		

		return issue;
	}
	
	public static Issue createIssue(
			String serviceName, String description, String text,
			String origin, Object problemObject, Throwable throwable) {
		return createIssue(serviceName, description, text, origin, problemObject, 
				throwable, new FrameworkIssue());
		
	}
	
	
	public static Issue createIssue() {
		return FrameworkUtils.createIssue(null, null, null, null, null, null, new FrameworkIssue());
	}

	/**
	 * Logs all contents of a message to the log. Hopefully from this output it should be possible to reconstruct the
	 * message if needed.
	 * 
	 * @param msg
	 *            The JMS Message to log
	 * @return the log message
	 */
	@SuppressWarnings("rawtypes")
	public static String prettyMessageContents(final Message msg) {
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("\n===START MESSAGE==============================\n");
			sb.append("==========MESSAGE HEADERS=====================\n");
			Enumeration e = msg.getPropertyNames();
			while (e.hasMoreElements()) {
				String propertyName = (String) e.nextElement();
				sb.append(propertyName);
				sb.append(":");
				sb.append(msg.getStringProperty(propertyName));
			}
			sb.append("\n==========MESSAGE BODY========================\n");

			if (msg instanceof TextMessage) {
				TextMessage message = (TextMessage) msg;
				sb.append(message.getText());
			} else {
				sb.append("Message not an instance of TextMessage\n");
				sb.append(XmlStream.toXML(msg));
			}
			sb.append("\n===END MESSAGE================================");
		} catch (JMSException e) {
			log.warn("Unable to log message contents for message: " + msg, e);
		}

		return sb.toString();
	}

	/**
	 * Logs message received information
	 * 
	 * @param localLog
	 *            The Log object to use to log
	 * @param message
	 *            The JMS Message to log
	 */
	public static void logMessageReceived(final Log localLog, final Message message) {
		if (localLog.isDebugEnabled()) {
			try {
				localLog.debug(String.format("Received message of type '%s' with message id '%s'",
						message.getStringProperty(MessageHeader.MESSAGE_TYPE),
						message.getStringProperty(MessageHeader.MESSAGE_ID)));
			} catch (JMSException e) {
				log.warn("Unable to log message received for message: " + message, e);
			}
		}
	}

	/**
	 * @param message
	 *            The message to dump to the file
	 * @return The written file
	 * @throws IOException
	 *             If there's an issue writing to the file
	 * @throws URISyntaxException
	 *             if unable to determine the location to write the file
	 */
	public static File writeMessageToFile(final Message message, String origin) throws IOException, URISyntaxException {
		StringBuilder sb = new StringBuilder();
		if (message instanceof TextMessage) {
			TextMessage tm = (TextMessage) message;
			try {
				sb.append(tm.getText());
			} catch (JMSException e) {
				sb.append("Unable to retrieve TextMessage text:\n" + ExceptionHelper.getStackTraceAsString(e));
			}
		} else {
			sb.append(XmlStream.toXML(message));
		}

		return writeToFile(sb.toString(), origin);
	}

	/**
	 * @param message
	 *            The message to dump to the file
	 * @return The written file
	 * @throws IOException
	 *             If there's an issue writing to the file
	 * @throws URISyntaxException
	 *             if unable to determine the location to write the file
	 */
	public static File writeMessageToFile(final com.jpmc.cto.framework.Message message, String origin) throws IOException, URISyntaxException {
		return writeToFile(XmlStream.toXML(message), origin);
	}
	
	/**
	 * @param message
	 *            The message to dump to the file
	 * @return The written file
	 * @throws IOException
	 *             If there's an issue writing to the file
	 * @throws URISyntaxException
	 *             if unable to determine the location to write the file
	 */
	protected static File writeToFile(final String message, final String origin) throws IOException, URISyntaxException {
		URL resource = FrameworkUtils.class.getClassLoader().getResource(".");

		StringBuilder sb = new StringBuilder();
		sb.append(UUID.randomUUID())
		  .append(".")
		  .append(System.currentTimeMillis())
		  .append(".")
		  .append(jmxPort)
		  .append(".wip");
		String fileName = sb.toString();

		File file = new File(new File(resource.toURI()), fileName);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file), message.length() + 255);
			writer.write(origin);
			writer.write("\r\n");
			writer.write(message);
		} finally {
			if (writer != null) {
				writer.close();
			}
		}

		return file;
	}
	
	public static boolean isOfType(CommandType type, CommandType [] list) {
		boolean retVal = false;
		if (type != null && list != null) {
			for (CommandType listItem : list) {
				if (listItem != null && type.getType().equals(listItem.getType())) {
					retVal = true;
					break;
				}
			}
		}
		
		return retVal;
	}
	
	public static boolean isOfType(MessageType type, MessageType [] list) {
		boolean retVal = false;
		if (type != null && list != null) {
			for (MessageType listItem : list) {
				if (listItem != null && type.getType().equals(listItem.getType())) {
					retVal = true;
					break;
				}
			}
		}
		
		return retVal;
	}
	
	public static boolean isOfType(EventType type, EventType [] list) {
		boolean retVal = false;
		if (type != null && list != null) {
			for (EventType listItem : list) {
				if (listItem != null && type.getType().equals(listItem.getType())) {
					retVal = true;
					break;
				}
			}
		}
		
		return retVal;
	}

	@Override
	public void setApplicationInfo(ApplicationInformation info) {
		machineName = MachineInformation.getMachineName();
		applicationName = info.getApplicationName();
//		jvmName = MachineInformation.getJvmName();
		jmxPort = MachineInformation.getJmxPort();
	}

	@Override
	public void setJmsTemplate(JmsTemplate template) {
		FrameworkUtils.template = template;
	}

	@Override
	public void afterPropertiesSet() {
		if (applicationName == null) {
			ApplicationInformation info = new ApplicationInformation();
			info.afterPropertiesSet();
			
			setApplicationInfo(info);
		}
	}
}
