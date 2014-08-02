package com.free.coreservices.archiver;


public class ArchiveConfig {
	private String baseDirectory;
	private int lastModifiedHours;
	private int maxLifeDays;
	private boolean useDirNameInArchiveKey=false;

	//create a new archive every time the archive process runs.  If you set this to true, the 
	// process will append any new archive items into the same archive.  keep in mind this
	// process uses a lot of memory so only use it for archives that are expected to stay small.
	private boolean concatArchives=false;
	
	private String archiveFilePrefix="archive-files";
	// the directory is named by a date, so you can use it to figure out its age
	private boolean ageIsDirectoryName=true;

	// use this if you have archives that have many dates worth of data in them.
	// it'll let you filter out the ones that are too old (needed for reports)
	// put the fully qualified class name, something that implements com.jpmc.dart.coreservices.archiver.ArchiveFilter
	private String customArchiveFilter;
	
	// each child directory will be an archive (think in/archive) 
	// if you set this to false, all subdirs get shoved into a huge archive
	// if this happens, you need to set a customArchiveFilter to throw 
	// out archive items that are too old
	private boolean childDirectoriesNeedArchived=true;
	
	
	
	public String getBaseDirectory() {
		return baseDirectory;
	}
	public void setBaseDirectory(String baseDirectory) {
		this.baseDirectory = baseDirectory;
	}
	public int getLastModifiedHours() {
		return lastModifiedHours;
	}
	public void setLastModifiedHours(int lastModifiedMinutes) {
		this.lastModifiedHours = lastModifiedMinutes;
	}
	public int getMaxLifeDays() {
		return maxLifeDays;
	}
		
	public void setMaxLifeDays(int maxLifeDays) {
		this.maxLifeDays = maxLifeDays;
	}
	public void setAgeIsDirectoryName(boolean ageIsDIrectoryName) {
		this.ageIsDirectoryName = ageIsDIrectoryName;
	}
	
	public boolean isAgeIsDirectoryName() {
		return ageIsDirectoryName;
	}
	
	public void setUseDirNameInArchiveKey(boolean useDirNameInArchiveKey) {
		this.useDirNameInArchiveKey = useDirNameInArchiveKey;
	}
	
	public boolean isUseDirNameInArchiveKey() {
		return useDirNameInArchiveKey;
	}
	
	public void setConcatArchives(boolean concatArchives) {
		this.concatArchives = concatArchives;
	}
	
	public boolean isConcatArchives() {
		return concatArchives;
	}
	
	public String getArchiveFilePrefix() {
		return archiveFilePrefix;
	}
	
	public void setArchiveFilePrefix(String archiveFilePrefix) {
		this.archiveFilePrefix = archiveFilePrefix;
	}

	public void setCustomArchiveFilter(String customArchiveFilter) {
		this.customArchiveFilter = customArchiveFilter;
	}
	
	public String getCustomArchiveFilter() {
		return customArchiveFilter;
	}
	
	public void setChildDirectoriesNeedArchived(
			boolean childDirectoriesNeedArchived) {
		this.childDirectoriesNeedArchived = childDirectoriesNeedArchived;
	}
	
	public boolean isChildDirectoriesNeedArchived() {
		return childDirectoriesNeedArchived;
	}
}
