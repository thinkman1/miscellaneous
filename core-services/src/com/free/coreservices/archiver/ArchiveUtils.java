package com.free.coreservices.archiver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.MapFile;
import org.apache.hadoop.io.Text;

public class ArchiveUtils {
	private static final Log LOG = LogFactory.getLog(ArchiveUtils.class);



	public static void visitArchives(File baseDir, String archivePrefix,FileSystem fs,ArchiveVisitor ...visitors) throws Exception {
		File archives[]=getPathToArchiveFiles(baseDir, archivePrefix);
		if (archives==null){
			return;
		}
		for (File archive : archives){
			File target = new File(baseDir,archive.getName());

			MapFile.Reader reader = new MapFile.Reader(fs, target.getAbsolutePath(), fs.getConf());
			try {
				Text key = new Text();
				ArchiverValue value = new ArchiverValue();
				while(reader.next(key, value)) {
					for (ArchiveVisitor visitor : visitors){
						visitor.visit(key, value);
					}
				}
			} catch (Exception e){
				LOG.error("caught exception visiting archives ",e);
			} finally {
				IOUtils.closeQuietly(reader);
			}
		}
	}

	public static File[] getPathToArchiveFiles(File baseDir,final String prefix) {
		LOG.trace("look in "+baseDir.getAbsolutePath()+" for achive files...");

		String names[]=baseDir.list(new NameFiler(prefix));

		if (names==null ){
			return null;
		}

		if (names.length < 1){
			return null;
		}

		Arrays.sort(names);

		File archiveFiles[]=new File[names.length];

		for (int i = 0 ; i < names.length;i++){
			String name = names[i];
			archiveFiles[i]=new File(name);
		}

		return archiveFiles;
	}

	public static void applyArchiveFilter(File baseDir,String prefix,ArchiveFilter filter,FileSystem fs) throws ArchiveFilterException, IOException {
		File archiveDirs[] = getPathToArchiveFiles(baseDir, prefix);

		if (archiveDirs==null){
			LOG.info("no archives found in "+baseDir.getAbsolutePath());
			return;
		}

		// apply the filter to each directory
		for (File farch : archiveDirs){
			File current = new File(baseDir,farch.getName());
			File nextArch = getNextArchiveFileName(baseDir, prefix);
			File locker = new File(nextArch.getAbsolutePath()+".tmp");
			FileUtils.touch(locker);

			MapFile.Reader reader=null;
			MapFile.Writer writer =null;

			reader = new MapFile.Reader(fs, current.getAbsolutePath(), fs.getConf());
			writer= new MapFile.Writer(fs.getConf(), fs, nextArch.getAbsolutePath(), Text.class, ArchiverValue.class);
			int count=0;
			try {
				Text key = new Text();
				ArchiverValue value = new ArchiverValue();

				while (reader.next(key, value)){

					LOG.trace("archive key is "+key);

					if (!filter.removeItem(key, value)){
						count++;
						writer.append(key, value);
					}
				}
			} catch (Exception e){
				// if we've put files in, we may need to clean up
				throw new ArchiveFilterException(current, nextArch, e);
			} finally {
				IOUtils.closeQuietly(reader);
				IOUtils.closeQuietly(writer);
			}

			LOG.info("filtered archive now has "+count+" items in "+nextArch.getAbsolutePath());

			// delete old archive since we have the new one

			LOG.info("delete old archive "+current.getAbsolutePath()+ " our data is NOW in "+nextArch.getAbsolutePath());

			deleteWithRetry(current);

			ArchiveUtils.unlockArchiveDirectory(nextArch);
		}
	}

	public static void deleteWithRetrySingle(File nuke) throws IOException {
		for (int i = 0 ; i < 50 ;i++){
			try {
				FileUtils.forceDelete(nuke);
				return;
			} catch (IOException e) {

			}
			LOG.info("couldn't delete "+nuke.getAbsolutePath());
			try {
				TimeUnit.SECONDS.sleep(10);
			} catch (InterruptedException e){
				Thread.currentThread().interrupt();
				LOG.info("break...?");
				return;
			}
		}
		throw new IOException("couldn't delete "+nuke.getAbsolutePath());
	}


