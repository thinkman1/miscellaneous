package com.jpmc.cto.dart.extract.edw.xml.builder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import jsr166y.ForkJoinPool;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.jpmc.cto.dart.exception.DartException;
import com.jpmc.cto.dart.extract.xml.utils.EdwFileWrapper;
import com.jpmc.cto.dart.model.EdwBuildSendZipMsg;
import com.jpmc.cto.dart.model.ExtractHistory;
import com.jpmc.cto.framework.configuration.MachineInformation;
import com.jpmc.cto.framework.exception.ApplicationException;
import com.jpmc.dart.commons.util.FileSystemHelper;
import com.jpmc.dart.coreservices.filefinder.FileFinderClient;
import com.jpmc.dart.dao.jdbc.ExtractHistoryDao;
import com.jpmc.vpc.model.dart.type.ExtractType;

import extra166y.ParallelArray;

/**
 * Builder class of EDW zip file
 *
 * @author r502440
 *
 */
public class EdwZipFileBuilder implements InitializingBean {

	private static final Log LOG = LogFactory.getLog(EdwZipFileBuilder.class);

	private static final String GZIP_SUFFIX = ".gz";

	private final FastDateFormat formatter = FastDateFormat.getInstance("yyyyMMdd");

	private String zipDirBasePath;
	private FileSystemHelper fsHelper;
	private ExtractHistoryDao extractHistoryDao;
	private TransactionTemplate txTemplate;
	private FileFinderClient coreServiceFileClient;

	private ForkJoinPool threadPool;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		Validate.notNull(extractHistoryDao, "ExtractHistoryDao object cannot be null in EDW Zip Builder.");
		Validate.notNull(fsHelper, "FileSystemHelper object cannot be null in EDW Zip Builder.");

		zipDirBasePath = fsHelper.getFilesystemRoot();
		if (StringUtils.isEmpty(zipDirBasePath)) {
			LOG.error("EDW Zip base directory cannot be null.");
			throw new DartException("EDW Zip base directory cannot be null");
		}

		Validate.notNull(coreServiceFileClient, "HttpClientFileClient object must not be null.");
		
		int cores = Runtime.getRuntime().availableProcessors();
		cores = cores/4;
		if (cores < 1){
			cores=1;
		}
		
