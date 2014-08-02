package com.jpmc.dart.coreservices.filefinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.jamonapi.MonitorFactory;
import com.jpmc.cto.dart.exception.DetailedFileNotFoundException;
import com.jpmc.dart.commons.image.CheckImage;
import com.jpmc.dart.commons.image.CheckImageV1;
import com.jpmc.dart.commons.image.ImageRepoClient;
import com.jpmc.dart.commons.util.FileNameUtils;
import com.jpmc.dart.coreservices.util.ZeroCopyByteArrayOutputStream;
import com.jpmc.dart.dao.jdbc.DebitDao;
import com.jpmc.dart.dao.jdbc.FileDao;
import com.jpmc.dart.filesync.client.FileSyncClient;
import com.jpmc.dart.helpers.NioFileHelpers;
import com.jpmc.vpc.model.dart.check.DartDebit;
import com.jpmc.vpc.model.dart.check.DartFile;
import com.jpmc.vpc.model.dart.type.CategoryType;
import com.jpmc.vpc.model.exception.VpcException;

public class CoreImageClient implements InitializingBean {
	public static enum ImgType {
		BW,
		GREY
	}

	public static final int HASH_LEN = 20;

	private static final String IMAGE_DIR_DATE_FORMAT = "yyyyMMdd";

	private static final String DIGEST_ALGORITHM = "SHA-1";

	private static FastDateFormat IMAGE_DATE_DIR_FAST_DATE_FORMAT = FastDateFormat
			.getInstance(IMAGE_DIR_DATE_FORMAT);

	private static final Log LOG = LogFactory.getLog(CoreImageClient.class);

	private ImageRepoClient nastyClient;

	private FileDao fileDao;

	private FileNameUtils fileNameUtils;

	private FileFinderClient fileFinder;

	private FileSyncClient fileSyncClient;

	private String baseImgDir;

	private DebitDao debitDao;

	private ThreadLocal<ZeroCopyByteArrayOutputStream> outputBuffer = new ThreadLocal<ZeroCopyByteArrayOutputStream>();

	// TODO: change this to true for 2.2.15
	private boolean lookForNew = true;

	// use soft references to hold the byte[] values so they get cleared if the
	// JVM gets low on memory.
	private ConcurrentMap<String, SoftReference<byte[]>> readAheadCache;

	private static ThreadLocal<MessageDigest> digester = new ThreadLocal<MessageDigest>() {
		@Override
		protected MessageDigest initialValue() {
			try {
				return MessageDigest.getInstance(DIGEST_ALGORITHM);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalArgumentException(e);
			}
		}
	};

	@Override
	public void afterPropertiesSet() throws Exception {
		StringBuilder buff = new StringBuilder();
		buff.append(fileNameUtils.getBaseDir());
		buff.append(File.separatorChar);
		buff.append("images");
		buff.append(File.separatorChar);
		baseImgDir = buff.toString();

		readAheadCache = new ConcurrentLinkedHashMap.Builder<String, SoftReference<byte[]>>()
				.maximumWeightedCapacity(1024 * 1024).build();
	}

	static byte[] getDigestHash(final byte[] bytes) {
		if (bytes != null && bytes.length > 0) {
			MessageDigest digest = digester.get();
			byte[] hash = null;
			byte[] testHash = null;

			int count = 0;
			do {
				hash = digest.digest(bytes);
				testHash = digest.digest(bytes);

				if (!Arrays.equals(hash, testHash)) {
					String message = "Hashes not equal!\n"
							+ ArrayUtils.toString(hash) + "\n"
							+ ArrayUtils.toString(testHash);
					if (count < 3) {
						LOG.warn(message);
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							LOG.warn("Thread interrupted", e);
						}
					} else {
						// Can't do this right now as the JVM will halt if we do
						// :(
						// throw new IllegalStateException(message);
						LOG.error(message);
					}
				} else {
					break;
				}

				count++;
			} while (count < 3);

			return hash;
		}

