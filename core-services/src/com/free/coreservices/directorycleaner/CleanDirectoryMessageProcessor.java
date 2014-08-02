package com.free.coreservices.directorycleaner;

import java.io.File;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jpmc.cto.framework.MessageProcessor;
import com.jpmc.cto.framework.MessageType;
import com.jpmc.cto.framework.exception.ApplicationException;
import com.jpmc.vpc.model.dart.event.DirectoryCleanerEvent;

public class CleanDirectoryMessageProcessor implements
		MessageProcessor<DirectoryCleanerEvent> {
	private static final Log LOG = LogFactory
			.getLog(CleanDatedDirectoryMessageProcessor.class);

	private static MessageType TYPE= new DirectoryCleanerEvent().getMessageType();


	@Override
	public MessageType[] getAcceptedTypes() {
		return new MessageType[] { TYPE};
	}

	@Override
	public String getName() {
		return "CleanDirectoryMessageProcessor";
	}

	public void processMessage(DirectoryCleanerEvent event)
			throws ApplicationException {

		File directory = new File(event.getDirectory());

		if (!directory.exists()) {
			LOG.info("hey, directory " + directory.getAbsolutePath()
					+ " does not exist");
			return;
		}

		LOG.info("Clean directory " + directory.getAbsolutePath());

		for (File f : directory.listFiles()) {
			boolean exclude = false;

			if (event.getFileExcludePatterns()!=null){
				for (String patt : event.getFileExcludePatterns()) {
					if (Pattern.matches(patt, f.getName())) {
						exclude = true;
					}
				}
			}

			if (!exclude) {
				if (f.isFile()) {
					long idle= ((System.currentTimeMillis()-f.lastModified())/DateUtils.MILLIS_PER_DAY);

					LOG.info("file '"+f.getAbsolutePath()+"' idle days is  "+idle+" configured for "+event.getDaysIdle());

					if (idle > event.getDaysIdle()) {
						LOG.info("file " + f.getAbsolutePath()
								+ " last update time is " + f.lastModified()
								+ " max time is " + idle);
						FileUtils.deleteQuietly(f);
					}
				} else if (f.isDirectory()) {
					// a directory, go and look at all the files under it, delete them if they are too old.
					Iterator<File> lookAt = FileUtils.iterateFiles(f, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
					while (lookAt.hasNext()){
						File next=lookAt.next();

						if (event.getFileExcludePatterns()!=null){
							for (String patt : event.getFileExcludePatterns()) {
								if (Pattern.matches(patt, next.getName())) {
									exclude = true;
								}
							}
						}

						if (!exclude){
							long idle= ((System.currentTimeMillis()-next.lastModified())/DateUtils.MILLIS_PER_DAY);

							LOG.info("file '"+next.getAbsolutePath()+"' idle days is  "+idle+" configured for "+event.getDaysIdle());

							if (idle > event.getDaysIdle()) {
								LOG.info("file " + next.getAbsolutePath()
										+ " last update time is " + next.lastModified()
										+ " max time is " + idle);
								FileUtils.deleteQuietly(next);
							}
						}
					}

					// delete the empty directory

					if (FileUtils.sizeOfDirectory(f)==0){
						FileUtils.deleteQuietly(f);
					}

				}
				else {
					LOG.warn("File " + String.valueOf(f) + " was passed and it is not a directory or a file??");
				}
			}
		}
	}

}
