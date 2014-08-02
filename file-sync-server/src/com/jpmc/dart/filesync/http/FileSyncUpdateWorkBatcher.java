package com.jpmc.dart.filesync.http;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.jpmc.cto.dart.model.filesync.FileSynchronization;
import com.jpmc.dart.coreservices.util.WorkBatchingCallback;
import com.jpmc.dart.coreservices.util.WorkBatchingMonitor;
import com.jpmc.dart.dao.jdbc.FileSynchronizationDao;

public class FileSyncUpdateWorkBatcher implements
		ApplicationListener<UpdateFinalStatusEvent>, InitializingBean, DisposableBean {
	private static class FileSyncBatchUpdateCallback implements WorkBatchingCallback<FileSynchronization>{
		private FileSynchronizationDao dao;

		public FileSyncBatchUpdateCallback(FileSynchronizationDao dao){
			this.dao=dao;
		}

		@Override
		public void act(List<FileSynchronization> gatheredItems) {
			dao.batchUpdate(gatheredItems);
			LOG.info("updated batch of "+gatheredItems.size());
		}
	}

	private FileSynchronizationDao fileSynchronizationDao;
	private WorkBatchingMonitor<FileSynchronization> databaseUpdateBatcher;
	private ExecutorService batchUpdatesThreadPool;
	private TransactionTemplate tx;
	private Queue<FileSynchronization> workQueue = new ConcurrentLinkedQueue<FileSynchronization>();
	private static final Log LOG = LogFactory.getLog(FileSyncUpdateWorkBatcher.class);

	@Override
	public void onApplicationEvent(UpdateFinalStatusEvent event) {
		workQueue.add(event.getFinal());
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.batchUpdatesThreadPool=Executors.newFixedThreadPool(15);

		databaseUpdateBatcher = new WorkBatchingMonitor<FileSynchronization>(batchUpdatesThreadPool,
				workQueue,
				new FileSyncBatchUpdateCallback(this.fileSynchronizationDao),
				200);
	}

	public void destroy() throws Exception {
		databaseUpdateBatcher.stop();
	}


	public void setBatchUpdatesThreadPool(ExecutorService batchUpdatesThreadPool) {
		this.batchUpdatesThreadPool = batchUpdatesThreadPool;
	}

	public void setTx(TransactionTemplate tx) {
		this.tx = tx;
	}

	public void setFileSynchronizationDao(
			FileSynchronizationDao fileSynchronizationDao) {
		this.fileSynchronizationDao = fileSynchronizationDao;
	}
}
