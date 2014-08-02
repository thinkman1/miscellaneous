package com.jpmc.dart.util.staxutils;

import javax.xml.stream.events.XMLEvent;

/**
 * The visitor interface to walk XML documents
 * 
 * @author E001668
 * 
 */
public interface StreamingXmlVisitor {
	void init();

	void visit(CurrentDocumentState state, XMLEvent currentEvent) throws Exception;
}
