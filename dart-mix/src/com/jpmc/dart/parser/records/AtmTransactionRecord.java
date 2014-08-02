/**
 *
 */
package com.jpmc.dart.parser.records;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;

import com.jpmc.dart.commons.util.DartConstants;
import com.jpmc.dart.parser.exception.DartFileParserException;
import com.jpmc.dart.xml.model.atm.AtmCash;
import com.jpmc.dart.xml.model.atm.AtmCheck;
import com.jpmc.dart.xml.model.atm.AtmTransaction;
import com.jpmc.vpc.file.FileException;
import com.jpmc.vpc.model.exception.ExceptionType;

/**
 * Parse an ATM transaction.
 *
 * TODO: For some reason my predecessors used a fixed-length file parser as the
 * root of an XML parser. Very odd. This needs to be reconsidered.
 *
 * @author e217297
 *
 */
public class AtmTransactionRecord extends DartBaseRecord {

	private boolean isFromFile;

	private OutputStream xmlOutputStream = null;

	private AtmTransaction atmTransaction = new AtmTransaction();

	public AtmTransactionRecord(final XMLStreamReader reader) {
		super(reader);
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.jpmc.vpc.file.FileRecord#processRecord()
	 */
	@Override
	public void processRecord() throws FileException {
		Validate.notNull(this.reader,
				"Need to have an open input stream.  File doesn't exist maybe??");

		while (true) {

			try {
				// This step is to make sure that while never goes in infinite
				// loop.
				if (!reader.hasNext()) {
					break;
				}
				if (XMLStreamConstants.START_ELEMENT == reader.getEventType()) {

					String tagName = reader.getName().getLocalPart();
					// since the start tag is Tran and it has no specific value
					// continuing reader to progress
					// This statement is required otherwise getValue causes
					// reader to traverse till tran end tag
					if (DartConstants.File.TXN_TAG.equals(tagName)) {
						// making reader to progress to next element
						reader.next();
						continue;
					}

					// breaking the loop once CheckItems/ CashItems tags starts
					if (DartConstants.Debit.CHECK_ITEM_TAG.equals(tagName)
							|| DartConstants.Debit.CASH_ITEM_TAG
									.equals(tagName)) {
						break;
					}

					String attrValue = getValue(reader);

					// Setting version of transaction record
					if (DartConstants.File.VERSION_TAG.equals(tagName)) {
						this.atmTransaction.setVersion(attrValue);
					}

					// File related tags starts
					// setting transaction state to file
					else if (DartConstants.File.TRAN_TYPE.equals(tagName)) {
						this.atmTransaction.setTranType(attrValue);
					}

					// Setting ref id to file
					else if (DartConstants.File.REF_ID_TAG.equals(tagName)) {
						this.atmTransaction.setReferenceId(attrValue);
					}
					// source - source is Source of the request
					else if (DartConstants.File.SOURCE_TAG.equals(tagName)) {
						this.atmTransaction.setSource(attrValue);
					}
					// transaction status
					else if (DartConstants.File.STATUS_TAG.equals(tagName)) {
						this.atmTransaction.setStatus(attrValue);
					}
					// Request Status
					else if (DartConstants.File.REQUEST_STATUS_TAG
							.equals(tagName)) {
						this.atmTransaction.setRequestStatus(attrValue);
					}
					// Return Status
					else if (DartConstants.File.RETURN_STATUS_TAG
							.equals(tagName)) {
						this.atmTransaction.setReturnStatus(attrValue);
					}
					// terminal id
					else if (DartConstants.File.TERM_ID_TAG.equals(tagName)) {
						this.atmTransaction.setAtmTerminalId(attrValue);
					}
					// local date time
					else if (DartConstants.File.LOC_DATE_TM_TAG.equals(tagName)) {
						this.atmTransaction.setLocalDateTime(attrValue);
					}
					// no.of items
					else if (DartConstants.File.NO_OF_DEBIT_ITEMS_TAG
							.equals(tagName)) {
						this.atmTransaction.setNumberOfItems(attrValue);
					}
					// no.of Escrow items
					else if (DartConstants.File.NO_OF_ESCROW_ITEMS_TAG
							.equals(tagName)) {
						this.atmTransaction.setNumberOfEscrowItems(attrValue);
					}
					// no.of exception items
					else if (DartConstants.File.NO_OF_EXCP_ITEMS_TAG
							.equals(tagName)) {
						this.atmTransaction
								.setNumberOfExceptionItems(attrValue);
					}
					// PAN
					else if (DartConstants.File.PAN_TAG.equals(tagName)) {
						this.atmTransaction.setPan(attrValue);
					}
					// getting transDate value
					else if (DartConstants.File.TRAN_DATE_TAG.equals(tagName)) {
						this.atmTransaction.setTranDate(attrValue);
					}
					// getting terminal address
					else if (DartConstants.Transaction.TERM_ADDRESS
							.equals(tagName)) {
						this.atmTransaction.setTermAddress(attrValue);
					}
					// getting terminal state
					else if (DartConstants.Transaction.TERM_STATE
							.equals(tagName)) {
						this.atmTransaction.setTermState(attrValue);
					}
					// getting terminal city
					else if (DartConstants.Transaction.TERM_CITY
							.equals(tagName)) {
						this.atmTransaction.setTermCity(attrValue);
					}
					// getting transTime and setting transDateTime
					else if (DartConstants.File.TRAN_TIME_TAG.equals(tagName)) {
						this.atmTransaction.setTranTime(attrValue);
					}
					// TranTimestamp
					else if (DartConstants.Transaction.TRAN_TIME_OFFSET
							.equals(tagName)) {
						this.atmTransaction.setTranTimeOffset(attrValue);
					}
					// card post date
					else if (DartConstants.File.CARD_POST_DT_TAG
							.equals(tagName)) {
						this.atmTransaction.setCardPostDate(attrValue);
					}
					// CardFIID
					else if (DartConstants.File.CARD_FIID_TAG.equals(tagName)) {
						this.atmTransaction.setCardFiid(attrValue);
					}
					// AcctFIID
					else if (DartConstants.File.ACCT_FIID_TAG.equals(tagName)) {
						this.atmTransaction.setAcctFiid(attrValue);
					}
					// TermFIID
					else if (DartConstants.File.TERM_FIID_TAG.equals(tagName)) {
						this.atmTransaction.setTermFiid(attrValue);
					}

					// sequence number
					else if (DartConstants.File.SEQ_NUMBER_TAG.equals(tagName)) {
						this.atmTransaction.setSequenceNumber(attrValue);
					}
					// TermPostDate
					else if (DartConstants.File.TERM_POST_DATE_TAG
							.equals(tagName)) {
						this.atmTransaction.setTermPostDate(attrValue);
					}
					// Term Type
					else if (DartConstants.File.TERM_TYPE.equals(tagName)) {
						this.atmTransaction.setTerminalType(attrValue);
					}
					// credit related properties
					// Account type
					else if (DartConstants.Credit.ACT_TYPE_TAG.equals(tagName)) {
						this.atmTransaction.setAccountType(attrValue);
					}
					// ulid
					else if (DartConstants.Credit.ULID_TAG.equals(tagName)) {
						this.atmTransaction.setUlid(attrValue);
					}
					// account number
					else if (DartConstants.Credit.ACT_NUMBER_TAG
							.equals(tagName)) {
						this.atmTransaction.setAccountNumber(attrValue);
					}
					// routing number
					else if (DartConstants.Credit.RT_NUMBER_TAG.equals(tagName)) {
						this.atmTransaction.setRoutingNumber(attrValue);
					}
					// deposit amount
					else if (DartConstants.Credit.DEPOSIT_AMT_TAG
							.equals(tagName)) {
						this.atmTransaction.setDepositAmount(attrValue);
					}
					// dispensed amount
					else if (DartConstants.Transaction.DISPENSE_AMT_TAG
							.equals(tagName)) {
						this.atmTransaction.setDispenseAmount(attrValue);
					}
					// fee amount
					else if (DartConstants.Transaction.FEE_AMT_TAG
							.equals(tagName)) {
						this.atmTransaction.setFeeAmount(attrValue);
					}
				} else {
					reader.next();
				}
			} catch (Exception e) {
				throw new DartFileParserException(
						ExceptionType.PARSER_FILE_UNPARSEABLE, e.getMessage(),
						e);
			}
		}

	}

	/**
	 * This method will write the transaction item out as xml using the
	 * designated writer
	 *
	 * @param writer
	 * @throws XMLStreamException
	 */
	private void writeAsXml(XMLStreamWriter writer) throws XMLStreamException {
		// write start tag
		writer.writeStartDocument("utf-8", "1.0");
		writer.writeStartElement(DartConstants.File.TXN_TAG);
		writer.writeAttribute("xmlns", "http://eatm.chase.com/xml/dart");

		// Setting version of transaction record
		if (StringUtils.trimToNull(this.atmTransaction.getVersion()) != null) {
			writer.writeStartElement(DartConstants.File.VERSION_TAG);
			writer.writeCharacters(this.atmTransaction.getVersion());
			writer.writeEndElement();
		}

		// Term Type
		if (StringUtils.trimToNull(this.atmTransaction.getTerminalType()) != null) {
			writer.writeStartElement(DartConstants.File.TERM_TYPE);
			writer.writeCharacters(this.atmTransaction.getTerminalType());
			writer.writeEndElement();
		}

		// File related tags starts
		// setting transaction state to file
		if (StringUtils.trimToNull(this.atmTransaction.getTranType()) != null) {
			writer.writeStartElement(DartConstants.File.TRAN_TYPE);
			writer.writeCharacters(this.atmTransaction.getTranType());
			writer.writeEndElement();
		}

		// source - source is Source of the request
		if (StringUtils.trimToNull(this.atmTransaction.getSource()) != null) {
			writer.writeStartElement(DartConstants.File.SOURCE_TAG);
			writer.writeCharacters(this.atmTransaction.getSource());
			writer.writeEndElement();
		}

		// Setting ref id to file
		if (StringUtils.trimToNull(this.atmTransaction.getReferenceId()) != null) {
			writer.writeStartElement(DartConstants.File.REF_ID_TAG);
			writer.writeCharacters(this.atmTransaction.getReferenceId());
			writer.writeEndElement();
		}

		// transaction status
		if (StringUtils.trimToNull(this.atmTransaction.getStatus()) != null) {
			writer.writeStartElement(DartConstants.File.STATUS_TAG);
			writer.writeCharacters(this.atmTransaction.getStatus());
			writer.writeEndElement();
		}
		// Request Status
		if (StringUtils.trimToNull(this.atmTransaction.getRequestStatus()) != null) {
			writer.writeStartElement(DartConstants.File.REQUEST_STATUS_TAG);
			writer.writeCharacters(this.atmTransaction.getRequestStatus());
			writer.writeEndElement();
		}
		// Return Status
		if (StringUtils.trimToNull(this.atmTransaction.getReturnStatus()) != null) {
			writer.writeStartElement(DartConstants.File.RETURN_STATUS_TAG);
			writer.writeCharacters(this.atmTransaction.getReturnStatus());
			writer.writeEndElement();
		}
		// terminal id
		if (StringUtils.trimToNull(this.atmTransaction.getAtmTerminalId()) != null) {
			writer.writeStartElement(DartConstants.File.TERM_ID_TAG);
			writer.writeCharacters(this.atmTransaction.getAtmTerminalId());
			writer.writeEndElement();
		}

		// getting terminal address
		if (StringUtils.trimToNull(this.atmTransaction.getTermAddress()) != null) {
			writer.writeStartElement(DartConstants.Transaction.TERM_ADDRESS);
			writer.writeCharacters(this.atmTransaction.getTermAddress());
			writer.writeEndElement();
		}
		// getting terminal city
		if (StringUtils.trimToNull(this.atmTransaction.getTermCity()) != null) {
			writer.writeStartElement(DartConstants.Transaction.TERM_CITY);
			writer.writeCharacters(this.atmTransaction.getTermCity());
			writer.writeEndElement();
		}

		// getting terminal state
		if (StringUtils.trimToNull(this.atmTransaction.getTermState()) != null) {
			writer.writeStartElement(DartConstants.Transaction.TERM_STATE);
			writer.writeCharacters(this.atmTransaction.getTermState());
			writer.writeEndElement();
		}

		// local date time
		if (StringUtils.trimToNull(this.atmTransaction.getLocalDateTime()) != null) {
			writer.writeStartElement(DartConstants.File.LOC_DATE_TM_TAG);
			writer.writeCharacters(this.atmTransaction.getLocalDateTime());
			writer.writeEndElement();
		}
		// no.of items
		if (StringUtils.trimToNull(this.atmTransaction.getNumberOfItems()) != null) {
			writer.writeStartElement(DartConstants.File.NO_OF_DEBIT_ITEMS_TAG);
			writer.writeCharacters(this.atmTransaction.getNumberOfItems());
			writer.writeEndElement();
		}
		// no.of exception items
		if (StringUtils.trimToNull(this.atmTransaction.getNumberOfExceptionItems()) != null) {
			writer.writeStartElement(DartConstants.File.NO_OF_EXCP_ITEMS_TAG);
			writer.writeCharacters(this.atmTransaction
					.getNumberOfExceptionItems());
			writer.writeEndElement();
		}
		// no.of Escrow items
		if (StringUtils.trimToNull(this.atmTransaction.getNumberOfEscrowItems()) != null) {
			writer.writeStartElement(DartConstants.File.NO_OF_ESCROW_ITEMS_TAG);
			writer.writeCharacters(this.atmTransaction.getNumberOfEscrowItems());
			writer.writeEndElement();
		}

		// PAN
		if (StringUtils.trimToNull(this.atmTransaction.getPan()) != null) {
			writer.writeStartElement(DartConstants.File.PAN_TAG);
			writer.writeCharacters(this.atmTransaction.getPan());
			writer.writeEndElement();
		}

		// account number
		if (StringUtils.trimToNull(this.atmTransaction.getAccountNumber()) != null) {
			writer.writeStartElement(DartConstants.Credit.ACT_NUMBER_TAG);
			writer.writeCharacters(this.atmTransaction.getAccountNumber());
			writer.writeEndElement();
		}

		// AcctFIID
		if (StringUtils.trimToNull(this.atmTransaction.getAcctFiid()) != null) {
			writer.writeStartElement(DartConstants.File.ACCT_FIID_TAG);
			writer.writeCharacters(this.atmTransaction.getAcctFiid());
			writer.writeEndElement();
		}

		// deposit amount
		if (StringUtils.trimToNull(this.atmTransaction.getDepositAmount()) != null) {
			writer.writeStartElement(DartConstants.Credit.DEPOSIT_AMT_TAG);
			writer.writeCharacters(this.atmTransaction.getDepositAmount());
			writer.writeEndElement();
		}
		// Account type
		if (StringUtils.trimToNull(this.atmTransaction.getAccountType()) != null) {
			writer.writeStartElement(DartConstants.Credit.ACT_TYPE_TAG);
			writer.writeCharacters(this.atmTransaction.getAccountType());
			writer.writeEndElement();
		}

		// fee amount
		if (StringUtils.trimToNull(this.atmTransaction.getFeeAmount()) != null) {
			writer.writeStartElement(DartConstants.Transaction.FEE_AMT_TAG);
			writer.writeCharacters(this.atmTransaction.getFeeAmount());
			writer.writeEndElement();
		}
		// dispensed amount
		if (StringUtils.trimToNull(this.atmTransaction.getDispenseAmount()) != null) {
			writer.writeStartElement(DartConstants.Transaction.DISPENSE_AMT_TAG);
			writer.writeCharacters(this.atmTransaction.getDispenseAmount());
			writer.writeEndElement();
		}

		// ulid
		if (StringUtils.trimToNull(this.atmTransaction.getUlid()) != null) {
			writer.writeStartElement(DartConstants.Credit.ULID_TAG);
			writer.writeCharacters(this.atmTransaction.getUlid());
			writer.writeEndElement();
		}

		// sequence number
		if (StringUtils.trimToNull(this.atmTransaction.getSequenceNumber()) != null) {
			writer.writeStartElement(DartConstants.File.SEQ_NUMBER_TAG);
			writer.writeCharacters(this.atmTransaction.getSequenceNumber());
			writer.writeEndElement();
		}

		// getting transDate value
		if (StringUtils.trimToNull(this.atmTransaction.getTranDate()) != null) {
			writer.writeStartElement(DartConstants.File.TRAN_DATE_TAG);
			writer.writeCharacters(this.atmTransaction.getTranDate());
			writer.writeEndElement();
		}

		// getting transTime
		if (StringUtils.trimToNull(this.atmTransaction.getTranTime()) != null) {
			writer.writeStartElement(DartConstants.File.TRAN_TIME_TAG);
			writer.writeCharacters(this.atmTransaction.getTranTime());
			writer.writeEndElement();
		}

		// TranTimestamp
		if (StringUtils.trimToNull(this.atmTransaction.getTranTimeOffset()) != null) {
			writer.writeStartElement(DartConstants.Transaction.TRAN_TIME_OFFSET);
			writer.writeCharacters(this.atmTransaction.getTranTimeOffset());
			writer.writeEndElement();
		}

		// card post date
		if (StringUtils.trimToNull(this.atmTransaction.getCardPostDate()) != null) {
			writer.writeStartElement(DartConstants.File.CARD_POST_DT_TAG);
			writer.writeCharacters(this.atmTransaction.getCardPostDate());
			writer.writeEndElement();
		}
		// CardFIID
		if (StringUtils.trimToNull(this.atmTransaction.getCardFiid()) != null) {
			writer.writeStartElement(DartConstants.File.CARD_FIID_TAG);
			writer.writeCharacters(this.atmTransaction.getCardFiid());
			writer.writeEndElement();
		}

		// TermFIID
		if (StringUtils.trimToNull(this.atmTransaction.getTermFiid()) != null) {
			writer.writeStartElement(DartConstants.File.TERM_FIID_TAG);
			writer.writeCharacters(this.atmTransaction.getTermFiid());
			writer.writeEndElement();
		}

		// routing number
		if (StringUtils.trimToNull(this.atmTransaction.getRoutingNumber()) != null) {
			writer.writeStartElement(DartConstants.Credit.RT_NUMBER_TAG);
			writer.writeCharacters(this.atmTransaction.getRoutingNumber());
			writer.writeEndElement();
		}

		// TermPostDate
		if (StringUtils.trimToNull(this.atmTransaction.getTermPostDate()) != null) {
			writer.writeStartElement(DartConstants.File.TERM_POST_DATE_TAG);
			writer.writeCharacters(this.atmTransaction.getTermPostDate());
			writer.writeEndElement();
		}

		// checkList
		if (!this.atmTransaction.getAtmCheckItems().isEmpty()) {
			Iterator<AtmCheck> checkIt = this.atmTransaction.getAtmCheckItems()
					.iterator();
			while(checkIt.hasNext()) {
				AtmCheck atmCheck = checkIt.next();
				AtmCheckRecord checkRecord = new AtmCheckRecord(null);
				checkRecord.setAtmCheck(atmCheck);
				checkRecord.writeAsXml(writer);
			}

		}

		// cashList
		if (!this.atmTransaction.getAtmCashItems().isEmpty()) {
			Iterator<AtmCash> cashIt = this.atmTransaction.getAtmCashItems()
					.iterator();
			while(cashIt.hasNext()) {
				AtmCash atmCash = cashIt.next();
				AtmCashRecord cashRecord = new AtmCashRecord(null);
				cashRecord.setAtmCash(atmCash);
				cashRecord.writeAsXML(writer);
			}

		}

		writer.writeEndElement();
		writer.writeEndDocument();

	}

	/**
	 * Set the output file, call before calling writeAsXml
	 *
	 * @param file
	 * @throws IOException
	 */
	public void setOutputFile(File file) throws IOException {

		xmlOutputStream = new BufferedOutputStream(
				FileUtils.openOutputStream(file));
		isFromFile = true;

	}

	/**
	 * Set the output file as a stream, call before calling writeAsXml
	 *
	 * @param stream
	 * @throws IOException
	 */
	public void setOutputFile(OutputStream stream) throws IOException {
		if (stream == null) {

			String message = String.format("stream is passed as null");
			throw new IOException(message);
		}
		this.xmlOutputStream = stream;
		isFromFile = false;
	}

	/**
	 * This method is used to write atmTransaction to given output stream, and
	 * closes given output stream once writing is done
	 *
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	public void writeAsXml() throws IOException, XMLStreamException {
		if (xmlOutputStream == null) {
			String message = String.format("Haven't set output destination");
			throw new IOException(message);
		}

		XMLStreamWriter writer = null;
		try {
			writer = XMLOutputFactory.newInstance().createXMLStreamWriter(xmlOutputStream);
			writeAsXml(writer);
//		} catch (Exception t) {
//			String errorMsg = "Exception while writing xml";
//			throw new XMLStreamException(errorMsg);
		} finally {
			if(isFromFile) {
				IOUtils.closeQuietly(xmlOutputStream);
			}
			//closing the writer
			if(writer != null) {
				try{
					writer.close();
				} catch (Exception ex) {
					//IGNORE this exception since it is related to closing writer
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.jpmc.vpc.file.FileRecord#toByteArray()
	 */
	@Override
	public byte[] toByteArray() throws IOException {
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.jpmc.vpc.file.FileRecord#getRecordLength()
	 */
	@Override
	public int getRecordLength() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.jpmc.vpc.file.FileRecord#validate()
	 */
	@Override
	public List<String> validate() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.jpmc.vpc.file.FileRecord#getRecordName()
	 */
	@Override
	public String getRecordName() {
		return "AtmTransaction";
	}

	/**
	 *
	 * @return
	 */
	public AtmTransaction getAtmTransaction() {
		return this.atmTransaction;
	}

	public void setAtmTransaction(final AtmTransaction atmTransaction) {
		this.atmTransaction = atmTransaction;
	}


	/**
	 * @return the xmlOutputStream
	 */
	public OutputStream getXmlOutputStream() {
		return xmlOutputStream;
	}


}