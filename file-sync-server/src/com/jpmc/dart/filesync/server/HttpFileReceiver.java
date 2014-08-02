package com.jpmc.dart.filesync.server;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.jpmc.cto.dart.exception.DartException;
import com.jpmc.cto.dart.model.filesync.ApplicationDatacenter;
import com.jpmc.cto.framework.configuration.ApplicationInformation;
import com.jpmc.dart.commons.file.FileSystemUtils;
import com.jpmc.dart.commons.util.FileNameUtils;
import com.jpmc.dart.commons.util.StringBuilderUtils;
import com.jpmc.dart.filesync.client.FileSyncClient;
import com.jpmc.dart.filesync.constants.FileSyncConstants;

@Controller
public class HttpFileReceiver implements InitializingBean{

	private HttpFileRecieverConf conf;

	private ApplicationDatacenter currentDatacenter;
	private FileNameUtils fileNameUtils;
	private static final Log LOG = LogFactory
			.getLog(HttpFileReceiver.class);


	private String targetFilePrefix;

	/** here to make life easier */
	private FileSyncClient fileSyncClient;
	boolean useMmio=true;

	private ThreadLocal<ChecksumChannel> channel = new ThreadLocal<ChecksumChannel>();

	@Override
	public void afterPropertiesSet() throws Exception {
		Validate.notNull(this.fileSyncClient, "We need the util for help with the datacenters.");
		StringBuilder buff = new StringBuilder();
		buff.append(this.targetFilePrefix);
		StringBuilderUtils.replaceString(buff, "\\\\", "\\");
		StringBuilderUtils.replaceString(buff, "////", "/");
		this.targetFilePrefix=buff.toString();
		this.currentDatacenter = this.fileSyncClient.getSourceDatacenter();
		Validate.notNull(this.currentDatacenter, "Couldn't determine the datacenter we're in.  We need that.");
	}

	/**
	 * @param request We don't need it - but the request sends it.
	 */
	@RequestMapping(value="/ping")
	public void ping(final HttpServletRequest request,final HttpServletResponse resp) throws Exception {
		resp.getWriter().append("A-OK!");
		resp.setStatus(200);
	}


