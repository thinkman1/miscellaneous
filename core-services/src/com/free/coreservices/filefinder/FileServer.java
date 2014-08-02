package com.free.coreservices.filefinder;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.free.commons.util.FileNameUtils;
import com.free.commons.util.StringBuilderUtils;
import com.free.coreservices.archiver.ArchiveConfig;
import com.free.coreservices.archiver.ArchiveUtils;
import com.free.coreservices.archiver.ListDirectoryVisitor;
import com.free.coreservices.archiver.ZeroCopyHelper;
import com.jpmc.dart.commons.file.FileSystemUtils;

@Controller
public class FileServer implements InitializingBean, ApplicationContextAware{
	private FileNameUtils fileNameUtils;

	private static final Log LOG = LogFactory.getLog(FileServer.class);

	private ApplicationContext context;
	private ArchiveConfig config[];
	private FileSystem fs;
	private String mypass;
	private String localDir;
	private ThreadLocal<StringBuilder> builder = new ThreadLocal<StringBuilder>();

	@Override
	public void afterPropertiesSet() throws Exception {
		String names[]= context.getBeanNamesForType(ArchiveConfig.class);
		config = new ArchiveConfig[names.length];
		for (int i = 0 ; i < names.length;i++){
			config[i]=context.getBean(names[i],ArchiveConfig.class);
		}
		Validate.isTrue(names.length>0);
		Configuration conf = new Configuration();
		fs = FileSystem.get(conf);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context=applicationContext;
	}

	public String getPath(StringBuilder buffer,String split, String requestURI){

		String relativeDestFileName = requestURI.split(split)[1];

		relativeDestFileName= fileNameUtils.getRelativeFileNameFromPrefix(relativeDestFileName, File.separator,buffer);

		LOG.trace("prefix is "+localDir+" file "+relativeDestFileName);

		buffer.setLength(0);
		buffer.append(localDir).append(File.separator).append(relativeDestFileName);

		StringBuilderUtils.replaceString(buffer, "/", File.separator);

		StringBuilderUtils.replaceString(buffer, File.separator+File.separator,File.separator );

		StringBuilderUtils.replaceString(buffer, "%5B", "[");
		StringBuilderUtils.replaceString(buffer, "%5D", "]");

		LOG.trace("path is "+buffer);

		return buffer.toString();
	}

	/**
	 * get the list of entries under an archived directory.  This will return the FULL path you would call getFile with to retrieve the data .
	 *
	 * If you point this at a directory that is not watched by the archive process it'll return an empty list
	 */
	@RequestMapping("/listArchive/**")
	public void getAllArchiveEntries(final HttpServletRequest request,
					HttpServletResponse response) throws Exception {
		if (builder.get()==null){
			builder.set(new StringBuilder());
		}


		String pass=request.getHeader("PASS");

		if (!StringUtils.equals(pass, mypass)){
			response.getWriter().print("So sorry");
			response.setStatus(401);
			return;
		}


		StringBuilder workBuffer = builder.get();

		String dest = getPath(workBuffer, "/listArchive", request.getRequestURI());
		// For static scan - but also ensures file is under the approved root directory
		boolean isValidFileName = FileSystemUtils.validateFileUnderBaseDir(localDir, dest);
		if (! isValidFileName) {
			String error =
					"File name passed in either does not have a valid name or is not under the approved local directory (" +
					this.localDir + "):  " + dest;
			LOG.error(error);
			response.setStatus(406);
		}
		else {

			String relative=fileNameUtils.getRelativeFileNameFromPrefix(dest, File.separator, workBuffer);

			// dest is a directory.
			// we're going to return a series of key names that should be in the directory
			ArchiveConfig conf = getArchiveConfigForFile(dest);

			if (conf !=null){
				try {
					// this call really only wants the archived files.  the service can look locally w/o server side help.
					ListDirectoryVisitor visitor = new ListDirectoryVisitor(relative, conf, response.getWriter());
					ArchiveUtils.visitArchives(new File(dest), conf.getArchiveFilePrefix(), fs, visitor);
					response.setStatus(200);
				} catch (Exception e){
					LOG.error("error trying to list directory ",e);
					response.getWriter().write(ExceptionUtils.getFullStackTrace(e));
					response.setStatus(500);
				}
			} else {
				response.setStatus(404);
			}
		}

	}
	