		threadPool = new ForkJoinPool(cores);
	}

	public void setThreadPool(ForkJoinPool threadPool) {
		this.threadPool = threadPool;
	}
	
	public int buildTarGz(File target,List<ExtractHistory> extractHistories, List<ExtractHistory> itemsToUpdate,
			UUID refId) throws Exception {
		File tarFile = new File(target.getAbsolutePath()+".tar");
		FileOutputStream fout = new FileOutputStream(tarFile);
		FastTarOutputStream tarOutput = new FastTarOutputStream(fout);
		String fileName = StringUtils.EMPTY;
		String fullFilePath = StringUtils.EMPTY;
		
		tarOutput.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
		tarOutput.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

		int recordCount=0;
		
		StopWatch sw = new StopWatch();
		sw.start();
		
		// sort by file name, move the items from the list to a parallel array
		ParallelArray<ExtractHistory> histories = ParallelArray.create(extractHistories.size(), ExtractHistory.class, threadPool);
		histories.asList().addAll(extractHistories);
		//extractHistories.clear();

		// sort & remove the duplicates by file name
		histories=histories.removeNulls().sort(new Comparator<ExtractHistory>() {
			@Override
			public int compare(ExtractHistory o1, ExtractHistory o2) {
				
				Validate.isTrue(o1!=null);
				Validate.isTrue(o2!=null);
				
				return o1.getFileName().compareTo(o2.getFileName());
			}
		});
		
		String previous = "";
		for (ExtractHistory history : histories) {
			fullFilePath = history.getFileName();
			if (!fullFilePath.equals(previous)) {
				previous = fullFilePath;
				File edwXmlFile = new File (fullFilePath);
				if (!edwXmlFile.exists()){
					edwXmlFile = coreServiceFileClient.getFile(fullFilePath);	
				}
				
				fileName = StringUtils.substringAfterLast(fullFilePath, File.separator);
				
				recordCount++;
				history.setRefId(refId);
				if (itemsToUpdate != null) {
					itemsToUpdate.add(history);
				}
				tarOutput.putArchiveEntryDirect(edwXmlFile, fileName);
				
				if ((recordCount%10000)==0){
					LOG.info("processed "+recordCount);
				}
			}
			
		}

		tarOutput.finish();
		IOUtils.closeQuietly(tarOutput);

		LOG.info("took "+sw.getTime()+" to build tar");
		
		// now compress.
		BufferedInputStream bin = new BufferedInputStream(new FileInputStream(tarFile));
		BufferedOutputStream bout = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(target)));
		IOUtils.copyLarge(bin, bout);
		
		IOUtils.closeQuietly(bin);	
		IOUtils.closeQuietly(bout);		
		
		tarFile.delete();
		
		sw.stop();
	
		LOG.info("took "+sw.getTime()+" to build tar & compress");
		
		return recordCount;
	}
	
	/**
	 *
	 * @param lastCycleDate
	 * @throws Exception
	 */
	public EdwFileWrapper buildZipFile(Date lastCycleDate) throws Exception {
		EdwFileWrapper wrapper = new EdwFileWrapper();

		String compressedFilePath = this.createTargetFilePath(lastCycleDate, GZIP_SUFFIX);
		StopWatch sw = new StopWatch();
		sw.start();
		List<ExtractHistory> extractHistories = this.getExtractHistoryItems(lastCycleDate);
		sw.stop();

		LOG.info("Pulled " + (extractHistories == null ? null : Integer.valueOf(extractHistories.size())) +
				" from the database for proc date " + lastCycleDate + ".");

		List<ExtractHistory> itemsToUpdate = new ArrayList<ExtractHistory>(extractHistories == null ? 100 : extractHistories.size());

		UUID refId = UUID.randomUUID();
		int recordCount = 0;

		try {
			File compressedFile = new File(compressedFilePath);
			Date now = new Date();
			sw = new StopWatch();
			sw.start();
			LOG.info("start building tar file");
			recordCount = buildTarGz(compressedFile, extractHistories, itemsToUpdate, refId);
			LOG.info("tar.gz file is "+compressedFilePath);
			extractHistoryDao.updateRefId(itemsToUpdate);
			sw.stop();

			// save the edw zip extract history
			if (CollectionUtils.isNotEmpty(extractHistories)) {
				
				ExtractHistory zipExtractHistory = this.assembleExtractHistory(compressedFile, lastCycleDate, now, sw.getTime(), extractHistories.size(), refId);
				extractHistoryDao.save(zipExtractHistory);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Extract history of ZIP file " + refId + " has been added to Database");
				}
			}

			wrapper.setGzip(compressedFile);
			wrapper.setProcessDate(lastCycleDate);
			wrapper.setRecordCount(recordCount);

			long time = sw.getTime();
			if (time == 0) {
				time = 1;
			}
			
			LOG.info("Processed " + recordCount + " files in " + sw.toString()+ ".  Files/Second:  " + Double.valueOf(((recordCount /time) * 60000)));

			return wrapper;
		} catch (FileNotFoundException fnfe) {
			String msg = String.format("%s does not exist or something bad happened!", compressedFilePath);
			LOG.error(msg, fnfe);
			throw new ApplicationException(msg, fnfe);
		} catch (IOException ioe) {
			LOG.error("Got IO exception", ioe);
			throw new ApplicationException("Got IO exception within EdwZipFileBuilder", ioe);
		} finally {
			//IOUtils.closeQuietly(tarOutput);
			//IOUtils.closeQuietly(fout);
		}
	}

	/**
	 * Work on re-work EDW Zip
	 *
	 * @param lastCycleDate
	 * @param message
	 * @return
	 * @throws Exception
	 */
	public EdwFileWrapper buildZipFile(EdwBuildSendZipMsg message) throws Exception {
		EdwFileWrapper wrapper = new EdwFileWrapper();

		String compressedFilePath = this.createTargetFilePath(message.getPublishDate(), GZIP_SUFFIX);
		List<ExtractHistory> extractHistories = this.getExtractHistoryItems(message.getPublishDate());
		UUID refId = message.getOriginalZipId();

		// sort them by file name so we can remove duplicates
		
		
		
		File compressedFile = null;
		int recordCount = 0;

		try {
			compressedFile = new File(compressedFilePath);
			Date now = new Date();
			StopWatch sw = new StopWatch();
			sw.start();
		
			LOG.info("start building tar file...");

			recordCount = buildTarGz(compressedFile, extractHistories, null, refId);
		
			sw.stop();
			
			// save or update the edw zip extract history
			if (CollectionUtils.isNotEmpty(extractHistories)) {
				ExtractHistory originalHistory = extractHistoryDao.findZipExtractHistoryByCalDate(message.getPublishDate());
				
				ExtractHistory newZipExtractHistoryItem = this.assembleExtractHistory(compressedFile, message.getPublishDate(),
						now, sw.getTime(), extractHistories.size(), refId);
				List<ExtractHistory> extractHistoriesXml = this.getExtractHistoryItems(message.getPublishDate());

				if (originalHistory == null) {
					this.saveNewExtractHistoryChanges(newZipExtractHistoryItem, extractHistoriesXml, refId,
							message.getPublishDate());
					if (LOG.isDebugEnabled()) {
						LOG.debug("Extract history of ZIP file " + refId + " has been saved.");
					}
				} else {
					this.updateNewExtractHistoryChanges(newZipExtractHistoryItem, extractHistoriesXml, refId,
							message.getPublishDate());
					if (LOG.isDebugEnabled()) {
						LOG.debug("Extract history of ZIP file " + refId + " has been updated.");
					}
				}
			}

			wrapper.setGzip(compressedFile);
			wrapper.setProcessDate(message.getPublishDate());
			wrapper.setRecordCount(recordCount);

			return wrapper;
		} catch (FileNotFoundException fnfe) {
			String msg = String.format("%s does not exist or something bad happened!", compressedFilePath);
			LOG.error(msg, fnfe);
			throw new ApplicationException(msg, fnfe);
		} catch (IOException ioe) {
			LOG.error("Got IO exception", ioe);
			throw new ApplicationException("Got IO exception within EdwZipFileBuilder", ioe);
		}
	}

	private void saveNewExtractHistoryChanges(final ExtractHistory newZipExtractHistoryItem,
			final List<ExtractHistory> extractHistoriesXml, final UUID refId, final Date date) {

		txTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				extractHistoryDao.save(newZipExtractHistoryItem);
				for (ExtractHistory eh : extractHistoriesXml) {
					extractHistoryDao.updateExtractHistoryRefId(refId, eh.getObjectId(), date);
				}
			}
		});
	}

	private void updateNewExtractHistoryChanges(final ExtractHistory newZipExtractHistoryItem,
			final List<ExtractHistory> extractHistoriesXml, final UUID refId, final Date date) {

		txTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				extractHistoryDao.updateZipExtractHistory(newZipExtractHistoryItem);
				for (ExtractHistory eh : extractHistoriesXml) {
					extractHistoryDao.updateExtractHistoryRefId(refId, eh.getObjectId(), date);
				}
			}
		});
	}

	/**
	 * @param calendarDate
	 * @return
	 */
	private List<ExtractHistory> getExtractHistoryItems(Date calendarDate) {
		return extractHistoryDao.findExtractHistoryByCalendarDate(calendarDate);
	}
	
	/**
	 * @param date
	 * @param suffix
	 * @return
	 */
	private String createTargetFilePath(Date date, String suffix) {
		String dateDir = formatter.format(date);
		StringBuilder dirPath = new StringBuilder(zipDirBasePath);
		dirPath.append(File.separator);
		dirPath.append("extract");
		dirPath.append(File.separator);
		dirPath.append("edw");
		dirPath.append(File.separator);
		dirPath.append(dateDir);

		File dir = new File(dirPath.toString());
		if (!dir.exists()) {
			dir.mkdirs();
		}

		return dirPath.toString() + File.separator + "dart." + dateDir + ".tar" + GZIP_SUFFIX;
	}

	private ExtractHistory assembleExtractHistory(File edwXmlFile, Date lastCycleDate, Date now, long time, int count,
			UUID refId) {
		ExtractHistory history = new ExtractHistory();
		history.setId(refId);
		history.setProcessDate(lastCycleDate);
		history.setCalendarDate(lastCycleDate);
		history.setExtractType(ExtractType.EDW_ZIP);
		history.setStartTime(now);
		history.setEndTime(DateUtils.addMilliseconds(now, (int) time));
		history.setFileName(edwXmlFile != null ? edwXmlFile.getAbsolutePath() : StringUtils.EMPTY);
		history.setItemCount(count);
		history.setMachineName(MachineInformation.getMachineName() + ":" + MachineInformation.getJmxPort());
		// leave ref if null
		// leave object id null

		return history;
	}

	/**
	 * @param zipDirBasePath
	 *            the zipDirBasePath to set
	 */
	public void setZipDirBasePath(String zipDirBasePath) {
		this.zipDirBasePath = zipDirBasePath;
	}

	public void setFsHelper(FileSystemHelper fsHelper) {
		this.fsHelper = fsHelper;
	}

	/**
	 * @param extractHistoryDao
	 *            the extractHistoryDao to set
	 */
	public void setExtractHistoryDao(ExtractHistoryDao extractHistoryDao) {
		this.extractHistoryDao = extractHistoryDao;
	}

	/**
	 * @param txTemplate
	 *            the txTemplate to set
	 */
	public void setTxTemplate(TransactionTemplate txTemplate) {
		this.txTemplate = txTemplate;
	}

	/**
	 * @param coreServiceFileClient
	 *            the coreServiceFileClient to set
	 */
	public void setCoreServiceFileClient(FileFinderClient coreServiceFileClient) {
		this.coreServiceFileClient = coreServiceFileClient;
	}
}
