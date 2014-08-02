package com.free.coreservices.perfcounter;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.free.util.EventHandlerBase;
import com.jpmc.cto.dart.model.system.PerformanceCounter;
import com.jpmc.cto.framework.Event;
import com.jpmc.cto.framework.EventType;
import com.jpmc.cto.framework.exception.ApplicationException;
import com.jpmc.dart.dao.jdbc.PerformanceCounterDao;
import com.jpmc.dart.dao.mapper.PerformanceCounterMapper;
import com.jpmc.dart.filesync.client.FileSyncClient;
import com.jpmc.vpc.model.dart.event.ProcessDateChangedEvent;

public class PerformanceCounterExtract extends EventHandlerBase {
	//private static final Log LOG = LogFactory.getLog(PerformanceCounterExtract.class);

	private static final int MAX_ROWS = 1000;

	private static class ExtractResults implements RowCallbackHandler {
		ExtractResults(PerformanceCounterMapper map,Writer writer){
			this.mapp=map;
			this.out=writer;
		}

		PerformanceCounterMapper mapp;
		Writer out;
		StringBuilder buff = new StringBuilder();
		FastDateFormat proc = FastDateFormat.getInstance("yyyyMMdd");
		FastDateFormat timestamp = FastDateFormat.getInstance("yyyyMMdd HH:mm:ss");

		@Override
		public void processRow(ResultSet rs) throws SQLException {
			PerformanceCounter count = (PerformanceCounter) this.mapp.mapRow(rs, 1);
			try {
				buff.setLength(0);

				buff.append(count.getId().toString());
				buff.append(",");
				buff.append(count.getMachine());
				buff.append(",");
				buff.append(count.getService());
				buff.append(",");
				buff.append(count.getCounterType().toString());
				buff.append(",");
				buff.append(count.getCounterValue());
				buff.append(",");
				buff.append(timestamp.format(count.getTimestamp()));
				buff.append(",");
				buff.append(count.getMiscData());
				buff.append(",");
				if (count.getCorrelationId()!=null){
					buff.append(count.getCorrelationId().toString());
				} else {
					buff.append(" ");
				}
				buff.append(",");
				buff.append(proc.format(count.getProcessDate()));
				buff.append("\n");

				out.write(buff.toString());
			} catch (IOException e){
				throw new RuntimeException(e);
			}
		}
	}

	@Autowired
	FileSyncClient fileSyncClient;

	@Autowired
	PerformanceCounterDao performanceCounterDao;

	private static final EventType events[] = new EventType[]{
		com.jpmc.vpc.model.dart.type.EventType.PROCESS_DATE_CHANGED
	};

	@Override
	public String getName() {
		return "performance counter extract";
	}

	@Override
	public EventType[] getAcceptedEvents() {
		return events;
	}

	@Override
	public void processEvent(Event arg0) throws ApplicationException {
		ProcessDateChangedEvent eve = (ProcessDateChangedEvent) arg0;

		try {
//			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
//
//			File outputFile = new File(fileNameUtils.getBaseDir()+File.separator+"performance-stats"+File.separator+sdf.format(eve.getOldProcDate()));
//			outputFile.mkdirs();
//
//			outputFile = new File(outputFile,"extract-performance-counters.csv.gz");
//
//
//			LOG.info("extract performance data to "+outputFile.getAbsolutePath());
//
//			Writer fout = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputFile)));
//
//			ExtractResults extract = new ExtractResults(new PerformanceCounterMapper(), fout);
//			performanceCounterDao.executeOverProcDate(eve.getOldProcDate(), extract, MAX_ROWS);
//
//			IOUtils.closeQuietly(fout);
//
//			// sync to other data center
//			fileSyncUtil.startFileSync(outputFile.getAbsolutePath());

		} catch (Exception e){
			throw new ApplicationException(e);
		}
	}
}
