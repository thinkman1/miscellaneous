package com.free.coreservices.perfcounter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.free.coreservices.perfcounter.collectors.AbstractCollector;
import com.jpmc.dart.helpers.ManagedTempFiles;

public class MetricGraphRrenderer implements ApplicationContextAware  {
	// TODO:
	// when collection done (by rrd file) event is received:
	// - generate graph with RRD data
	// - dump to file
	// - mmap the file?
	
	private static final Log LOG = LogFactory.getLog(MetricGraphRrenderer.class);
	
	private ManagedTempFiles managedTempFiles;
	
	private ApplicationContext context;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context=applicationContext;
	}
	
	
	public void draw(int width, int height , long startTime, long endTime, String targetFile,RrdDb db){
		
	}
	
	public void draw(int width, int height , long startTime, long endTime, String targetFile,AbstractCollector collector){

		try {
			RrdGraphDef gDef = new RrdGraphDef();

			gDef.setWidth(500);
			gDef.setHeight(300);
			gDef.setFilename(targetFile);
			gDef.setLazy(false);
			gDef.hrule(startTime, Color.BLUE);
			
			//TODO: figure out what a good min value would be.  maybe ask the RRD file?
			gDef.hrule(1, Color.BLUE);
			
			//gDef.setMaxValue(maxValue)
			//gDef.setMinValue(minValue)
			gDef.setStartTime(startTime);
			gDef.setEndTime(endTime);
			
			
			gDef.setAltAutoscale(true);

			collector.setupGraph(gDef);
			
			gDef.setFilename(targetFile);
			
			//gDef.setValueAxis(1000, 5);
//			gDef.setLazy(false);
			
			RrdGraph graph = new RrdGraph(gDef);
			BufferedImage bi = new BufferedImage(500,300,BufferedImage.TYPE_INT_RGB);
			graph.render(bi.getGraphics());
			

			
		} catch (Exception e){
			LOG.warn("crap, this happened when trying to draw a graph ",e);
		}
	}
	
	/**
	 * dump a graph to 
	 * @param output
	 * @param startTime
	 * @param endTime
	 * @throws Exception
	 */
	public void writeGraph(String collectorName,OutputStream output, long startTime, long endTime) throws Exception {
		File temp = managedTempFiles.getManagedTempFile("graph", ".png"); 
		
		AbstractCollector collector=context.getBean(collectorName,AbstractCollector.class);
		
		draw(500, 300,startTime,endTime, temp.getAbsolutePath(),collector);
		
		FileInputStream fin = new FileInputStream(temp);
		try {
			fin.getChannel().transferTo(0, temp.length(), Channels.newChannel(output));
		} catch (Exception e){
			LOG.error("whoa, that's bad",e);
		} finally {
			IOUtils.closeQuietly(fin);
		}
		
	}

	public void setManagedTempFiles(ManagedTempFiles managedTempFiles) {
		this.managedTempFiles = managedTempFiles;
	}
	
}
