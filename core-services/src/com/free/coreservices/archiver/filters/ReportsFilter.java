/**
 * 
 */
package com.free.coreservices.archiver.filters;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.Text;

import com.free.commons.util.StringBuilderUtils;
import com.free.coreservices.archiver.ArchiveFilter;
import com.free.coreservices.archiver.ArchiverValue;
import com.jpmc.vpc.model.dart.event.FileArchiveActionEvent;

/**
 * @author e001668
 *
 */
public class ReportsFilter implements ArchiveFilter {
	private static final Log LOG = LogFactory.getLog(ReportsFilter.class);
	private StringBuilder buffer = new StringBuilder();
	private FileArchiveActionEvent conf;
	
	/* (non-Javadoc)
	 * @see com.jpmc.dart.coreservices.archiver.ArchiveFilter#removeItem(org.apache.hadoop.io.Text, com.jpmc.dart.coreservices.archiver.ArchiverValue)
	 */
	@Override
	public boolean removeItem(Text key, ArchiverValue data) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		buffer.setLength(0);
		buffer.append(key.toString());
		if (buffer.charAt(0)==File.separatorChar){
			buffer.deleteCharAt(0);
		}
		
		int index=buffer.indexOf(File.separator);
		
		if (index < 0){
			LOG.info("data doesn't seem right, key formatted funky "+buffer.toString());
			
			return false;
		}
		
		buffer.delete(index, buffer.length());
		StringBuilderUtils.removeString(buffer, "_");
		try {
			Date date = sdf.parse(buffer.toString());	
			long diff=System.currentTimeMillis()-date.getTime();
			
			boolean remove=(diff > conf.getLiveDaysInMills());
			
			if (remove){
				LOG.info("remove "+key+" from archive");
			}
					
			return remove;
		} catch (Exception e){
			LOG.warn("date didn't parse "+buffer.toString());
		}
		return false;
	}

	@Override
	public void setConfig(FileArchiveActionEvent conf) {
		this.conf=conf;
	}
}
