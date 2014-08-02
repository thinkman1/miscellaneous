package com.jpmc.dart.testweb.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import com.jpmc.dart.testweb.util.AtmZipFileUtils;
import com.jpmc.dart.testweb.util.DartTestWebFileManageUtils;

@Controller
public class DropController {
	private static final Log LOG = LogFactory.getLog(DropController.class);

	@Autowired
	private AtmZipFileUtils atmZipFileUtils;

	@Autowired
	private DartTestWebFileManageUtils fileManageUtils;

	@Autowired
	@Qualifier("landingZoneUrl")
	private String dropUrl;

	@RequestMapping("/drop/home")
	public ModelAndView home() {
		ModelAndView mav = new ModelAndView("Drop.Home");
		return mav;
	}

	@RequestMapping("/drop/uploadAndDrop")
	public void upload(HttpServletRequest request, HttpServletResponse response) throws Exception {
	    MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
	    MultiValueMap<String, MultipartFile> map = multipartRequest.getMultiFileMap();
	    StringBuffer rspStr = new StringBuffer();
		if(map != null) {
			List<MultipartFile> fileList =  map.get("zipFile");
			for(MultipartFile mpf : fileList) {
				File localFile = fileManageUtils.getTempFile(StringUtils.trimAllWhitespace(mpf.getOriginalFilename()), "zip");
				OutputStream out = new FileOutputStream(localFile);
				out.write(mpf.getBytes());
				out.close();

				String statusCode = "Starting";
				String reasonPhrase = "Start dropping file" + mpf.getOriginalFilename();
				LOG.info("******************************" + mpf.getOriginalFilename() + mpf.getSize() + "*******************************");
				try{
					StatusLine statusLine = dropFile(localFile);
					statusCode = String.valueOf(statusLine.getStatusCode());
					reasonPhrase = statusLine.getReasonPhrase();
				} catch(Exception e) {
					statusCode = "Exception";
					reasonPhrase = e.toString();
				}
				rspStr.append(statusCode + "|" + reasonPhrase);
			}
			LOG.info("******************************" + rspStr.toString() + "******************************");
			response.getWriter().write(rspStr.toString());
		}


	}

//     From http-landing-service-web
//	   com.jpmc.dart.landingservice.validation.impl.DartPostFileVerifier
//	/**
//	 * process the Multipart request.
//	 *
//	 * @param request
//	 *            - HttpServletRequest
//	 * @param clientIpAddress
//	 *            client ip address for logging purpose
//	 * @return - List<byte[]>
//	 * @throws Exception
//	 *             if exception occurs
//	 */
//	@SuppressWarnings("unchecked")
//	private List<byte[]> handleMultipartRequest(
//			final HttpServletRequest request, final String clientIpAddress)
//			throws Exception {
//		if (LOG.isTraceEnabled()) {
//			LOG.trace("Entered handleMultipartRequest method");
//		}
//		List<byte[]> files = new ArrayList<byte[]>();
//
//		try {
//			MultipartHttpServletRequest newRequest = (MultipartHttpServletRequest) request;
//			// parse the request and find the files.
//			Iterator<String> fileNames = newRequest.getFileNames();
//			while (fileNames.hasNext()) {
//
//				String fileName = (String) fileNames.next();
//				if (LOG.isDebugEnabled()) {
//					LOG.debug(String.format("fileName :: %s", fileName));
//				}
//				MultipartFile mpf = newRequest.getFile(fileName);
//				files.add(mpf.getBytes());
//			}
//		} catch (Throwable t) {
//
//			throw new Exception(
//					String.format(
//							"Error While reading file from Multipart request. file came from atm %s",
//							clientIpAddress), t);
//		}
//		if (LOG.isTraceEnabled()) {
//			LOG.trace("Exited handleMultipartRequest method successfully");
//		}
//		return files;
//	}


	private StatusLine dropFile(File file) throws Exception  {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(dropUrl);
		FileInputStream fs = null;
		StatusLine statusLine = null;
		try {
			fs = new FileInputStream(file);
			InputStreamEntity reqEntity = new InputStreamEntity(fs, -1);
	        reqEntity.setContentType("binary/octet-stream");
	        reqEntity.setChunked(true);

	        httppost.setEntity(reqEntity);
	        httppost.setHeader("X-eatm-ip","123.45.678");

	        RequestLine requestLine = httppost.getRequestLine();

	        LOG.info("Executing request " + requestLine);

	        HttpResponse response = httpclient.execute(httppost);
	        HttpEntity resEntity = response.getEntity();

	        LOG.info("----------------------------------------");
	        statusLine = response.getStatusLine();

	        LOG.info(statusLine);
	        if (resEntity != null) {
	            LOG.info("Response content length: " + resEntity.getContentLength());
	            LOG.info("Chunked?: " + resEntity.isChunked());
	        }
	        EntityUtils.consume(resEntity);
			}
		finally {
			IOUtils.closeQuietly(fs);
			httpclient.getConnectionManager().shutdown();
		}

        return statusLine;
	}

	/**
	 * @return the loadFileUtils
	 */
	public AtmZipFileUtils getAtmZipFileUtils() {
		return atmZipFileUtils;
	}

	/**
	 * @param loadFileUtils the loadFileUtils to set
	 */
	public void setAtmZipFileUtils(AtmZipFileUtils loadFileUtils) {
		this.atmZipFileUtils = loadFileUtils;
	}

	/**
	 * @return the dropUrl
	 */
	public String getDropUrl() {
		return dropUrl;
	}

	/**
	 * @param dropUrl the dropUrl to set
	 */
	public void setDropUrl(String dropUrl) {
		this.dropUrl = dropUrl;
	}
}
