package com.jpmc.dart.commons.image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.jpmc.dart.commons.util.DartFileUtils;
import com.jpmc.dart.commons.util.DateUtils;
import com.jpmc.dart.commons.util.ProcessDateUtils;
import com.jpmc.dart.dao.jdbc.DebitDao;
import com.jpmc.vpc.commons.hash.StringHash;
import com.jpmc.vpc.dao.jdbc.ConfigurationDao;
import com.jpmc.vpc.model.dart.check.DartDebit;
import com.jpmc.vpc.model.exception.VpcException;

/**
 * Act as a service client for the image repo. All the image related calls
 * should thru this. This class works with PROD NAS for both save and retrieve
 * of image calls.
 *
 * @author Senthilkumar.M
 * @author Satya G
 */
public class ImageRepoClient implements InitializingBean {

   	private static final Log LOG = LogFactory.getLog(ImageRepoClient.class);

	/**
	 * This constant indicates the sub folder that is created under PROC_DATE
	 * folder which will be prefixed with img & ends with a number 1 to 5.
	 */
	public static final String IMGNUM = "img$IMGNUM";

	/**
	 * No.of file folders
	 */
	private static final int NUM_FILE_FOLDERS = 293;

	/**
	 * This property is used internally to indicate that findImage is called for
	 * B/W Image.
	 */
	private static final String BW_IMG = "BW_IMG";

	/**
	 * This property is used internally to indicate that findImage is called for
	 * grey Image.
	 */
	private static final String GREY_IMG = "GREY_IMG";

	public static final String INSERTED_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

	private static final String DOT = ".";

	private static final String IMG_FILE_EXT = "img";

	private DebitDao debitDao;

	private String bwImgRoot = null;
	private String greyImgRoot = null;

	private ProcessDateUtils procDateUtils = null;

	/**
	 * This indicates the no.of retries to make to retrieve image from NAS if
	 * its down. default is 1 attempt made
	 */
	private int retry = 1;

	/**
	 * This indicates the sleep time during each retry attempt made to retrieve
	 * image from NAS if its down. default is 0 milli seconds
	 */
	private long sleepTimeInRetry;

	@Override
	public void afterPropertiesSet() throws Exception {

		Validate.notEmpty(bwImgRoot, "bw image root path must not be empty.");
		Validate.notEmpty(greyImgRoot, "grey image root path must not be empty.");
	}

	/**
	 *
	 * @param configDao
	 *            configuration dao
	 */
	public ImageRepoClient(final ConfigurationDao dao) {
		procDateUtils = new ProcessDateUtils();
		procDateUtils.setConfigDao(dao);
	}

	/**
	 *
	 * @param ds
	 *            data source
	 */
	public ImageRepoClient(final DataSource ds) {

		this(new ConfigurationDao(ds));
	}

	/**
	 * This method is used to return image root. Image root is
	 * <NAS_CONFIG_CONFIGURATION>/<IMG_CONFIG_CONFIGURATION>
	 *
	 * @param nasConfig
	 *            PROD or DR nas configuration
	 * @param imgConfig
	 *            B/W or GREY image configuration
	 * @return image root
	 */
	public String getImageRoot(String nasConfig, String imgConfig) {

		StringBuilder path = new StringBuilder();
		path.append(nasConfig);
		path.append(File.separator);
		path.append(imgConfig);
		return path.toString();
	}

	/**
	 * This method is used to find the B/W image for the given debit.
	 *
	 * @param debit
	 *            debit object com.jpmc.vpc.model.dart.check.DartDebit
	 *
	 * @return check image object if image is found otherwise null
	 * @throws FileNotFoundException
	 *             if B/W image is not found for the given debit
	 * @throws IOException
	 *             if I/O problems occurred in retrieving image or macthId for
	 *             the debit is null
	 */
	public CheckImage findBwImage(final DartDebit debit) throws VpcException,
			IOException {

		return findBwImage(debit.getFileId(), debit.getId(), debit
				.getMatchUUID(), debit.getProcessDate(), debit
				.getInsertedDate());
	}

	/**
	 * This method is used to find the B/W image for the given debit details.
	 *
	 * @param fileId
	 *            file id
	 * @param debitId
	 *            debit id
	 * @param matchId
	 *            match id
	 * @param processDate
	 *            process date
	 * @param insertedDate
	 *            inserted date
	 * @return check image object if image is found otherwise null
	 * @throws FileNotFoundException
	 *             if B/W image is not found for the given debit
	 * @throws IOException
	 *             if I/O problems occurred in retrieving image or macthId for
	 *             the debit is null
	 */
	public CheckImage findBwImage(final UUID fileId, final UUID debitId,
			final UUID matchId, final Date processDate, final Date insertedDate)
			throws VpcException, IOException {

		return findImage(fileId, debitId, matchId, processDate, insertedDate,
				BW_IMG, true);
	}

