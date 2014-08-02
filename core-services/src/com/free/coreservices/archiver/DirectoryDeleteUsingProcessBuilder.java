package com.free.coreservices.archiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This Class is used to delete the directory Using ProcessBuilder
 * @author 308280
 *
 * "borrowed" from dart eod
 *
 */
public class DirectoryDeleteUsingProcessBuilder  {

	protected static final Log LOG = LogFactory.getLog(DirectoryDeleteUsingProcessBuilder.class);

	private static final String OS_NAME_LOWER_CASE = System.getProperty("os.name").toLowerCase();
	private static final String SHELL = "sh";
	private static final String SHELL_ARGS = "-c";
	private static final String RM = "rm -rfv ";

	private static final String CMD = "cmd";
	private static final String SLASH_C = "/C";
	private static final String RM_DIR = "rmdir /S /Q ";
	private static final String RM_FILE = "del /Q  ";

	/**
	 * Need to read our streams!  Otherwise we block.
	 *
	 * @author E217297
	 *
	 */
	private class StreamGobbler extends Thread
	{
	    InputStream is;
	    InputStreamReader isr = null;
    	BufferedReader br = null;

	    StreamGobbler(InputStream is) {
	        this.is = is;
	    }

	    public void run() {
	    	isr = new InputStreamReader(is);
	    	br = new BufferedReader(isr);
	    	LineIterator it = IOUtils.lineIterator(br);
	    	try {
				while (it.hasNext()) {
					String line = it.nextLine();
					LOG.debug("Delete detail:  " + line);
				}
			} catch (IllegalStateException e) {
				// We seem to get the 'stream closed' error....
				LOG.warn(e.getMessage());
			}

	    }

	    public void close() {
	    	IOUtils.closeQuietly(isr);
	    }
	}

	/**
	 * @param recursive Not needed since we are using an OS command
	 */
	public boolean deleteDir(File dir, boolean recursive) throws IOException {
		String dirAbsPath = dir.getAbsolutePath();
		boolean fileDeleted = false;
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		LOG.debug(String.format("working.. on directory %s ", dirAbsPath));

		Process p = null;
		StreamGobbler gobbler = null;
		try {
				ProcessBuilder rmProcess = null;
				// static variable
				if (OS_NAME_LOWER_CASE.indexOf("windows") > -1) {
					if (dir.isFile()){
						rmProcess = new ProcessBuilder(CMD, SLASH_C, RM_FILE+ dir);
					} else {
						rmProcess = new ProcessBuilder(CMD, SLASH_C, RM_DIR + dir);
					}
				} else if (OS_NAME_LOWER_CASE.indexOf("unix") > -1
						   || OS_NAME_LOWER_CASE.indexOf("linux") > -1) {
					rmProcess = new ProcessBuilder(SHELL, SHELL_ARGS, RM + dir);
				}

				rmProcess.redirectErrorStream(true);

				if (rmProcess != null) {
					p = rmProcess.start();

					gobbler = new StreamGobbler(p.getInputStream());
					gobbler.start();

					int returnCode = p.waitFor();

					if (returnCode != 0 && dir.exists()) {
						LOG.info(String.format(
								"Got a non 0 return code and dir still exists %s from rm of %s should try again tomorrow",
								Integer.valueOf(returnCode), dir));
					}
					else {
						fileDeleted = true;
					}
				}
		} catch (InterruptedException e) {
			throw new IOException("I was interrupted", e);
		} finally {
			if (p != null) {
				gobbler.close();
				IOUtils.closeQuietly(p.getInputStream());
				IOUtils.closeQuietly(p.getOutputStream());
				IOUtils.closeQuietly(p.getErrorStream());

				p.destroy();
			}
		}

		stopWatch.stop();
		LOG.debug(String.format("finished work on directory %s.  Time taken:  %s", dirAbsPath, stopWatch.toString()));

		return fileDeleted;
	}
}
