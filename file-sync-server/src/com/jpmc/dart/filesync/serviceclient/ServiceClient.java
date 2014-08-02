package com.jpmc.dart.filesync.serviceclient;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import jsr166y.ForkJoinPool;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.jpmc.cto.dart.model.filesync.ApplicationDatacenter;
import com.jpmc.cto.dart.model.filesync.FileSyncMsg;
import com.jpmc.cto.dart.model.filesync.FileSynchronization;
import com.jpmc.cto.framework.configuration.ApplicationAware;
import com.jpmc.cto.framework.configuration.ApplicationInformation;
import com.jpmc.cto.framework.jms.JmsTemplate;
import com.jpmc.dart.commons.monitor.JmxQueueInspector;
import com.jpmc.dart.dao.jdbc.FileSynchronizationDao;
import com.jpmc.dart.filesync.client.FileSyncClient;
import com.jpmc.vpc.commons.xstream.VPCStream;
import com.jpmc.vpc.model.dart.type.FileSyncStatusType;
import com.jpmc.vpc.model.system.Issue;

import extra166y.Ops;
import extra166y.ParallelArray;

public class ServiceClient  implements ApplicationAware, InitializingBean{
	private static final Log LOG = LogFactory.getLog(ServiceClient.class);

	private FileSynchronizationDao fileSynchronizationDao;
	private String destinationQueue = null;
	private JmxQueueInspector jmxWrapper;
	private int jmsQueueMax = 200;
	private int retryCountMax = 50;
	private ApplicationDatacenter currentDataCenter;
	private ApplicationInformation applicationInformation;
	private JmsTemplate ctoJmsTemplate;
	private ForkJoinPool threadPool;
	private FileSyncClient fileSyncClient;

	@Override
	public void afterPropertiesSet() throws Exception {
		Validate.notNull(fileSyncClient, "FileSyncClient is needed");
		this.threadPool=new ForkJoinPool(4);
		this.currentDataCenter = this.fileSyncClient.getSourceDatacenter();
		Validate.notNull(this.currentDataCenter, "Current datacenter could not be pulled from FileSyncUtil");
	}

	/**
	 * @param destinationQueue
	 *            the destinationQueue to set
	 */
	public void setDestinationQueue(final String destinationQueue) {
		this.destinationQueue = destinationQueue;
	}

	@Override
	public void setApplicationInfo(ApplicationInformation arg0) {
		this.applicationInformation = arg0;
	}


	public List<FileSynchronization> getStuckWork(int resultSize) throws Exception {
		int queueSize=jmxWrapper.getQueueSize(destinationQueue);

		List<FileSynchronization> fileSyncs = new ArrayList<FileSynchronization>();

		if (queueSize > jmsQueueMax){
			return fileSyncs;
		}

		Date minDate = fileSynchronizationDao.getMinDateForFileSyncStuck();

		LOG.debug("picked date "+minDate);

		// no work, just return
		if (minDate == null){
			return fileSyncs;
		}

		LOG.debug("picked date "+minDate+" to find stuck items");

		List<FileSynchronization> stuck = fileSynchronizationDao.findFileSynchronizationByStatus(
				FileSyncStatusType.QUEUED, resultSize, minDate, currentDataCenter);

		filterStuckItems(stuck);

		return stuck;
	}