	/**
	 * This method is used to find the grey scale image for the given debit.
	 *
	 * @param debit
	 *            debit object
	 * @return check image object if image is found otherwise null
	 * @throws FileNotFoundException
	 *             if grey image is not found for the given debit
	 * @throws IOException
	 *             if I/O problems occurred in retrieving image or macthId for
	 *             the debit is null
	 */
	public CheckImage findGreyImage(final DartDebit debit) throws VpcException,
			IOException {

		return findGreyImage(debit.getFileId(), debit.getId(), debit
				.getMatchUUID(), debit.getProcessDate(), debit
				.getInsertedDate());
	}
	
	/**
	 * This method is used to find the grey scale image for the given debit
	 * details.
	 *
	 * @param fileId
	 *            file id
	 * @param debitId
	 *            debit id
	 * @param matchId
	 *            match id
	 * @param processDate
	 *            process date
	 * @param insertedDate
	 *            inserted date
	 * @return check image object if image is found otherwise null
	 * @throws FileNotFoundException
	 *             if grey image is not found for the given debit
	 * @throws IOException
	 *             if I/O problems occurred in retrieving image or macthId for
	 *             the debit is null
	 */
	public CheckImage findGreyImage(final UUID fileId, final UUID debitId,
			final UUID matchId, final Date processDate, final Date insertedDate)
			throws VpcException, IOException {

		return findImage(fileId, debitId, matchId, processDate, insertedDate,
				GREY_IMG, true);
	}

	/**
	 * This method is used to find the image (B/w or grey scale) based on image
	 * type given for the given debit details.
	 *
	 * This method searches for the image first in new directory structure
	 * (based on inserted date) if not found it searches in old directory
	 * structure.
	 *
	 * @param fileId
	 *            file id
	 * @param debitId
	 *            debit id
	 * @param matchId
	 *            match id
	 * @param processDate
	 *            process date
	 * @param insertedDate
	 *            inserted date
	 *
	 * @param imgType
	 *            BW_IMG for B/W image GREY_IMG for grey scale image
	 *
	 * @return check image object if image is found otherwise null
	 * @throws FileNotFoundException
	 *             if image is not found for the given debit
	 *
	 * @throws IOException
	 *             if I/O problems occurred in retrieving image or macthId for
	 *             the debit is null
	 */
	private CheckImage findImage(final UUID fileId, final UUID debitId,
			final UUID matchId, final Date processDate,
			final Date insertedDate, final String imgType, final boolean findMatchDebitImg) throws VpcException,
			IOException {

		CheckImage img = null;
		String errorMessage = null;
		try {
			img = findImageByInsertedDateDirStructure(fileId, debitId,
					matchId, processDate, imgType, insertedDate, findMatchDebitImg);
		} catch (IOException ioe) {
			// Ignore the exception
			errorMessage = String
					.format(
							"Image with fileId: %s, debitId: %s, matchId : %s, insertedDate: %s not found in new directory structure",
							fileId, debitId, (matchId == null ? " "
									: matchId), insertedDate);
			LOG.error(errorMessage, ioe);
			throw ioe;
		}
		return img;
	}

	/**
	 * This method is used to find the image (B/w or grey scale) based on image
	 * type given for the given debit details in new directory structure (based
	 * on inserted date).
	 *
	 * @param fileId
	 *            file id
	 * @param debitId
	 *            debit id
	 * @param matchId
	 *            match id
	 * @param insertedDate
	 *            inserted date
	 *
	 * @param imageType
	 *            BW_IMG for B/W image GREY_IMG for grey scale image
	 * @return check image object if image is found otherwise null
	 * @throws FileNotFoundException
	 *             if image is not found for the given debit
	 *
	 * @throws IOException
	 *             if I/O problems occurred in retrieving image or macthId for
	 *             the debit is null
	 */
	private CheckImage findImageByInsertedDateDirStructure(final UUID fileId,
			final UUID debitId, final UUID matchId, final Date processDate,
			final String imageType, final Date insertedDate, final boolean findMatchDebitImg)
			throws VpcException, IOException {

		CheckImage img = null;
		String errorMessage = null;
		if (BW_IMG.equals(imageType)) {

			try {
				img = findImage(fileId, debitId, matchId, processDate,
						bwImgRoot, imageType, insertedDate, findMatchDebitImg);
			} catch (IOException ioe) {
				errorMessage = String
						.format("Image with fileId: %s, debitId: %s, matchId : %s, processDate: %s not found in prod nas bw root %s",
								fileId, debitId, (matchId == null ? "" : matchId), processDate, bwImgRoot);
				LOG.warn(errorMessage, ioe);
				throw ioe;
			}
		} else {
			try {
				img = findImage(fileId, debitId, matchId, processDate,
						greyImgRoot, imageType, insertedDate, findMatchDebitImg);
			} catch (IOException ioe) {
				errorMessage = String
						.format("Image with fileId: %s, debitId: %s, matchId : %s, processDate: %s not found in prod nas grey root %s",
								fileId, debitId, (matchId == null ? "" : matchId), processDate, greyImgRoot);
				LOG.warn(errorMessage, ioe);
				throw ioe;
			}
		}

		return img;
	}

