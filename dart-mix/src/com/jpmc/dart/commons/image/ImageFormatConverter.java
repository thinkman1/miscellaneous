package com.jpmc.dart.commons.image;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.JPEGDecodeParam;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codec.TIFFField;
import com.sun.media.jai.codecimpl.JPEGImageDecoder;
import com.sun.media.jai.codecimpl.TIFFImageEncoder;

/**
 * This class is used to convert Image format
 *
 * @author R502440
 *
 */
public class ImageFormatConverter {

	private static final String JPEG_EXT = ".jpeg";

	private static final String JPG_EXT = ".jpg";

	private static final String TIFF_EXT = ".tif";

	/**
	 * Convert JPEG format to TIFF format, and make the image black and white
	 *
	 * @param jpeg
	 *            - JPEG file needs to be converted
	 * @return
	 * @throws Exception
	 */
	public static File convertFromJPEGToBwTIFF(File jpeg) throws Exception {
		return convertFromJPEGToBwTIFF(jpeg, StringUtils.EMPTY);
	}

	/**
	 * Convert JPEG format to TIFF format, and make the image black and white
	 *
	 * @param jpeg
	 *            - JPEG file needs to be converted
	 * @param location
	 *            - absolute directory path to keep tiff file
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static File convertFromJPEGToBwTIFF(File jpeg, String location) throws FileNotFoundException, IOException {
		if (jpeg == null) {
			return null;
		}

		File tiff = null;
		OutputStream tiffOps = null;
		FileInputStream fis = null;
		ByteArraySeekableStream bass = null;

		try {
			fis = new FileInputStream(jpeg);
			byte[] image = IOUtils.toByteArray(fis);

			bass = new ByteArraySeekableStream(image);
			JPEGImageDecoder jpegDecoder = new JPEGImageDecoder(bass,
					new JPEGDecodeParam());

			RenderedImage ri = jpegDecoder.decodeAsRenderedImage();
			BufferedImage bi = convertRenderedImage(ri);

			TIFFEncodeParam param = composeTiffParam(ri);

			tiff = createTifImg(jpeg, location);
			tiffOps = new FileOutputStream(tiff);

			TIFFImageEncoder encoder = new TIFFImageEncoder(tiffOps, param);
			encoder.encode(bi);
		} finally {
			IOUtils.closeQuietly(tiffOps);
			IOUtils.closeQuietly(fis);
			IOUtils.closeQuietly(bass);
		}

		return tiff;
	}

	protected static TIFFEncodeParam composeTiffParam(RenderedImage ri) {
		final int XRES_TAG = 282;
		final int YRES_TAG = 283;
		final int RESOLUTION_TAG = 296;
		final int ORIENTATION_TAG = 274;
		// 200 is DPI
		long[] resolution = { 200, 1 };

		TIFFField xResolution = new TIFFField(XRES_TAG, TIFFField.TIFF_RATIONAL, 1, new long[][] { resolution });
		TIFFField yResolution = new TIFFField(YRES_TAG, TIFFField.TIFF_RATIONAL, 1, new long[][] { resolution });
		TIFFField resolutionUnit = new TIFFField(RESOLUTION_TAG, TIFFField.TIFF_SHORT, 1, new char[] { 2 });
		TIFFField orientation = new TIFFField(ORIENTATION_TAG, TIFFField.TIFF_SHORT, 1, new char[] { 1 });

		TIFFEncodeParam param = new TIFFEncodeParam();
		param.setExtraFields(new TIFFField[] { xResolution, yResolution, resolutionUnit, orientation });
		param.setCompression(TIFFEncodeParam.COMPRESSION_GROUP4);
		param.setTileSize(ri.getWidth(), ri.getHeight());
		param.setLittleEndian(true);

		return param;
	}

	/**
	 *
	 * @param img
	 * @return
	 */
	protected static BufferedImage convertRenderedImage(final RenderedImage img) {

		ColorModel cm = img.getColorModel();

		int width = img.getWidth();
		int height = img.getHeight();

		WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
		img.copyData(raster);

		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		String[] keys = img.getPropertyNames();
		if (keys != null) {
			for (int i = 0; i < keys.length; i++) {
				properties.put(keys[i], img.getProperty(keys[i]));
			}
		}

		BufferedImage bi = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);

		byte[] bw = { (byte) 0xff, (byte) 0 };
		IndexColorModel blackAndWhite = new IndexColorModel(1, 2, bw, bw, bw);
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY, blackAndWhite);

		Graphics2D graphics = (Graphics2D) result.getGraphics();
		graphics.drawImage(bi, 0, 0, null);
		graphics.dispose();

		return result;
	}

	/**
	 *
	 * @param jpeg
	 * @param location
	 * @return
	 */
	protected static File createTifImg(File jpeg, String location) {
		String tiffImgName = StringUtils.EMPTY;
		File tiff = null;

		if (StringUtils.endsWith(jpeg.getName(), JPEG_EXT)) {
			if (StringUtils.isEmpty(location)) {
				tiffImgName = StringUtils.replace(jpeg.getAbsolutePath(), JPEG_EXT, TIFF_EXT);
				tiff = new File(tiffImgName);
			} else {
				tiffImgName = StringUtils.replace(jpeg.getName(), JPEG_EXT, TIFF_EXT);
				tiff = new File(location + File.separator + tiffImgName);
			}
		} else {
			if (StringUtils.isEmpty(location)) {
				tiffImgName = StringUtils.replace(jpeg.getAbsolutePath(), JPG_EXT, TIFF_EXT);
				tiff = new File(tiffImgName);
			} else {
				tiffImgName = StringUtils.replace(jpeg.getName(), JPG_EXT, TIFF_EXT);
				tiff = new File(location + File.separator + tiffImgName);
			}
		}

		return tiff;
	}
}
