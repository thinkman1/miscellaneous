package com.jpmc.dart.filesync.server;

public class HttpFileRecieverConf {
	private String passKey;
	private int writeFileRetryCount;
	private int writeFileTimeInbetweenRetry;
	
	public String getPassKey() {
		return passKey;
	}
	public void setPassKey(String passKey) {
		this.passKey = passKey;
	}
	public int getWriteFileRetryCount() {
		return writeFileRetryCount;
	}
	public void setWriteFileRetryCount(int writeFileRetryCount) {
		this.writeFileRetryCount = writeFileRetryCount;
	}
	public int getWriteFileTimeInbetweenRetry() {
		return writeFileTimeInbetweenRetry;
	}
	public void setWriteFileTimeInbetweenRetry(int writeFileTimeInbetweenRetry) {
		this.writeFileTimeInbetweenRetry = writeFileTimeInbetweenRetry;
	}
}
