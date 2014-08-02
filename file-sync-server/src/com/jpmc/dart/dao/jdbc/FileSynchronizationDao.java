package com.jpmc.dart.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.jpmc.cto.dart.model.filesync.ApplicationDatacenter;
import com.jpmc.cto.dart.model.filesync.FileSynchronization;
import com.jpmc.dart.dao.mapper.FileSynchronizationMapper;
import com.jpmc.dart.dao.stmtstr.FileSynchronizationPreparedStatmentSetter;
import com.jpmc.dart.dao.util.DartSqlContextFactory;
import com.jpmc.dart.dao.util.Database;
import com.jpmc.dart.dao.util.ListSplitHelper;
import com.jpmc.vpc.model.dart.type.FileSyncStatusType;

/**
 * @author w461936
 * 
 */
public class FileSynchronizationDao extends BaseDao {

	private static final int DEFAULT_PARTITION_PERIOD = 2;
	private int partitionPeriod = DEFAULT_PARTITION_PERIOD;

	private final String insertFileSynchronization;
	private final String findFileSynchronizationById;
	private final String findFileSynchronizationByIdAndInsertDate;
	private final String findFileSynchronizationByInsertDate;
	private final String findFileSynchronizationByStatusId;
	private final String updateFileSyncInfoByIdAfterSuccess;
	private final String updateFileSyncInfoByIdAfterFail;
	private final String findUnfinishedFIleSynch;
	private final String updateFileSyncInfo;
	private final String updateFileSyncStatusTypeById;
	private final String getMinDateForFileSync;
	private final String getMinDateForStuckFileSync;
	private final String findFileSyncCountBySourceFilenameAndInsertDate;
	private final String findForwardDateListInAscendingOrder;
	private final String findBackwardDateListInDescendingOrder;
	private final String findFileSyncUnprocessedCount;
	private final String updateFileSyncInfoStart;
	private final String updateFileSyncBatch;
	private final String getMinDateForFileSyncAll;

	/**
	 * public constructor
	 */
	public FileSynchronizationDao() {
		insertFileSynchronization = DartSqlContextFactory.getSqlBean("insertFileSynchronization");
		findFileSynchronizationById = DartSqlContextFactory
				.getSqlBean("findFileSynchronizationById");
		findFileSynchronizationByIdAndInsertDate = DartSqlContextFactory
				.getSqlBean("findFileSynchronizationByIdAndInsertDate");
		findFileSynchronizationByInsertDate = DartSqlContextFactory
				.getSqlBean("findFileSynchronizationByInsertDate");

		updateFileSyncInfoByIdAfterSuccess = DartSqlContextFactory
				.getSqlBean("updateFileSyncInfoByIdAfterSuccess");
		updateFileSyncInfoByIdAfterFail = DartSqlContextFactory
				.getSqlBean("updateFileSyncInfoByIdAfterFail");
		findUnfinishedFIleSynch = DartSqlContextFactory.getSqlBean("findUnfinishedFIleSynch");
		updateFileSyncInfo = DartSqlContextFactory.getSqlBean("updateFileSyncInfo");
		updateFileSyncStatusTypeById = DartSqlContextFactory
				.getSqlBean("updateFileSyncStatusTypeById");
		getMinDateForFileSync = DartSqlContextFactory.getSqlBean("getMinDateForFileSync");
		findFileSynchronizationByStatusId = DartSqlContextFactory
				.getSqlBean("findFileSynchronizationByStatusId");
		findFileSyncCountBySourceFilenameAndInsertDate = DartSqlContextFactory
				.getSqlBean("findFileSyncCountBySourceFilenameAndInsertDate");
		findForwardDateListInAscendingOrder = DartSqlContextFactory
				.getSqlBean("findForwardDateListInAscendingOrder");
		findBackwardDateListInDescendingOrder = DartSqlContextFactory
				.getSqlBean("findBackwardDateListInDescendingOrder");

		findFileSyncUnprocessedCount = DartSqlContextFactory
				.getSqlBean("findFileSyncUnprocessedCount");
		updateFileSyncInfoStart = DartSqlContextFactory.getSqlBean("updateFileSyncInfoStart");
		updateFileSyncBatch = DartSqlContextFactory.getSqlBean("updateFileSyncBatch");
		getMinDateForFileSyncAll = DartSqlContextFactory.getSqlBean("getMinDateForFileSyncAll");
		getMinDateForStuckFileSync = DartSqlContextFactory.getSqlBean("getMinDateForStuckFileSync");

	}