	// continue
	/**
	 * get a file based on the full path
	 *
	 * TODO:  this is a really big method...
	 */
	@RequestMapping("/getFile/**")
	public void getFile(final HttpServletRequest request,
		final HttpServletResponse response) throws Exception {
		RandomAccessFile readFile = null;
		try {

			LOG.trace("full URI is "+request.getRequestURI());

			if (request.getRequestURI().endsWith("ping")){
				response.getWriter().print("PONG! "+UUID.randomUUID());
				response.setStatus(200);
				return;
			}

			String pass=request.getHeader("PASS");
			if (!StringUtils.equals(pass, mypass)){
				response.getWriter().print("So sorry");
				response.setStatus(401);
				return;
			}

			if (builder.get()==null){
				builder.set(new StringBuilder());
			}

			StringBuilder absDestFileName = builder.get();

			// Ignoring the return - it's in the builder too.
			getPath(absDestFileName, "/getFile", request.getRequestURI());

			String path=absDestFileName.toString();

			// Needed for Static scan - but also verifies the given path is under the approved local directory structure.
			if (! FileSystemUtils.validateFileUnderBaseDir(this.localDir, path)) {
				String error =
						"File name passed in either does not have a valid name or is not under the approved local directory (" +
						this.localDir + "):  " + path;
				LOG.error(error);
				response.setStatus(406);
			}
			else {
				File dest = new File(path);

				LOG.trace("dest is "+dest.getAbsolutePath());

				if (dest.exists()){
					// use channels to zap the data
					// dart is optimized for transactions, so we shouldn't be serving huge files out of here.
					response.setContentLength((int)dest.length());

					readFile = new RandomAccessFile(dest, "r");
					long totalFile=readFile.length();
					long totalFileSent=0;
					while (totalFileSent<totalFile){
						//we can only send Integer.MAX at a time
						totalFileSent+=readFile.getChannel().transferTo(
								totalFileSent,Integer.MAX_VALUE,
								Channels.newChannel(response.getOutputStream()));
					}

					IOUtils.closeQuietly(readFile);

					response.setStatus(200);
					return;
				}
				// get the archive config for the directory
				ArchiveConfig conf = getArchiveConfigForFile(dest);
				if (conf != null){

					figureOutFileKey(absDestFileName, conf, File.separator);

					String key=absDestFileName.toString();

					// almost done, tell archive utils where the archive file(s) are
					absDestFileName.setLength(0);
					absDestFileName.append(dest.getAbsolutePath());

					figureOutArchiveDirectory(absDestFileName, conf,File.separator);

					File whereArchiveFilesAre=new File(absDestFileName.toString());

					boolean isValidFilePath = FileSystemUtils.validateFileUnderBaseDir(this.localDir, whereArchiveFilesAre);

					LOG.trace("look in "+whereArchiveFilesAre.getAbsolutePath()+" for "+key);

					// get the bare data from the file, try to make this as zero copy as possible.
					// the archive entries should be small enough for this not to be an issue.
					ZeroCopyHelper in= ArchiveUtils.getJustDataForArchivedFile(whereArchiveFilesAre,conf.getArchiveFilePrefix(),key,fs);

					if (isValidFilePath == false) {
						String error =
								"File name passed in either does not have a valid name or is not under the approved local directory (" +
								this.localDir + "):  " + dest;
						LOG.error(error);
						response.setStatus(406);
					}
					else if (in!=null){
						response.setContentLength(in.length);
						response.getOutputStream().write(in.data,0,in.length);
						response.getOutputStream().flush();
						response.setStatus(200);
					} else {
						response.setStatus(404);
						response.getWriter().append("file "+dest.getAbsolutePath()+" not found, looked in archive");
					}
				} else {
					response.setStatus(404);
					response.getWriter().append("file "+dest.getAbsolutePath()+" not found, no archive conf found");
				}
			}
		} catch (Exception e) {
			// don't worry about NAS blips. the client will retry if the the status code isn't 200 or 404.
			response.setStatus(500);
			LOG.error("errror",e);
			response.getWriter().write(ExceptionUtils.getFullStackTrace(e));
			return;
		}
		finally {
			IOUtils.closeQuietly(readFile);
		}
	}

