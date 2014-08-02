package com.free.jms;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.jms.core.SessionCallback;

import com.free.jms.JmsUtils.DefaultMessageCreator;
import com.jpmc.cto.framework.Event;
import com.jpmc.cto.framework.Message;
import com.jpmc.cto.framework.configuration.ApplicationConfiguration;
import com.jpmc.cto.framework.configuration.ApplicationConfigurationAware;
import com.jpmc.cto.framework.exception.Issue;
import com.jpmc.cto.framework.mail.MailMessage;

public class JmsTemplate implements ApplicationConfigurationAware, InitializingBean {
	private static Log log = LogFactory.getLog(JmsTemplate.class);
	
	private org.springframework.jms.core.JmsTemplate springTemplate;

	private ApplicationConfiguration appConfig;
	
	public void publishMessage(String destination, Message message) {
		publishMessage(destination, message, true, null);
	}
	
	public void publishMessage(String destination, Message message, MessagePostProcessor mpp) {
		publishMessage(destination, message, true, mpp);
	}
	
	public void publishMessage(String destination, Message message, boolean persistent) {
		publishMessage(destination, message, persistent, null);
	}
	
	public void publishEvent(Event event) {
		publishEvent(event, true);
	}
	
	public void publishEvent(Event event, boolean persistent) {
		publishMessage(appConfig.getEventQueue(), event, persistent);
	}
	
	public void publishEmail(MailMessage mail) {
		publishEmail(mail, true);
	}
	
	public void publishEmail(MailMessage mail, boolean persistent) {
		publishMessage(appConfig.getEmailQueue(), mail, persistent);
	}
	
	public void publishMessage(String destination, Message message, boolean persistent, MessagePostProcessor mpp) {
		if (MessageContext.exists()) {
			MessageContext.get().add(new MessageAndDestination(message, destination, mpp, persistent));
		} else {
			JmsUtils.sendMessage(destination, message, mpp, persistent, springTemplate);
		}
	}
	
	
	// !!! Issues and Commands always send immediately - no check on Context !!!
	// Open for debate if we ever want to change commands
	
	/**
	 *	Publishes an issue immediately.  Uses raw JMS since spring can get tricky with transaction
	 *	propagation and I really wanted to make sure it went out immediately and not maintain two
	 *	connection factories to do so (as was done in the past). 
	 */
	public void publishIssue(final Issue issue) {
//		JmsUtils.sendMessage(appConfig.getIssueQueue(), issue, null, true, issueTemplate);
		
		Connection connection = null;
		Session session = null;
		MessageProducer producer = null;
		try {
			connection = getSpringTemplate().getConnectionFactory().createConnection();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			
			Queue queue = session.createQueue(appConfig.getIssueQueue());
			producer = session.createProducer(queue);

			javax.jms.Message message = DefaultMessageCreator.getMessage(session, issue, null);
			log.info("Publishing issue " + issue.getId() + " to queue " + appConfig.getIssueQueue());
			producer.send(queue, message, DeliveryMode.PERSISTENT, javax.jms.Message.DEFAULT_PRIORITY, javax.jms.Message.DEFAULT_TIME_TO_LIVE);
		} catch (JMSException e) {
			throw org.springframework.jms.support.JmsUtils.convertJmsAccessException(e);
		} finally {
			if (producer != null) {
				try {
					producer.close();
				} catch (JMSException e) {
					log.warn("Problem closing producer", e);
				}
			}
			
			if (session != null) {
				try {
					session.close();
				} catch (JMSException e) {
					log.warn("Problem closing session", e);
				}
			}
			
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					log.warn("Problem closing connection", e);
				}
			}
		}
		
	}
	
	public void publishCommand(Command command) {
		Destination d = springTemplate.execute(new SessionCallback<Destination>() {
			@Override
			public Destination doInJms(Session session) throws JMSException {
				return session.createTopic(appConfig.getCommandQueue());
			}
		});
		
		springTemplate.send(d, new JmsUtils.DefaultMessageCreator(command, null, d.toString()));
	}
	
	public org.springframework.jms.core.JmsTemplate getSpringTemplate() {
		return springTemplate;
	}

	public void setSpringTemplate(org.springframework.jms.core.JmsTemplate springTemplate) {
		this.springTemplate = springTemplate;
	}

	@Override
	public void setConfiguration(ApplicationConfiguration ac) {
		this.appConfig = ac;
	}

	@Override
	public void afterPropertiesSet() {
		Validate.notNull(appConfig, "appConfig cannot be null");
		Validate.notNull(springTemplate, "springTemplate cannot be null");
		
		Validate.notNull(appConfig.getCommandQueue(), "appConfig.commandQueue cannot be null");
		Validate.notNull(appConfig.getEmailQueue(), "appConfig.emailQueue cannot be null");
		Validate.notNull(appConfig.getEventQueue(), "appConfig.eventQueue cannot be null");
		Validate.notNull(appConfig.getIssueQueue(), "appConfig.issueQueue cannot be null");
	}
}
