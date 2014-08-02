package com.free.coreservices.archiver;

import java.io.ByteArrayInputStream;

public class ZeroCopyByteArrayInputStream extends ByteArrayInputStream {
	public ZeroCopyByteArrayInputStream(byte data[], int limit){
		super(data);
		count=limit;
	}
}
