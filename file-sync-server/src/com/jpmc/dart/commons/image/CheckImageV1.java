package com.jpmc.dart.commons.image;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.jpmc.vpc.model.check.CheckImageConstant;

public class CheckImageV1 implements CheckImage {
	public static final int VERSION = 1;

	private byte[] imageFront;

	private byte[] imageBack;

	private byte[] frontHash;

	private byte[] backHash;

	private boolean valid = true;

	public int getVersion() {
		return VERSION;
	}
	
	/**
	 * @return the imageBack
	 */
	public final byte[] getImageBack() {
		return imageBack;
	}

	/**
	 * @param imageBack
	 *            the imageBack to set
	 */
	public final void setImageBack(final byte[] imageBack) {
		this.imageBack = imageBack;
	}

	/**
	 * @return the imageFront
	 */
	public final byte[] getImageFront() {
		return imageFront;
	}

	/**
	 * @param imageFront
	 *            the imageFront to set
	 */
	public final void setImageFront(final byte[] imageFront) {
		this.imageFront = imageFront;
	}

	/**
	 * @return the hash
	 */
	public final byte[] getFrontHash() {
		return frontHash;
	}

	/**
	 * @param hash
	 *            the hash to set
	 */
	public final void setFrontHash(final byte[] hash) {
		this.frontHash = hash;
	}

	/**
	 * @return the backHash
	 */
	public final byte[] getBackHash() {
		return backHash;
	}

	/**
	 * @param backHash
	 *            the backHash to set
	 */
	public final void setBackHash(final byte[] backHash) {
		this.backHash = backHash;
	}

	public ByteBuffer getBuffer() throws IOException{
		ByteArrayOutputStream byos = new ByteArrayOutputStream();
		write(this, byos);
		return ByteBuffer.wrap(byos.toByteArray());
	}
	
	public static CheckImageV1 parse(File f) throws IOException {
		FileInputStream fin = new FileInputStream(f);
		try {
			return parse(fin);
		} finally {
			IOUtils.closeQuietly(fin);
		}
	}
	
	public static CheckImageV1 parse(InputStream is) throws IOException {
		CheckImageV1 check = new CheckImageV1();

		long count = is.skip(CheckImageConstant.VERSION_LEN);
		checkByteCount(CheckImageConstant.VERSION_LEN, count);

		byte[] frontHash = new byte[CheckImageConstant.HASH_LEN];
		count = is.read(frontHash);
		checkByteCount(CheckImageConstant.HASH_LEN, count);

		byte[] imgLength = new byte[CheckImageConstant.DATA_LEN];
		count = is.read(imgLength);
		checkByteCount(CheckImageConstant.DATA_LEN, count);
		int frontLength = Integer.parseInt(new String(imgLength));

		byte[] frontImage = new byte[frontLength];
		count = 0;
		if (frontLength > 0) {
			count = is.read(frontImage);
		}
		checkByteCount(frontLength, count);

		byte[] backHash = new byte[CheckImageConstant.HASH_LEN];
		count = is.read(backHash);
		checkByteCount(CheckImageConstant.HASH_LEN, count);

		imgLength = new byte[CheckImageConstant.DATA_LEN];
		count = is.read(imgLength);
		checkByteCount(CheckImageConstant.DATA_LEN, count);
		int backLength = Integer.parseInt(new String(imgLength));

		byte[] backImage = new byte[backLength];
		count = 0;
		if (backLength > 0) {
			count = is.read(backImage);
		}
		checkByteCount(backLength, count);

		check.setBackHash(backHash);
		check.setFrontHash(frontHash);
		check.setImageBack(backImage);
		check.setImageFront(frontImage);

		return check;
	}

	public static CheckImageV1 parse(final byte[] bytes) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		return parse(is);
	}

	private static void checkByteCount(long expected, long actual) {
		Validate.isTrue(expected == actual, "Unable to read "
				+ "correct number of bytes", actual);
	}

//	public static byte[] toByteArray(final CheckImageV1 image)
//			throws IOException {
//		ByteArrayOutputStream os = new ByteArrayOutputStream(25000);
//
//		os.write(StringUtils.leftPad("" + CheckImageV1.VERSION,
//		        CheckImageConstant.VERSION_LEN, '0').getBytes());
//
//		os.write(image.getFrontHash());
//		os.write(StringUtils.leftPad("" + image.getImageFront().length,
//		        CheckImageConstant.DATA_LEN, '0').getBytes());
//		os.write(image.getImageFront());
//
//		os.write(image.getBackHash());
//		os.write(StringUtils.leftPad("" + image.getImageBack().length,
//		        CheckImageConstant.DATA_LEN, '0').getBytes());
//		os.write(image.getImageBack());
//
//		return os.toByteArray();
//	}

	public static void write(final CheckImageV1 image, OutputStream os) throws IOException{
		os.write(StringUtils.leftPad("" + CheckImageV1.VERSION,
		        CheckImageConstant.VERSION_LEN, '0').getBytes());

		os.write(image.getFrontHash());
		os.write(StringUtils.leftPad("" + image.getImageFront().length,
		        CheckImageConstant.DATA_LEN, '0').getBytes());
		os.write(image.getImageFront());

		os.write(image.getBackHash());
		os.write(StringUtils.leftPad("" + image.getImageBack().length,
		        CheckImageConstant.DATA_LEN, '0').getBytes());
		os.write(image.getImageBack());
	}
	
	
//	public static void write(final CheckImageV1 image) 
//			throws IOException {
//		ByteArrayOutputStream os = new ByteArrayOutputStream(25000);
//		CheckImageV1.write(image,os);
//	}

	
	/**
	 * @return the valid
	 */
	public final boolean isValid() {
		return valid;
	}

	/**
	 * @param valid
	 *            the valid to set
	 */
	public final void setValid(final boolean valid) {
		this.valid = valid;
	}
}
