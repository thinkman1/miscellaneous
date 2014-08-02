package com.jpmc.dart.filesync.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.util.zip.CRC32;

/**
 * borrow code from ReadableByteChannelImpl and mangle to do checksums.
 *
 * @author e001668
 *
 */
public class ChecksumChannel extends AbstractInterruptibleChannel implements
		ReadableByteChannel {
	InputStream in;
	private static final int TRANSFER_SIZE = 8192;
	private byte buf[] = new byte[TRANSFER_SIZE];

	CRC32 crc = new CRC32();

	ChecksumChannel() {
	}

	public int read(ByteBuffer dst) throws IOException {
		int len = dst.remaining();
		int totalRead = 0;
		int bytesRead = 0;

		while (totalRead < len) {
			int bytesToRead = Math.min((len - totalRead), TRANSFER_SIZE);
			if ((totalRead > 0) && !(in.available() > 0))
				break; // block at most once
			try {
				begin();
				bytesRead = in.read(buf, 0, bytesToRead);
			} finally {
				end(bytesRead > 0);
			}
			if (bytesRead < 0) {
				break;
			}

			totalRead += bytesRead;

			crc.update(buf, 0, bytesRead);
			dst.put(buf, 0, bytesRead);
		}
		if ((bytesRead < 0) && (totalRead == 0))
			return -1;

		return totalRead;
	}

	@Override
	protected void implCloseChannel() throws IOException {
	}



	public void reset(InputStream in){
		this.in=in;
		crc.reset();
	}

	public String getCheckSum() {
		return String.valueOf(crc.getValue());
	}
}