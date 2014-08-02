package com.jpmc.dart.image;

public class ImageServlet {
	@Override
	public void init() throws ServletException {
		super.init();
		coreImageClient = (CoreImageClient) ContextFactory.getObject("CoreImageClient");

		EvictionListener<String, SoftReference<ImgAndContTypeWrapper>> evict = new EvictionListener<String, SoftReference<ImgAndContTypeWrapper>>() {
			@Override
			public void onEviction(String key, SoftReference<ImgAndContTypeWrapper> value) {
				LOG.info("evict image for key "+key);
			}
		};
		imageCache = new ConcurrentLinkedHashMap.Builder<String, SoftReference<ImgAndContTypeWrapper>>().maximumWeightedCapacity(1024 * 1024).listener(evict).build();
	}

	/**
	 * @param request
	 *            servlet request object
	 * @param response
	 *            servlet response object
	 * @throws ServletException
	 *             if any exception in writing image to client
	 * @throws IOException
	 *             if any exception in writing image to client
	 */
	protected void doGet(final HttpServletRequest request,
			final HttpServletResponse response) throws ServletException, IOException {

		String fileId = request.getParameter("fileId");
		String debitId = request.getParameter("debitId");
		String imageSide = request.getParameter("imageSide");

		ServletOutputStream sos = response.getOutputStream();

		try {
			ImgAndContTypeWrapper wrapper = getCheckImageFromNasAndSetContentType(request, response);

//			if (chkImg == null) {
//				chkImg = getCheckImageFromZip(request, response);
//			}
			/*
			 * could not find image either from IMG file or zip file
			 */
			if (wrapper == null) {
				throw new FileNotFoundException("Image not found");
			}

			CheckImage chkImg = wrapper.getImg();
			String contentType = wrapper.getContentType();
			response.setContentType(contentType);

			if (IMAGE_BOTH_SIDES.equals(imageSide)) {

				renderFrontAndRearBytes(request, response, chkImg.getImageFront(), chkImg.getImageBack());

			} else if (FRONT_IMAGE_SIDE.equals(imageSide)) {

				renderBytes(response, sos, chkImg.getImageFront());

			} else if (REAR_IMAGE_SIDE.equals(imageSide)) {

				renderBytes(response, sos, chkImg.getImageBack());
			}
		} catch (FileNotFoundException fnf) {

			LOG.error(
					String.format(
							"Images are not present for fileId :: %s and debitId :: %s",
							fileId, debitId), fnf);
			InputStream is = getServletContext().getResourceAsStream(
					"/images/no image.jpg");
			byte[] noImgBytes = IOUtils.toByteArray(is);
			sos.write(noImgBytes);

		} catch (Throwable t) {
			LOG.error("Error in rendering image to JSP", t);
			writeErrorImage("RUNTIME ERROR", sos);
		} finally {
			sos.flush();
			IOUtils.closeQuietly(sos);
		}
	}

	private class ImgAndContTypeWrapper {
		private CheckImage img;
		private String contentType;

		public CheckImage getImg() {
			return img;
		}
		public void setImg(CheckImage img) {
			this.img = img;
		}
		public String getContentType() {
			return contentType;
		}
		public void setContentType(String contentType) {
			this.contentType = contentType;
		}
	}

	/**
	 * This method is used to retrieve image from NAS. First it retrieves gray
	 * image, if it is not present then it retrieves bw image. The content type
	 * is set as JPEG for gray images and TIFF for bw images
	 *
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 *            HttpServletResponse
	 * @return CheckImage
	 * @throws Exception
	 */
	public ImgAndContTypeWrapper getCheckImageFromNasAndSetContentType(
			final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {

		String debitId = request.getParameter("debitId");
		String processDateStr = request.getParameter("processDate");
		Date processDate = DateUtils.convertStringToDate(processDateStr,DartFileUtils.PROC_DT_FMT);
		String fileId = request.getParameter("fileId");

		ImgAndContTypeWrapper wrapper = new ImgAndContTypeWrapper();

		MonitorFactory.add("request", "request", 1d);

		// check if cached
		if (imageCache.containsKey(debitId)){
			MonitorFactory.add("cache hit", "cache", 1d);
			
			SoftReference<ImgAndContTypeWrapper> ref = imageCache.get(debitId);
			if (ref != null && ref.get() != null) {
				return imageCache.get(debitId).get();
			}
		}

		MonitorFactory.add("cache miss", "cache", 1d);
		CheckImage chkImgFromNas = null;
		try {
			chkImgFromNas = coreImageClient.getImage(UUID.fromString(debitId), UUID.fromString(fileId), processDate, ImgType.GREY);
			wrapper.setImg(chkImgFromNas);
			wrapper.setContentType(JPEG_CONTENT_TYPE);
		} catch (Exception fnfe) {
			LOG.info("Grey images not found for debit " + debitId + ", going to extract bw images instead.", fnfe);
		}
		if (chkImgFromNas == null) {
			try {
				chkImgFromNas = coreImageClient.getImage(UUID.fromString(debitId), UUID.fromString(fileId), processDate, ImgType.BW);
				wrapper.setImg(chkImgFromNas);
				wrapper.setContentType(TIFF_CONTENT_TYPE);
			} catch (Exception fnfe) {
				LOG.info("BW images not found for debit " + debitId + ", no images to display.", fnfe);
			}
			if (chkImgFromNas == null){
				return null;
			}
		}
		if (chkImgFromNas!=null){
			MonitorFactory.add("cache put", "cache", 1d);

			imageCache.put(debitId, new SoftReference<ImgAndContTypeWrapper>(wrapper));
		}

		return wrapper;
	}
}
