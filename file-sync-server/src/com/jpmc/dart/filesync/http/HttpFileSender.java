package com.jpmc.dart.filesync.http;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.jpmc.dart.commons.util.StringBuilderUtils;
import com.jpmc.dart.filesync.constants.FileSyncConstants;
import com.jpmc.dart.utils.PerformanceMessagePublisher;

public class HttpFileSender implements ApplicationContextAware, InitializingBean {
	private static final Log LOG = LogFactory.getLog(HttpFileSender.class);
	private String userName;
	private String passkey;
	private int retryCount = 10;
	private ApplicationContext context;
	private ThreadLocal<HttpClient> client = new ThreadLocal<HttpClient>();
	private ThreadLocal<ByteBuffer> reusableSendBuffer = new ThreadLocal<ByteBuffer>();

	private String targetUrl;

	private String pingUrl;
	private String fileSendUrl;
	private String fileSizeUrl;

	public static final String RESPONSE_TEXT_KEY = "RESPONSE_TEXT_KEY";

	private PerformanceMessagePublisher performanceMessagePublisher;

	@Override
	public void afterPropertiesSet() throws Exception {
		StringBuilder workBuffer=new StringBuilder();
		workBuffer.append(targetUrl);
		if (StringBuilderUtils.endsWith(workBuffer, "/")){
			workBuffer.deleteCharAt(workBuffer.length()-1);
		}
		String base=workBuffer.toString();

		pingUrl=base+"/ping";
		fileSizeUrl=base+"/fileSizeCheck";
		fileSendUrl=base+"/fileSync";
	}


	public long  getTargetFileSize(String url,String relativePath, StringBuilder workBuffer) {
		try {

			//url =url.replace("/fileSync","/fileSizeCheck");
			workBuffer.setLength(0);
			workBuffer.append(fileSizeUrl);

			// try to turn \ into / for the urls in a heap friendly way
			workBuffer.append(relativePath);

			// replace winders slash with the correct slash
			StringBuilderUtils.replaceString(workBuffer, "\\", "/");

			// remove any double path markers
			StringBuilderUtils.replaceString(workBuffer, "//", "/");

			GetMethod head  = new GetMethod(workBuffer.toString());
			if (client.get()==null){
				client.set((HttpClient)context.getBean("httpClient"));
			}

			int ret=0;

			try {
				ret =client.get().executeMethod(head);
			} catch (Exception e){
				// exception, get another connection and try again
				client.set((HttpClient)context.getBean("httpClient"));
				ret =client.get().executeMethod(head);
			}

			if (ret !=200){
				LOG.info("return is "+ret+" for url "+url);
				return -1;
			}

			return Long.parseLong(StringUtils.trim(head.getResponseBodyAsString()));

		} catch (Exception e){
			LOG.warn("caught exception trying to get file size on target",e);
		}

		return -1;
	}


	public boolean pingTargetServer() {
		try {
			GetMethod head  = new GetMethod(pingUrl);
			if (client.get()==null){
				client.set((HttpClient)context.getBean("httpClient"));
			}

			int ret=0;

			try {
				ret =client.get().executeMethod(head);
			} catch (Exception e){
				// exception, get another connection and try again
				client.set((HttpClient)context.getBean("httpClient"));
				ret =client.get().executeMethod(head);
			}

			if (ret!=200){
				LOG.info("return is "+ret+" for url "+pingUrl);
			}

			return (ret ==200);
		} catch (Exception e){
			LOG.warn("caught exception trying to ping target",e);
		}


		return false;
	}

	public int sendFiles(final Map<String, String> headerData,
			Map<String, String> response,
			RandomAccessFile file,
			boolean useNio, UUID correlationId) throws Exception {

		StopWatch sw = new StopWatch();

		sw.start();
		HttpMethodRetryHandler retryHandler = new DefaultHttpMethodRetryHandler(
				retryCount, true);
		PostMethod method = new PostMethod(fileSendUrl);
		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
				retryHandler);


		if(useNio){
			if (reusableSendBuffer.get()==null){
				reusableSendBuffer.set(ByteBuffer.allocate(NioFileRequestEntity.SEND_BUFFER));
			}
		}

		method.setRequestEntity(
				new NioFileRequestEntity(
						file,"binary/octet-stream",reusableSendBuffer.get())
		);

		setRequetHeader(method, headerData);

		if (client.get()==null){
			client.set((HttpClient)context.getBean("httpClient"));
		}

		try {
			client.get().executeMethod(method);
		} catch (Exception e){
			// exception, get another connection and try again
			client.set((HttpClient)context.getBean("httpClient"));
			client.get().executeMethod(method);
		}

		Header[] responseHeaders = method.getResponseHeaders();
		for (int i = 0; i < responseHeaders.length; i++) {
			response.put(responseHeaders[i].getName(),
					responseHeaders[i].getValue());
		}
		response.put(RESPONSE_TEXT_KEY, IOUtils.toString(method.getResponseBodyAsStream()));

		sw.stop();

		return method.getStatusCode();
	}

	private void setRequetHeader(final HttpMethod method,
			final Map<String, String> additionalData) {

		for (String headerName : additionalData.keySet()) {
			method.setRequestHeader(headerName, additionalData.get(headerName));
		}
		method.setRequestHeader(
				FileSyncConstants.HttpReqestHeaderNames.PASSKEY, passkey);
	}
	
	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName
	 *            the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the passkey
	 */
	public String getPasskey() {
		return passkey;
	}

	/**
	 * @param passkey
	 *            the passkey to set
	 */
	public void setPasskey(String passkey) {
		this.passkey = passkey;
	}

	/**
	 * @return the retryCount
	 */
	public int getRetryCount() {
		return retryCount;
	}

	/**
	 * @param retryCount the retryCount to set
	 */
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context=applicationContext;
	}

	public void setTargetUrl(String baseUrl) {
		this.targetUrl = baseUrl;
	}

	public void setPerformanceMessagePublisher(
			PerformanceMessagePublisher performanceMessagePublisher) {
		this.performanceMessagePublisher = performanceMessagePublisher;
	}
}