	/**
	 * This method will update the information of the file_synchronization table
	 * after the filesync failed. The parameters are the values for the
	 * corresponding fields in the file_synchronization table
	 * 
	 * @param destServerName
	 * @param destFileName
	 * @param lastMsg
	 * @param id
	 * @return 1 if data is updated else return 0
	 */
	public int updateFileSyncInfoByIdAfterFail(final String destServerName,
			final String destFileName,
			final String lastMsg, final UUID id, final Date insertDate) {
		return getJdbcTemplate().update(updateFileSyncInfoByIdAfterFail,
				new Object[] { destServerName, destFileName, lastMsg, id.toString(), insertDate });

	}

	/**
	 * This method will update the information of the file_synchronization table
	 * after the fileSync executed successfully. The parameters are the value
	 * for corresponding fields in the file_synchronization table
	 * 
	 * @param copyStartTime
	 * @param copyFinishTime
	 * @param destServerName
	 * @param destFileName
	 * @param lastMsg
	 * @param id
	 * @return 1 if data is updated else return 0
	 */
	public int updateFileSyncInfoByIdAfterSuccess(final Date copyStartTime,
			final Date copyFinishTime,
			final String destServerName, final String destFileName, final String lastMsg,
			final UUID id,
			final Date insertDate) {
		return getJdbcTemplate().update(
				updateFileSyncInfoByIdAfterSuccess,
				new Object[] { copyStartTime, copyFinishTime, destServerName, destFileName,
						lastMsg, id.toString(),
						insertDate });
	}

	public int update(final FileSynchronization fileSynchronization) {
		return getJdbcTemplate().update(updateFileSyncInfo,
				new FileSynchronizationPreparedStatmentSetter(fileSynchronization, true));
	}

	public void batchUpdateStart(List<FileSynchronization> fileSynchronization) {

		final List<List<FileSynchronization>> stuff = ListSplitHelper.splitIntoBuckets(
				fileSynchronization, 400);

		// create it with a dummy object
		final FileSynchronizationPreparedStatmentSetter setter = new FileSynchronizationPreparedStatmentSetter(
				new FileSynchronization(), true);

		for (final List<FileSynchronization> stuff1 : stuff) {
			if (!stuff1.isEmpty()) {
				getJdbcTemplate().batchUpdate(updateFileSyncInfoStart,
						new BatchPreparedStatementSetter() {
							@Override
							public void setValues(PreparedStatement ps, int row)
									throws SQLException {
								FileSynchronization fileSync = stuff1.get(row);
								setter.setValues(ps, fileSync);
							}

							@Override
							public int getBatchSize() {
								return stuff1.size();
							}
						});
			}
		}
	}

	public void batchUpdate(List<FileSynchronization> fileSynchronization) {

		final List<List<FileSynchronization>> stuff = ListSplitHelper.splitIntoBuckets(
				fileSynchronization, 400);

		// create it with a dummy object
		final FileSynchronizationPreparedStatmentSetter setter = new FileSynchronizationPreparedStatmentSetter(
				new FileSynchronization(), true);

		for (final List<FileSynchronization> stuff1 : stuff) {
			if (!stuff1.isEmpty()) {
				getJdbcTemplate().batchUpdate(updateFileSyncBatch,
						new BatchPreparedStatementSetter() {
							@Override
							public void setValues(PreparedStatement ps, int row)
									throws SQLException {
								FileSynchronization fileSync = stuff1.get(row);
								setter.setValues(ps, fileSync);
							}

							@Override
							public int getBatchSize() {
								return stuff1.size();
							}
						});
			}
		}
	}

