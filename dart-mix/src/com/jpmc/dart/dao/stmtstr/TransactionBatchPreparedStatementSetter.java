package com.jpmc.dart.dao.stmtstr;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.StatementCreatorUtils;

import com.jpmc.dart.dao.util.DaoConstants;
import com.jpmc.vpc.model.dart.check.DartTransaction;

/**
 * BatchPreparedStatementSetter class for bean
 * <code>com.jpmc.vpc.model.dart.check.DartTransaction</code> and table DART_TRANSACTION
 *
 * @see com.jpmc.vpc.model.dart.check.DartTransaction
 */
public class TransactionBatchPreparedStatementSetter extends BasePreparedStatementSetter<DartTransaction> implements
		BatchPreparedStatementSetter, PreparedStatementSetter {

	private static final Log LOG = LogFactory.getLog(TransactionBatchPreparedStatementSetter.class);

	/**
	 * @param singleObject
	 *            single object to save
	 */
	public TransactionBatchPreparedStatementSetter(final DartTransaction singleObject) {
		super(singleObject);
	}

	/**
	 * @param items
	 *            list of items to save or update
	 */
	public TransactionBatchPreparedStatementSetter(final List<DartTransaction> items) {
		super(items);
	}

	/**
	 * @param items
	 *            list of items
	 * @param isUpdate
	 *            if true is an update
	 */
	public TransactionBatchPreparedStatementSetter(final List<DartTransaction> items, final boolean isUpdate) {
		super(items, isUpdate);
	}

	/**
	 * Sets the objects on the prepared statement
	 *
	 * @param ps
	 *            statement to set values on
	 * @param setMe
	 *            object to get the values from and put in the database
	 * @throws SQLException
	 *             if there was a problem setting on the <code>ps</code> *
	 * @see com.jpmc.vpc.dao.stmtstr.BasePreparedStatementSetter#setValues(java.sql.PreparedStatement,
	 *      java.lang.Object)
	 */
	protected void setValues(final PreparedStatement ps, final DartTransaction setMe) throws SQLException {

		StatementCreatorUtils.cleanupParameters(new Object[0]);

		// Table - TRANSACTION
		int i = 1;

		StatementCreatorUtils.setParameterValue(ps, i++, Types.CHAR, setMe.getId().toString()); // ID
		StatementCreatorUtils.setParameterValue(ps, i++, Types.DATE, setMe.getProcessDate()); // PROC_DATE
		StatementCreatorUtils.setParameterValue(ps, i++, Types.NUMERIC, Integer.valueOf(setMe.getCreditCount())); // CREDIT_COUNT
		StatementCreatorUtils.setParameterValue(ps, i++, Types.NUMERIC, Integer.valueOf(setMe.getDebitCount())); // DEBIT_COUNT
		StatementCreatorUtils.setParameterValue(ps, i++, Types.NUMERIC, Long.valueOf(setMe.getCreditAmount())); // CREDIT_AMOUNT
		StatementCreatorUtils.setParameterValue(ps, i++, Types.NUMERIC, Long.valueOf(setMe.getDebitAmount())); // DEBIT_AMOUNT
		StatementCreatorUtils.setParameterValue(ps, i++, Types.NUMERIC, setMe.getStatus() != null ? Integer.valueOf(setMe.getStatus().getId()) : null); // STATUS
		StatementCreatorUtils.setParameterValue(ps, i++, Types.NUMERIC, setMe.getCategory() != null ? Integer.valueOf(setMe.getCategory().getId()) : null); // CATEGORY
		UUID fileId = setMe.getFileId();
		StatementCreatorUtils.setParameterValue(ps, i++, Types.CHAR, fileId != null ? fileId.toString() : null); // FILE_ID
		StatementCreatorUtils.setParameterValue(ps, i++, Types.TIMESTAMP, new Timestamp(setMe.getCreateTime().getTime())); // CREATE_TIME
		StatementCreatorUtils.setParameterValue(ps, i++, Types.NUMERIC, setMe.getSequence() != null && setMe.getSequence().intValue() >=0 ? setMe.getSequence() : null); // SEQUENCE_NUMBER
		StatementCreatorUtils.setParameterValue(ps, i++, Types.TIMESTAMP, new Timestamp(setMe.getCreateTime().getTime())); // LAST_UPDATED_DATE - nice to have these match

		// ATM_TRAN_LOCAL_TIMESTAMP - yes this is a timestamp with timezone in the DB but there is no way
		// from Java to set it as such - at least not that we could find.
		if (setMe.getTransactionTimestamp() != null) {
			StatementCreatorUtils.setParameterValue(ps, i++, Types.VARCHAR,
					FastDateFormat.getInstance(DaoConstants.TS_TZ_FORMAT).format(setMe.getTransactionTimestamp()));
		} else {
			StatementCreatorUtils.setParameterValue(ps, i++, Types.VARCHAR, null);
		}

		StatementCreatorUtils.setParameterValue(ps, i++, Types.NUMERIC, setMe.getTransactionSource() != null ? Integer.valueOf(setMe.getTransactionSource().getId()) : null); // TXN_SOURCE
		StatementCreatorUtils.setParameterValue(ps, i++, Types.NUMERIC, setMe.getTransactionStatus() != null ? Integer.valueOf(setMe.getTransactionStatus().getId()) : null); // TXN_STATUS
		StatementCreatorUtils.setParameterValue(ps, i++, Types.NUMERIC, setMe.getTransactionReturnStatus() != null ? Integer.valueOf(setMe.getTransactionReturnStatus().getId()) : null); // TXN_RETURN_STATUS
		StatementCreatorUtils.setParameterValue(ps, i++, Types.NUMERIC, setMe.getTransactionRequestStatus() != null ? Integer.valueOf(setMe.getTransactionRequestStatus().getId()) : null); // TXN_REQUEST_STATUS
		StatementCreatorUtils.setParameterValue(ps, i++, Types.NUMERIC, setMe.getTransactionType() != null ? Integer.valueOf(setMe.getTransactionType().getId()) : null); // TXN_TYPE
		StatementCreatorUtils.setParameterValue(ps, i++, Types.DATE, setMe.getAtmBusinessDate()); // ATM_BUSINESS_DATE
		StatementCreatorUtils.setParameterValue(ps, i++, Types.INTEGER, setMe.getDaysOld()); // DAYS_OLD
		StatementCreatorUtils.setParameterValue(ps, i++, Types.VARCHAR, setMe.getPan()); // PAN
		StatementCreatorUtils.setParameterValue(ps, i++, Types.INTEGER, setMe.getLocationId()); // LOCATION_ID
		StatementCreatorUtils.setParameterValue(ps, i++, Types.VARCHAR, setMe.getAtmId()); // ATM_ID
		int passThruToInt = BooleanUtils.toInteger(setMe.isPassThru());
		StatementCreatorUtils.setParameterValue(ps, i++, Types.INTEGER, Integer.valueOf(passThruToInt)); // PASS_THRU
		StatementCreatorUtils.setParameterValue(ps, i++, Types.VARCHAR, setMe.getBeneficiaryEci()); // BENEFICIARY_ECI
		StatementCreatorUtils.setParameterValue(ps, i++, Types.VARCHAR, setMe.getConductorEci()); // CONDUCTOR_ECI
		StatementCreatorUtils.setParameterValue(ps, i++, Types.VARCHAR, setMe.getClientId()); // CLIENT_ID

		LOG.debug("Transaction Statement Setter Index:  " + (i - 1));
	}
}