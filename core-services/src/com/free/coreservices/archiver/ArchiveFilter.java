package com.free.coreservices.archiver;

import org.apache.hadoop.io.Text;

public interface ArchiveFilter {
	/**
	 * return false if you want the item to no longer be in the archive
	 * @param key
	 * @param data
	 * @return
	 */
	public boolean removeItem(Text key, ArchiverValue data);
	
	public void setConfig(FileArchiveActionEvent conf);
	
}
