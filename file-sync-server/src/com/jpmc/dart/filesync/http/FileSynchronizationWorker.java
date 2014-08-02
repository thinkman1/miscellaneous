package com.jpmc.dart.filesync.http;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.support.TransactionTemplate;

import com.jpmc.cto.dart.model.filesync.FileSyncMsg;
import com.jpmc.cto.dart.model.filesync.FileSynchronization;
import com.jpmc.cto.framework.MessageProcessor;
import com.jpmc.cto.framework.configuration.ApplicationInformation;
import com.jpmc.cto.framework.exception.ApplicationException;
import com.jpmc.dart.commons.util.FileNameUtils;
import com.jpmc.dart.commons.util.StringBuilderUtils;
import com.jpmc.dart.coreservices.filefinder.FileFinderClient;
import com.jpmc.dart.dao.jdbc.FileSynchronizationDao;
import com.jpmc.dart.filesync.constants.FileSyncConstants;
import com.jpmc.vpc.model.dart.type.FileSyncStatusType;
import com.jpmc.vpc.model.type.ObjectType;

public class FileSynchronizationWorker implements InitializingBean, MessageProcessor<FileSyncMsg>,ApplicationContextAware {

	private static final ObjectType OBJECT_CONSTANT_TYPE[] = new ObjectType[] { ObjectType.DART_FILE_SYNC_MSG };

	private FileSynchronizationDao fileSynchronizationDao;
	private int fileReadTimeBetweenRetry = 5000;
	private int fileReadRetryCount = 10;
	private HttpFileSender httpFileSender;
	private String sourcePrefix;

	boolean useMmio=true;
	private FileNameUtils fileNameUtils;
	private TransactionTemplate tx;

	private String name=ApplicationInformation.getJvmName();

	private FileFinderClient fileFinderClient;

	private int byteBufferReadSize=100000;

	private ThreadLocal<CRC32> localCrcEr= new ThreadLocal<CRC32>();

	private ThreadLocal<ByteBuffer> reusableChecksumBuffer = new ThreadLocal<ByteBuffer>();

	private ThreadLocal<StringBuilder> buffer=new ThreadLocal<StringBuilder>();

	private ApplicationContext context;