	protected void filterStuckItems(List<FileSynchronization> stuck) {
		LOG.debug("potential stales " + stuck.size());
		CollectionUtils.filter(stuck, new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				FileSynchronization sync = (FileSynchronization) object;
				// only retry if it's sitting out there for an > hour or so
				long current = System.currentTimeMillis();
				if (sync.getEnqueuedTime() == null) {
					return true;
				}

				long diff = (current - sync.getEnqueuedTime().getTime());
				long diffOver = DateUtils.MILLIS_PER_HOUR / 2;

				LOG.info("time diff is  " + diff + " is it less then " + diffOver);
				if (diff > diffOver) {
					return true;
				}

				return false;
			}
		});
	}


	public List<FileSynchronization> getWork(int resultSize) throws Exception {
		int queueSize=jmxWrapper.getQueueSize(destinationQueue);

		List<FileSynchronization> fileSyncs = new ArrayList<FileSynchronization>();

		if (queueSize > jmsQueueMax){
			return fileSyncs;
		}

		// get the date to query on
		Date minDate = fileSynchronizationDao.getMinDateForFileSync();

		LOG.debug("picked date "+minDate);

		// no work, just return
		if (minDate == null){
			return fileSyncs;
		}

		fileSyncs = fileSynchronizationDao.findFileSynchronizationToStart(resultSize,minDate,currentDataCenter);

		if (CollectionUtils.isNotEmpty(fileSyncs)) {
			LOG.info("have  "+fileSyncs.size()+" items to throw over the wall for date "+minDate);
		} else {
			LOG.info("null filesync for date "+minDate);
			return fileSyncs;
		}

		return fileSyncs;
	}

	public void markWorkToDo(List<FileSynchronization> fileSyncs ) throws Exception {
		boolean tooManyRetries = false;
		FileSynchronization tooMany = null;

		// Check the retry counts
		Iterator<FileSynchronization> iterator = fileSyncs.iterator();
		while (iterator.hasNext()) {
			FileSynchronization fileSync = iterator.next();
			if (fileSync.getRetryCount() > retryCountMax) {
				if (!tooManyRetries) {
					tooManyRetries = true;
					fileSynchronizationDao.updateFileSyncStatusTypeById(
							FileSyncStatusType.ERROR, fileSync.getId(),fileSync.getInsertDate());
					tooMany = fileSync;
				}
				iterator.remove();
			}
		}

		for (FileSynchronization fileSync : fileSyncs){
			fileSync.setFileSyncStatus(FileSyncStatusType.QUEUED);
			fileSync.setEnqueuedTime(new Date());
			fileSync.setRetryCount(fileSync.getRetryCount()+1);
		}

		fileSynchronizationDao.batchUpdateStart(fileSyncs);


		if (tooManyRetries) {
			Issue serviceClientIssue = new Issue();
			serviceClientIssue.setApplicationName(applicationInformation.getApplicationName());
			serviceClientIssue.setId(UUID.randomUUID());
			serviceClientIssue.setCorrelationId(tooMany.getId());
			serviceClientIssue.setDate(new Date());
			serviceClientIssue
					.setDescription("fileSync Object has been republished too many times. ("
							+ tooMany.getRetryCount() + ")");
			serviceClientIssue
					.setErrorText("fileSync Object has been republished too many times. "
							+ tooMany.getSourceFilename());
			serviceClientIssue.setHostName(ApplicationInformation.getMachineName());
			serviceClientIssue.setOrigin(destinationQueue);

			FileSyncMsg fileSyncMsg = new FileSyncMsg();
			fileSyncMsg.setInsertDate(tooMany.getInsertDate());
			fileSyncMsg.setId(tooMany.getId());
			serviceClientIssue.setObject(VPCStream.toXML(fileSyncMsg));

			ctoJmsTemplate.publishIssue(serviceClientIssue);
		}
	}
	
	/**
	 * publish out the file sync messages in parallel
	 * @param fileSyncs
	 */
	public void multiPublishWork(List<FileSynchronization> fileSyncs) {
		ParallelArray<FileSynchronization> workArray = ParallelArray.create(fileSyncs.size(), FileSynchronization.class, threadPool);
		for (int i=0; i < fileSyncs.size();i++){
			workArray.set(i, fileSyncs.get(i));
		}

		workArray.apply(new Ops.Procedure<FileSynchronization>() {
			@Override
			public void op(FileSynchronization fileSync) {
				publishFileSyncMsg(fileSync);
			}
		});

	}