	public void figureOutFileKey(StringBuilder builder,ArchiveConfig conf,String seperator){
		// make the key we use to acces it.
		if (conf.isUseDirNameInArchiveKey()){

			if (conf.isChildDirectoriesNeedArchived()){
				int indexOf=builder.indexOf(conf.getBaseDirectory());
				indexOf+=conf.getBaseDirectory().length();
				builder.delete(0, indexOf+1);
				// this should leave you with something like this: "path under base directory"/01/01.../file.xml
				//what we want is to remove the name of the directory that would of been
				// directly under the base directory name
				int indexOfSlash=builder.indexOf(seperator);
				builder.delete(0, indexOfSlash+1);
			} else {
				builder.delete(0, builder.indexOf(conf.getBaseDirectory()));
				builder.delete(0, conf.getBaseDirectory().length());
			}
		} else {
			// if the archive key is just the file name, it's easier =-)
			builder.delete(0, builder.lastIndexOf(seperator)+1);
		}
		if (StringBuilderUtils.startsWith(builder, File.separator)){
			builder.deleteCharAt(0);
		}
	}

	public void figureOutArchiveDirectory(StringBuilder builder,ArchiveConfig conf,String seperator){
		if (!conf.isChildDirectoriesNeedArchived()){
			// somethig like /soh340na06/cto_dart_dev/reports/dart/ALL_ITEMS_REPORT/20130101/blahblah.html
			builder.setLength(0);
			builder.append(localDir);
			// we are assuming the configs have been scrubbed of the nas prefix.
			//you'll see stuff like /out/ or /achive instead of //blahblah/prod/out
			// if not, please ask for your money back.
			builder.append(conf.getBaseDirectory());
		} else {
			// the archive file should be two directories under the path that is getting monitored
			// you should end up with something like:
			// /sohfoobar/cto_prod/out/20130101
			int indexOfMon=builder.indexOf(conf.getBaseDirectory());
			indexOfMon+=conf.getBaseDirectory().length();
			indexOfMon++;
			indexOfMon = builder.indexOf(seperator,indexOfMon);
			indexOfMon++;
			builder.delete(indexOfMon,builder.length());
		}
	}

	public ArchiveConfig getArchiveConfigForFile(String dest) {
		// find which config has the prefix of the file
		String wholePath = dest;
		StringBuilder builder = new StringBuilder();

		for (ArchiveConfig ac : config){
			builder.setLength(0);
			builder.append(localDir);
			// we are assuming the configs have been scrubbed of the nas prefix.
			//you'll see stuff like /out/ or /achive instead of //blahblah/prod/out
			// if not, please ask for your money back.
			builder.append(ac.getBaseDirectory());

			StringBuilderUtils.replaceString(builder, "\\\\", "\\");
			StringBuilderUtils.replaceString(builder, "////", "//");

			LOG.trace("prefix " + builder + " with input " + dest);

			if (wholePath.startsWith("//")) {
				wholePath = wholePath.substring(1);
			}
			
			if (builder.toString().startsWith("//")) {
				builder = builder.deleteCharAt(0);
			}
			
			if (StringBuilderUtils.startsWith(wholePath , builder)){
				return ac;
			}
		}

		return null;
	}

	public ArchiveConfig getArchiveConfigForFile(File dest) {
		return getArchiveConfigForFile(dest.getAbsolutePath());
	}

	public void setFileNameUtils(FileNameUtils fileNameUtils) {
		this.fileNameUtils = fileNameUtils;
	}

	public void setMypass(String mypass) {
		this.mypass = mypass;
	}

	public void setLocalDir(String localDir) {
		this.localDir = localDir;
	}
}