	/**
	 * This method is used to find the image (B/w or grey scale) based on image
	 * type given for the given debit details in old directory structure (based
	 * on process date).
	 *
	 * @param fileId
	 *            file id
	 * @param debitId
	 *            debit id
	 * @param matchId
	 *            match id
	 * @param processDate
	 *            process date
	 * @param imageType
	 *            BW_IMG for B/W image GREY_IMG for grey scale image
	 * @return check image object if image is found otherwise null
	 * @throws FileNotFoundException
	 *             if image is not found for the given debit
	 * @throws IOException
	 *             if I/O problems occurred in retrieving image or macthId for
	 *             the debit is null
	 * @throws VpcException
	 */
	public CheckImage findImageByProcDateDirStructure(final UUID fileId,
			final UUID debitId, final UUID matchId, final Date processDate,
			final String imageType, final boolean findMatchDebitImg) throws IOException, VpcException {


		CheckImage img = null;
		String errorMessage = null;

		if (BW_IMG.equals(imageType)) {

			String procDateStr = DateUtils.convertDateToString(processDate, DartFileUtils.PROC_DT_FMT);
			try {
				img = findImage(fileId, debitId, matchId, bwImgRoot,
						imageType, procDateStr, findMatchDebitImg);
			} catch (IOException ioe) {

				errorMessage = String
						.format("Image with fileId: %s, debitId: %s, matchId : %s, processDate: %s not found in prod nas bw root %s",
								fileId, debitId, (matchId == null ? "" : matchId), processDate, bwImgRoot);
				LOG.warn(errorMessage, ioe);
				throw ioe;
			}
		} else if (GREY_IMG.equals(imageType)) {

			String procDateStr = DateUtils.convertDateToString(processDate, DartFileUtils.PROC_DT_FMT);
			// check in PROD_NAS first
			try {
				img = findImage(fileId, debitId, matchId, greyImgRoot, imageType,
						procDateStr, findMatchDebitImg);
			} catch (IOException ioe) {
				errorMessage = String
						.format("Image with fileId: %s, debitId: %s, matchId : %s, processDate: %s not found in prod nas grey root %s",
								fileId, debitId, (matchId == null ? "" : matchId), processDate, greyImgRoot);
				LOG.warn(errorMessage, ioe);
				throw ioe;
			}
		}
		return img;
	}
	
	/**
	 * This method is used to find CheckImage object for the given image file
	 * object.
	 *
	 * @param imgFile
	 *            image file
	 * @return check image object if image is found otherwise null
	 * @throws FileNotFoundException
	 *             if image is not found for the given debit
	 */
	public CheckImage findImage(final File imgFile) throws IOException {

		if (LOG.isTraceEnabled()) {
		    LOG.trace(String.format("Retrieving the file %s from the path %s",
					imgFile.getName(), imgFile.getAbsolutePath()));
		}
		for (int i = 1; i <= this.retry; i++) {

			try {
				return ImageUtils.retrieveImage(imgFile);
			} catch (IOException ioe) {
				LOG.warn(String.format("Error retrieving the file %s from the path %s - attempt %d of %d",
										imgFile.getName(), imgFile.getAbsolutePath(),
										Integer.valueOf(i), Integer.valueOf(this.retry)));
				try {
					if (i < retry) {
						Thread.sleep(this.sleepTimeInRetry);
					} else {
						throw ioe;
					}
				} catch (InterruptedException ie) {
					// Restore the interrupted status
					Thread.currentThread().interrupt();
				}
			}
		}
		return null;
	}

