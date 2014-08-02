package com.jpmc.cto.web.service.auth;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.jpmc.cto.pcf.services.AuthenticationServiceClient;

public class WebAuthenticationFilter extends OncePerRequestFilter {
	private static transient Log log = LogFactory.getLog(WebAuthenticationFilter.class);
	
	private static final String AUTH_HEADER = "Authorization";
	private static final String SERVICE_NAME = "auth-service";
	private static final ConcurrentSkipListMap<String, String> cache = new ConcurrentSkipListMap<String, String>();
	
	private AuthenticationServiceClient authService;
	private String accessKey;
	private String secretKey;
	private String namespace;

	@Override
	public void initFilterBean() throws ServletException {
		log.info("WebAuthenticationFilter.init()");
		validateNotNull(getAccessKey(), "accessKey");
		validateNotNull(getSecretKey(), "secretKey");
		validateNotNull(getNamespace(), "namespace");
		validateNotNull(getAuthService(), "authService");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		String authHeader = request.getHeader(AUTH_HEADER);
		
		int respStatus = HttpServletResponse.SC_OK;
		try {
			WebAuthenticationCredential cred = new WebAuthenticationCredential();
			if (authHeader != null) {
				String[] headerPieces = authHeader.split(" ");
				if (headerPieces.length == 3) {
					cred.setNamespace(headerPieces[0]);
					cred.setValuesFromCredentialString(headerPieces[1]);
					cred.setSignatureFromSignatureString(headerPieces[2]);

					String sig = getSignature(cred);
					if (!cred.getSignature().equals(sig)) {
						log.info("Signature passed in (" + cred.getSignature() + ") does not match signature from auth service ( " + sig + ")");
						respStatus = HttpServletResponse.SC_FORBIDDEN;
					}
				} else {
					log.info("Authorization header was not correctly formatted. Was '" + authHeader + "' which did not have 3 separate pieces.");
					respStatus = HttpServletResponse.SC_BAD_REQUEST;
				}
			} else {
				log.info("Missing authorization header");
				respStatus = HttpServletResponse.SC_UNAUTHORIZED;
			}
		} catch (Exception e) {
			log.warn("An exception occurred while trying to authorize request.", e);
			respStatus = HttpServletResponse.SC_BAD_REQUEST;
		} finally {
			String errorMsg = null;
			switch (respStatus) {
				case HttpServletResponse.SC_OK:
					log.debug("Valid authorization header found...continuing on to service call.");
					filterChain.doFilter(request, response);
					break;
				case HttpServletResponse.SC_UNAUTHORIZED:
					errorMsg = "Invalid Authorization: missing header";
					break;
				case HttpServletResponse.SC_BAD_REQUEST:
					errorMsg = "Invalid Authorization: invalid format";
					break;
				case HttpServletResponse.SC_FORBIDDEN:
					errorMsg = "Invalid Authorization: incorrect signature";
					break;
				default:
					errorMsg = "Invalid Authorization: unknown exception - code " + respStatus; 
					break;
			}
			if (errorMsg != null) {
				log.error(errorMsg + " - " + authHeader);
				response.setStatus(respStatus);
				response.getOutputStream().write(errorMsg.getBytes());
				response.getOutputStream().flush();
			}
		}
	}

	@Override
	public void destroy() {
		log.info("WebAuthenticationFilter.destroy()");
	}
	
	protected String getSignature(WebAuthenticationCredential cred) {
		if (!cache.containsKey(cred.getCachingKey()) || !cred.getSignature().equals(cache.get(cred.getCachingKey()))) {
			updateKeyInCache(cred);
		}
		return cache.get(cred.getCachingKey());
	}
	
	protected void updateKeyInCache(WebAuthenticationCredential origCred) {
		cache.remove(origCred.getCachingKey());
		
		WebAuthenticationCredential svcCred = new WebAuthenticationCredential();
		svcCred.setAccessKey(accessKey);
		svcCred.setDate(new Date());
		svcCred.setNamespace(namespace);
		svcCred.setService(SERVICE_NAME);
		svcCred.setSignature(WebServiceAuthenticator.getSignature(secretKey, svcCred));
		
		String sig = authService.requestSignature(origCred.getService(), origCred.getNamespace(),
				origCred.getDateAsStr(), origCred.getAccessKey(), svcCred.getAsAuthHeader());
		if (sig != null) {
			log.info("Updated credential cache with new credentials for: " + origCred.getCachingKey());
			cache.put(origCred.getCachingKey(), sig);
		} else {
			log.warn("No signature was returned from the auth service for orig: " + origCred.getAsAuthHeader() + " and service: " + svcCred.getAsAuthHeader());
		}
	}
	
	protected static ConcurrentSkipListMap<String, String> getCache() {
		return cache;
	}
	
	protected void validateNotNull(Object o, String name) {
		if (o == null || "".equals(o.toString().trim())) {
			throw new IllegalArgumentException(name + " must not be null");
		}
	}

	public AuthenticationServiceClient getAuthService() {
		return authService;
	}

	public void setAuthService(AuthenticationServiceClient authService) {
		this.authService = authService;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}
}
