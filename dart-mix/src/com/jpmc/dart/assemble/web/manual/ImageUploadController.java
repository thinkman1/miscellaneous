package com.jpmc.dart.assemble.web.manual;

public class ImageUploadController {
	/**
	 * @param response Not used locally
	 */
	@RequestMapping(DartAssemblyWebConstants.Actions.PROCESS_UPLOADED_IMAGE)
	public void processUploadedImage(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String folderName = (String)request.getSession().getAttribute("folderName");
		File folderFile = fileManageUtils.getFile(folderName);

		try {
			MultipartHttpServletRequest newRequest = (MultipartHttpServletRequest) request;
			// parse the request and find the files.
			@SuppressWarnings("unchecked")
			Iterator<String> fileNames = newRequest.getFileNames();
			while (fileNames.hasNext()) {
				String fileName = fileNames.next();
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("fileName :: %s", fileName));
				}
				MultipartFile mpf = newRequest.getFile(fileName);
				File jpgFile = new File(folderFile, UUID.randomUUID().toString() + ".jpg");
				LOG.info("Image File Saved: " + jpgFile.getAbsolutePath());
				OutputStream out = new FileOutputStream(jpgFile);
				out.write(mpf.getBytes());
				out.close();

				File tifFile = ImageFormatConverter.convertFromJPEGToBwTIFF(jpgFile, folderFile.getAbsolutePath());
				LOG.info("Image File Saved: " + tifFile.getAbsolutePath());
			}
		} catch (Exception t) {
			throw new Exception("Error While reading file from Multipart request.", t);
		}
		if (LOG.isTraceEnabled()) {
			LOG.trace("Exited handleMultipartRequest method successfully");
		}

	}

	@RequestMapping(DartAssemblyWebConstants.Actions.SHOW_IMAGE)
	public void getImageBytes(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String folderName = (String)req.getSession().getAttribute("folderName");
		String imgName = req.getParameter("imgName");
		File folderFile = fileManageUtils.getFile(folderName);
		File imgFile = new File(folderFile, imgName);

		// For static scan - need to make sure relative path is under our defined base directory
		if (! FileSystemUtils.validateFileUnderBaseDir(folderFile.getAbsolutePath(), imgFile)) {
			throw new IOException("The file '" + imgFile + "' does not fall under an approved directory '" +
					folderName + "' and will not be processed.");
		}

		FileInputStream foo = null;
		ServletOutputStream sos = resp.getOutputStream();

		try {
			foo = new FileInputStream(imgFile);
			IOUtils.copy(foo, sos);
		} catch (FileNotFoundException fnf) {

			LOG.error(String.format("Images are not present :: %s.", imgName), fnf);
			InputStream is = this.getClass().getResourceAsStream(
					"/images/no image.jpg");
			byte[] noImgBytes = IOUtils.toByteArray(is);
			sos.write(noImgBytes);

		} catch (Exception e) {
			LOG.error("Error in rendering image : " + imgName + " to JSP", e);
			writeErrorImage("RUNTIME ERROR", sos);
		} finally {
			IOUtils.closeQuietly(foo);
			sos.flush();
			sos.close();
		}
	}

	@RequestMapping(DartAssemblyWebConstants.Actions.GET_IMAGE_UPLOAD_TOOL)
	public ModelAndView getImageUploadTool() {
		return new ModelAndView("imageUploadTool");
	}

	@RequestMapping(DartAssemblyWebConstants.Actions.GET_ATM_ADDRESS)
	public void getAtmAddress(@RequestParam("atmId") String atmId, HttpServletResponse response)throws ApplicationException {

		AtmLocation atmLocation = transactionUtils.findAtmAddressById(atmId);

		try {
			xstream.toXML(atmLocation, response.getOutputStream());
		}catch (IOException e) {
			LOG.error("Get exception during writting atm address to response", e);
			throw new ApplicationException(e);
		}
	}

	@RequestMapping(DartAssemblyWebConstants.Actions.GET_DEPOSIT_AMOUNT)
	public void getDepositAmount(@ModelAttribute AtmTransaction atmTransaction, HttpServletResponse response) throws ApplicationException {

		String depositAmount = transactionUtils.getDepositAmount(atmTransaction);
		double d = Long.parseLong(depositAmount) / 100.0;
		DecimalFormat df = new DecimalFormat("$###,##0.00");
		try {
			response.getWriter().print(df.format(d));
		}catch (IOException e) {
			LOG.error("Get exception during writting deposit amount to response", e);
			throw new ApplicationException(e);
		}
	}
}
