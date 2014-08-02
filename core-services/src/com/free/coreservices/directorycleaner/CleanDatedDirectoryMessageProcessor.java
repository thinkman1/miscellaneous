package com.free.coreservices.directorycleaner;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.free.coreservices.archiver.ArchiveUtils;
import com.jpmc.cto.framework.MessageProcessor;
import com.jpmc.cto.framework.MessageType;
import com.jpmc.cto.framework.exception.ApplicationException;
import com.jpmc.vpc.model.dart.event.DirectoryCleanerEvent;

public class CleanDatedDirectoryMessageProcessor implements MessageProcessor<DirectoryCleanerEvent>{
	private static final Log LOG = LogFactory.getLog(CleanDatedDirectoryMessageProcessor.class);
	
	private static MessageType TYPE= new DirectoryCleanerEvent().getMessageType();
	
	@Override
	public MessageType[] getAcceptedTypes() {
		return new MessageType[] { TYPE};
	}
	
	@Override
	public String getName() {
		return "CleanDatedDirectoryMessageProcessor";
	}

	public void processMessage(DirectoryCleanerEvent event) throws ApplicationException {
		// expected format is yyyyMMdd	
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.setLenient(false);
		
		String dirSplit[] =StringUtils.split(event.getDirectory(),File.separator);
		String dirName =dirSplit[dirSplit.length-1];
		
		LOG.info("process directory "+event.getDirectory());
		
		try {
			Date date = sdf.parse(dirName);
			
			long diff = ((System.currentTimeMillis()-date.getTime())/DateUtils.MILLIS_PER_DAY);
			
			LOG.info("idle days is "+diff+" configured for "+event.getDaysIdle());
			
			if (diff > event.getDaysIdle()){
				ArchiveUtils.deleteWithRetry(new File(event.getDirectory()));
			} else {
				LOG.info("directory is too young "+event.getDirectory());
			}
		} catch (ParseException e){
			LOG.warn("can't process directory "+dirName);
		} catch ( Exception e) {
			throw new ApplicationException(e);
		}
	}
}
