package com.jpmc.dart.filesync.servicesla;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.StatefulJob;

import com.jpmc.cto.dart.model.filesync.ApplicationDatacenter;
import com.jpmc.cto.dart.model.filesync.FileSynchronization;
import com.jpmc.cto.framework.concurrent.LockManager;
import com.jpmc.cto.framework.exception.SystemException;
import com.jpmc.cto.framework.scheduling.ExecutionContext;
import com.jpmc.cto.framework.scheduling.ScheduledJob;
import com.jpmc.dart.filesync.client.FileSyncClient;
import com.jpmc.dart.filesync.http.HttpFileSender;
import com.jpmc.dart.filesync.serviceclient.ServiceClient;

public class FileSyncWorkScanner extends ScheduledJob implements StatefulJob{
	private static final Log LOG = LogFactory.getLog(FileSyncWorkScanner.class);

	private ServiceClient serviceClient;
	private Integer resultSize;

	private LockManager lockManager;

	private HttpFileSender httpFileSender;

	private FileSyncClient fileSyncClient;

	private volatile ApplicationDatacenter currentDataCenter;

	public FileSyncWorkScanner() {
		setLockDuration(30);
		setLockDurationUnit(TimeUnit.SECONDS);
	}

	@Override
	public void executeService(ExecutionContext arg0) throws SystemException {

		if(this.currentDataCenter == null){
			synchronized(FileSyncWorkScanner.class){
	        	if(this.currentDataCenter == null) {
	            	this.currentDataCenter = fileSyncClient.getSourceDatacenter();
	            }
	        }
		}

		boolean targetUp=httpFileSender.pingTargetServer();
		boolean unlock=false;

		List<FileSynchronization> workToDo = new ArrayList<FileSynchronization>();

		try {
			boolean didit=lockManager.acquire("FILE_SYNC_WORK_SCAN_"+currentDataCenter.getDatacenterName(), 1000, TimeUnit.SECONDS);
			if (!didit){
				LOG.info("didn't get lock, oh well");

				return;
			}
			unlock=true;

			if (targetUp){
				try {
					List<FileSynchronization> fileSyncs = serviceClient.getWork(resultSize.intValue());
					serviceClient.markWorkToDo(fileSyncs);
					workToDo.addAll(fileSyncs);
				} catch (Exception e) {
					throw new SystemException(e);
				}
			} else {
				LOG.info("target NOT up!");
			}
		} catch (SystemException e){
			throw e;
		} finally {
			if (unlock){
				lockManager.release("FILE_SYNC_WORK_SCAN_"+ currentDataCenter.getDatacenterName());
			}
		}

		if (targetUp){
			if (CollectionUtils.isNotEmpty(workToDo)){
				serviceClient.multiPublishWork(workToDo);
			}
		}
	}

	@Override
	public String getName() {
		return "File Sync Work Scanner";
	}

	public void setResultSize(Integer resultSize) {
		this.resultSize = resultSize;
	}

	public void setServiceClient(ServiceClient serviceClient) {
		this.serviceClient = serviceClient;
	}

	public void setHttpFileSender(HttpFileSender httpFileSender) {
		this.httpFileSender = httpFileSender;
	}

	public void setLock(LockManager lockManager) {
		this.lockManager=lockManager;
	}

	public void setFileSyncClient(FileSyncClient fileSyncClient) {
		this.fileSyncClient = fileSyncClient;
	}
}