	public static void deleteWithRetry(File nuke) throws IOException {
		for (int i = 0 ; i < 50 ;i++){
			try {
				DirectoryDeleteUsingProcessBuilder del = new DirectoryDeleteUsingProcessBuilder();
				del.deleteDir(nuke, true);
				return;
			} catch (IOException e){
				try {
					TimeUnit.SECONDS.sleep(10);
				} catch (InterruptedException ee){
					return;
				}
			}
		}
		throw new IOException("couldn't delete "+nuke.getAbsolutePath());

	}

	public static ByteArrayInputStream getInputStreamForArchivedFile(File baseDir,String prefix,String fileName,FileSystem fs) throws IOException {
		ZeroCopyHelper help =getJustDataForArchivedFile(baseDir, prefix, fileName, fs);
		if (help!=null){
			ZeroCopyByteArrayInputStream byis = new ZeroCopyByteArrayInputStream(help.data, help.length);
			return byis;
		}
		return null;
	}

	public static ZeroCopyHelper getJustDataForArchivedFile(File baseDir,String prefix,String fileName,FileSystem fs) throws IOException {
		File files[] = getPathToArchiveFiles(baseDir, prefix);

		if (files==null){
			return null;
		}

		ArchiverValue valueHolder = new ArchiverValue();
		Text key=new Text();

		ZeroCopyHelper helper =null;

		for (File file : files){
			LOG.trace("test file "+file.getAbsolutePath());

			MapFile.Reader reader = new MapFile.Reader(fs,new File(baseDir,file.getName()).getAbsolutePath(),fs.getConf());
			try {
				ArchiverValue value=valueHolder;
				key.set(fileName);

				LOG.trace("test file "+file.getAbsolutePath()+" key "+key.toString());

				value=(ArchiverValue)reader.get(key, value);
				if (value!=null){
					helper = new ZeroCopyHelper();
					helper.data=value.getBytes();
					helper.length=value.getRealSize();
				}

				if (LOG.isTraceEnabled()){
					reader.reset();

					value=valueHolder;
					while (reader.next(key, value)){
						LOG.trace("I see key "+key);
					}
				}

			} catch (Exception e){
				LOG.error("error trying to read key "+fileName+" from archive file "+file.getAbsolutePath(),e);
				throw new IOException(e);
			} finally {
				IOUtils.closeQuietly(reader);
			}
		}

		return helper;
	}


	public static File getPathToHighestNamedArchiveFile(File baseDir,final String prefix) {
		File archiveFile = null;

		String names[]=baseDir.list(new NameFiler(prefix));

		if (names.length < 1){
			return null;
		}

		Arrays.sort(names);

		archiveFile=new File(baseDir,names[names.length-1]);

		return archiveFile;
	}

	public static File getNextArchiveFileName(File baseDir,final String prefix) {
		String names[]=baseDir.list(new NameFiler(prefix,true));

		if (names.length < 1){
			return new File(baseDir,prefix+"-part-0000000000");
		}

		Arrays.sort(names);

		int number = Integer.parseInt(names[names.length-1].split("-part-")[1]);
		number++;
		String num=String.valueOf(number);

		return new File(baseDir,prefix+"-part-"+StringUtils.leftPad(num, 10,'0'));
	}

	public static void createArchiveDirectory(File archiveDirectory) throws IOException {
		File lockFIle = new File(archiveDirectory.getParentFile(),archiveDirectory.getName()+".tmp");
		LOG.trace("lock file is "+lockFIle.getAbsolutePath());

		FileUtils.touch(lockFIle);

		if (!archiveDirectory.exists()){
			if (!archiveDirectory.mkdir()){
				throw new IOException("can't create archive directory "+archiveDirectory.getAbsolutePath());
			}
		}
	}

	public static void unlockArchiveDirectory(File archiveDirectory) throws IOException {
		File lockFIle = new File(archiveDirectory.getParentFile(),archiveDirectory.getName()+".tmp");
		deleteWithRetry(lockFIle);
	}

}
