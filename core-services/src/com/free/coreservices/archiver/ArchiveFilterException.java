package com.free.coreservices.archiver;

import java.io.File;

public class ArchiveFilterException extends Exception {
	private static final long serialVersionUID = 1L;
	File source, target;

	public ArchiveFilterException(File source, File target, Throwable e){
		super(e);
		this.source=source;
		this.target=target;
	}

	public File getSource() {
		return source;
	}

	public File getTarget() {
		return target;
	}
}
