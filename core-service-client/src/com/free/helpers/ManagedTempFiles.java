package com.free.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * class generates temp files that are automatically cleaned up when the file
 * object goes out of scope. This class is meant to be injected via spring. if
 * you don't want to use spring call the afterPropertiesSet() method after
 * creating the object and destroy() when you don't need it anymore. If you
 * don't call the lifecycle methods and try to use it you may have something
 * that hangs on shutdown.
 * 
 * @author E001668
 * 
 */
public class ManagedTempFiles implements InitializingBean, DisposableBean {

	private static final Log LOG = LogFactory.getLog(ManagedTempFiles.class);

	public class TempFile extends WeakReference<File> {
		String path;

		public TempFile(File file, ReferenceQueue q) {
			super(file, q);
			path = file.getAbsolutePath();
			try {
				file.deleteOnExit();
			} catch (ExceptionInInitializerError e) {
				// the JVM is shutting down, ignore.
			}

		}
	}

	private Thread collector;

	private boolean run = true;

	// a reference queue is a way to be notified when an object is garbage
	// collected.
	// the idea here is to keep track of temp files that get created by this
	// class with the queue and
	// when the File object is garbage collected a separate thread will delete
	// the physical file if it still exists
	private ReferenceQueue oldFiles = new ReferenceQueue();

	// a word about reference queues: if you want this stuff to work, somebody
	// has to maintain a reference to the
	// weakreference guy (TempFile in this case) otherwise the object never gets
	// put in the ReferenceQueue.
	// once oldStuff is the only place that holds a reference to the RuleFile,
	// the object will get placed into the reference queue.
	List<ManagedTempFiles.TempFile> oldStuff = new CopyOnWriteArrayList<ManagedTempFiles.TempFile>();

	private void deleteFile(TempFile temp) {
		if (temp.path != null) {
			File f = new File(temp.path);
			if (f.exists()) {
				if (f.isDirectory()) {
					String list[] = f.list();
					int deleteCount = 0;
					for (String file : list) {
						File deleteIt = new File(f.getAbsolutePath() + File.separatorChar + file);
						boolean success = deleteIt.delete();
						if (!success) {
							LOG.debug("delete file " + deleteIt.getAbsolutePath() + " in dir "
									+ f.getAbsolutePath() + " failed, try on next sweep!");
							break;
						}
						LOG.debug("delete file " + deleteIt.getAbsolutePath() + " in dir "
								+ f.getAbsolutePath() + " from managed temp dir");
						deleteCount++;
					}

					if (deleteCount == list.length) {
						boolean success = f.delete();
						if (!success) {
							LOG.debug("delete file from managed temp dir " + f.getAbsolutePath()
									+ " FAILED, try the next sweep ");
						} else {
							oldStuff.remove(temp);
							LOG.debug("delete managed temp DIR " + f.getAbsolutePath());
						}
					}
				} else {
					boolean success = f.delete();
					if (success) {
						LOG.debug("delete managed temp file " + f.getAbsolutePath());
						oldStuff.remove(temp);
					} else {
						LOG.info("delete managed temp file " + f.getAbsolutePath()
								+ " FAILED, try the next sweep ");
					}
				}
			}
		}
	}

	public void afterPropertiesSet() throws Exception {
		collector = new Thread(new Runnable() {
			public void run() {
				while (run) {
					try {
						ManagedTempFiles.TempFile collected = (ManagedTempFiles.TempFile) oldFiles
								.poll();
						if (collected != null) {
							deleteFile(collected);
						}

						for (Iterator<TempFile> it = oldStuff.iterator(); it.hasNext();) {
							TempFile f = it.next();
							if (f.get() == null) {
								deleteFile(f);
							}
						}

						Thread.sleep(4000);
					} catch (InterruptedException e) {
						break;
					} catch (Exception e) {
						LOG.warn(e);
					}
				}
			}
		});
		collector.setName("Managed Temp File collector");
		collector.start();
	}

	public void destroy() {
		System.gc();
		run = false;
		if (collector != null) {
			collector.interrupt();
		}
		for (TempFile f : oldStuff) {
			deleteFile(f);
		}

	}

	/**
	 * get a managed temp file. The file object returned will have the physical
	 * file deleted automatically when the file object gets garbage collected
	 * this method will create the physical file in /tmp
	 * 
	 * @param prefix
	 * @param suffix
	 * @return
	 * @throws IOException
	 */
	public File getManagedTempFile(String prefix, String suffix) throws IOException {
		return getManagedTempFile(prefix, suffix, null);
	}

