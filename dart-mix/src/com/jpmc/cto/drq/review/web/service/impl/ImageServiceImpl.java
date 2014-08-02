package com.jpmc.cto.drq.review.web.service.impl;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jpmc.cto.drq.common.images.CheckImage;
import com.jpmc.cto.drq.common.images.ImageHelper;
import com.jpmc.cto.drq.model.base.BaseItemReview;
import com.jpmc.cto.drq.review.web.image.ImageSide;
import com.jpmc.cto.drq.review.web.image.ImageType;
import com.jpmc.cto.drq.review.web.service.ImageService;

public class ImageServiceImpl implements ImageService, InitializingBean {
	private static transient Log log = LogFactory.getLog(ImageServiceImpl.class);
	
	private ImageHelper imageHelper;
	private ThreadPoolExecutor threadPool;
	private Cache<UUID, CheckImage> imageCache;
	private Cache<UUID, Future<CheckImage>> futureCache;
	private int cacheSize = 250;
	
	static {
		ImageIO.scanForPlugins(); 
		ImageIO.setUseCache(false);
	}
	
	@Override
	public CheckImage getImage(final BaseItemReview item) throws IOException {
		Validate.notNull(item, "item cannot be null");
		Validate.notNull(item.getId(), "id on item cannot be null");
		
		CheckImage image = null;
		if (!item.isElectronic()) {
			try {
				image = imageCache.get(item.getId(), new Callable<CheckImage>() {
					@Override
					public CheckImage call() throws IOException {
						CheckImage ci = null;
						
						Future<CheckImage> future = futureCache.getIfPresent(item.getId());
						if (future != null) {
							try {
								ci = future.get(10, TimeUnit.SECONDS);
							} catch (InterruptedException e) {
								log.info("Thread interrupted retrieving " + item.getId(), e);
							} catch (Exception e) {
								log.warn("Exception while retrieving " + item.getId(), e);
							}
						}
						
						if (ci == null) { // go get it the old fashioned way...
							ci = imageHelper.retrieveImage(item);
						}
						
						return ci;
					}
				});
			} catch (ExecutionException e) {
				throw new IOException("Exception while retrieving " + item.getId(), e);
			}
		}
		
		return image;
	}
	
	@Override
	public byte[] getImageBytes(BaseItemReview item, ImageType type, ImageSide side) throws IOException {
		// TODO - GRAYSCALE and BEST_AVAIL probably shouldn't be the same
		CheckImage image = getImage(item);
		
		byte [] bytes = null;
		switch (type) {
		case BLACK_WHITE:
			bytes = getImageBytesForValidation(image, side);
			break;
		case GRAYSCALE:
		case BEST_AVAILABLE:
			bytes = getBestImageBytesAvailable(image, side);
			break;
		default:
			break;
		}
		
		return bytes;
	}
	
	protected byte[] getBestImageBytesAvailable(CheckImage image, ImageSide side) throws IOException {
		byte[] imageBytes = null;
		if (image != null) {
			if (side == ImageSide.FRONT) {
				imageBytes = image.getMonochromeFront();
				if (image.getGrayscaleFront() != null && image.getGrayscaleFront().length > 0) {
					imageBytes = image.getGrayscaleFront();
				}
			} else if (side == ImageSide.BACK) {
				imageBytes = image.getMonochromeBack();
				if (image.getGrayscaleBack() != null && image.getGrayscaleBack().length > 0) {
					imageBytes = image.getGrayscaleBack();
				}
			}
		}

		return imageBytes;
	}

	protected byte[] getImageBytesForValidation(CheckImage image, ImageSide side) throws IOException {
		byte[] imageBytes = null;
		if (image != null) {
			if (side == ImageSide.FRONT) {
				imageBytes = image.getMonochromeFront();
			} else if (side == ImageSide.BACK) {
				imageBytes = image.getMonochromeBack();
			}
		}

		return imageBytes;
	}
	
	@Override
	public void submit(BaseItemReview item) {
		// could be synchronized but I don't <i>think</i> I care
		Validate.notNull(item, "item cannot be null");
		Validate.notNull(item.getId(), "id on item cannot be null");
		
		CheckImage ci = imageCache.getIfPresent(item.getId());
		if (ci == null) {
			Future<CheckImage> future = futureCache.getIfPresent(item.getId());
			if (future == null) {
				future = threadPool.submit(new ImageWorker(item, imageHelper));
				futureCache.put(item.getId(), future);
			}
		}
	}
	
	@Override
	public void afterPropertiesSet() throws IOException {
		imageCache = CacheBuilder.newBuilder().maximumSize(getCacheSize()).initialCapacity(getCacheSize()).weakValues().build();
		futureCache = CacheBuilder.newBuilder().maximumSize(getCacheSize()).initialCapacity(getCacheSize()).build();
	}

	public ImageHelper getImageHelper() {
		return imageHelper;
	}

	public void setImageHelper(ImageHelper imageHelper) {
		this.imageHelper = imageHelper;
	}

	public int getCacheSize() {
		return cacheSize;
	}

	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	public Cache<UUID, CheckImage> getImageCache() {
		return imageCache;
	}

	public void setImageCache(Cache<UUID, CheckImage> imageCache) {
		this.imageCache = imageCache;
	}

	public Cache<UUID, Future<CheckImage>> getFutureCache() {
		return futureCache;
	}

	public void setFutureCache(Cache<UUID, Future<CheckImage>> futureCache) {
		this.futureCache = futureCache;
	}

	public ThreadPoolExecutor getThreadPool() {
		return threadPool;
	}

	public void setThreadPool(ThreadPoolExecutor threadPool) {
		this.threadPool = threadPool;
	}
}
