package com.jpmc.dart.filesync.server;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

import org.apache.commons.io.output.NullOutputStream;

public class ChecksumInputStreamFilter extends FilterInputStream {
	// private static final Log LOG =
	// LogFactory.getLog(ChecksumInputStreamFilter.class);

	private CheckedOutputStream checkOut =
			new CheckedOutputStream(new NullOutputStream(), new CRC32());
	private long bytesRead = 0;

	public ChecksumInputStreamFilter(InputStream in) {
		super(in);
	}

	@Override
	public int read() throws IOException {
		int data = super.read();
		checkOut.write(data);
		bytesRead++;
		return data;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int count = super.read(b);
		if (count < 0) {
			return count;
		}
		checkOut.write(b, 0, count);
		bytesRead += count;
		return count;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int count = super.read(b, off, len);
		if (count < 0) {
			return count;
		}
		checkOut.write(b, off, count);
		bytesRead += count;
		return count;
	}

	@Override
	public void close() throws IOException {
		super.close();
		checkOut.close();
	}

	public String getChecksum() {
		return String.valueOf(checkOut.getChecksum().getValue());
	}
}
