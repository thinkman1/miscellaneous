package com.free.jms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.jms.core.ProducerCallback;

import com.free.util.FrameworkUtils;
import com.jpmc.cto.commons.xstream.XmlStream;
import com.jpmc.cto.framework.MessageHeader;
import com.jpmc.cto.framework.exception.FrameworkException;

public class JmsUtils {
	private static Log log = LogFactory.getLog(JmsUtils.class);
	
	public static Source getSource(Message message) throws FrameworkException {
		Source source = null;
		
		try {
			if (message instanceof TextMessage) {
				TextMessage tm = (TextMessage) message;
				String text = tm.getText();
				source = new StreamSource(new StringReader(text));
			} else if (message instanceof BytesMessage) {
				BytesMessage m = (BytesMessage) message;
				ByteArrayOutputStream os = new ByteArrayOutputStream();

				byte[] bytes = new byte[1024];
				int count = 0;
				while ((count = m.readBytes(bytes, bytes.length)) != -1) {
					os.write(bytes, 0, count);
				}

				ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
				source = new StreamSource(is);
			} else {
				throw new JMSException("Unsupported message type: " + message.getClass().getName() 
						+ "\n" + XmlStream.toXML(message));
			}
		} catch (JMSException e) {
			throw new FrameworkException("Error retrieving object from JMS message", e);
		}
		
		return source;
	}
	
	public static void sendMessage(final String dest, final com.jpmc.cto.framework.Message message, 
			final MessagePostProcessor mpp, final boolean persistent, 
			org.springframework.jms.core.JmsTemplate jmsTemplate) {
		final String destination = StringUtils.trimToNull(dest);
		
		jmsTemplate.execute(new ProducerCallback<Object>() {
			@Override
			public Object doInJms(Session session, MessageProducer producer) throws JMSException {
				Queue q = session.createQueue(destination);
				
				Message m = DefaultMessageCreator.getMessage(session, message, mpp);
				
				if (log.isDebugEnabled()) {
					String type = (message.getMessageType() != null) ? message.getMessageType().getType() : null;
					log.debug(String.format("Publishing message of type '%s' with "
						+ "id '%s' to '%s'", type, message.getId(), destination));
					if (log.isTraceEnabled()) {
						log.trace(FrameworkUtils.prettyMessageContents(m));
					}
				}
				
				int mode = persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
				producer.send(q, m, mode, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
				
				return null;
			}
		});
	}
	
	public static class DefaultMessageCreator implements MessageCreator {
		private com.jpmc.cto.framework.Message message;
		private MessagePostProcessor postProcessor;

		public DefaultMessageCreator(
				com.jpmc.cto.framework.Message message, 
				MessagePostProcessor postProcessor,
				String destination) {
			this.message = message;
			this.postProcessor = postProcessor;
		}
		
		public static javax.jms.Message getMessage(Session session, com.jpmc.cto.framework.Message message, MessagePostProcessor mpp) throws JMSException {
			String xml = XmlStream.toXML(message);
			Message tm = session.createTextMessage(xml);
			
			if (mpp != null) {
				if (mpp instanceof MessagePostProcessor) {
					tm = mpp.postProcessMessage(tm);
				} else {
					log.warn("postProcessor " + mpp.getClass().getName() 
						+ " was not an instance of org.springframework.jms.core." 
						+ "MessagePostProcessor.  Skipping execution");
				}
			}
			
			String id = null;
			if (message.getId() != null) {
				id = message.getId().toString();
				tm.setStringProperty(MessageHeader.MESSAGE_ID, id);
			}
			
			String type = null;
			if (message.getMessageType() != null) {
				type = message.getMessageType().getType();
				tm.setStringProperty(MessageHeader.MESSAGE_TYPE, type);
			}
			
			return tm;
		}

		@Override
		public javax.jms.Message createMessage(Session session) throws JMSException {
			return getMessage(session, message, postProcessor);

		}

		public com.jpmc.cto.framework.Message getMessage() {
			return message;
		}

		public void setMessage(com.jpmc.cto.framework.Message message) {
			this.message = message;
		}

		public MessagePostProcessor getPostProcessor() {
			return postProcessor;
		}

		public void setPostProcessor(MessagePostProcessor postProcessor) {
			this.postProcessor = postProcessor;
		}
	}
}
