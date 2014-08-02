package com.jpmc.cto.dart.extract.edw.xml.builder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import com.jpmc.cto.dart.exception.DartException;
import com.jpmc.cto.dart.extract.edw.model.jaxb.v100.DartDataType;
import com.jpmc.cto.dart.extract.edw.model.jaxb.v100.ObjectFactory;
import com.jpmc.cto.dart.extract.edw.model.jaxb.v100.TrackingDataType;
import com.jpmc.cto.dart.extract.edw.model.jaxb.v100.UnitOfWorkType;
import com.jpmc.cto.dart.extract.edw.xml.builder.objects.v100.XmlUnitOfWorkBuilder;
import com.jpmc.dart.commons.util.DartFileUtils;
import com.jpmc.dart.commons.util.FileSystemHelper;
import com.jpmc.vpc.model.dart.check.DartFile;
import com.jpmc.vpc.model.dart.type.EventType;

/**
 * EDW xml builder for schema version 1.0.0
 *
 * @author R502440
 *
 */
public class EdwXmlBuilderImplV100 implements EdwXmlBuilder, InitializingBean {

	private static final Log LOG = LogFactory.getLog(EdwXmlBuilderImplV100.class);

	private static final String CONTEXT_PATH = "com.jpmc.cto.dart.extract.edw.model.jaxb.v100";
	private final FastDateFormat formatter = FastDateFormat.getInstance("yyyyMMdd");

	private final ObjectFactory factory = new ObjectFactory();

