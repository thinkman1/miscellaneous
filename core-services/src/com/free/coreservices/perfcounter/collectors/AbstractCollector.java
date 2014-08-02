package com.free.coreservices.perfcounter.collectors;

import java.io.File;
import java.util.Date;

import org.rrd4j.graph.RrdGraphDef;

import com.jpmc.dart.coreservices.util.MachineInformation;

public abstract class AbstractCollector {

	protected String fileName;
	protected String baseRrdPath;
	protected MachineInformation machineInformation;
	
	public AbstractCollector(String filePath){
		this.fileName=filePath;
	}
	
	public String getFileName() {
		File file = new File( baseRrdPath+machineInformation.getMachineName()+"-"+fileName);
		file.getParentFile().mkdirs();
		return file.getAbsolutePath() ;
	}

	public abstract void stopCollect() throws Exception;
	
	public abstract void setupCollector() throws Exception;
	
	public abstract void collect(Date timestamp) throws Exception ;
	
	public abstract void startCollection() throws Exception;
	
	public abstract void setupGraph(RrdGraphDef gDef);
	
	public long getSeconds(Date timestamp){
		return timestamp.getTime()/1000;
	}

	
	public void setBaseRrdPath(String rrdPath) {
		this.baseRrdPath = rrdPath;
	}
	
	public String getBaseRrdPath() {
		return baseRrdPath;
	}
	
	public void setBaseName(String baseName) {
		this.baseRrdPath=baseName+File.separator+"rrdFiles"+File.separator;
	}
	
	public void setMachineInformation(MachineInformation machineInformation) {
		this.machineInformation = machineInformation;
	}
}
