package com.free.coreservices.archiver;

import java.io.File;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.Text;

import com.free.commons.util.StringBuilderUtils;

public class ListDirectoryVisitor implements ArchiveVisitor {
	private static final Log LOG = LogFactory.getLog(ListDirectoryVisitor.class);
	Writer servletPrinter;
	ArchiveConfig conf;
	String baseDir;
	StringBuilder buff = new StringBuilder();
	
	
	public ListDirectoryVisitor(String baseDir, ArchiveConfig conf,Writer output){
		this.servletPrinter=output;
		this.conf=conf;
		this.baseDir=baseDir;
	}
	
	@Override
	public void visit(Text file, ArchiverValue value) {
		buff.setLength(0);
		buff.append(baseDir);
		buff.append("/");
		buff.append(file.toString());
		buff.append("\n");
		StringBuilderUtils.replaceString(buff, "//", "/");
		StringBuilderUtils.replaceString(buff, File.separator, "/");
		try {
			servletPrinter.write(buff.toString());
		} catch (Exception e){
			LOG.error("error printing to output stream ",e);
		}
	}

}

