package com.jpmc.dart.filesync.servicesla;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.StatefulJob;
import org.springframework.beans.factory.InitializingBean;

import com.jpmc.cto.dart.model.filesync.ApplicationDatacenter;
import com.jpmc.cto.dart.model.filesync.FileSynchronization;
import com.jpmc.cto.framework.concurrent.LockManager;
import com.jpmc.cto.framework.configuration.MachineInformation;
import com.jpmc.cto.framework.exception.SystemException;
import com.jpmc.cto.framework.scheduling.ExecutionContext;
import com.jpmc.cto.framework.scheduling.ScheduledJob;
import com.jpmc.dart.filesync.client.FileSyncClient;
import com.jpmc.dart.filesync.http.HttpFileSender;
import com.jpmc.dart.filesync.serviceclient.ServiceClient;

/**
 * This class is responsible for scanning the file sync stuck items
 *
 * @author r502440
 *
 */
public class StuckItemsScanner extends ScheduledJob implements StatefulJob, InitializingBean {

	private static final Log LOG = LogFactory.getLog(StuckItemsScanner.class);

	private ApplicationDatacenter currentDataCenter;
	private LockManager lockManager;
	private FileSyncClient fileSyncClient;
	private ServiceClient serviceClient;
	private HttpFileSender httpFileSender;

	/**
	 * @see com.jpmc.cto.framework.scheduling.ScheduledJob#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws SystemException {
		super.afterPropertiesSet();
		Validate.notNull(this.fileSyncClient, "Need the FileSyncClient set in order to work");
		Validate.notNull(this.serviceClient, "Need the ServiceClient set in order to work");
		Validate.notNull(this.httpFileSender, "Need the HttpFileSender set in order to work");
	}


	@Override
	public void executeService(ExecutionContext arg0) throws SystemException {
		LOG.info("Entering Stuck items scanner job.");

		if(this.currentDataCenter == null){
			synchronized(FileSyncWorkScanner.class){
	        	if(this.currentDataCenter == null) {
	            	this.currentDataCenter = fileSyncClient.getSourceDatacenter();
	            }
	        }
		}

		boolean targetUp = httpFileSender.pingTargetServer();

		StopWatch sw = new StopWatch();
		boolean isLockAquired = false;

		try {
			isLockAquired = lockManager.acquire("STUCK_ITEMS_SCANNER_" + currentDataCenter.getDatacenterName(), 10,
					TimeUnit.MINUTES);

			if (isLockAquired) {
				if (targetUp) {
					// Sleep to hold the lock
					Thread.sleep(10 * 1000);

					sw.start();

					List<FileSynchronization> stuck = serviceClient.getStuckWork(1000);

					if (CollectionUtils.isEmpty(stuck)) {
						LOG.info("No stuck items to work on.");
						return;
					}

					serviceClient.markWorkToDo(stuck);

					if (targetUp){
						if (CollectionUtils.isNotEmpty(stuck)){
							serviceClient.multiPublishWork(stuck);
						}
					}

					sw.stop();
					LOG.info("Complete Stuck items scan job in " + sw.getTime() + " milliseconds.");
				} else {
					LOG.info("target is not up.");
				}
			} else {
				LOG.info(MachineInformation.getMachineName() + " unable to get lock " + "STUCK_ITEMS_SCANNER_"
						+ currentDataCenter.getDatacenterName()+" we probably don't care unless the other side couldn't acquire either");
			}
		} catch (Exception e) {
			throw new SystemException(e);
		} finally {
			if (isLockAquired) {
				lockManager.release("STUCK_ITEMS_SCANNER_" + currentDataCenter.getDatacenterName());
			}
		}
	}


	public void setFileSyncClient(FileSyncClient fileSyncClient) {
		this.fileSyncClient = fileSyncClient;
	}

	public void setLockManager(LockManager lockManager) {
		this.lockManager = lockManager;
	}

	public void setServiceClient(ServiceClient serviceClient) {
		this.serviceClient = serviceClient;
	}

	public void setHttpFileSender(HttpFileSender httpFileSender) {
		this.httpFileSender = httpFileSender;
	}

	@Override
	public String getName() {
		return "File Sync Stuck Items Scanner";
	}
}