	/**
	 * This method is used to return image root that is used for DART. The Dart
	 * image root convention is <IMG_ROOT>/<PROC_DT_STRING>/hh/mm/<0,1,2,3>
	 *
	 * @param fileId
	 *            file id for creating file folder
	 * @param imgRoot
	 *            image root configured in database
	 * @param procDateStr
	 *            process date string
	 * @return dart image root
	 */
	public static String getImageAbsolutePath(final String imgRoot,
			final UUID fileId, final UUID debitId, final Date procDate,
			final Date insertedDate) {

		// this method can be moved to dart-commons
		// or DartImageUtils (if we go for DartImageUtils - check saveImage call
		// documentation within method)
		String absolutePath = getImageAbsolutePath(imgRoot, procDate,
				insertedDate);
		String imgFileName = getImageFile(absolutePath, fileId, debitId);
		return imgFileName;
	}

	/**
	 * This method returns the file name
	 *
	 * @param fileId
	 * @param debitId
	 * @return
	 */
	public static String getImageFile(String imgRoot, final UUID fileId,
			final UUID debitId) {
		StringBuilder imgRootBuilder = new StringBuilder(imgRoot);
		imgRootBuilder.append(fileId.toString());
		imgRootBuilder.append(DOT);
		imgRootBuilder.append(debitId.toString());
		imgRootBuilder.append(DOT);
		imgRootBuilder.append(IMG_FILE_EXT);
		if(LOG.isTraceEnabled()) {
		    LOG.trace("File Name :"+imgRootBuilder.toString());
		}
		return imgRootBuilder.toString();
	}

	/**
	 * This method constructs the path leaving out the filename
	 *
	 * @param imgRoot
	 *            java.lang.String image root
	 * @param insertedDate
	 *            java.util.Date inserted date of the debit
	 * @return java.lang.String
	 */
	public static String getImageAbsolutePath(final String imgRoot,
			final Date procDate, final Date insertedDate) {
		StringBuilder imgRootBldr = new StringBuilder(imgRoot);
		imgRootBldr.append(File.separator);
		imgRootBldr.append(DartFileUtils.getFileRelativePath(procDate,
				insertedDate));
		return imgRootBldr.toString();
	}

	/**
	 *
	 * @param fileId
	 * @param debitId
	 * @param matchId
	 * @param insertedDate
	 * @param imgRoot
	 * @return
	 * @throws VpcException
	 * @throws IOException
	 */
	private CheckImage findImage(final UUID fileId, final UUID debitId,
			final UUID matchId, final Date processDate, final String imgRoot,
			final String imageType,
			final Date insertedDate,
			final boolean findMatchDebitImg) throws VpcException, IOException {

		try {

			// find the debit image
			return findImage(new File(getImageFile(getImageAbsolutePath(
					imgRoot, processDate, insertedDate), fileId, debitId)));
		} catch (FileNotFoundException fnf1) {

			if (!findMatchDebitImg
				|| matchId == null) {

				throw fnf1;
			}
			DartDebit matchedDebit = null;
			try {
				String msg = String
						.format("Image is not found in the repository for fileId::%s, debitId::%s so finding.. using matchId::%s",
								fileId, debitId, matchId);
				if (LOG.isTraceEnabled()) {
				    LOG.trace(msg);
				}

				// for the case of moved or pulled case, the debit will not have
				// images, so get it from the cross referred debit.
				matchedDebit = debitDao.findRescanDebit(matchId, procDateUtils
						.getMinSelectionRangeDate());
				UUID matchedDebitFileId = matchedDebit.getFileId();
				Date matchedDebitProcDate = matchedDebit.getProcessDate();
				Date matchedDebitInsDate = matchedDebit.getInsertedDate();
				if (LOG.isTraceEnabled()) {
				    LOG.trace(String.format(
					    "Retrieving.. image using match debit fileId :: %s, debitId :: %s, processDate :: %s, insertedDate :: %s",
					    matchedDebitFileId, matchId,
					    matchedDebitProcDate,
					    matchedDebitInsDate));
				}
				return findImage(matchedDebitFileId, matchedDebit.getId(), matchedDebit.getMatchUUID(), matchedDebitProcDate, matchedDebit.getInsertedDate(), imageType, false);
				/*return findImage(new File(getImageFile(getImageAbsolutePath(
						imgRoot, matchedDebitProcDate, matchedDebitInsDate),
						matchedDebitFileId, matchId)));*/
			} catch (FileNotFoundException fnf2) {
				String errorMsg = String
						.format("Image is not found in the repository for the cross referred debit whose fileId :: %s, debitId :: %s, procDate :: %s",
								matchedDebit.getFileId(), matchId, matchedDebit.getInsertedDate());
				LOG.error(errorMsg, fnf2);
				throw fnf2;
			}
		}
	}
	
