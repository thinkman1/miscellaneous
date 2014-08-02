package com.jpmc.cto.dart.extract.xml.utils;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import com.jpmc.cto.framework.exception.ApplicationException;
import com.jpmc.dart.commons.ssh.PwdAndKbInterUserInfo;

/**
 * EDW Zip file sender - sftp
 *
 * @author r502440
 *
 */
public class FileSenderSftp implements FileSender {
	private static final Log LOG = LogFactory.getLog(FileSenderSftp.class);

	/**
	 * full path of edw file location e.g. 'sftp://hostname/somepath'
	 */
	private String uri;
	private int numTries = 1;

	public FileSenderSftp() {
	}

	public FileSenderSftp(final String uri, final int numTries) {
		this.numTries = numTries;
		this.uri = uri;
	}

	/**
	 * @see com.jpmc.cto.FileSenderSftp.extract.service.util.FileSender#sendFiles(java.io.File)
	 */
	@Override
	public void sendFiles(File file) throws ApplicationException {

		if (file == null) {
			LOG.error("EDW Sender: File cannot be found.");
			throw new ApplicationException("EDW Sender: File cannot be found.");
		}

		boolean sendSucceed = false;
		Exception excep = null;

		for (int i = 0; i < numTries; i++) {

			try {
				FileSystemManager manager = VFS.getManager();
				FileObject zipToSend = manager.resolveFile(file.getAbsolutePath());

				LOG.info("Get original file " + file.getAbsolutePath() + " successfully.");

				String zipTargetPath = this.buildConnectionString(uri, file.getName());
				FileObject remoteFile = manager.resolveFile(zipTargetPath, this.createDefaultOptions());

				LOG.info("Resolve remote file " + zipTargetPath + " successfully.");

				remoteFile.copyFrom(zipToSend, Selectors.SELECT_SELF);

				LOG.info("Sent file " + file.getAbsolutePath() + " to " + this.getUri() + " successfully.");

				sendSucceed = true;
				break;

			} catch (Exception ex) {
				LOG.warn("Got exception while trying to send file to EDW, " + (i + 1) + " time", ex);
				sendSucceed = false;
				excep = ex;

				try {
					/** sleep before re-try */
					Thread.sleep(500);
				} catch (InterruptedException ie) {
					LOG.warn("Interrupted", ie);
					Thread.currentThread().interrupt();
					break;
				}
			}
		}

		if (!sendSucceed) {
			throw new ApplicationException("Failed to send " + file.getName() + " after " + numTries + " retries.",
					excep);
		}
	}

	/**
	 *
	 * @param uri
	 * @param edwZipName
	 * @return
	 */
	private String buildConnectionString(String uri, String edwZipName) {
		return uri + File.separator + edwZipName;
	}

	private FileSystemOptions createDefaultOptions() throws FileSystemException {
		FileSystemOptions options = new FileSystemOptions();

		/**
		 * SSH Key checking - don't do that
		 */
		SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(options, "no");

		/**
		 * set directory as user home - don't do that
		 */
		SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(options, false);

		/**
		 * Timeout is count by Milliseconds
		 */
		SftpFileSystemConfigBuilder.getInstance().setTimeout(options, Integer.valueOf(50000));

		/**
		 * Set UserInfo
		 */
		SftpFileSystemConfigBuilder.getInstance().setUserInfo(options, new PwdAndKbInterUserInfo(null));

		return options;
	}

	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * @param uri
	 *            the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * @return the numTries
	 */
	public int getNumTries() {
		return numTries;
	}

	/**
	 * @param numTries
	 *            the numTries to set. Must be >= 1. Default is 1 if not set.
	 */
	public void setNumTries(int numTries) {
		if (numTries < 1) {
			throw new IllegalArgumentException("numtries must be >= 1.  Default is 1 if not set.");
		}
		this.numTries = numTries;
	}
}
