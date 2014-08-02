package com.free.coreservices.perfcounter.collectors;

import java.awt.Color;
import java.io.File;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.graph.RrdGraphDef;

import com.jpmc.dart.coreservices.metricreport.MetricType;

/**
 * Monitors files that come into DART and what goes out to posting
 * @author e001668
 *
 */
public class InputOutputStatsCollector extends JmxCollector {
	private static final Log LOG = LogFactory.getLog(InputOutputStatsCollector.class);
	private RrdDb collectDb;

	public InputOutputStatsCollector(){
		super("InputOutputs");
	}

	@Override
	public void stopCollect() throws Exception {
		collectDb.close();
	}


	@Override
	public void setupCollector() throws Exception {
		String filePath = getFileName();
		if (!new File(filePath).exists()){
			RrdDef rrdDef = new RrdDef(filePath,300);
			rrdDef.addDatasource("input files", DsType.COUNTER, 1000, Double.NaN, Double.NaN);
			rrdDef.addDatasource("posted to vpc", DsType.COUNTER, 1000, Double.NaN, Double.NaN);
			rrdDef.addDatasource("posted to drq", DsType.COUNTER, 1000, Double.NaN, Double.NaN);
			rrdDef.addArchive(ConsolFun.TOTAL, 0.5,1 , 576); // 1 step, 576 rows -> take last sampled value (48 hours of data -> 300 seconds is one sample * 576 samples)

			RrdDb db = new RrdDb(rrdDef);


			// record the total
//			Sample sample = db.createSample();
//		    sample.setTime(getSeconds(new Date()));
//		    sample.setValue("input files",0);
//		    sample.setValue("posted to drq",0);
//		    sample.setValue("posted to vpc",0);
//		    sample.update();


			db.close();
		}
	}

	@Override
	public void collect(Date timestamp) throws Exception {

		// ask all the xml agg services you know about for the value of the DRQ_POST metric
		List<JmxStatBean> stats = poll(MetricType.DRQ_POST, "dart-xml-aggregator");

		double drqCount=0;

		for (JmxStatBean jm : stats){
			drqCount+= Double.parseDouble(jm.getCount());
		}

		// ask all the xml agg services you know about for the value of the VPC_POST metric
		stats = poll(MetricType.VPC_POST, "dart-xml-aggregator");

		double vpcCount=0;

		for (JmxStatBean jm : stats){
			vpcCount+= Double.parseDouble(jm.getCount());
		}

		// ask all the landing zones you know about for the value of the LANDING_ZONE_FILES metric
		stats = poll(MetricType.LANDING_ZONE_FILES, "DartHttpLandingService");

		double inputCount=0;

		for (JmxStatBean jm : stats){
			inputCount+= Double.parseDouble(jm.getCount());
		}


		LOG.debug("I saw this many transactions to VPC "+vpcCount);
		LOG.debug("I saw this many transactions to DRQ "+drqCount);
		LOG.debug("I saw this many transactions come in to dart "+inputCount);

		// record the total
		Sample sample = collectDb.createSample();
	    sample.setTime(getSeconds(timestamp));
	    sample.setValue("input files",inputCount);
	    sample.setValue("posted to drq",drqCount);
	    sample.setValue("posted to vpc",vpcCount);
	    sample.update();

	}

	@Override
	public void startCollection() throws Exception {
		collectDb=new RrdDb(getFileName());
	}

	@Override
	public void setupGraph(RrdGraphDef gDef) {
		// create a virtual datasource from the 'input files' data source
		gDef.datasource("input files increase", getFileName(), "input files",ConsolFun.TOTAL);
		// graph the value of the virtual datasource and give it a legend of 'DART input files
		gDef.line("input files increase", Color.RED, "DART input files", 2);

		gDef.datasource("drq files increase", getFileName(), "posted to drq",ConsolFun.TOTAL);
		gDef.line("drq files increase", Color.BLACK, "Posted to DRQ", 2);

		gDef.datasource("vpc files increase", getFileName(), "posted to vpc",ConsolFun.TOTAL);
		gDef.line("vpc files increase", Color.BLUE, "Posted to VPC", 2);

		gDef.setTitle("Input Files and posting outputs");
		gDef.setVerticalLabel("Input Files / Posting output counts");
		//gDef.setUnit("");
	}
}
