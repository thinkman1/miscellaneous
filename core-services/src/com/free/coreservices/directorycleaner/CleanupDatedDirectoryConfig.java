package com.free.coreservices.directorycleaner;

public class CleanupDatedDirectoryConfig {
	private String directory;
	private int daysIdle;
	private String filePatternExclude[];
	
	public void setFilePatternExclude(String[] filePatternExclude) {
		this.filePatternExclude = filePatternExclude;
	}
	
	public String[] getFilePatternExclude() {
		return filePatternExclude;
	}
	
	public void setDaysIdle(int daysIdle) {
		this.daysIdle = daysIdle;
	}
	public int getDaysIdle() {
		return daysIdle;
	}
	
	public String getDirectory() {
		return directory;
	}
	public void setDirectory(String directory) {
		this.directory = directory;
	}	
}
