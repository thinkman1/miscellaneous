package com.free.coreservices.filefinder;

import java.io.File;
import java.util.List;

public interface FileFinderClient {
	public File getFile(String path) throws Exception;

	public List<String> listFilesInDirectoryAndArchive(String pathToList) throws Exception;

	/**
	 * find a file under a base path (say image root path) when you don't know
	 * the proc date
	 * 
	 * @param rootPath
	 * @param relativePath
	 * @return
	 */
	public File findFileRelativePathUnderRootPaths(File rootPath, String relativePath)
			throws Exception;
}
