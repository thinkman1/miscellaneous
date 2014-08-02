package com.jpmc.dart.filesync.http;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jpmc.cto.dart.model.filesync.ApplicationDatacenter;
import com.jpmc.cto.dart.model.filesync.FileSyncMsg;
import com.jpmc.cto.dart.model.filesync.FileSynchronization;
import com.jpmc.cto.dart.test.db.InitializeDataSource;
import com.jpmc.dart.commons.util.DartFileUtils;
import com.jpmc.dart.commons.util.FileSystemHelper;
import com.jpmc.dart.dao.jdbc.FileSynchronizationDao;
import com.jpmc.dart.filesync.mock.HttpFileSenderCallFailed;
import com.jpmc.dart.filesync.mock.HttpFileSenderException;
import com.jpmc.dart.filesync.mock.HttpFileSenderOk;
import com.jpmc.vpc.model.dart.type.FileSyncStatusType;
import com.jpmc.vpc.model.exception.ExceptionType;
import com.jpmc.vpc.model.exception.VpcException;

public class FileSynchronizationWorkerTest extends TestCase {
	private static FileSystemXmlApplicationContext ac;
	private FileSynchronizationDao fileSynchronizationDao;
	private static JdbcTemplate jdbcTemplate;
	private FileSynchronizationWorker fileSyncWorker;
	private FileSynchronization fileSync;
	private FileSystemHelper helper;
	private String sourceFileName;
	private FileSyncMsg fileSyncMsg;
	private File srcFile;
	private static final Log LOG = LogFactory.getLog(FileSynchronizationWorkerTest.class);

	static {
	}


	@Override
	protected void setUp() throws Exception {
		jdbcTemplate = InitializeDataSource.getInitializedDartDataSource();

		ac = new FileSystemXmlApplicationContext(
				new String[] { 		"src/test/resources/testContext.xml"});
		ac.registerShutdownHook();


		fileSynchronizationDao = (FileSynchronizationDao) ac
				.getBean("FileSynchronizationDao");
		jdbcTemplate = (JdbcTemplate) ac.getBean("jdbcTemplate");
		fileSyncWorker = (FileSynchronizationWorker) ac
				.getBean("FileSyncWorker");
		fileSync = getStubbedFileSynchronization();
		fileSynchronizationDao.setJdbcTemplate(jdbcTemplate);
		helper = (FileSystemHelper) ac.getBean("fileSystemHelper");

		sourceFileName = UUID.randomUUID()+"test.xml";
		srcFile=new File(helper.getFilesystemRoot() + File.separator + sourceFileName);
		srcFile.getParentFile().mkdirs();
		DartFileUtils.copyFile(new File("src/main/resources/applicationContext.xml") ,srcFile, 3, 40L);

		fileSync.setSourceFilename(sourceFileName);
		fileSync.setDestFilename(sourceFileName);
		fileSynchronizationDao.save(fileSync);

		fileSyncMsg = new FileSyncMsg();
		fileSyncMsg.setId(fileSync.getId());
		fileSyncMsg.setInsertDate(fileSync.getInsertDate());
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		jdbcTemplate.update("DELETE FROM SERVER_DATACENTER");
	}

	public void testFileSendAbsPath() throws Exception {
		fileSync = getStubbedFileSynchronization();
		fileSync.setSourceFilename(helper.getFilesystemRoot() + File.separator + sourceFileName);
		fileSync.setDestFilename(helper.getFilesystemRoot() + File.separator + sourceFileName);
		fileSynchronizationDao.save(fileSync);
		fileSyncMsg = new FileSyncMsg();
		fileSyncMsg.setId(fileSync.getId());
		fileSyncMsg.setInsertDate(fileSync.getInsertDate());


		fileSyncWorker.setHttpFileSender(new HttpFileSenderOk());
		fileSyncWorker.processMessage(fileSyncMsg);


		TimeUnit.SECONDS.sleep(60);

		FileSynchronization fileSyncFromDb = fileSynchronizationDao
				.findFileSynchronizationByIdAndInsertDate(fileSync.getId(),
						fileSync.getInsertDate());

		LOG.info("source is "+sourceFileName+" DB OBJ is "+
				ReflectionToStringBuilder.reflectionToString(fileSyncFromDb,ToStringStyle.MULTI_LINE_STYLE));


		//assertEquals(sourceFileName, fileSyncFromDb.getSourceFilename());
		//assertEquals(sourceFileName, fileSyncFromDb.getDestFilename());
		assertEquals(FileSyncStatusType.COMPLETE,
				fileSyncFromDb.getFileSyncStatus());
		assertEquals(calculateCheckSum(srcFile), fileSyncFromDb.getFileHash());
		assertEquals(fileSync.getRetryCount(), fileSyncFromDb.getRetryCount());
		assertNotNull(fileSyncFromDb.getCopyStartTime());
		assertNotNull(fileSyncFromDb.getCopyFinishTime());
		assertNotNull(fileSyncFromDb.getDestServer());
		assertEquals(fileSync.getDestFilename(),
				helper.getFilesystemRoot()+File.separator+fileSyncFromDb.getDestFilename());
		assertEquals("Copy Complete", fileSyncFromDb.getLastMessage());
	}

