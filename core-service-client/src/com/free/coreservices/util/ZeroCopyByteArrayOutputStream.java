package com.free.coreservices.util;

import java.io.ByteArrayOutputStream;

/**
 * use this to avoid the Arrays.copyOf(buf, count) ByteArrayOutputStream has.
 * this provides direct access to the buffer, just make sure you use size() to
 * know how many bytes. Also keep in mind this very thread UNSAFE.
 * 
 * @author e001668
 * 
 */
public class ZeroCopyByteArrayOutputStream extends ByteArrayOutputStream {
	@Override
	public byte[] toByteArray() {
		return buf;
	}

}
