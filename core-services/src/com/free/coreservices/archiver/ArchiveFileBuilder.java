package com.free.coreservices.archiver;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.MapFile;
import org.apache.hadoop.io.Text;

import com.free.commons.util.StringBuilderUtils;

public class ArchiveFileBuilder {
	private static final int BUFFER_SIZE=32000;

	private static final Log LOG = LogFactory.getLog(ArchiveFileBuilder.class);

	// how often we put a key in the map.  more keys is faster, but we also need the index to fit into memory
	private int keyPerEntries=100;

	private void makeKeyFromFileName(File file, File baseDir,boolean useDirNameInKey, StringBuilder buffer){
		buffer.setLength(0);
		if (useDirNameInKey){
			buffer.append(file.getAbsolutePath());
			StringBuilderUtils.removeString(buffer, baseDir.getAbsolutePath());
			if (buffer.charAt(0)==File.separatorChar){
				buffer.deleteCharAt(0);
			}
		} else {
			buffer.append(file.getName());
		}
	}

	/**
	 * create a new archive under sourceDirectory, only stuff it in it files that have a last change time > idleTime
	 * @param sourceDirectory
	 * @param archiveNamePrefix
	 * @param idleTime
	 * @return the files that were archived
	 * @throws Exception
	 */
	public List<File> archiveFiles(File sourceDirectory,
			String archiveNamePrefix,
			long idleTime, boolean useDirNameInKey,FileSystem fs,DirectoryCrawler crawler) throws Exception{
		List<File> archivedStuff = new ArrayList<File>();

		Validate.isTrue(sourceDirectory.exists(),"source directory does not exist "+sourceDirectory.getAbsolutePath());

		File newArchive = ArchiveUtils.getNextArchiveFileName(sourceDirectory, archiveNamePrefix);

		// create dir & lock file
		ArchiveUtils.createArchiveDirectory(newArchive);

		MapFile.Writer writer = new MapFile.Writer(fs.getConf(), fs,newArchive.getAbsolutePath(), Text.class, ArchiverValue.class);

		try {
			Text key = new Text();
			ArchiverValue value = new ArchiverValue(BUFFER_SIZE);
			writer.setIndexInterval(keyPerEntries);


			// now list all the files under the directory that are not archive files
			//List<File> files = new ArrayList<File>(FileUtils.listFiles(sourceDirectory, null, true));

			// list all files that have are of archive age
			//ArchiveFileFinder finder = new ArchiveFileFinder();
			List<File> files=crawler.visit(sourceDirectory, idleTime,archiveNamePrefix);

			LOG.info("crawler found "+files.size());

			Collections.sort(files);

			ByteBuffer bufff = ByteBuffer.allocate(BUFFER_SIZE);
			StringBuilder nameFix=new StringBuilder();

			// now read each file and add to the archive (the crawler will only return files that are old enough)
			for (File f : files){
				//if (FileUtils.sizeOf(f)>0){
					// don't archive the archives!
//					if ((!f.getName().contains(archiveNamePrefix))&&(!f.getName().contains("-part-"))&& (!f.getAbsolutePath().contains(archiveNamePrefix))){

						value.reset();
						// get rid of all the subdirectory nonsense we do. just use the file name (this function will clean the StringBuilder before re-use)
						makeKeyFromFileName(f, sourceDirectory,useDirNameInKey, nameFix);

						RandomAccessFile read = new RandomAccessFile(f, "r");
						bufff.clear();
						int readN=read.getChannel().read(bufff);
						while (readN>-1){
							value.append(bufff,readN);
							bufff.clear();
							readN=read.getChannel().read(bufff);
						}

						IOUtils.closeQuietly(read);

						String keyVal=nameFix.toString();
						key.set(keyVal);

						LOG.trace("make archive with key "+key);
						writer.append(key, value);
						archivedStuff.add(f);
//					}
				//}
			}

			IOUtils.closeQuietly(writer);

			if (archivedStuff.size()<20){
				LOG.info("didn't archive anything from "+sourceDirectory+" so remove the new archive and keep the old, I only saw "+archivedStuff.size());

				// if we didn't archive anything, then remove the new archive since it is empty
				ArchiveUtils.deleteWithRetry(newArchive);

				// unlock the new archive
				ArchiveUtils.unlockArchiveDirectory(newArchive);
				return new ArrayList<File>();
			}

			MapFile.Reader reader= new MapFile.Reader(fs, newArchive.getAbsolutePath(), fs.getConf());

			try {
				// make sure each file in the archive can be accessed (along with the archived data pulled from an existing archive, if we did a concat)
				Iterator<File> filz = archivedStuff.iterator();
				while (filz.hasNext()){
					File file = filz.next();
					makeKeyFromFileName(file, sourceDirectory,useDirNameInKey, nameFix);
					key.set(nameFix.toString());
					value.reset();
					value=(ArchiverValue)reader.get(key, value);
					if (value==null){
						throw new IllegalStateException("file "+file+"(key "+key+") does not exist in the new archive!");
					}
				}
			} finally {
				IOUtils.closeQuietly(reader);
			}

			// unlock the new archive
			ArchiveUtils.unlockArchiveDirectory(newArchive);
		} catch (Exception e){
			LOG.warn("Error happened while archiving directory " + sourceDirectory.getAbsolutePath(), e);
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(writer);
			IOUtils.closeQuietly(fs);
		}

		return archivedStuff;
	}

	public void setKeyPerEntries(int keyPerEntries) {
		this.keyPerEntries = keyPerEntries;
	}
}
