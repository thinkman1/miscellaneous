package com.free.coreservices.filefinder.impl.httpclient;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.free.coreservices.filefinder.FileFinderClient;
import com.free.coreservices.util.FilePathException;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
import com.jamonapi.MonitorFactory;
import com.jpmc.dart.commons.file.FileSystemUtils;
import com.jpmc.dart.commons.util.FileNameUtils;
import com.jpmc.dart.commons.util.StringBuilderUtils;

public class HttpClientFileClient implements FileFinderClient, ApplicationContextAware, InitializingBean, DisposableBean {

	/**
	 * since network operations are kinda expensive, keep a LRU cache of file
	 * references that have been created by ManagedTempFiles (used by
	 * HttpClientConnect). It uses soft references to store the values, in case
	 * the JVM becomes memory starved the references will be cleared. Once the
	 * references fall out of the cache, the get deleted automatically by
	 * ManagedTempFiles.
	 */
	private ConcurrentMap<String, SoftReference<File>> fileCache;

	private static final Log LOG = LogFactory.getLog(HttpClientFileClient.class);

	private ApplicationContext context;
	private FileNameUtils fileNameUtils;
	private String workDirectory;
	private File workDir;

	private ThreadLocal<StringBuilder> stringBuilder = new ThreadLocal<StringBuilder>();
	private Map<String, HttpClientConnect> urlConnections = new HashMap<String, HttpClientConnect>();
	private String remoteUrls[];

	private class EvictListener implements EvictionListener<String, SoftReference<File>> {
		@Override
		public void onEviction(String key, SoftReference<File> value) {
			LOG.info("soft reference for key " + key + " evicted");
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context = applicationContext;
	}

	public void clearSoftReferenceCache() {
		fileCache.clear();
	}

	@Override
	public void destroy() throws Exception {
		clearSoftReferenceCache();
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		fileCache = new ConcurrentLinkedHashMap.Builder<String, SoftReference<File>>()
				.maximumWeightedCapacity(10).listener(new EvictListener()).build();

		workDir = new File(workDirectory);

		for (String remoteUrl : remoteUrls) {
			LOG.trace("set up " + remoteUrl);

			HttpClientConnect con = (HttpClientConnect) context.getBean("HttpClientConnect");
			con.setHostUrl(remoteUrl);
			con.setWorkDir(workDir);
			urlConnections.put(remoteUrl, con);
		}
	}

	@Override
	public List<String> listFilesInDirectoryAndArchive(String pathToList) throws Exception {

		if (stringBuilder.get() == null) {
			stringBuilder.set(new StringBuilder());
		}

		StringBuilder buff = stringBuilder.get();

		String relativePath = fileNameUtils.getRelativeFileNameFromPrefix(pathToList,
				File.separator, buff);

		// For static scan - need to make sure relative path is under our
		// defined base directory
		if (!FileSystemUtils.validateFileUnderBaseDir(this.fileNameUtils.getBaseDir(), pathToList)) {
			throw new FilePathException(this.fileNameUtils.getBaseDir(), pathToList);
		}

		File local = new File(fileNameUtils.getBaseDir(), relativePath);

		List<String> files = new ArrayList<String>();

		LOG.trace("local is " + local.getAbsolutePath() + " exists? " + local.exists());

		Collection<File> filez = new ArrayList<File>();

		// look locally
		if (local.exists()) {

			// Collection<File> filez=FileUtils.listFiles(local, new
			// FileFilter(),new FileFilter());
			filez = FileUtils.listFiles(local, null, true);
			CollectionUtils.filter(filez, new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					File foo = (File) object;
					if (foo.isDirectory()) {
						return false;
					}
					String path = foo.getAbsolutePath();
					if (path.contains("-part-")) {
						return false;
					}

					return true;
				}
			});

			for (File f : filez) {
				String name = fileNameUtils.getRelativeFileNameFromPrefix(f.getAbsolutePath(),
						File.separator, buff);
				buff.setLength(0);
				buff.append(name);
				StringBuilderUtils.replaceString(buff, File.separator, "/");
				files.add(buff.toString());
			}
		}
		// now list the stuff in the archive
		buff.setLength(0);
		buff.append("/listArchive");
		if (!relativePath.startsWith("/")) {
			buff.append("/");
		}
		buff.append(relativePath);

