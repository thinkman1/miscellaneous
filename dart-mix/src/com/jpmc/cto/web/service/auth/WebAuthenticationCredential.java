package com.jpmc.cto.web.service.auth;

import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;

public class WebAuthenticationCredential {
	private static final String AUTH_HEADER_FORMAT = "%s Credential=%s/%s/%s Signature=%s";
	private FastDateFormat df = FastDateFormat.getInstance("yyyyMMdd");
	
	private String namespace;
	private String signature;
	private String accessKey;
	private Date date;
	private String service;

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public Date getDate() {
		return date;
	}

	public String getDateAsStr() {
		return df.format(date);
	}

	public void setDate(Date date) {
		this.date = date;
	}
	
	public void setDateAsStr(String date) {
		try {
			setDate(df.parse(date));
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid Authorization: invalid format");
		}
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}
	
	public String getCachingKey() {
		return service + "_" + accessKey;
	}
	
	public String getAsAuthHeader() {
		return String.format(AUTH_HEADER_FORMAT, namespace, accessKey, getDateAsStr(), service, signature);
	}
	
	public void setValuesFromCredentialString(String credentialStr) {
		if (credentialStr != null) {
			String[] credPieces = credentialStr.split("=");
			if (credPieces.length == 2) {
				String[] creds = credPieces[1].split("/");
				if (creds.length == 3) {
					setAccessKey(creds[0]);
					setDateAsStr(creds[1]);
					// TODO: Trim the semicolon if it's there?
					setService(creds[2]);
				} else {
					throw new IllegalArgumentException("Invalid Authorization: invalid format");
				}
			} else {
				throw new IllegalArgumentException("Invalid Authorization: invalid format");
			}
		} else {
			throw new IllegalArgumentException("Invalid Authorization: invalid format");
		}
	}
	
	public void setSignatureFromSignatureString(String sigStr) {
		if (sigStr != null) {
			String[] sigPieces = sigStr.split("=");
			if (sigPieces.length == 2) {
				setSignature(sigPieces[1]);
			} else {
				throw new IllegalArgumentException("Invalid Authorization: invalid format");
			}
		} else {
			throw new IllegalArgumentException("Invalid Authorization: invalid format");
		}
	}
	
	public void setValuesFromAuthHeader(String authHeader) {
		if (authHeader != null) {
			String[] headerPieces = authHeader.split(" ");
			if (headerPieces.length == 3) {
				setNamespace(headerPieces[0]);
				setValuesFromCredentialString(headerPieces[1]);
				setSignatureFromSignatureString(headerPieces[2]);
			} else {
				throw new IllegalArgumentException("Invalid Authorization: invalid format");
			}
		} else {
			throw new IllegalArgumentException("Invalid Authorization: invalid format");
		}
	}
}