	/**
	 * find the image from the repo
	 *
	 * @param fileId
	 *            file id
	 * @param debitId
	 *            debit id
	 * @param matchId
	 *            match id
	 * @param processDateStr
	 *            process date
	 * @param imageRoot
	 *            image root configured in database
	 * @return check image object if image is found otherwise null
	 * @throws FileNotFoundException
	 *             if image is not found for the given inputs
	 * @throws IOException
	 *             if I/O problems occurred in retrieving image or macthId is
	 *             passed as null
	 * @throws VpcException
	 */
	private CheckImage findImage(final UUID fileId, final UUID debitId,
			final UUID matchId, final String imageRoot, final String imgType, final String procDateStr, final boolean findMatchDebitImg)
			throws IOException, VpcException {
		String dartImgRoot = ImageRepoClient.getDartImageRoot(fileId,
				imageRoot, procDateStr);
		try {

			// find the debit image
			return findImage(fileId, debitId, dartImgRoot);
		} catch (FileNotFoundException fnf1) {

			if (!findMatchDebitImg ||
				matchId == null) {

				throw fnf1;
			}
			DartDebit matchedDebit = null;
			try {
				String msg = String
						.format(
								"Image is not found in the repository for fileId::%s, debitId::%s so finding.. using matchId::%s",
								fileId, debitId, matchId);
				if (LOG.isTraceEnabled()) {
				    LOG.trace(msg);
				}
				// for the case of moved or pulled case, the debit will not have
				// images, so get it from the cross referred debit.
				matchedDebit = debitDao.findRescanDebit(matchId, procDateUtils
						.getMinSelectionRangeDate());
				UUID matchedDebitFileId = matchedDebit.getFileId();
				Date matchedDebitProcDate = matchedDebit.getProcessDate();
				if (LOG.isTraceEnabled()) {
				    LOG.trace(String.format(
					    "Retrieving.. image using match debit fileId :: %s, debitId :: %s, procDate :: %s",
					    matchedDebitFileId, matchId,
					    matchedDebitProcDate));
				}
				return findImage(matchedDebitFileId, matchedDebit.getId(), matchedDebit.getMatchUUID(), matchedDebitProcDate, matchedDebit.getInsertedDate(), imgType, false);
				/*return findImage(matchedDebitFileId, matchId, ImageRepoClient
						.getDartImageRoot(matchedDebitFileId, imageRoot,
								DateUtils.convertDateToString(
										matchedDebitProcDate,
										PROC_DATE_FLDR_FORMAT)));*/
			} catch (FileNotFoundException fnf2) {

				String errorMsg = String
						.format(
								"Image is not found in the repository for the cross referred debit whose fileId :: %s, debitId :: %s, procDate :: %s",
								matchedDebit.getFileId(), matchId, matchedDebit
										.getProcessDate());
				LOG.error(errorMsg, fnf2);
				throw fnf2;
			}
		}
	}

	/**
	 * find the image from the repo
	 *
	 * @param fileId
	 *            file id
	 * @param debitId
	 *            debit id
	 * @param imageRoot
	 *            image root
	 * @return check image object if image is found
	 * @throws FileNotFoundException
	 *             if image is not found for the given inputs
	 * @throws IOException
	 *             if I/O problems occurred in retrieving image or macthId is
	 *             passed as null
	 */
	public CheckImage findImage(final UUID fileId, final UUID debitId,
			final String imageRoot) throws IOException {

		return findImage(fileId, debitId, imageRoot, this.retry,
				this.sleepTimeInRetry);
	}

	/**
	 * This method is used to retrieve image for the given file id and debit id
	 * in the image root.
	 *
	 * This method tries to retry at most N attempts where N is the retry
	 * property value set and after each attempt it sleeps for M milliseconds
	 * where M is the sleep property value set. This method does retry attempts
	 * since NAS can be down sometimes.
	 *
	 * @param fileId
	 *            file id
	 * @param debitId
	 *            debit id
	 * @param imageRoot
	 *            image root where to find the image
	 * @param retry
	 *            maximum no.of retry attempts to make to retrieve image
	 * @param sleepTimeInRetry
	 *            the sleep time in milliseconds after making each retry attempt
	 *            except for the last retry
	 * @return check image object if found
	 * @throws FileNotFoundException
	 *             if image is not found for the given file id and debit id in
	 *             the given image root after making N retries
	 * @throws IOException
	 *             if io problems occurred in retrieving image
	 */
	public CheckImage findImage(final UUID fileId, final UUID debitId,
			final String imageRoot, final int retry, final long sleepTimeInRetry)
			throws IOException {

		for (int i = 1; i <= retry; i++) {
			if (LOG.isTraceEnabled()) {
			    LOG.trace(String.format(
				    "Attempt %d to retrieve image for fileId :: %s, debitId :: %s in imageRoot %s",
				    Integer.valueOf(i), fileId, debitId, imageRoot));
			}

			try {
				return ImageUtils.retrieveImage(fileId, debitId, imageRoot);
			} catch (FileNotFoundException fne) {
				LOG.warn(String.format(
					    "caught error %s when attempting to retrieve image for fileId :: %s, debitId :: %s in imageRoot %s",
					    fne.getMessage(), fileId, debitId,
					    imageRoot));

				// No need to sleep in the last retry
				if (i < retry) {
					try {
						Thread.sleep(sleepTimeInRetry);
					} catch (InterruptedException ie) {
						// Restore the interrupted status
						Thread.currentThread().interrupt();
					}
				}
			}
		}

		String errorMsg = String.format("Image not found after %d attempts for fileId :: %s, debitId :: %s in imageRoot %s",
				Integer.valueOf(retry), fileId, debitId, imageRoot);
		throw new FileNotFoundException(errorMsg);
	}

