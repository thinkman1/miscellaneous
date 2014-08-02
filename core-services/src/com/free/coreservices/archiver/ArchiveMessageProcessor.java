package com.free.coreservices.archiver;

import java.awt.TrayIcon.MessageType;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.omg.CORBA.portable.ApplicationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;


/**
 * acts on a file archive message
 * @author e001668
 *
 */
public class ArchiveMessageProcessor implements MessageProcessor<FileArchiveActionEvent>, InitializingBean, DisposableBean{
	private static final Log LOG = LogFactory.getLog(ArchiveMessageProcessor.class);

	private ArchiveFileBuilder archiveBuilder;
	private LockManager locky;
	private boolean archive=true;

	private int numberOfCrawlerThreads;

	private DirectoryCrawler crawler;

	private ForkJoinPool deleteThreadPool ;

	private class DeleteOp implements Ops.Procedure<File>{
		@Override
			public void op(File a) {
				try {
					ArchiveUtils.deleteWithRetrySingle(a);
				} catch (Exception e){
					LOG.warn("couldn't delete "+a.getAbsolutePath());
				}
			}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		numberOfCrawlerThreads=Runtime.getRuntime().availableProcessors()/10;
		if (numberOfCrawlerThreads==0){
			numberOfCrawlerThreads=2;
		}

		crawler=new DirectoryCrawler(numberOfCrawlerThreads);

		LOG.info("use "+numberOfCrawlerThreads+" threads to crawl directories ");

		deleteThreadPool= new ForkJoinPool(numberOfCrawlerThreads);

	}

	@Override
	public void destroy() throws Exception {
		deleteThreadPool.shutdown();
		crawler.shutdown();
	}

	@Override
	public String getName() {
		return "ArchiveMessageProcessor";
	}

	private static final MessageType types[] = new MessageType[]{
		FileArchiveActionEvent.TYPE
	};

	@Override
	public MessageType[] getAcceptedTypes() {
		return types;
	}

	@Override
	public void processMessage(FileArchiveActionEvent arg0)
			throws ApplicationException {

		LOG.info("working on directory "+arg0.getDirectory());

		if (!archive){
			LOG.info("ignore archive event "+ToStringBuilder.reflectionToString(arg0));
			return;
		}

		String lockName=arg0.getDirectory()+"-"+"_LOCK";

		try {
			// we can't have two processors looking at the same directory.  it'd be madness.
			if (!locky.acquire(lockName, 60, TimeUnit.SECONDS)){
				LOG.warn("Somebody already looking at this directory? "+arg0.getDirectory());
				return;
			}
		} catch (LockAcquisitionException e){
			LOG.warn("Somebody already looking at this directory? "+arg0.getDirectory(),e);
			return;
		}

		FileSystem fs = null;
		try {
			Configuration conf = new Configuration();
			fs=FileSystem.get(conf);

			ArchiveFilter filter = null;
			try {
				if (!StringUtils.isEmpty(arg0.getCustomArchiveFilter())){
					filter=(ArchiveFilter) Class.forName(arg0.getCustomArchiveFilter()).newInstance();
					filter.setConfig(arg0);
				}
			} catch (Exception e){
				throw new ApplicationException("can't get item filter",e);
			}

			File baseDir = new File(arg0.getDirectory());

			if (!baseDir.exists()){
				LOG.info("whoa, direectory not there "+baseDir.getAbsolutePath());
				return;
			}

			// check if the directory is too old
			if (arg0.isAgeByName()){
				boolean delete = false;

				Date date = null;

				LOG.info("test date "+baseDir.getName());

				try {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
					sdf.setLenient(false);
					date=sdf.parse(baseDir.getName());
				} catch (Exception e){
					LOG.error("basedir name is "+baseDir+" can not be converted to date when config says it should, I will ignore it",e);
					return ;
				}

				delete= directoryTooOld(arg0, baseDir,date);

				LOG.info("is "+baseDir.getAbsolutePath()+" too old? "+delete);

				if (delete){
					try {
						ArchiveUtils.deleteWithRetry(baseDir);
					} catch (IOException e) {
						LOG.error("Exception deleteing dir ",e);
						throw new RuntimeException("basedir name is "+baseDir+" can not be deleted "+e.getMessage());
					}
					return;
				}
			}

			// not too old, archive files (if needed)
			List<File> archivedFiles=new ArrayList<File>();
			try {
				// only bother to archive if the time to live > the number of archive hours.
				// it's lame to search for files to archive if we won't archive them.
				if (arg0.getLiveDaysInMills()>(arg0.getIdleHours()*DateUtils.MILLIS_PER_HOUR)){
					archivedFiles=archiveBuilder.archiveFiles(baseDir,
							arg0.getAchiveFilePrefix(),
							(arg0.getIdleHours()*DateUtils.MILLIS_PER_HOUR),
							arg0.isUseDirNameInArchive(),fs,crawler);
				} else {
					LOG.info("Skip archive of "+arg0.getDirectory()+" since archive time <= live time");
				}
			} catch (Exception e){
				LOG.error("Exception archiving directory ",e);
				throw new RuntimeException("basedir name is "+baseDir+" can not be archived "+e.getMessage(),e);
			}

			LOG.info("Delete files that were archived: "+archivedFiles.size());

			// now delete the archived files (in parallel!)
			if (archivedFiles!=null){
				File files[]=new File[archivedFiles.size()];
				archivedFiles.toArray(files);
				ParallelArray<File> nukeFiles = ParallelArray.createFromCopy(files, deleteThreadPool);

				nukeFiles.apply(new DeleteOp());
			}

			LOG.info("Delete files that were archived: "+archivedFiles.size()+" done!");

			// apply custom filter, if there is one
			if (filter!=null){
				LOG.info("Apply filter to archived data");
				ArchiveUtils.applyArchiveFilter(baseDir, arg0.getAchiveFilePrefix(), filter,fs);
			}
		} catch (Exception e){
			LOG.error("cught exception ",e);
			throw new ApplicationException("caught exception processing archive for dir "+arg0.getDirectory(),e);
		} finally {
			locky.release(lockName);
			IOUtils.closeQuietly(fs);
			LOG.info("file "+arg0.getDirectory()+" is done!");
		}
	}

	public boolean directoryTooOld(FileArchiveActionEvent action, File baseDir, Date age) {
		long maxAge = action.getLiveDaysInMills();

		// if there is a filter, apply it instead
		if (!StringUtils.isEmpty(action.getCustomArchiveFilter())){
			LOG.info(baseDir.getAbsolutePath()+" has archive filter, ignore");

			return false;
		}

		if (action.isAgeByName()){
			if (age!=null){

				LOG.info(baseDir.getAbsolutePath()+" age by name.  diff  is "+(System.currentTimeMillis()-age.getTime())+" max age is "+maxAge);

				return ((System.currentTimeMillis()-age.getTime()) > maxAge);
			}
			return false;
		}
		// use last modified time
		LOG.info(baseDir.getAbsolutePath()+" age by timestamp.  diff  is "+(System.currentTimeMillis()-baseDir.lastModified())+" max age is "+maxAge);

		return ((System.currentTimeMillis()-baseDir.lastModified()) > maxAge);
	}

	public void setArchiveBuilder(ArchiveFileBuilder archiveBuilder) {
		this.archiveBuilder = archiveBuilder;
	}

	public void setLocky(LockManager locky) {
		this.locky = locky;
	}

	public void setArchive(boolean archive) {
		this.archive = archive;
	}

}
