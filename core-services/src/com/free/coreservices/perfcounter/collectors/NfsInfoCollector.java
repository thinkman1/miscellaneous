package com.free.coreservices.perfcounter.collectors;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.graph.RrdGraphDef;
import org.springframework.beans.factory.DisposableBean;


/**
 * Collect the NFS stats
 * @author E001668
 *
 */
public class NfsInfoCollector extends AbstractCollector implements DisposableBean{
	private static final Log LOG = LogFactory.getLog(NfsInfoCollector.class);
	private RrdDb rrdDb;

	private long firstCollect=0;
	private long lastCollect=0;

	public NfsInfoCollector(){
		super("NFSStats");
	}

	@Override
	public void stopCollect() throws Exception {
		rrdDb.close();
	}


	@Override
	public void setupCollector() throws Exception {
		String filePath = getFileName();
		if (!new File(filePath).exists()){
			RrdDef rrdDef = new RrdDef(filePath,300);
			rrdDef.addDatasource("nfs operations", DsType.COUNTER, 1000, Double.NaN, Double.NaN);
			rrdDef.addArchive(ConsolFun.TOTAL, 0.5,1 , 288); // 1 step, 288 rows -> take total
			RrdDb db = new RrdDb(rrdDef);
			db.close();
		}
	}

	@Override
	public void startCollection() throws Exception {
		rrdDb = new RrdDb(getFileName());
	}

	@Override
	public void destroy() throws Exception {
		rrdDb.close();
	}

	@Override
	public void collect(Date timestamp) throws Exception {

		Validate.isTrue(rrdDb!=null);

		lastCollect=timestamp.getTime()/1000;
		if (firstCollect==0){
			firstCollect=lastCollect;
		}

		LOG.info("collect NFS stats...");

		Process p = Runtime.getRuntime().exec("/usr/sbin/nfsstat");

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			List<String> lines = new ArrayList<String>();
			String line = reader.readLine();
			while (line!=null){
				lines.add(line);
				line = reader.readLine();
			}

			// total calls is what we care about, line 3
			String linez=StringUtils.trim(lines.get(2).split(" ")[0]);

			LOG.debug("total nfs operations is "+linez);

			Sample sample = rrdDb.createSample();
		    sample.setTime(getSeconds(timestamp));
		    sample.setValue("nfs operations",Double.parseDouble(linez));
		    sample.update();
		}
		finally {
			IOUtils.closeQuietly(reader);
		}

	}

	public void setupGraph(RrdGraphDef gDef){
		gDef.datasource("nfs ops total", getFileName(), "nfs operations",ConsolFun.TOTAL);
		gDef.line("nfs ops total", Color.BLACK, "total nfs operations", 2);

		gDef.setTitle("NFS Operations");
		gDef.setVerticalLabel("NFS Operations");
	}
}