		return new byte[HASH_LEN];
	}

	public void putImageFile(UUID itemId, Date procDate, byte frontImg[], byte backImage[],
			byte frontGrey[], byte backGrey[]) throws Exception {
		FastDateFormat fdf = FastDateFormat.getInstance(IMAGE_DIR_DATE_FORMAT);

		StringBuilder targetDir = new StringBuilder(baseImgDir)
				.append(fdf.format(procDate))
				.append(File.separatorChar)
				.append(itemId.toString().substring(0, 3))
				.append(File.separator)
				.append(itemId.toString());

		StringBuilder file = new StringBuilder(targetDir);

		if (outputBuffer.get() == null) {
			outputBuffer.set(new ZeroCopyByteArrayOutputStream());
		}

		if ((frontImg != null) && (backImage != null)) {
			// first, the B&W image
			CheckImageV1 image = new CheckImageV1();
			image.setImageFront(frontImg);
			image.setImageBack(backImage);
			image.setFrontHash(getDigestHash(frontImg));
			image.setBackHash(getDigestHash(backImage));

			file.append(".bw.img");

			File target = new File(file.toString());
			target.getParentFile().mkdirs();

			ZeroCopyByteArrayOutputStream zout = outputBuffer.get();
			zout.reset();
			CheckImageV1.write(image, zout);
			NioFileHelpers.writeFileWithRetry(target,
					ByteBuffer.wrap(zout.toByteArray(), 0, zout.size()));

			fileSyncClient.startFileSync(target.getAbsolutePath());
		}

		if ((frontGrey != null) && (backGrey != null)) {
			// first, the B&W image
			CheckImageV1 image = new CheckImageV1();
			image.setImageFront(frontGrey);
			image.setImageBack(backGrey);
			image.setFrontHash(getDigestHash(frontGrey));
			image.setBackHash(getDigestHash(backGrey));

			file.setLength(0);
			file.append(targetDir);
			file.append(".grey.img");

			File target = new File(file.toString());
			target.getParentFile().mkdirs();

			ZeroCopyByteArrayOutputStream zout = outputBuffer.get();
			zout.reset();
			CheckImageV1.write(image, zout);
			NioFileHelpers.writeFileWithRetry(target,
					ByteBuffer.wrap(zout.toByteArray(), 0, zout.size()));

			fileSyncClient.startFileSync(target.getAbsolutePath());
		}
	}

	public CheckImage getImage(UUID debitId, UUID fileId, Date procDate, ImgType type)
			throws Exception {
		Validate.isTrue(procDate != null);
		DartDebit debit = debitDao.findById(debitId, procDate);

		if (debit == null) {
			// uh, take a guess!
			debit = new DartDebit();
			debit.setId(debitId);
			debit.setFileId(fileId);
			debit.setProcessDate(procDate);
		}
		return getImage(debit, type);
	}

	public CheckImageV1 getImgFromFile(File file) throws Exception {
		return CheckImageV1.parse(FileUtils.readFileToByteArray(file));
	}

	/**
	 * Get the image! Seems simple but more complex than you might think....
	 * 
	 * Approach taken:
	 * 
	 * <ul>
	 * <li>If not found then look for the image on disk using the DEBIT passed
	 * in. The location is : imageroot/date/first 3 chars if item UUID/item
	 * id.(bw or grey)
	 * 
	 * <li>Look at the ZIP file first. Good transactions always go here and DART
	 * has many more GOOD transactions by far. But - only look in the ZIP file
	 * for good debits. This gets the most common path handled first
	 * 
	 * <li>If not found and there is a match id on the debit use the same method
	 * as the previous step but use the match id instead
	 * 
	 * <li>Finally use the legacy method of looking for the image on the disk.
	 * 
	 * </ul>
	 * 
	 * @param debit
	 * @param type
	 * @return
	 * @throws Exception
	 */
	public CheckImage getImage(DartDebit debit, ImgType type) throws Exception {
		if (debit == null) {
			return null;
		}

		UUID itemId = debit.getId();
		Date procDate = debit.getProcessDate();

		StringBuilder readNameBuffer = new StringBuilder();

		boolean bandw = true;

		if (type == ImgType.GREY) {
			bandw = false;
		}

		// Only want to go to the ZIP file for GOOD items. Divert/Rescan needs
		// to be on disk - and - the cache will make it where you get
		// the wrong image for the debit.
		if (CategoryType.GOOD.equals(debit.getCategory())
				|| CategoryType.MANUAL.equals(debit.getCategory())) {
			File dartFile = getFilePath(debit, readNameBuffer);

			// TODO: comment out for now...
			// Validate.isTrue(dartFile!=null);

			if (dartFile != null) {
				if (bandw) {
					CheckImage imgFromFile = getImageFromZipFile(dartFile, debit.getFileId(),
							debit.getImgFrontName(), debit.getImgBackName());
					if (imgFromFile != null) {
						return imgFromFile;
					}
				} else {
					CheckImage imgFromFile = getImageFromZipFile(dartFile, debit.getFileId(),
							debit.getViewerImageFront(), debit.getViewerImageBack());
					if (imgFromFile != null) {
						return imgFromFile;
					}
				}
			} else {
				LOG.warn("warning.... can't find real file for debit " + debit.getId()
						+ " proc date " + debit.getProcessDate()
						+ " we should probably try to figure out why...");
			}
		}

		if (lookForNew) {

			// Look for the image in the file system under the UUID sub dir
			// format.
			String itemIdStr = itemId.toString();

			readNameBuffer.append(baseImgDir);
			readNameBuffer.append(IMAGE_DATE_DIR_FAST_DATE_FORMAT.format(procDate));
			readNameBuffer.append(File.separator);
			readNameBuffer.append(itemIdStr.substring(0, 3));
			readNameBuffer.append(File.separator);
			readNameBuffer.append(itemIdStr);

			if (bandw) {
				readNameBuffer.append(".bw.img");
			} else {
				readNameBuffer.append(".grey.img");
			}

			File target = fileFinder.getFile(readNameBuffer.toString());
			if (target != null) {
				return getImgFromFile(target);
			}

			// If we haven't found it the normal way we need to look for it
			// using the MATCH id.
			if (debit.getMatchUUID() != null) {
				LOG.info("look for image for matched item " + debit.getId() + " matched to "
						+ debit.getMatchUUID());

				// look for the rescan, we don't have the proc date
				CheckImage fromRescan = getMatchedItemImageFromFs(debit.getMatchUUID(),
						readNameBuffer, IMAGE_DATE_DIR_FAST_DATE_FORMAT, bandw);
				if (fromRescan != null) {
					return fromRescan;
				}
			}
			LOG.warn("warning.... used nasty old client to find images for " + debit.getId()
					+ " proc date " + debit.getProcessDate()
					+ " we should probably try to figure out why...");
		}

		// delegate to old stuff...
		return tryLegacyImageClient(debit, bandw);
	}

	/**
	 * @param debit
	 * @param bandw
	 * @return
	 * @throws VpcException
	 * @throws IOException
	 */
	protected CheckImage tryLegacyImageClient(DartDebit debit, boolean bandw)
			throws Exception {

		CheckImage returnValue = null;

		try {
			if (bandw) {
				returnValue = nastyClient.findBwImage(debit);
			}
			else {
				returnValue = nastyClient.findGreyImage(debit);
			}

		} catch (Exception ex) {
			LOG.warn("Caught Exception while using legacy client for debit id " + debit.getId(), ex);
			if (ex instanceof DetailedFileNotFoundException) {
				DetailedFileNotFoundException dfnfe = (DetailedFileNotFoundException) ex;
				File imageFile = dfnfe.getFileAttempted();
				LOG.warn("Attempting to find the file " + imageFile + " in the other datacenters.");

				// Let's try the other data center
				File target = fileFinder.getFile(imageFile.getAbsolutePath());
				if (target == null || target.exists() == false) {
					LOG.error("Image file was not found via Core Services.  File does not exist:  "
							+ imageFile);
					throw dfnfe.getFnfe();
				}
				returnValue = this.getImgFromFile(target);
			}
			else {
				throw ex;
			}

		}
		return returnValue;
	}

	/**
	 * TODO: Why is this public?
	 * 
	 * @param fdf
	 *            Not currently used.
	 */
	public CheckImage getMatchedItemImageFromFs(UUID matchId, StringBuilder readNameBuffer,
			FastDateFormat fdf, boolean bw) throws Exception {
		// if the rescan isn't the same proc date, see if you can find it under
		// one of the other proc dates
		readNameBuffer.setLength(0);
		readNameBuffer.append(File.separator);
		readNameBuffer.append(matchId.toString().substring(0, 3));
		readNameBuffer.append(File.separator);
		readNameBuffer.append(matchId.toString());

		if (bw) {
			readNameBuffer.append(".bw.img");
		} else {
			readNameBuffer.append(".grey.img");
		}

		File imgFile = fileFinder.findFileRelativePathUnderRootPaths(new File(baseImgDir),
				readNameBuffer.toString());

		if (imgFile == null) {
			// can't find the image file for the UUID directly so look for the
			// file.
			DartDebit matched = debitDao.getDebitByIdOnly(matchId);

			if (matched != null) {
				DartFile inputFile = fileDao
						.findById(matched.getFileId(), matched.getProcessDate());

				if (inputFile != null) {
					// get the file
					File rescanZipFile = fileFinder.getFile(inputFile.getFileName());
					if (rescanZipFile != null) {
						if (bw) {
							return getImageFromZipFile(rescanZipFile, inputFile.getId(),
									matched.getImgFrontName(), matched.getImgBackName());
						}
						return getImageFromZipFile(rescanZipFile, inputFile.getId(),
								matched.getViewerImageFront(), matched.getViewerImageBack());
					}
				}
			}

		} else {
			return CheckImageV1.parse(imgFile);
		}

		return null;
	}

	/**
	 * @param readNameBuffer
	 *            Not currently used
	 */
	public File getFilePath(DartDebit debit, StringBuilder readNameBuffer) throws Exception {
		// FastDateFormat IMAGE_DATE_DIR_FAST_DATE_FORMAT =
		// FastDateFormat.getInstance(IMAGE_DIR_DATE_FORMAT);

		// look in the database for the file path.
		DartFile dartFile = fileDao.findById(debit.getFileId(), debit.getProcessDate());

		if (dartFile == null) {
			return null;
		}

		File file = fileFinder.getFile(dartFile.getFileName());

		if (file != null) {
			return file;
		}
		return null;
	}

	public CheckImage getImageFromZipFile(File file, UUID fileId, String frontImageName,
			String backImageName) throws Exception {
		String id = fileId.toString();

		MonitorFactory.add("read ahead cache access:", "", 1);

		if ((readAheadCache.containsKey(id + "_" + frontImageName)) &&
				(readAheadCache.containsKey(id + "_" + backImageName))) {
			LOG.debug("Read ahead cache hit for image for File id / image names:" + id + ":"
					+ frontImageName + ":" + backImageName);

			CheckImageV1 image = new CheckImageV1();
			// get the front 7 rear image, but
			SoftReference<byte[]> front = readAheadCache.get(id + "_" + frontImageName);
			SoftReference<byte[]> rear = readAheadCache.get(id + "_" + backImageName);

			if ((front != null) && (front.get() != null)) {
				image.setImageFront(front.get());
			}

			if ((rear != null) && (rear.get() != null)) {
				image.setImageBack(rear.get());
			}

			if ((image.getImageFront() != null) && (image.getImageBack() != null)) {
				MonitorFactory.add("read ahead cache hit:", "", 1);
				return image;
			}
			MonitorFactory.add("read ahead cache miss (when it should hit):", "", 1);
			LOG.debug("Read ahead cache hit for image for File id / image names:" + id + ":"
					+ frontImageName + ":" + backImageName);
			return getImageFromFile(file, fileId, frontImageName, backImageName);
		}
		MonitorFactory.add("read ahead cache miss:", "", 1);

		return getImageFromFile(file, fileId, frontImageName, backImageName);

	}

	public synchronized CheckImage getImageFromFile(File file, UUID fileId, String frontImageName,
			String backImageName) throws Exception {

		String id = fileId.toString();

		// since the read ahead may of populated this, go ahead and check again
		if ((readAheadCache.containsKey(id + "_" + frontImageName)) &&
				(readAheadCache.containsKey(id + "_" + backImageName))) {
			LOG.debug("Read ahead cache hit for image for File id / image names:" + id + ":"
					+ frontImageName + ":" + backImageName);

			CheckImageV1 image = new CheckImageV1();
			// get the front 7 rear image
			image.setImageFront(readAheadCache.get(id + "_" + frontImageName).get());
			image.setImageBack(readAheadCache.get(id + "_" + backImageName).get());

			if ((image.getImageFront() != null) && (image.getImageBack() != null)) {
				MonitorFactory.add("read ahead cache hit second try:", "", 1);
				return image;
			}
		}

		ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
		try {
			// since the zip is already open, do a read ahead
			ZipEntry zen = zin.getNextEntry();
			while (zen != null) {
				if (!zen.getName().endsWith(".xml")) {
					String key = id + "_" + zen.getName();
					if (!readAheadCache.containsKey(key)) {
						LOG.debug("Adding image cache for key :" + key);
						readAheadCache
								.put(key, new SoftReference<byte[]>(IOUtils.toByteArray(zin)));
					} else {
						if (readAheadCache.get(key).get() == null) {
							LOG.debug("Updating image cache for key :" + key);
							readAheadCache.put(key,
									new SoftReference<byte[]>(IOUtils.toByteArray(zin)));
						}
					}
				}
				zen = zin.getNextEntry();
			}
		} finally {
			IOUtils.closeQuietly(zin);
		}

		SoftReference<byte[]> frontRef = readAheadCache.get(id + "_" + frontImageName);
		SoftReference<byte[]> backRef = readAheadCache.get(id + "_" + backImageName);

		if (frontRef == null) {
			return null;
		}

		if (backRef == null) {
			return null;
		}

		CheckImageV1 image = new CheckImageV1();
		// get the front image
		image.setImageFront(frontRef.get());
		image.setImageBack(backRef.get());

		if ((image.getImageFront() == null) || (image.getImageBack() == null)) {
			return null;
		}

		return image;
	}

	public void setFileFinder(FileFinderClient fileFinder) {
		this.fileFinder = fileFinder;
	}

	public void setFileSyncClient(FileSyncClient fileSyncClient) {
		this.fileSyncClient = fileSyncClient;
	}

	public void setFileNameUtils(FileNameUtils fileNameUtils) {
		this.fileNameUtils = fileNameUtils;
	}

	public void setNastyClient(ImageRepoClient nastyClient) {
		this.nastyClient = nastyClient;
	}

	public void setFileDao(FileDao fileDao) {
		this.fileDao = fileDao;
	}

	public void setDebitDao(DebitDao debitDao) {
		this.debitDao = debitDao;
	}

	public void setLookForNew(boolean lookForNew) {
		this.lookForNew = lookForNew;
	}
}