	private String xmlSrcDirBasePath;
	private FileSystemHelper fsHelper;
	private boolean schemaValidation = false;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		Validate.notNull(fsHelper, "FileSystemHelper object cannot be null in EDW XML Builder.");
		xmlSrcDirBasePath = fsHelper.getFilesystemRoot();
		if (StringUtils.isEmpty(xmlSrcDirBasePath)) {
			LOG.error("EDW XML base directory cannot be null.");
			throw new DartException("EDW XML base directory cannot be null");
		}
	}

	@Override
	public File buildXml(DartFile dartFile, UUID transactionId, EventType event, Date procDate) throws Exception {

		if (dartFile == null) {
			LOG.error("Dart File cannot be null");
			throw new DartException("Dart File cannot be null");
		}

		// List<DartTransaction> transactions = dartFile.getTransactions();
		Validate.notEmpty(dartFile.getTransactions(), "Transaction list cannot be empty or null");

		// root element: dartData
		DartDataType dartData = factory.createDartDataType();

		// dartData's element
		TrackingDataType tracking = factory.createTrackingDataType();
		// tracking's attribute: type
		if (EventType.TRANSACTION_COMPLETE.equals(event)) {
			tracking.setType("OUTPUT");
		} else {
			tracking.setType("INPUT");
		}

		dartData.setTracking(tracking);

		// dartData's element
		UnitOfWorkType unitOfWork = XmlUnitOfWorkBuilder.buildUnitOfWork(dartFile, factory);

		// Finally set the unit of work on the root of the XML.
		dartData.setUnitOfWork(unitOfWork);

		File result = this.createXml(dartData, transactionId, event, procDate);

		return result;
	}

	/**
	 * e.g.
	 * 'BASE_PATH/extract/edw/20130923/outgoing/4df/OUT-EDW_4dfdabbd-0303-42d3-966d-7257b5a5aa4f_20130923_000900.xml'
	 * 'BASE_PATH/extract/edw/20130923/incoming/f24/IN-EDW_f24098f7-632d-431e-afcd-4379eda76908_20130923_000900.xml'
	 *
	 * @param dartData
	 * @param transactionId
	 * @param event
	 * @param procDate
	 * @return
	 * @throws JAXBException
	 * @throws IOException
	 * @throws SAXException 
	 * @throws DartException 
	 */
	private File createXml(DartDataType dartData, UUID transactionId, EventType event, Date procDate)
			throws JAXBException, IOException, SAXException {

		String currentDayFolderPath = this.buildXmlDirectoryPath(transactionId, event, procDate);
		File currentDayFolder = new File(currentDayFolderPath);
		if (!currentDayFolder.exists()) {
			currentDayFolder.mkdirs();
		}

		String fileName = this.buildEdwXmlName(transactionId, procDate, event);
		File result = new File(currentDayFolderPath + File.separator + fileName);
		OutputStream os = null;

		try {
			// Hard code the retry times and interval: retry every 3 seconds, 5 times
			os = DartFileUtils.createOutputStream(result, 5, 3*1000);
			JAXBContext context = JAXBContext.newInstance(CONTEXT_PATH);
			JAXBElement<DartDataType> element = factory.createDartData(dartData);
			Marshaller marshaller = context.createMarshaller();

			if (schemaValidation) {
				SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
				InputStream schemaIs = new ClassPathResource("dart-transaction-1.0.0.xsd").getInputStream();
				Source schemaSource = new StreamSource(schemaIs);
				Schema schema = schemaFactory.newSchema(schemaSource);
				marshaller.setSchema(schema);
			}

			marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
			marshaller.marshal(element, os);
		} finally {
			IOUtils.closeQuietly(os);
		}

		return result;
	}

	/**
	 *
	 * For instance, current Date is 2013-04-30, Transaction Id is
	 * cad75636-5770-41cb-91e6-a7747dc3fe95, event type is Capture_Complete
	 * event then the EDW XML path is
	 * 'xmlSrcDirBasePath/extract/edw/20130430/incoming/cad'
	 *
	 * @param transactionId
	 * @param event
	 * @param procDate
	 * @return
	 */
	private String buildXmlDirectoryPath(UUID transactionId, EventType event, Date procDate) {
		StringBuilder dirPath = new StringBuilder(xmlSrcDirBasePath);
		dirPath.append(File.separator);
		dirPath.append("extract");
		dirPath.append(File.separator);
		dirPath.append("edw");
		dirPath.append(File.separator);
		dirPath.append(formatter.format(procDate));
		dirPath.append(File.separator);
		if (EventType.CAPTURE_COMPLETE.equals(event)) {
			dirPath.append("incoming");
		} else {
			dirPath.append("outgoing");
		}
		dirPath.append(File.separator);
		dirPath.append(transactionId.toString().substring(0, 3));

		return dirPath.toString();
	}

	/**
	 * e.g. IN-EDW_bba55c54-bae1-4d88-899d-c474136d4b94_20130430_1504776.xml
	 *
	 * @param transactionId
	 * @param formatter
	 * @param now
	 * @return
	 */
	private String buildEdwXmlName(UUID transactionId, Date now, EventType event) {
		FastDateFormat formatter = FastDateFormat.getInstance("yyyyMMdd_HHMMSS");

		StringBuilder xmlFileName = new StringBuilder();
		if (EventType.CAPTURE_COMPLETE.equals(event)) {
			xmlFileName.append("IN-EDW");
		} else {
			xmlFileName.append("OUT-EDW");
		}
		xmlFileName.append("_");
		xmlFileName.append(transactionId.toString());
		xmlFileName.append("_");
		xmlFileName.append(formatter.format(now).toString());
		xmlFileName.append(".xml");

		return xmlFileName.toString();
	}

	public void setFsHelper(FileSystemHelper fsHelper) {
		this.fsHelper = fsHelper;
	}

	/**
	 * @param xmlSrcDirBasePath
	 *            the xmlSrcDirBasePath to set
	 */
	public void setXmlSrcDirBasePath(String xmlSrcDirBasePath) {
		this.xmlSrcDirBasePath = xmlSrcDirBasePath;
	}

	public void setSchemaValidation(boolean schemaValidation) {
		this.schemaValidation = schemaValidation;
	}
}
