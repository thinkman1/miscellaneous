package com.free.coreservices.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.free.helpers.ManagedTempFiles;

public class HttpWrapper implements ApplicationContextAware {
	// private static Log LOG = LogFactory.getLog(HttpWrapper.class);
	private ManagedTempFiles temps;
	private String workDirectory;
	private ApplicationContext context;
	private ThreadLocal<HttpClient> localClient = new ThreadLocal<HttpClient>();

	private class UseInputStream extends FileInputStream {
		// since managed temp files keeps track of stuff with weak references,
		// keep a ref around to keep the file from getting deleted out from
		// under the stream.
		@SuppressWarnings("unused")
		private File fileUsed;

		public UseInputStream(File f) throws Exception {
			super(f);
			this.fileUsed = f;
		}

		@Override
		public void close() throws IOException {
			super.close();
			this.fileUsed = null;
		}

	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		context = applicationContext;
	}

	/**
	 * returns a stream that has the results of the call. The returned stream
	 * needs to be closed!
	 * 
	 * @param uri
	 * @return
	 * @throws BadStatusCodeException
	 * @throws Exception
	 */
	public InputStream executeGet(String uri, Header... headers) throws BadStatusCodeException,
			Exception {

		HttpMethodRetryHandler retryHandler = new DefaultHttpMethodRetryHandler(10, true);

		HttpMethodParams parms = new HttpMethodParams();
		parms.setParameter(HttpMethodParams.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

		GetMethod meth = new GetMethod(uri.toString());
		meth.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryHandler);
		meth.setParams(parms);

		if (headers != null) {
			for (Header header : headers) {
				meth.addRequestHeader(header);
			}
		}

		if (localClient.get() == null) {
			localClient.set((HttpClient) context.getBean("httpClient"));
		}

		try {
			localClient.get().executeMethod(meth);
			if (meth.getStatusCode() == 200) {
				// dump to a file
				File tempFIle = temps.getManagedTempFile("http", "return", new File(workDirectory));
				FileOutputStream fout = new FileOutputStream(tempFIle);
				IOUtils.copy(meth.getResponseBodyAsStream(), fout);
				IOUtils.closeQuietly(fout);
				return new UseInputStream(tempFIle);
			}

			throw new BadStatusCodeException(meth.getStatusCode());
		} catch (Exception e) {
			throw e;
		} finally {
			meth.releaseConnection();
		}
	}

	public void setTemps(ManagedTempFiles temps) {
		this.temps = temps;
	}

	public void setWorkDirectory(String workDirectory) {
		this.workDirectory = workDirectory;
	}
}