	private static final Log LOG = LogFactory
			.getLog(FileSynchronizationWorker.class);

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context=applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
	}

	@Override
	public String getName() {
		return "FileSynchronizationWorker";
	}

	@Override
	public ObjectType[] getAcceptedTypes() {
		return OBJECT_CONSTANT_TYPE;
	}

	@Override
	public void processMessage(FileSyncMsg fileSyncMsg) throws ApplicationException {
		final FileSynchronization fileSync = fileSynchronizationDao
				.findFileSynchronizationByIdAndInsertDate(fileSyncMsg.getId(),
						fileSyncMsg.getInsertDate());

		StopWatch sw = new StopWatch();
		sw.start();
		File file =null;
		long length=0;

		if (buffer.get()==null){
			buffer.set(new StringBuilder());
		}

		Map<String, String> requestHeaders = new HashMap<String, String>();

		int responseCode = 0;
		Map<String, String> response = null;
		Date copyStartTime = new Date();

		RandomAccessFile randFile = null;

		try {
			String relativeSourceFileName = fileSync.getSourceFilename();
			String absSourceFileName = null;

			StringBuilder builder = buffer.get();
			builder.setLength(0);

			builder.append(sourcePrefix);
			if (StringBuilderUtils.startsWith(builder, "//")){
				builder.deleteCharAt(0);
			}

			String prefix=builder.toString();

			builder.setLength(0);

			relativeSourceFileName = fileNameUtils.getRelativeFileNameFromPrefix(relativeSourceFileName,File.separator,builder);

			builder.setLength(0);
			builder.append(prefix).append(File.separator).append(relativeSourceFileName);

			//absSourceFileName = prefix + File.separator + relativeSourceFileName;
			absSourceFileName=builder.toString();

			file=fileFinderClient.getFile(absSourceFileName);

			if (file==null){
				throw new FileNotFoundException("file "+absSourceFileName+" can not be found! (even in the archives)");
			}

			length=file.length();

			randFile=new RandomAccessFile(file, "r");


			fileSync.setFileHash(calculateCheckSum(randFile));
			fileSync.setSourceFilename(relativeSourceFileName);
			fileSync.setSourceServer(name);
			response = new HashMap<String, String>();

			if (LOG.isTraceEnabled()){
				LOG.trace("try sending file (relative) "+relativeSourceFileName+" full "+absSourceFileName);
			}

			requestHeaders=getHeaderData(fileSync,relativeSourceFileName,file);

			responseCode = httpFileSender.sendFiles(requestHeaders, response,randFile,useMmio,fileSync.getId());
		} catch (Exception e) {

			int rc =fileSynchronizationDao.updateFileSyncInfoByIdAfterFail(
					fileSync.getDestServer(), fileSync.getDestFilename(),
					StringUtils.left("Exception: " + e.getMessage(),999), fileSync.getId(),fileSync.getInsertDate());

			LOG.error("Call Exception update count "+rc, e);

			return;
		} finally {
			IOUtils.closeQuietly(randFile);
		}

		String destServerName = response
				.get(FileSyncConstants.HttpResponseHeaderNames.LOCAL_SERVER_NAME);
		String fileName = response
				.get(FileSyncConstants.HttpResponseHeaderNames.LOCAL_FILE_NAME);



		if (responseCode == 200) {
			fileSync.setCopyStartTime(copyStartTime);
			fileSync.setCopyFinishTime(new Date());
			fileSync.setDestServer(destServerName);
			fileSync.setDestFilename(fileName);
			fileSync.setFileSyncStatus(FileSyncStatusType.COMPLETE);
			fileSync.setLastMessage("Copy Complete");

			if (LOG.isDebugEnabled()){
				LOG.debug("update "+fileSync.getId()+" insert date "+fileSync.getInsertDate());
			}

			//  publish as a
			context.publishEvent(new UpdateFinalStatusEvent(fileSync));
		} else {
			LOG.info("Failed: "
					+ response.get(HttpFileSender.RESPONSE_TEXT_KEY) + " "
					+ responseCode);

			fileSynchronizationDao.updateFileSyncInfoByIdAfterFail(
					destServerName,
					fileName,
					"Call Failed: "
							+ StringUtils.left(response.get(HttpFileSender.RESPONSE_TEXT_KEY+" " + responseCode),999)
							, fileSync.getId(),fileSync.getInsertDate());
			// throw an issue if we've republished this back in and it still doesn't work.
			// that way we don't have to update the DB if the repub fails.
			Validate.isTrue(fileSync.getRetryCount() < fileReadRetryCount,
					"Item has been retried too many times.  Actual retry count:  " + String.valueOf(fileSync.getRetryCount()) +
					"  Configured value:  " + String.valueOf(fileReadRetryCount));
		}

		sw.stop();

		LOG.info("took "+sw.getTime()+" to transfer ("+fileSync.getId()+ ")  "+fileSync.getSourceFilename()+" size "+length);
	}
	
	/**
	 * @return the fileNameUtils
	 */
	public FileNameUtils getFileNameUtils() {
		return fileNameUtils;
	}

	/**
	 * @param fileNameUtils the fileNameUtils to set
	 */
	public void setFileNameUtils(FileNameUtils fileNameUtils) {
		this.fileNameUtils = fileNameUtils;
	}

	private Map<String, String> getHeaderData(final FileSynchronization fileSync, String relativeFileName,File file)  throws Exception{
		Map<String, String> headerData = new HashMap<String, String>();

		headerData.put(
				FileSyncConstants.HttpReqestHeaderNames.X_CTO_DART_DATACENTER,
				fileSync.getDestDatacenter().getDatacenterName());
		headerData.put(
				FileSyncConstants.HttpReqestHeaderNames.X_CTO_DART_FILENAME,
				relativeFileName);
		headerData.put(FileSyncConstants.HttpReqestHeaderNames.CHECK_SUM,
				fileSync.getFileHash());
		headerData.put(FileSyncConstants.HttpReqestHeaderNames.FILE_SIZE,
				String.valueOf(file.length()));

		if (LOG.isTraceEnabled()){
			LOG.trace(" file size: "+headerData.get(FileSyncConstants.HttpReqestHeaderNames.FILE_SIZE));
		}

		return headerData;
	}

	private String calculateCheckSum(RandomAccessFile file) throws Exception {
		StopWatch sw = new StopWatch();
		sw.start();
		BufferedInputStream fin = null;

		try {
			if (localCrcEr.get()==null){
				localCrcEr.set(new CRC32());
			} else {
				localCrcEr.get().reset();
			}

			CRC32 chk=localCrcEr.get();

			// the goal here is to limit the amount of GC we do by re-using as many objects as possible.
			if (reusableChecksumBuffer.get()==null){
				// allocate as a heap byte buffer so the byte[] underneath will get reused
				reusableChecksumBuffer.set(ByteBuffer.allocate(byteBufferReadSize));
			}

			ByteBuffer threadLocalBb=reusableChecksumBuffer.get();
			threadLocalBb.clear();

			int read=0;
			while (read >-1){
				threadLocalBb.clear();
				read=file.getChannel().read(threadLocalBb);
				if (read>-1){
					// threadLocalBb.array() will give you the data without having to copy it
					chk.update(threadLocalBb.array(),0,read);
				}
			}
			return String.valueOf(chk.getValue());
		} finally {
			sw.stop();
			IOUtils.closeQuietly(fin);
		}
	}

	/**
	 * @return the fileSynchronizationDao
	 */
	public FileSynchronizationDao getFileSynchronizationDao() {
		return fileSynchronizationDao;
	}

	/**
	 * @param fileSynchronizationDao
	 *            the fileSynchronizationDao to set
	 */
	public void setFileSynchronizationDao(
			FileSynchronizationDao fileSynchronizationDao) {
		this.fileSynchronizationDao = fileSynchronizationDao;
	}

	/**
	 * @return the fileReadTimeBetweenRetry
	 */
	public int getFileReadTimeBetweenRetry() {
		return fileReadTimeBetweenRetry;
	}

	/**
	 * @param fileReadTimeBetweenRetry
	 *            the fileReadTimeBetweenRetry to set
	 */
	public void setFileReadTimeBetweenRetry(int fileReadTimeBetweenRetry) {
		this.fileReadTimeBetweenRetry = fileReadTimeBetweenRetry;
	}

	/**
	 * @return the fileReadRetryCount
	 */
	public int getFileReadRetryCount() {
		return fileReadRetryCount;
	}

	/**
	 * @param fileReadRetryCount
	 *            the fileReadRetryCount to set
	 */
	public void setFileReadRetryCount(int fileReadRetryCount) {
		this.fileReadRetryCount = fileReadRetryCount;
	}

	/**
	 * @return the httpFileSender
	 */
	public HttpFileSender getHttpFileSender() {
		return httpFileSender;
	}

	/**
	 * @param httpFileSender
	 *            the httpFileSender to set
	 */
	public void setHttpFileSender(HttpFileSender httpFileSender) {
		this.httpFileSender = httpFileSender;
	}

	public void setUseMmio(boolean useMmio) {
		this.useMmio = useMmio;
	}

	public void setTx(TransactionTemplate tx) {
		this.tx = tx;
	}

	public void setFileFinderClient(FileFinderClient fileFinderClient) {
		this.fileFinderClient = fileFinderClient;
	}

	public void setSourcePrefix(String sourcePrefix) {
		this.sourcePrefix = sourcePrefix;
	}
}