	/**
	 * return the byte count of the passed in file if it exists
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping(value="/fileSizeCheck")
	public void fileSize(final HttpServletRequest request,
			final HttpServletResponse response) throws Exception {
		String relativeDestFileName = request.getRequestURI().split("/fileSizeCheck")[1];
		relativeDestFileName= fileNameUtils.getRelativeFileNameFromPrefix(relativeDestFileName, File.separator);

		try {
			String abs=targetFilePrefix + File.separator + relativeDestFileName;
			LOG.trace("checking for "+abs);
			File f = new File(abs);
			Validate.isTrue(f.exists(), "File " + abs + " not found");
			if (! FileSystemUtils.validateFileUnderBaseDir(fileNameUtils.getBaseDir(), abs)) {
				response.setStatus(404);
				response.getWriter().write("The file '" + abs + "' does not fall under an approved directory '" +
						fileNameUtils.getBaseDir() + "' and will not be processed.");
			}
			else {
				response.setStatus(200);
				response.getWriter().write(String.valueOf(f.length()));
			}
		} catch (Exception e){
			LOG.error("file not found?",e);
			response.setStatus(404);
			response.getWriter().write("file not found?");
		}
	}

	@RequestMapping(value="/fileSync")
	public void receiveFiles(final HttpServletRequest request,
			final HttpServletResponse response) throws Exception {
			String relativeDestFileName = StringUtils.trimToEmpty(
					request.getHeader(FileSyncConstants.HttpReqestHeaderNames.X_CTO_DART_FILENAME));
			String absDestFileName = null;
			String prefix = targetFilePrefix;

			if (LOG.isTraceEnabled()){
				LOG.trace("BEFORE prefix is "+prefix+" file "+relativeDestFileName);
			}

			relativeDestFileName= fileNameUtils.getRelativeFileNameFromPrefix(relativeDestFileName, File.separator);

			if (LOG.isTraceEnabled()){
				LOG.trace("prefix is "+prefix+" file "+relativeDestFileName);
			}

			absDestFileName = prefix + File.separator + relativeDestFileName;

			// save it under .tmp so we don't move/archive/etc before we should
			File dest = new File(absDestFileName+".tmp");

			// Validate for Static Scan
			if (! FileSystemUtils.validateFileUnderBaseDir(prefix, dest)) {
				throw new DartException("The file '" + dest + "' does not fall under an approved directory '" +
						prefix + "' and will not be processed.");
			}

			response.setHeader(
					FileSyncConstants.HttpResponseHeaderNames.LOCAL_FILE_NAME,
					absDestFileName);


			if (LOG.isTraceEnabled()){
				LOG.trace("relative file name is "+relativeDestFileName);
				LOG.trace("final file name is "+absDestFileName);
			}

			response.setHeader(
					FileSyncConstants.HttpResponseHeaderNames.LOCAL_SERVER_NAME,
					ApplicationInformation.getMachineName());

			// Check passkey
			String passkeyFromRequest = request.getHeader(FileSyncConstants.HttpReqestHeaderNames.PASSKEY);
			if (!StringUtils.equals(conf.getPassKey(),passkeyFromRequest)) {
				response.setStatus(401);
				response.getWriter().print("invalid username or passkey.");
				return;
			}

			// Check the datacenter
			String datacenterFromRequest = request.getHeader(FileSyncConstants.HttpReqestHeaderNames.X_CTO_DART_DATACENTER);
			datacenterFromRequest = StringEscapeUtils.escapeHtml(datacenterFromRequest);

			if (! currentDatacenter.getDatacenterName().equals(datacenterFromRequest)) {
				response.setStatus(400);
				response.getWriter().print("Datacenter is not as expected.  Expected:  " + currentDatacenter.getDatacenterName() +
						"  Received:  " + datacenterFromRequest);
				return;
			}

			Validate.isTrue(NumberUtils.isDigits(request.getHeader(FileSyncConstants.HttpReqestHeaderNames.FILE_SIZE)),
					"no sir, it's " + request.getHeader(FileSyncConstants.HttpReqestHeaderNames.FILE_SIZE));

			StopWatch sw = new StopWatch();
			sw.start();

			InputStream is = request.getInputStream();
			ChecksumChannel checkChannel = channel.get();
			if (checkChannel==null){
				checkChannel = new ChecksumChannel();
				channel.set(checkChannel);
			}

			checkChannel.reset(is);

			if (LOG.isTraceEnabled()){
				LOG.trace("data size says: "+request.getHeader(
						FileSyncConstants.HttpReqestHeaderNames.FILE_SIZE));
			}

			long readSize = Long.parseLong(request.getHeader(
					FileSyncConstants.HttpReqestHeaderNames.FILE_SIZE));

			if (LOG.isTraceEnabled()){
				LOG.trace("dest file is "+dest.getAbsolutePath());
			}

			Validate.isTrue(dest.isDirectory()!=true);

			File dir = dest.getParentFile();

			if (LOG.isTraceEnabled()){
				LOG.trace("create dir "+dir.getAbsolutePath()+" create file "+dest.getAbsolutePath());
			}

			dir.mkdirs();

			if (LOG.isTraceEnabled()){
				LOG.trace("file exists?  "+dest.exists());
			}
			
			boolean del= dest.delete();

			if (LOG.isTraceEnabled()){
				LOG.trace("delete says "+del);
			}

			RandomAccessFile fout = new RandomAccessFile(dest, "rw");
			fout.setLength(0);

			long xferSize =0;

			// use zero copy method to dump buffer to file
			xferSize= fout.getChannel().transferFrom(
					checkChannel, 0, readSize);

			fout.close();

			String checkSum = checkChannel.getCheckSum();

			Validate.isTrue(xferSize == readSize,"xfer says "+xferSize+" header says "+readSize);

			String checkSumFromRequest = request
					.getHeader(FileSyncConstants.HttpReqestHeaderNames.CHECK_SUM);

			if (LOG.isTraceEnabled()){
				LOG.trace("sent checksum "+checkSumFromRequest+" calculated "+checkSum);
			}

			if (!StringUtils.equals(checkSum, checkSumFromRequest)) {
				response.setStatus(400);
				response.getWriter().print("Checksum is not as expected.");
				return;
			}

			// finally, rename to target file name
			// This was validated above and is only being renamed to remove the .tmp at the end.
			boolean reName=dest.renameTo(new File(absDestFileName));

			sw.stop();

			LOG.info("took "+sw.getTime()+" milliseconds to xfer "+readSize+" socket to disk, file "+dest.getAbsolutePath());

			if (!reName){
				response.setStatus(400);
				response.getWriter().print("target file could not be renamed");
				LOG.info("could not re-name "+dest.getAbsolutePath()+" to final name, sorry bro.");
				return;
			}

			response.setStatus(200);
			response.getWriter().print("Success.");
	}

	public void setConf(HttpFileRecieverConf conf) {
		this.conf = conf;
	}

	public HttpFileRecieverConf getConf() {
		return conf;
	}

	/**
	 * @return the datacenter
	 */
	public ApplicationDatacenter getDatacenter() {
		return currentDatacenter;
	}

	/**
	 * @param fileSyncUtil
	 *            the fileSyncUtil to set
	 */
	public void setFileSyncClient(FileSyncClient fileSyncClient) {
		this.fileSyncClient = fileSyncClient;
	}

	public void setUseMmio(boolean useMmio) {
		this.useMmio = useMmio;
	}

	/**
	 * @return the fileNameUtils
	 */
	public FileNameUtils getFileNameUtils() {
		return fileNameUtils;
	}

	/**
	 * @param fileNameUtils the fileNameUtils to set
	 */
	public void setFileNameUtils(FileNameUtils fileNameUtils) {
		this.fileNameUtils = fileNameUtils;
	}

	public void setTargetFilePrefix(String targetFilePrefix) {
		this.targetFilePrefix = targetFilePrefix;
	}
}