//	/**
//	 * This method is used to find the FileSync records that are not yet
//	 * processed and start them.
//	 *
//	 * @param resultSize
//	 *            maximum no.of records to fetch from FILE_SYNC
//	 * @return list of file FileSynchronizations if any successfully sorted
//	 *         empty list if there are no records to start or lock is not
//	 *         acquired for some reason
//	 * @throws Exception
//	 *             Thrown on JMX Bean lookup - giving up.
//	 */
//	public void startFileSynchronizations(final int resultSize)
//			throws Exception {
//
//		List<FileSynchronization> fileSyncs = null;
//
//		// get the date to query on
//		Date minDate = fileSynchronizationDao.getMinDateForFileSync();
//
//		LOG.debug("picked date "+minDate);
//
//		// no work, just return
//		if (minDate ==null){
//			return;
//		}
//
//		fileSyncs = fileSynchronizationDao.findFileSynchronizationToStart(resultSize,minDate,currentDataCenter);
//
//		// find any ones that have been 'in process" that can be retried automagically
//		List<FileSynchronization> stuck = fileSynchronizationDao.findFileSynchronizationByStatus(FileSyncStatusType.QUEUED, 100, minDate, currentDataCenter);
//
//		LOG.debug("potential stales "+stuck.size());
//
//		CollectionUtils.filter(stuck, new Predicate() {
//			@Override
//			public boolean evaluate(Object object) {
//				FileSynchronization sync=(FileSynchronization)object;
//				// only retry if it's sitting out there for an > hour or so
//				long current = System.currentTimeMillis();
//				if (sync.getEnqueuedTime()==null){
//					return true;
//				}
//
//				long diff=(current-sync.getEnqueuedTime().getTime());
//				long diffOver=DateUtils.MILLIS_PER_HOUR/2;
//
//				LOG.info("time diff is  "+diff+" is it less then "+diffOver);
//
//				if (diff>diffOver){
//					return true;
//				}
//				return false;
//			}
//		});
//
//		if (stuck.size()>0){
//			LOG.warn("stuck items: "+stuck.size());
//			fileSyncs.addAll(stuck);
//		}
//
//		if (fileSyncs!=null) {
//			LOG.info("have  "+fileSyncs.size()+" items to throw over the wall for date "+minDate);
//		} else {
//			LOG.info("null filesync for date "+minDate);
//		}
//
//		boolean tooManyRetries = false;
//		FileSynchronization tooMany = null;
//		if (fileSyncs != null) {
//			// Check the retry counts
//			Iterator<FileSynchronization> iterator = fileSyncs.iterator();
//			while (iterator.hasNext()) {
//				FileSynchronization fileSync = iterator.next();
//				if (fileSync.getRetryCount() > retryCountMax) {
//					if (!tooManyRetries) {
//						tooManyRetries = true;
//						fileSynchronizationDao.updateFileSyncStatusTypeById(
//								FileSyncStatusType.ERROR, fileSync.getId(),minDate);
//						tooMany = fileSync;
//					}
//					iterator.remove();
//				}
//			}
//
//			int queueSize=jmxWrapper.getQueueSize(destinationQueue);
//
//			LOG.info("queue size is "+queueSize);
//
//			if (queueSize <= jmsQueueMax) {
//				ParallelArray<FileSynchronization> workArray = ParallelArray.create(fileSyncs.size(), FileSynchronization.class, threadPool);
//				for (int i=0; i < fileSyncs.size();i++){
//					workArray.set(i, fileSyncs.get(i));
//				}
//
//				workArray.apply(new Ops.Procedure<FileSynchronization>() {
//					@Override
//					public void op(FileSynchronization fileSync) {
//						fileSync.setFileSyncStatus(FileSyncStatusType.QUEUED);
//						fileSync.setEnqueuedTime(new Date());
//						fileSync.setRetryCount(fileSync.getRetryCount()+1);
//						publishFileSyncMsg(fileSync);
//					}
//				});
//			}
//			fileSynchronizationDao.batchUpdate(fileSyncs);
//
//
//			if (tooManyRetries) {
//				Issue serviceClientIssue = new Issue();
//				serviceClientIssue.setApplicationName(applicationInformation.getApplicationName());
//				serviceClientIssue.setId(UUID.randomUUID());
//				serviceClientIssue.setCorrelationId(tooMany.getId());
//				serviceClientIssue.setDate(new Date());
//				serviceClientIssue
//						.setDescription("fileSync Object has been republished too many times. ("
//								+ tooMany.getRetryCount() + ")");
//				serviceClientIssue
//						.setErrorText("fileSync Object has been republished too many times. "
//								+ tooMany.getSourceFilename());
//				serviceClientIssue.setHostName(ApplicationInformation.getMachineName());
//				serviceClientIssue.setOrigin(destinationQueue);
//
//				FileSyncMsg fileSyncMsg = new FileSyncMsg();
//				fileSyncMsg.setInsertDate(tooMany.getInsertDate());
//				fileSyncMsg.setId(tooMany.getId());
//				serviceClientIssue.setObject(VPCStream.toXML(fileSyncMsg));
//
//				ctoJmsTemplate.publishIssue(serviceClientIssue);
//			}
//		}
//	}

	/**
	 * This method is used to publish the message corresponding to given file
	 * sync. only INSERT_DATE and ID will be on the message.
	 *
	 * @param syncs
	 *            file sync
	 */
	public void publishFileSyncMsg(final FileSynchronization fileSync) {
		FileSyncMsg fileSyncMsg = new FileSyncMsg();
		fileSyncMsg.setInsertDate(fileSync.getInsertDate());
		fileSyncMsg.setId(fileSync.getId());
		ctoJmsTemplate.publishMessage(destinationQueue, fileSyncMsg,false);
	}

	/**
	 * @param fileSyncDao
	 *            the fileSyncDao to set
	 */
	public void setFileSynchronizationDao(
			final FileSynchronizationDao fileSynchronizationDao) {
		this.fileSynchronizationDao = fileSynchronizationDao;
	}

	public JmxQueueInspector getJmxWrapper() {
		return jmxWrapper;
	}

	/**
	 * set the jmxWrapper
	 *
	 * @param jmxWrapper
	 *            the jmxWrapper to set
	 */
	public void setJmxWrapper(JmxQueueInspector jmxWrapper) {
		this.jmxWrapper = jmxWrapper;
	}

	/**
	 * @return the jmsQueueMax
	 */
	public int getJmsQueueMax() {
		return jmsQueueMax;
	}

	/**
	 * @param jmsQueueMax
	 *            the jmsQueueMax to set
	 */
	public void setJmsQueueMax(int jmsQueueMax) {
		this.jmsQueueMax = jmsQueueMax;
	}

	/**
	 * @return the retryCountMax
	 */
	public int getRetryCountMax() {
		return retryCountMax;
	}

	/**
	 * @param retryCountMax
	 *            the retryCountMax to set
	 */
	public void setRetryCountMax(int retryCountMax) {
		this.retryCountMax = retryCountMax;
	}

	public void setFileSyncClient(FileSyncClient fileSyncClient) {
		this.fileSyncClient = fileSyncClient;
	}

	public ApplicationDatacenter getCurrentDataCenter() {
		return currentDataCenter;
	}
	public void setCtoJmsTemplate(JmsTemplate ctoJmsTemplate) {
		this.ctoJmsTemplate = ctoJmsTemplate;
	}

}
