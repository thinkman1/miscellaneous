package com.jpmc.dart.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

public class NioFileHelpers {
	public static File getTempFileWithRetry(ManagedTempFiles files, String prefix, String suffix,
			File makeInDirectory) throws Exception {
		File ret = null;
		Exception throwMe = null;

		for (int i = 0; i < 10; i++) {
			try {
				ret = files.getManagedTempFile(prefix, suffix, makeInDirectory);
				return ret;
			} catch (IOException e) {
				TimeUnit.SECONDS.sleep(15);
				// try again...
				throwMe = e;
			}
		}
		throw throwMe;
	}

	public static void writeFileWithRetry(File data, InputStream input, long count)
			throws Exception {
		Exception throwMe = null;
		for (int i = 0; i < 10; i++) {
			RandomAccessFile file = null;
			try {
				file = new RandomAccessFile(data, "rw");
				file.getChannel().transferFrom(Channels.newChannel(input), 0, count);
				return;
			} catch (IOException e) {
				TimeUnit.SECONDS.sleep(15);
				// try again...
				throwMe = e;
			} finally {
				IOUtils.closeQuietly(file);
			}
		}
		throw throwMe;
	}

	public static void writeFileWithRetry(File data, InputStream input) throws Exception {
		Exception throwMe = null;

		long count = 0;
		if (input instanceof FileInputStream) {
			FileInputStream fin = (FileInputStream) input;
			count = fin.getChannel().size();
		}

		for (int i = 0; i < 10; i++) {
			RandomAccessFile file = null;
			try {
				if (count > 0) {
					file = new RandomAccessFile(data, "rw");
					file.getChannel().transferFrom(Channels.newChannel(input), 0, count);
					return;
				}
				FileOutputStream fout = null;
				try {
					fout = new FileOutputStream(data);
					IOUtils.copyLarge(input, fout);
				} finally {
					IOUtils.closeQuietly(fout);
				}

			} catch (IOException e) {
				TimeUnit.SECONDS.sleep(15);
				// try again...
				throwMe = e;
			} finally {
				IOUtils.closeQuietly(file);
			}
		}
		throw throwMe;
	}

	public static void writeFileWithRetry(File data, ByteBuffer buff) throws Exception {
		Exception throwMe = null;
		for (int i = 0; i < 10; i++) {
			RandomAccessFile file = null;
			try {
				file = new RandomAccessFile(data, "rw");
				file.getChannel().write(buff);
				return;
			} catch (IOException e) {
				TimeUnit.SECONDS.sleep(15);
				// try again...
				throwMe = e;
			} finally {
				IOUtils.closeQuietly(file);
			}
		}
		throw throwMe;
	}

	public static void writeFileWithRetry(File target, String data) throws Exception {
		Exception throwMe = null;

		for (int i = 0; i < 10; i++) {
			FileWriter writer = null;
			try {
				writer = new FileWriter(target);
				writer.write(data);
				return;
			} catch (IOException e) {
				TimeUnit.SECONDS.sleep(15);
				// try again...
				throwMe = e;
			} finally {
				IOUtils.closeQuietly(writer);
			}
		}
		throw throwMe;
	}

}