	/**
	 * @param fileSynchronization
	 *            FileSynchronization object
	 * @return 1 if data is inserted else return 0
	 */
	public int save(final FileSynchronization fileSynchronization) {
		// truncate the insertDate
		if (fileSynchronization.getInsertDate() != null) {
			fileSynchronization.setInsertDate(new java.sql.Date(DateUtils.truncate(
					fileSynchronization.getInsertDate(),
					Calendar.DAY_OF_MONTH).getTime()));
		}
		return getJdbcTemplate().update(insertFileSynchronization,
				new FileSynchronizationPreparedStatmentSetter(fileSynchronization, false));
	}

	/**
	 * Saves the list of <code>FileSynchronization</code> objects to the
	 * database in a batch mode
	 * 
	 * @param fileSynchronizations
	 *            list of FileSynchronization objects to save in a batch mode
	 */
	public void save(final List<FileSynchronization> fileSynchronizations) {
		// truncate the insertDate
		for (FileSynchronization fileSync : fileSynchronizations) {
			if (fileSync.getInsertDate() != null) {
				fileSync.setInsertDate(new java.sql.Date(DateUtils.truncate(
						fileSync.getInsertDate(), Calendar.DAY_OF_MONTH).getTime()));
			}
		}

		List<List<FileSynchronization>> listOfItems = ListSplitHelper.splitIntoBuckets(
				fileSynchronizations,
				Database.MAX_DML_CHANGES);

		for (List<FileSynchronization> list : listOfItems) {

			getJdbcTemplate().batchUpdate(insertFileSynchronization,
					new FileSynchronizationPreparedStatmentSetter(list, false));
		}
	}