	/**
	 * This method is used to retrieve image file object for the given file id
	 * and debit id in the image root.
	 *
	 * This method tries to retry atmost N attempts where N is the retry
	 * property value set and after each attempt it sleeps for M milli seconds
	 * where M is the sleep property value set. This method does retry attempts
	 * since NAS can be down sometimes.
	 *
	 * @param fileId
	 *            file id
	 * @param debitId
	 *            debit id
	 * @param imageRoot
	 *            image root where to find the image
	 * @return image file object if found
	 * @throws FileNotFoundException
	 *             if image file is not found for the given file id and debit id
	 *             in the given image root
	 */
	public File findImageFileObject(final UUID fileId, final UUID debitId,
			final String imageRoot) throws FileNotFoundException {

		return findImageFileObject(fileId, debitId, imageRoot, this.retry,
				this.sleepTimeInRetry);
	}
	
	/**
	 * This method is used to retrieve image file object for the given file id
	 * and debit id in the image root.
	 *
	 * This method tries to retry atmost N attempts where N is the retry
	 * property value set and after each attempt it sleeps for M milli seconds
	 * where M is the sleep property value set. This method does retry attempts
	 * since NAS can be down sometimes.
	 *
	 * @param fileId
	 *            file id
	 * @param debitId
	 *            debit id
	 * @param imageRoot
	 *            image root where to find the image
	 * @param retry
	 *            maximum no.of retry attempts to make to retrieve image
	 * @param sleepTimeInRetry
	 *            the sleep time in milliseconds after making each retry attempt
	 *            except for the last retry
	 * @return image file object if found
	 * @throws FileNotFoundException
	 *             if image file is not found for the given file id and debit id
	 *             in the given image root after making N retries
	 */
	public File findImageFileObject(final UUID fileId, final UUID debitId,
			final String imageRoot, final int retry, final long sleepTimeInRetry)
			throws FileNotFoundException {

		File imageFile = null;
		for (int i = 1; i <= retry; i++) {

			if (LOG.isTraceEnabled()) {
			    LOG.trace(String.format(
				    "Attempt %d to retrieve image file for fileId :: %s, debitId :: %s in imageRoot %s",
				    Integer.valueOf(i), fileId, debitId, imageRoot));
			}

			imageFile = ImageUtils.getImageFile(ImageUtils.getImageDirectory(
					fileId, debitId, imageRoot), debitId);
			if (!imageFile.exists()) {
				if (LOG.isTraceEnabled()) {
				    LOG.trace(String.format(
					    "Seems NAS is down when attempting to retrieve image file for fileId :: %s, debitId :: %s in imageRoot %s",
					    fileId, debitId, imageRoot));
				}
				// No need to sleep in the last retry
				if (i < retry) {

					try {
						Thread.sleep(sleepTimeInRetry);
					} catch (InterruptedException ie) {
						// Restore the interrupted status
			            Thread.currentThread().interrupt();
					}
				}
			} else {
				return imageFile;
			}
		}

		String errorMsg = String.format("Image File not found after %d attempts for fileId :: %s, debitId :: %s in imageRoot %s",
				Integer.valueOf(retry), fileId, debitId, imageRoot);
		throw new FileNotFoundException(errorMsg);
	}