	public void testFileSendCallFailed() throws Exception {
		fileSyncWorker.setHttpFileSender(new HttpFileSenderCallFailed());
		fileSyncWorker.processMessage(fileSyncMsg);

		FileSynchronization fileSyncFromDb = fileSynchronizationDao
				.findFileSynchronizationByIdAndInsertDate(fileSync.getId(),
						fileSync.getInsertDate());

		assertEquals(fileSync.getRetryCount() + 1,
				fileSyncFromDb.getRetryCount());
		assertNull(fileSyncFromDb.getEnqueuedTime());
		assertEquals(FileSyncStatusType.READY,
				fileSyncFromDb.getFileSyncStatus());
		assertNull(fileSyncFromDb.getCopyStartTime());
		assertNull(fileSyncFromDb.getCopyFinishTime());
		assertNotNull(fileSyncFromDb.getDestServer());
		assertNotNull(fileSyncFromDb.getDestFilename());
		assertTrue(fileSyncFromDb.getLastMessage().startsWith("Call Failed:"));

	}


	public void testFileSendException() throws Exception {
		fileSyncWorker.setHttpFileSender(new HttpFileSenderException());
		fileSyncWorker.processMessage(fileSyncMsg);

		FileSynchronization fileSyncFromDb = fileSynchronizationDao
				.findFileSynchronizationByIdAndInsertDate(fileSync.getId(),
						fileSync.getInsertDate());

		assertEquals(fileSync.getRetryCount() + 1,
				fileSyncFromDb.getRetryCount());
		assertNull(fileSyncFromDb.getEnqueuedTime());
		assertEquals(FileSyncStatusType.READY,
				fileSyncFromDb.getFileSyncStatus());
		assertNull(fileSyncFromDb.getCopyStartTime());
		assertNull(fileSyncFromDb.getCopyFinishTime());
		assertEquals(fileSync.getDestServer(), fileSyncFromDb.getDestServer());
		assertEquals(fileSync.getDestFilename(), fileSyncFromDb.getDestFilename());
		assertTrue(fileSyncFromDb.getLastMessage().startsWith("Exception:"));

	}

	public void testFileSendSuccess() throws Exception {
		fileSyncWorker.setHttpFileSender(new HttpFileSenderOk());
		fileSyncWorker.processMessage(fileSyncMsg);


		TimeUnit.SECONDS.sleep(20);

		FileSynchronization fileSyncFromDb = fileSynchronizationDao
				.findFileSynchronizationByIdAndInsertDate(fileSync.getId(),
						fileSync.getInsertDate());

		assertEquals(FileSyncStatusType.COMPLETE,
				fileSyncFromDb.getFileSyncStatus());
		assertEquals(calculateCheckSum(srcFile), fileSyncFromDb.getFileHash());
		assertEquals(fileSync.getRetryCount(), fileSyncFromDb.getRetryCount());
		assertNotNull(fileSyncFromDb.getCopyStartTime());
		assertNotNull(fileSyncFromDb.getCopyFinishTime());
		assertNotNull(fileSyncFromDb.getDestServer());
		assertEquals(fileSync.getDestFilename(),
				fileSyncFromDb.getDestFilename());
		assertEquals("Copy Complete", fileSyncFromDb.getLastMessage());
	}



	private String calculateCheckSum(File file) throws VpcException {
		//File sourceFileObj = new File(helper.getProdNasPath() + File.separator + sourceFileName);
		long checkSum = 0;
		try {
			checkSum = FileUtils.checksumCRC32(file);
		} catch (IOException e) {
			throw new VpcException(ExceptionType.UNKNOWN, e.getMessage(), e);
		}
		return String.valueOf(checkSum);
	}
	
	/**
	 * This method is used to stub data into FileSynchronization object.
	 *
	 * @return stubbed FileSynchronization object.
	 */
	private FileSynchronization getStubbedFileSynchronization() {

		FileSynchronization fileSynchronization = new FileSynchronization();
		ApplicationDatacenter appDc1 = new ApplicationDatacenter(1,"DC 1");
		ApplicationDatacenter appDc2 = new ApplicationDatacenter(2,"DC 2");

		fileSynchronization.setId(UUID.randomUUID());
		fileSynchronization.setInsertDate(DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH));
		fileSynchronization.setRetryCount(0);
		fileSynchronization.setFileSyncStatus(FileSyncStatusType.QUEUED);
		fileSynchronization.setEnqueuedTime(new Date());
		fileSynchronization.setCopyStartTime(null);
		fileSynchronization.setCopyFinishTime(null);
		fileSynchronization.setSourceDatacenter(appDc1);
		fileSynchronization.setSourceFilename(null);
		fileSynchronization.setDestDatacenter(appDc2);
		fileSynchronization.setFileHash(null);
		fileSynchronization.setDestServer(null);
		fileSynchronization.setDestFilename("destFileName");
		fileSynchronization.setLastMessage(null);

		return fileSynchronization;
	}
}
