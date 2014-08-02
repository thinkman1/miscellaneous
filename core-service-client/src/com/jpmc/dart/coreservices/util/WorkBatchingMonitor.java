package com.jpmc.dart.coreservices.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WorkBatchingMonitor<T> {
	private Log LOG = LogFactory.getLog(WorkBatchingMonitor.class);

	final private ExecutorService executor;
	final private Queue<T> monitorQueue;
	final private WorkBatchingCallback<T> callback;
	final private int batchSize;
	final private long maxHeadAge=DateUtils.MILLIS_PER_MINUTE/4;
	volatile boolean go=true;
	volatile long headItemAge=0;
	private WorkBatchingErrorCallback<T> errorHandler;

	private Thread monitorThread = new Thread() {
		public void run() {
			while (go){
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e){
					Thread.currentThread().interrupt();
					LOG.warn("monitor thread interrupted!!!, I need to die (after I finish the rest of my batches for this run).");
					go=false;
				}

				int numberOfBatches = monitorQueue.size()/batchSize;

				boolean submit=false;

				if (numberOfBatches > 0){
					// add one since the chances of having an even number here are zilch.
					numberOfBatches++;
					headItemAge=0;
					submit=true;
				} else {
					int monitorQueueSize=monitorQueue.size();

					LOG.trace("(batch size Zero) queue size "+monitorQueueSize);

					if ((monitorQueueSize>0)&&(headItemAge==0)){
						headItemAge=System.currentTimeMillis();
					} else if ((monitorQueueSize>0)&&(headItemAge>0)){
						long ageOfOldest = System.currentTimeMillis()-headItemAge;
						if (ageOfOldest> maxHeadAge){
							submit=true;
							numberOfBatches=1;
						}
					} else if (!go){
						// finish any stray work (when we don't have enough stray items to make a batch)
						if (monitorQueueSize>0){
							submit=true;
							numberOfBatches=1;
						}
					}
				}

				if (submit){
					for (int i = 0 ; i < numberOfBatches ; i++){
						final List<T> items = new ArrayList<T>();

						LOG.trace("submit batch "+i);

						for (int j = 0 ; j < batchSize ;j++){
							if (monitorQueue.peek()!=null){
								items.add(monitorQueue.remove());
							} else {
								break;
							}
						}
						try {
							executor.submit(new Callable<T>() {
								@Override
								public T call() throws Exception {
									try {
										callback.act(items);
									} catch (Throwable e){
										// make sure a bad task doesn't kill the monitor thread
										LOG.warn("caught exception after I submitted batch!",e);
									}
									return null;
								}
							});
						} catch (Exception reject){
							if (errorHandler!=null){
								errorHandler.handleError(items, reject);
							} else {
								LOG.error("No error hander, I rejected batch!");
							}
						}
					}
				}
			}
		}
	};

	public WorkBatchingMonitor(final ExecutorService executor,Queue<T> monitorQueue,WorkBatchingCallback<T> callback, int batchSizeIn) {
		this(executor,monitorQueue,callback,batchSizeIn,null);
	}

	public WorkBatchingMonitor(final ExecutorService executor,Queue<T> monitorQueue,WorkBatchingCallback<T> callback, int batchSize,WorkBatchingErrorCallback<T> errorHandler) {
		this.executor=executor;
		this.monitorQueue=monitorQueue;
		this.callback=callback;
		this.batchSize=batchSize;
		this.errorHandler=errorHandler;
		monitorThread.start();
	}

	public void stop() throws InterruptedException{
		monitorThread.interrupt();
		go=false;
		monitorThread.join(3000);
	}
}