	/**
	 * find the image from the repo specifically used for DART web service call.
	 *
	 * @param fileId
	 *            file id
	 * @param debitId
	 *            debit id
	 * @return check image object if image is found otherwise null
	 * @throws FileNotFoundException
	 *             if image is not found for the given inputs
	 * @throws IOException
	 *             if I/O problems occurred in retrieving image
	 */
	public CheckImage findImageForWS(final UUID fileId, final UUID debitId)
			throws VpcException, IOException {

		DartDebit debitFromDb = debitDao.findRescanDebit(debitId, procDateUtils
				.getMinSelectionRangeDate());
		if (debitFromDb == null) {

			String errorMsg = String.format(
							"findImageForWS::failed the given debitId :: %s is not present in DB",
							debitId);
			// throwing FileNotFoundException so that ImageService responds to RSI saying
			// image not found
			// otherwise it goes as SERVER ERROR response
			throw new FileNotFoundException(errorMsg);
		}
		return findImage(debitFromDb.getFileId(), debitFromDb.getId(),
				debitFromDb.getMatchUUID(), debitFromDb.getProcessDate(),
				debitFromDb.getInsertedDate(), GREY_IMG, true);
	}

	/**
	 * Saves a file under the B/W image root in PROD NAS.
	 *
	 * @param fileId
	 *            the image's file id
	 * @param itemId
	 *            the image's item id
	 * @param processDate
	 *            the image's item id process date
	 * @param frontData
	 *            front bytes to write to image file
	 * @param backData
	 *            back bytes to write to image file
	 * @param createDirs
	 *            pass false if directory into which the image gets saved is
	 *            already exists otherwise pass true
	 * @throws IOException
	 *             Issues writing item to file
	 */
	public void saveBWImage(final UUID fileId, final UUID itemId,
			final Date processDate, final byte[] frontData,
			final byte[] backData, final boolean createDirs,
			final Date insertedDate) throws IOException {

		saveImage(fileId, itemId, processDate, frontData, backData,
				bwImgRoot, createDirs, insertedDate);
	}

	/**
	 * Saves a file under the grey image root in PROD NAS.
	 *
	 * @param fileId
	 *            the image's file id
	 * @param itemId
	 *            the image's item id
	 * @param processDate
	 *            the image's item id process date
	 * @param frontData
	 *            front bytes to write to image file
	 * @param backData
	 *            back bytes to write to image file
	 * @param createDirs
	 *            pass false if directory into which the image gets saved is
	 *            already exists otherwise pass true
	 * @throws IOException
	 *             Issues writing item to file
	 */
	public void saveGreyImage(final UUID fileId, final UUID itemId,
			final Date processDate, final byte[] frontData,
			final byte[] backData, final boolean createDirs,
			final Date insertedDate) throws IOException {

		saveImage(fileId, itemId, processDate, frontData, backData,
				greyImgRoot, createDirs, insertedDate);
	}

	/**
	 * Saves a file under the given image root
	 *
	 * @param fileId
	 *            the image's file id
	 * @param itemId
	 *            the image's item id
	 * @param processDate
	 *            the image's item id process date
	 * @param frontData
	 *            front bytes to write to image file
	 * @param backData
	 *            back bytes to write to image file
	 * @param createDirs
	 *            pass false if directory into which the image gets saved is
	 *            already exists otherwise pass true
	 * @throws IOException
	 *             Issues writing item to file
	 */
	private void saveImage(final UUID fileId, final UUID itemId,
			final Date processDate, final byte[] frontData,
			final byte[] backData, final String imageRoot,
			final boolean createDirs, final Date insertedDate)
			throws IOException {

		String imageAbsPath = getImageAbsolutePath(imageRoot, processDate,
				insertedDate);
		File imgDir = new File(imageAbsPath);
		if (!imgDir.exists()) {
			// ignore return value of mkDirs since this method is invoked by
			// multiple threads
			imgDir.mkdirs();
		}
		File imgFile = new File(getImageFile(imageAbsPath, fileId, itemId));
		// define an overloaded saveImage method in ImageUtils class as
		// so no need to have separate class for DART.
		// TODO - need to check with VPC team. most importantly need to get
		// permission.
		saveImage(imgFile, frontData, backData);
	}
	
	/**
	 * This method is used to return image root that is used for DART. The Dart
	 * image root convention is <IMG_ROOT>/<PROC_DT_STRING>/<293 folder
	 * structure>/img$IMGNUM
	 *
	 * @param fileId
	 *            file id for creating file folder
	 * @param imgRoot
	 *            image root configured in database
	 * @param procDateStr
	 *            process date string
	 * @return dart image root
	 */
	public static String getDartImageRoot(final UUID fileId,
			final String imgRoot, final String procDateStr) {

		StringBuilder imgRootBldr = new StringBuilder(imgRoot);
		imgRootBldr.append(File.separator);
		imgRootBldr.append(procDateStr);
		imgRootBldr.append(File.separator);

		int fileFolderLocation = (Math.abs(StringHash.hashCode(fileId
				.toString())
				% NUM_FILE_FOLDERS) + 1);
		imgRootBldr.append(fileFolderLocation);
		imgRootBldr.append(File.separator);

		imgRootBldr.append(IMGNUM);
		return imgRootBldr.toString();
	}


