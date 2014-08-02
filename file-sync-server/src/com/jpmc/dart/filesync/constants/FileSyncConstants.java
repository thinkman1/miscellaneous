package com.jpmc.dart.filesync.constants;

/**
 * This class defines the constants used in http
 * @author w461936
 *
 */
public final class FileSyncConstants {
	
	public static class HttpReqestHeaderNames {
		
		public static final String X_CTO_DART_DATACENTER = "X_CTO_DART_DATACENTER";
		
		public static final String X_CTO_DART_FILENAME = "X_CTO_DART_FILENAME";
		
		public static final String PASSKEY = "PASSKEY";	
		
		public static final String CHECK_SUM = "CHECK_SUM";	
		
		public static final String FILE_SIZE = "FILE_SIZE";
		
	}

	public static class HttpResponseHeaderNames {
		
		public static final String LOCAL_SERVER_NAME = "LOCAL_SERVER_NAME";
		
		public static final String LOCAL_FILE_NAME = "LOCAL_FILE_NAME";
	}
}
