package com.jpmc.cto.web.service.auth;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.time.FastDateFormat;

public class WebServiceAuthenticator {
	
	private static FastDateFormat df = FastDateFormat.getInstance("yyyyMMdd");
	
	public static final String ENCRYPT_ENCODING_UTF8 = "UTF-8";
	public static final String ENCRYPT_ENCODING_UTF16 = "UTF-16";
	public static final String ENCRYPT_ENCODING_UTF32 = "UTF-32";
	public static final String ENCRYPT_ENCODING_ASCII = "US-ASCII";
	public static final String ENCRYPT_ENCODING_ISO = "ISO-8859-1";
	public static final String ENCRYPT_ENCODING_EBCDIC = "Cp1047";
	
	public static final String ENCRYPT_ALGORITHM_SHA1 = "HmacSHA1";
	public static final String ENCRYPT_ALGORITHM_SHA256 = "HmacSHA256";
	public static final String ENCRYPT_ALGORITHM_MD5 = "HmacMD5";
	
	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0]; 
	
	private WebServiceAuthenticator() {
	}
	
	public static String getSignature(String secretKey, WebAuthenticationCredential cred) {
		return getSignature(secretKey, cred.getDate(), cred.getService(), cred.getNamespace());
	}
	
	public static String getSignature(String secretKey, Date date, String service, String namespace) {
		String signature = null;
		try {
			// Handle any "null" parameters by just making them empty strings...
			byte[] dateStr = date == null ? EMPTY_BYTE_ARRAY : df.format(date).getBytes(ENCRYPT_ENCODING_UTF8);
			byte[] sk = secretKey == null ? EMPTY_BYTE_ARRAY : secretKey.getBytes(ENCRYPT_ENCODING_UTF8);
			byte[] svc = service == null ? EMPTY_BYTE_ARRAY : service.getBytes(ENCRYPT_ENCODING_UTF8);
			byte[] ns = namespace == null ? EMPTY_BYTE_ARRAY : namespace.getBytes(ENCRYPT_ENCODING_UTF8);
		
			signature = hex(hmac(hmac(hmac(sk, dateStr), svc), ns));
		} catch (Exception e) {
			signature = null;
		}
		return signature;
	}
	
	protected static String hex(byte[] input) throws UnsupportedEncodingException {
		return hex(input, ENCRYPT_ENCODING_UTF8);
	}
	
	protected static String hex(byte[] input, String encoding) throws UnsupportedEncodingException {
		return new String(Hex.encodeHex(input, true));
	}
	
	protected static byte[] hmac(byte[] initKey, byte[] input) throws UnsupportedEncodingException, GeneralSecurityException {
		return hmac(initKey, input, ENCRYPT_ALGORITHM_SHA256, ENCRYPT_ENCODING_UTF8);
	}
	
	protected static byte[] hmac(byte[] initKey, byte[] input, String algorithm, String encoding) throws GeneralSecurityException {
		Mac mac = Mac.getInstance(algorithm);
		mac.init(new SecretKeySpec(initKey, algorithm));
		return mac.doFinal(input);
	}
}