	public String getBwImgRoot() {
		return bwImgRoot;
	}

	public void setBwImgRoot(String bwImgRoot) {
		this.bwImgRoot = bwImgRoot;
	}

	public String getGreyImgRoot() {
		return greyImgRoot;
	}

	public void setGreyImgRoot(String greyImgRoot) {
		this.greyImgRoot = greyImgRoot;
	}

	/**
	 * @return the debitDao
	 */
	public DebitDao getDebitDao() {
		return debitDao;
	}

	/**
	 * @param debitDao
	 *            the debitDao to set
	 */
	public void setDebitDao(final DebitDao debitDao) {
		this.debitDao = debitDao;
	}

	/**
	 * @return the retry
	 */
	public int getRetry() {
		return retry;
	}

	/**
	 * @param retry
	 *            the retry to set
	 */
	public void setRetry(final int retry) {
		this.retry = retry;
	}

	/**
	 * @return the sleepTimeInRetry
	 */
	public long getSleepTimeInRetry() {
		return sleepTimeInRetry;
	}

	/**
	 * @param sleepTimeInRetry
	 *            the sleepTimeInRetry to set
	 */
	public void setSleepTimeInRetry(final long sleepTimeInRetry) {
		this.sleepTimeInRetry = sleepTimeInRetry;
	}

	// Note temporarily added this method and waiting for the confirmation for
	// saveImage in Imageutils. and then remove the entire code below
	/**
	 *
	 * Saves a file to an appropriate directory structure in the image root
	 *
	 * @param itemId
	 *            the image's item id
	 * @param fileId
	 *            the image's file id
	 * @param frontData
	 *            front bytes to write to image file
	 * @param backData
	 *            front bytes to write to image file
	 * @param imageRoot
	 *            the root folder to write images into
	 * @throws IOException
	 *             Issues writing item to file
	 */
	private static void saveImage(File imgFile, final byte[] frontData,
			final byte[] backData) throws IOException {

		if (LOG.isTraceEnabled()) {
			int frontDataSize = getDataLength(frontData);
			int backDataSize = getDataLength(backData);
			String message = String.format("Saving image (%s, %s, %s)",
					Integer.valueOf(frontDataSize), Integer.valueOf(backDataSize), imgFile.getName());
			LOG.trace(message);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Saving image file:  " + String.valueOf(imgFile));
		}

		byte[] bytes = toByteArray(frontData, backData);

		int numAttempts = 0;
		do {
			numAttempts++;
			try {
				writeImageFile(bytes, imgFile);
				break;
			} catch (IOException e) {
				if (numAttempts == MAX_RETRY) {
					throw e;
				}
				LOG.warn("Failed saving image", e);

				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException ie) {
				    LOG.warn("Interrupted", ie);
				}
			}
		} while (numAttempts < MAX_RETRY);
	}

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
	
	/**
	 * @param data
	 *            data
	 * @return 0 if null else size
	 */
	private static int getDataLength(final byte[] data) {
		int dataSize = 0;
		if (data != null) {
			dataSize = data.length;
		}
		return dataSize;
	}

	public static final int HASH_LEN = 20;

	public static final int DATA_LEN = 7;

	private static final String DIGEST_ALGORITHM = "SHA-1";
	public static final int VERSION_LEN = 4;
	static final int MAX_RETRY = 3;

	private static byte[] toByteArray(final byte[] frontData,
			final byte[] backData) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream(25000);

		os.write(StringUtils.leftPad("" + CheckImageV1.VERSION, VERSION_LEN,
				'0').getBytes());

		int frontDataSize = getDataLength(frontData);
		int backDataSize = getDataLength(backData);
		byte[] frontHash = getDigestHash(frontData);
		os.write(frontHash);
		os.write(StringUtils.leftPad("" + frontDataSize, DATA_LEN, '0')
				.getBytes());
		if (frontData != null) {
			os.write(frontData);
		}

		byte[] backHash = getDigestHash(backData);
		os.write(backHash);
		os.write(StringUtils.leftPad("" + backDataSize, DATA_LEN, '0')
				.getBytes());
		if (backData != null) {
			os.write(backData);
		}

		return os.toByteArray();
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

	private static void writeImageFile(final byte[] bytes, final File file)
			throws IOException {
		FileOutputStream fos = null;
		try {
			// Hard code the retry times and interval: retry every 3 seconds, 5 times
			fos = (FileOutputStream) DartFileUtils.createOutputStream(file, 5, 3*1000);
			fos.write(bytes);
		} finally {
			IOUtils.closeQuietly(fos);
		}
	}

}