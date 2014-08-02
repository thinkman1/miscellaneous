package com.jpmc.dart.coreservices.util;

public class BadStatusCodeException extends Exception {
	private static final long serialVersionUID = 1L;
	int statusCode;

	public BadStatusCodeException(int statusCode){
		this.statusCode=statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}
}