	/**
	 * Gets the list of FileSynchronization for the given FILE_SYNC_STATUS_ID
	 * 
	 * @param fileSyncStatus
	 *            FILE_SYNC_STATUS_ID
	 * @param resultSize
	 *            maximum no of records to return
	 * @return a list of FileSynchronization objects
	 */
	@SuppressWarnings("unchecked")
	public List<FileSynchronization> findFileSynchronizationByStatus(
			final FileSyncStatusType fileSyncStatus,
			final int resultSize, Date insertDate, ApplicationDatacenter dataCenter) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
		jdbcTemplate.setMaxRows(resultSize);
		insertDate = DateUtils.truncate(insertDate, Calendar.DAY_OF_MONTH);
		return jdbcTemplate.query(
				findFileSynchronizationByStatusId,
				new Object[] { Integer.valueOf(fileSyncStatus.getId()),
						new java.sql.Date(insertDate.getTime()),
						Integer.valueOf(dataCenter.getId()) }, new FileSynchronizationMapper());
	}

	public FileSynchronization findFileSynchronizationByIdAndInsertDate(final UUID id,
			final Date insertDate) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());

		return (FileSynchronization) jdbcTemplate.queryForObject(
				findFileSynchronizationByIdAndInsertDate,
				new Object[] {
						id.toString(),
						new java.sql.Date(DateUtils.truncate(insertDate, Calendar.DAY_OF_MONTH)
								.getTime()) },
				new FileSynchronizationMapper());
	}

	public FileSynchronization findFileSynchronizationById(final UUID id) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());

		return (FileSynchronization) jdbcTemplate.queryForObject(findFileSynchronizationById,
				new Object[] { id.toString() },
				new FileSynchronizationMapper());
	}

	public void findFileSynchronizationByInsertDate(final Date insertDate,
			final RowCallbackHandler rowCallbackHandler) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
		jdbcTemplate.query(
				findFileSynchronizationByInsertDate,
				new Object[] { new java.sql.Date(DateUtils.truncate(insertDate,
						Calendar.DAY_OF_MONTH).getTime()) }, rowCallbackHandler);
	}

	@SuppressWarnings("unchecked")
	public List<Date> findForwardDateListInAscendingOrder(final Date date) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
		return jdbcTemplate.queryForList(findForwardDateListInAscendingOrder,
				new Object[] { new java.sql.Date(DateUtils.truncate(date, Calendar.DAY_OF_MONTH)
						.getTime()) }, Date.class);
	}

	@SuppressWarnings("unchecked")
	public List<Date> findBackwardDateListInDescendingOrder(final Date date) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
		return jdbcTemplate.queryForList(findBackwardDateListInDescendingOrder,
				new Object[] { new java.sql.Date(DateUtils.truncate(date, Calendar.DAY_OF_MONTH)
						.getTime()) }, Date.class);
	}

	public boolean isExistFileSyncWithSourceFilenameAndInsertDate(final String sourceFilename,
			final Date insertDate) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
		int count = 0;
		count = jdbcTemplate.queryForInt(findFileSyncCountBySourceFilenameAndInsertDate,
				new Object[] {
						sourceFilename,
						new java.sql.Date(DateUtils.truncate(insertDate, Calendar.DAY_OF_MONTH)
								.getTime()) });
		if (count > 0) {
			return true;
		}
		return false;

	}

	public int getUnfinishedCount(Date insertDate) {
		return getJdbcTemplate().queryForInt(
				findUnfinishedFIleSynch,
				new Object[] { new java.sql.Date(DateUtils.truncate(insertDate,
						Calendar.DAY_OF_MONTH).getTime()) });
	}

	/**
	 * This method will update the file_sync_status_id for the row with the
	 * designated id in File_Synchronization
	 * 
	 * @param status
	 *            the status to set
	 * @param id
	 *            the id the the row
	 * @return 1 if data is updated else return 0
	 */
	public int updateFileSyncStart(FileSynchronization sync) {
		return getJdbcTemplate().update(updateFileSyncInfoStart,
				new FileSynchronizationPreparedStatmentSetter(sync, true));
	}

	/**
	 * This method will update the file_sync_status_id for the row with the
	 * designated id in File_Synchronization
	 * 
	 * @param status
	 *            the status to set
	 * @param id
	 *            the id the the row
	 * @return 1 if data is updated else return 0
	 */
	public int updateFileSyncStatusTypeById(final FileSyncStatusType status, final UUID id,
			final Date insertDate) {
		return getJdbcTemplate().update(
				updateFileSyncStatusTypeById,
				new Object[] {
						new Integer(status.getId()),
						id.toString(),
						new java.sql.Date(DateUtils.truncate(insertDate, Calendar.DAY_OF_MONTH)
								.getTime()) });
	}

	/**
	 * Gets the list of FileSynchronization for the given FILE_SYNC_STATUS_ID
	 * 
	 * @param fileSyncStatus
	 *            FILE_SYNC_STATUS_ID
	 * @param resultSize
	 *            maximum no of records to return
	 * @return a list of FileSynchronization objects
	 */
	@SuppressWarnings("unchecked")
	public List<FileSynchronization> findFileSynchronizationToStart(
			final int resultSize, Date insertDate, ApplicationDatacenter dataCenter) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
		jdbcTemplate.setMaxRows(resultSize);

		return jdbcTemplate.query(
				findFileSynchronizationByStatusId,
				new Object[] { Integer.valueOf(FileSyncStatusType.READY.getId()),
						new java.sql.Date(insertDate.getTime()),
						Integer.valueOf(dataCenter.getId()) }, new FileSynchronizationMapper());
	}

	/**
	 * Get the date that the file sync service needs to use to gather sync
	 * candidates.
	 */
	public Date getMinDateForFileSync(ApplicationDatacenter currentDataCenter) {
		return (Date) getJdbcTemplate().queryForObject(getMinDateForFileSync,
				new Object[] { Integer.valueOf(currentDataCenter.getId()) }, Date.class);
	}

	/**
	 * Get the date that the file sync service needs to use to gather sync
	 * candidates.
	 */
	public Date getMinDateForFileSync() {
		return (Date) getJdbcTemplate().queryForObject(getMinDateForFileSyncAll, Date.class);
	}

	/**
	 * Get the date that the file sync service needs to use to gather stuck sync
	 * candidates.
	 */
	public Date getMinDateForFileSyncStuck() {
		return (Date) getJdbcTemplate().queryForObject(getMinDateForStuckFileSync, Date.class);
	}

	/**
	 * Get the number of items which not complete
	 * 
	 * @return
	 */
	public int findFileSyncUnprocessedCount() {
		return getJdbcTemplate().queryForInt(findFileSyncUnprocessedCount,
				new Object[] { new Timestamp(getPartitionDate().getTime()) });
	}

	/**
	 * This method is used to return partition date. This method is used by
	 * child classes of this class.
	 * 
	 * @return date
	 */
	protected Date getPartitionDate() {
		return DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DATE), -partitionPeriod);
	}
}