		StringBuilderUtils.replaceString(buff, File.separator, "/");
		if (buff.charAt(0) != '/') {
			buff.insert(0, '/');
		}

		// look on each remote to check the archived files
		for (String uri : remoteUrls) {
			LOG.trace("try url " + uri);

			HttpClientConnect connect = urlConnections.get(uri);
			List<String> ret = connect.lististFiles(buff.toString());

			for (String fi : ret) {
				if (!files.contains(fi)) {
					files.add(fi);
				}
			}
		}

		return files;
	}

	public File getFileLocal(String path, StringBuilder work, String separator) {
		String relativeFile = fileNameUtils.getRelativeFileNameFromPrefix(path, separator, work);
		String baseDir = fileNameUtils.getBaseDir();
		work.setLength(0);
		work.append(baseDir).append(separator).append(relativeFile);
		// remove any // or \\ nonsense
		StringBuilderUtils.replaceString(work, separator + separator, separator);

		LOG.info("look for " + work);

		File ret = new File(work.toString());

		if (ret.exists()) {
			try {
				// For static scan - need to make sure relative path is under
				// our defined base directory
				if (!FileSystemUtils.validateFileUnderBaseDir(baseDir, ret)) {
					LOG.warn("The file '" + work + "' does not fall under an approved directory '" +
							baseDir + "' and will not be processed.");
				}
			} catch (IOException e) {
				LOG.warn("Error occurred while validating the file'" + work
						+ "' exists under an approved directory '" +
						baseDir + "'.", e);
			}
			return ret;
		}
		return null;
	}

	@Override
	public File getFile(String path) throws Exception {
		if (stringBuilder.get() == null) {
			stringBuilder.set(new StringBuilder());
		}

		StringBuilder buff = stringBuilder.get();

		// look for the file locally
		File local = getFileLocal(path, buff, File.separator);

		if (local != null) {
			return local;
		}

		LOG.info("file " + path + " not found locally...");

		String rel = fileNameUtils.getRelativeFileNameFromPrefix(path, File.separator, buff);
		buff.setLength(0);
		buff.append("/getFile");
		if (!rel.startsWith(File.separator)) {
			buff.append(File.separator);
		}
		buff.append(rel);
		StringBuilderUtils.replaceString(buff, File.separator, "/");
		if (buff.charAt(0) != '/') {
			buff.insert(0, '/');
		}

		String name = buff.toString();

		if (fileCache.containsKey(name)) {
			if (fileCache.get(name).get() != null) {
				MonitorFactory.add("network file cache", "hit", 1.0);

				return fileCache.get(name).get();
			}
		}

		for (String uri : remoteUrls) {
			LOG.trace("try url " + uri);

			HttpClientConnect connect = urlConnections.get(uri);
			File data = connect.getFile(name);
			if (data != null) {
				if (data.length() > 0) {
					fileCache.put(name, new SoftReference<File>(data));
					return data;
				}
			}
		}
		return null;
	}

	/**
	 * find a file under a base path (say image root path) when you don't know
	 * the proc date
	 * 
	 * @param rootPath
	 * @param relativePath
	 * @return
	 */
	public File findFileRelativePathUnderRootPaths(File rootPath, String relativePath)
			throws Exception {

		if (!rootPath.exists()) {
			return null;
		}

		// base path, under here's the probably a bunch of proc dates
		for (String childDir : rootPath.list()) {
			// list the files under this subdir
			File checkdir = new File(rootPath, childDir);

			LOG.info("check for file " + checkdir.getAbsolutePath());

			File isthere = getFile(new File(checkdir, relativePath).getAbsolutePath());

			if (isthere != null) {
				return isthere;
			}
		}

		return null;
	}

	public void setFileNameUtils(FileNameUtils fileNameUtils) {
		this.fileNameUtils = fileNameUtils;
	}

	public void setRemoteUrls(String[] remoteUrls) {
		this.remoteUrls = remoteUrls;
	}

	public void setWorkDirectory(String workDirectory) {
		this.workDirectory = workDirectory;
	}

}
