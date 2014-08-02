package com.free.coreservices.archiver;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.hadoop.io.Writable;

public class ArchiverValue implements Writable{
	private ByteArrayOutputStreamAdapted chunk;
	private static final int VERSION=1;

	public ArchiverValue(){
		chunk = new ByteArrayOutputStreamAdapted();
	}

	public ArchiverValue(int size){
		chunk = new  ByteArrayOutputStreamAdapted(size);
	}

	public void reset() {
		this.chunk.reset();
	}

	public byte[] getBytes(){
		// zero copy method to get data
		return chunk.getData();
	}

	public int getRealSize() {
		return chunk.getSize();
	}

	@Override
	public void readFields(DataInput arg0) throws IOException {
		// version the archive so we don't get stuck if we change the format...
		int version=arg0.readInt();
		int size = arg0.readInt();
		chunk.reset();
		chunk.put(arg0, size);
	}

	@Override
	public void write(DataOutput arg0) throws IOException {
		// version the archive so we don't get stuck if we change the format...
		arg0.writeInt(VERSION);
		arg0.writeInt(chunk.size());
		arg0.write(chunk.getData(),0,chunk.size());
	}


	public ByteArrayInputStream getInputStream(){
		return new ByteArrayInputStream(chunk.getData(),0,chunk.size());
	}

	public void append(byte data[]) throws IOException{
		chunk.write(data);
	}

	public void append(ByteBuffer data,int len) throws IOException{
		chunk.put(data,len);
	}
}
