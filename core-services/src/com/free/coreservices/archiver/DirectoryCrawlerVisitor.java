package com.free.coreservices.archiver;

import java.io.File;

public interface DirectoryCrawlerVisitor {
	public void visit(File file);
}
