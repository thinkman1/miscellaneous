package com.jpmc.dart.dao.stmtstr;

public class TransactionDao {
	public void saveTransaction(final DartTransaction singleObject) {
		getJdbcTemplate().update(saveTransactionQuery,
				new TransactionBatchPreparedStatementSetter(singleObject));
	}

	/**
	 * Saves the list of <code>DartTransaction</code> objects to the database in
	 * a batch mode
	 *
	 * @param objects
	 *            list of objects to save in a batch mode
	 */
	public void saveTransaction(final List<DartTransaction> objects) {
		List<List<DartTransaction>> listOfItems = ListSplitHelper
				.splitIntoBuckets(objects, Database.MAX_DML_CHANGES);

		for (List<DartTransaction> list : listOfItems) {
			getJdbcTemplate().batchUpdate(saveTransactionQuery,
					new TransactionBatchPreparedStatementSetter(list));
		}
	}

	/**
	 * This method is used to update transaction status for the given transId.
	 *
	 * @param status
	 *            transaction status to update
	 * @param transId
	 *            the transactionid
	 * @param procDate
	 *            the process date
	 * @return no.of rows updated. 0 if none updated
	 */
	public int updateTxnStatus(final DartTransactionStatus status,
			final UUID transId, final Date procDate) {

		Integer statusId = Integer.valueOf(status.getId());

		return getJdbcTemplate()
				.update(updateTxnStatusByIdAndProcDate,
						new Object[] {statusId, transId.toString(),
								new java.sql.Date(procDate.getTime()),
								statusId });
	}

	/**
	 * This method is used to get DartTransaction for the given transaction id
	 * and process date.
	 *
	 * @param id
	 *            transaction id
	 * @param procDate
	 *            process date
	 * @return DartTransaction Object if found otherwise null
	 */
	public DartTransaction getTransaction(final UUID txnId, final Date procDate) {
		DartTransaction singleObject = null;
		try {
			singleObject = (DartTransaction) getJdbcTemplate().queryForObject(
					getTxnByIdAndProcDate,
					new Object[] { txnId.toString(),
							new java.sql.Date(procDate.getTime()) },
					new TransactionMapper());
		} catch (IncorrectResultSizeDataAccessException e) {
			// JUST IGNORE THIS EXCEPTION
		}
		return singleObject;
	}
}