	/**
	 * get a managed temp file. The file object returned will have the physical
	 * file deleted automatically when the file object gets garbage collected
	 * this method will create the physical file in directory
	 * 
	 * @param prefix
	 * @param suffix
	 * @param directory
	 * @return
	 * @throws IOException
	 */
	public File getManagedTempFile(String prefix, String suffix, File directory) throws IOException {

		if (directory != null) {
			// if we're not in temp, do something different
			// we seem to throw goofy IO exceptions when we try to do this on
			// the dart nas
			File foo = new File(directory, prefix + "-" + UUID.randomUUID() + "-" + suffix);
			FileUtils.touch(foo);
			TempFile f = new TempFile(foo.getAbsoluteFile(), oldFiles);
			oldStuff.add(f);
			return f.get();
		}
		TempFile f = new TempFile(File.createTempFile(prefix, suffix), oldFiles);
		oldStuff.add(f);
		return f.get();
	}

	/**
	 * mark this file object so that when the file object goes out of scope the
	 * physical file is deleted.
	 * 
	 * @param file
	 */
	public void deleteFileObjectAutomaticallyOnGc(File file) {
		TempFile f = new TempFile(file, oldFiles);
		oldStuff.add(f);
	}

	/**
	 * Create a managed temp directory file. A managed temp directory will have
	 * all it's contents delted when the file object that represents the
	 * diectory gets garbage collected. this method will create the physical
	 * file in directory
	 * 
	 * @param prefix
	 * @param suffix
	 * @param directory
	 *            where you want the manged directory to be placed
	 * @return
	 * @throws IOException
	 */
	public File getManagedTempDir(String prefix, String suffix) throws IOException {
		return getManagedTempDir(prefix, suffix, null);
	}

	/**
	 * Create a managed temp directory file. A managed temp directory will have
	 * all it's contents delted when the file object that represents the
	 * diectory gets garbage collected. this method will create the physical
	 * file in directory
	 * 
	 * @param prefix
	 * @param suffix
	 * @param directory
	 *            where you want the manged directory to be placed
	 * @return
	 * @throws IOException
	 */
	public File getManagedTempDir(String prefix, String suffix, File directory) throws IOException {
		File marker = null;
		if (directory != null) {
			marker = File.createTempFile(prefix, suffix, directory);
		} else {
			marker = File.createTempFile(prefix, suffix);
		}

		String path = marker.getAbsolutePath();

		marker.delete();

		marker = new File(path);
		boolean makeDir = marker.mkdir();

		if (!makeDir) {
			LOG.warn("create dir failed?");
			throw new IOException("can not create temp directory in " + directory.getAbsolutePath());
		}

		TempFile f = new TempFile(marker, oldFiles);
		oldStuff.add(f);

		return f.get();
	}

	/**
	 * handy method for testing
	 * 
	 * @param gzipFile
	 * @param prefix
	 * @param suffix
	 * @param dirTarget
	 * @return
	 */
	public File gzipDecompressFileAndCopyToTemp(File gzipFile, String prefix, String suffix,
			File dirTarget) {
		try {
			File target = getManagedTempFile(prefix, suffix, dirTarget);

			if (!gzipFile.exists()) {
				throw new IllegalArgumentException("target file must exist");
			}

			InputStream fin = null;
			FileOutputStream fout = null;

			try {
				fin = new GZIPInputStream(new FileInputStream(gzipFile));
				fout = new FileOutputStream(target);

				IOUtils.copyLarge(fin, fout);
			} finally {
				IOUtils.closeQuietly(fin);
				IOUtils.closeQuietly(fout);
			}

			return target;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public File extractEntryFromZipToManagedTempFile(File zipFile, String entryName, File dirTarget) {
		try {
			File target = getManagedTempFile("ziptemp", "tmp", dirTarget);

			if (!zipFile.exists()) {
				throw new IllegalArgumentException("target file must exist");
			}

			ZipInputStream fin = new ZipInputStream(new FileInputStream(zipFile));
			FileOutputStream fout = new FileOutputStream(target);

			ZipEntry entry = fin.getNextEntry();
			while (entry != null) {
				if (entry.getName().equals(entryName)) {
					IOUtils.copyLarge(fin, fout);
					IOUtils.closeQuietly(fin);
					IOUtils.closeQuietly(fout);
					return target;
				}
				entry = fin.getNextEntry();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		throw new IllegalArgumentException("entry name must exist in ZIP file");
	}

	public File copyFileToMangedTempFile(File source, File directory, String targetPrefix,
			String targetSuffix) throws Exception {
		File target = getManagedTempFile(targetPrefix, targetSuffix, directory);
		RandomAccessFile fileSource = new RandomAccessFile(source, "r");
		RandomAccessFile targetFile = new RandomAccessFile(target, "rw");
		fileSource.getChannel().transferTo(0, fileSource.length(), targetFile.getChannel());
		fileSource.close();
		targetFile.close();
		return target;
	}

}