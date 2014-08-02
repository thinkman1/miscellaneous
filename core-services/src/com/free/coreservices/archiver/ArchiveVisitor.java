package com.free.coreservices.archiver;

import org.apache.hadoop.io.Text;

public interface ArchiveVisitor {
	void visit(Text file, ArchiverValue value);
}
