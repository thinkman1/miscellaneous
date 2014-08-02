package com.free.coreservices.archiver;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * provide a subclass that adapts byte array output stream to act like a growable buffer.  you can append in bulk with put() and access the inner
 * data with getDadta()
 *
 * It is also re-usable.  so we don't have to keep growing the internal buffer;
 *
 * This should help remove the need for lots of small temp buffers.
 * @author e001668
 *
 */
public class ByteArrayOutputStreamAdapted extends ByteArrayOutputStream {
	private int maxSize=-1;

	public ByteArrayOutputStreamAdapted(){

	}

	public ByteArrayOutputStreamAdapted(int size){
		super(size);
	}

	/**
	 * instead of using a ton of temp buffers, do the put in one shot.  resets the stream before the put
	 * @param src
	 * @param bytes
	 * @return
	 * @throws Exception
	 */
	public void put(DataInput src,int bytes) throws IOException{
		reset();
		if ((this.buf.length- this.count)>bytes){
			src.readFully(this.buf,0, bytes);
		} else {
			// bytes won't fit, expand size
			 buf = Arrays.copyOf(buf, Math.max(buf.length << 1, this.buf.length+bytes));
			 src.readFully(this.buf,0, bytes);
		}
		this.count=bytes;
	}

	/**
	 * appends the bytes to the buffer
	 * @param buff
	 * @throws IOException
	 */
	public void put(ByteBuffer buff,int len) throws IOException {
		write(buff.array(),0,len);
	}

	public int getSize() {
		return count;
	}

	public byte[] getData() {
		return this.buf;
	}

	/**
	 * set the size to shink buffer to after put into
	 * @param maxSize
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

}
