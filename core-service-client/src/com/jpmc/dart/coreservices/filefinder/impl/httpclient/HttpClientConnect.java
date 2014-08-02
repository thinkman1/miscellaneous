package com.jpmc.dart.coreservices.filefinder.impl.httpclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.Header;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jpmc.dart.commons.util.StringBuilderUtils;
import com.jpmc.dart.coreservices.util.BadStatusCodeException;
import com.jpmc.dart.coreservices.util.HttpWrapper;
import com.jpmc.dart.helpers.ManagedTempFiles;
import com.jpmc.dart.helpers.NioFileHelpers;

public class HttpClientConnect {
	private static final Log LOG = LogFactory.getLog(HttpClientConnect.class);
	private String hostUrl;
	private ThreadLocal<StringBuilder> stringBuilder = new ThreadLocal<StringBuilder>();
	private ManagedTempFiles temps;
	private File workDir;
	private String mypass;
	private HttpWrapper httpWrapper;

	public List<String> lististFiles(String fileUri) throws Exception {
		if (stringBuilder.get() == null) {
			stringBuilder.set(new StringBuilder());
		}

		StringBuilder buff = stringBuilder.get();
		buff.setLength(0);
		buff.append(hostUrl);
		buff.append(fileUri);

		List<String> files = new ArrayList<String>();
		LOG.trace("for archive list call " + buff);

		for (int i = 0; i < 10; i++) {

			BufferedReader reader = null;
			try {
				InputStream stream = httpWrapper.executeGet(buff.toString(), new Header("PASS",
						mypass));
				reader = new BufferedReader(new InputStreamReader(stream));
				String line = reader.readLine();
				while (line != null) {
					files.add(line);
					line = reader.readLine();
				}
				break;
			} catch (BadStatusCodeException e) {
				if (e.getStatusCode() == 404) {
					break;
				}
			} catch (SocketTimeoutException e) {
				LOG.info("socket timeout, skip to the next host", e);
				i = 11;
			} catch (Exception e) {
				LOG.error("caught this exception get file, sleep a little and retry", e);
				TimeUnit.SECONDS.sleep(30);
			} finally {
				IOUtils.closeQuietly(reader);
			}
		}

		return files;
	}

	public File getFile(String fileUri) throws Exception {

		if (stringBuilder.get() == null) {
			stringBuilder.set(new StringBuilder());
		}

		StringBuilder buff = stringBuilder.get();

		buff.setLength(0);
		buff.append(fileUri);

		StringBuilderUtils.replaceString(buff, "//", "/");

		buff.insert(0, hostUrl);

		// once the file object goes out of scope, the temp file will get
		// deleted.
		// yay for weak references!

		LOG.debug("full url is " + buff.toString());

		for (int i = 0; i < 10; i++) {
			InputStream stream = null;
			try {
				stream = httpWrapper.executeGet(buff.toString(), new Header("PASS", mypass));
				File ret = temps.getManagedTempFile("httpclient", "cache", workDir);
				NioFileHelpers.writeFileWithRetry(ret, stream);

				return ret;
			} catch (SocketTimeoutException e) {
				LOG.info("socket timeout, skip to the next host", e);
				i = 11;
			} catch (BadStatusCodeException e) {
				if (e.getStatusCode() == 404) {
					i = 11;
				} else {
					LOG.error("caught this exception get file, sleep a little and retry", e);
					TimeUnit.SECONDS.sleep(5);
				}
			} catch (Exception e) {
				LOG.error("caught this exception get file, sleep a little and retry", e);
				TimeUnit.SECONDS.sleep(5);
			} finally {
				IOUtils.closeQuietly(stream);
			}
		}

		return null;
	}

	public void setHostUrl(String hostUrl) {
		this.hostUrl = hostUrl;
	}

	public void setTemps(ManagedTempFiles temps) {
		this.temps = temps;
	}

	public void setWorkDir(File workDir) {
		this.workDir = workDir;
	}

	public void setMypass(String mypass) {
		this.mypass = mypass;
	}

	public void setHttpWrapper(HttpWrapper httpWrapper) {
		this.httpWrapper = httpWrapper;
	}
}