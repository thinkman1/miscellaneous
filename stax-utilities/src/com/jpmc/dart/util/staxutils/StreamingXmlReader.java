package com.jpmc.dart.util.staxutils;

import java.io.InputStream;
import java.io.Reader;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang.StringUtils;

import com.jpmc.dart.util.staxutils.predicate.StreamingXmlPath;

/**
 * provides a visitor interface with state tracking for processing XML
 * documents.
 * 
 * @see StreamingXmlPath for supported paths
 * @author e001668
 * 
 */
public class StreamingXmlReader {

	// XMLInputfactory isn't thread safe....
	ThreadLocal<XMLInputFactory> inputFactory = new ThreadLocal<XMLInputFactory>();

	public XMLInputFactory getFactory() {
		if (inputFactory.get() == null) {
			inputFactory.set(XMLInputFactory.newInstance());
		}
		return inputFactory.get();
	}

	public void vistXml(Reader source, StreamingXmlVisitor... visitors) throws Exception {
		vistXml(getFactory().createXMLEventReader(source), visitors);
	}

	public void vistXml(InputStream source, StreamingXmlVisitor... visitors) throws Exception {
		vistXml(getFactory().createXMLEventReader(source), visitors);
	}

	public static void buildXpath(StringBuilder buffer, List<StartElement> elements) {
		buffer.setLength(0);
		buffer.append("/");
		for (int i = 0; i < elements.size(); i++) {
			StartElement ele = elements.get(i);
			if (!StringUtils.isEmpty(ele.getName().getPrefix())) {
				buffer.append(ele.getName().getPrefix());
				buffer.append(":");
			}
			buffer.append(ele.getName().getLocalPart());

			if (i < elements.size() - 1) {
				buffer.append("/");
			}
		}
	}

	public void vistXml(XMLEventReader source, StreamingXmlVisitor... visitors) throws Exception {
		StringBuilder currentPath = new StringBuilder();
		CurrentDocumentState state = new CurrentDocumentState();

		state.setCurrentPath(currentPath);

		for (StreamingXmlVisitor v : visitors) {
			v.init();
		}

		while (source.hasNext()) {
			XMLEvent event = source.nextEvent();
			if (event.isStartDocument()) {
				currentPath.setLength(0);
				currentPath.append("/");
			} else if (event.isStartElement()) {
				StartElement ele = (StartElement) event;
				state.getStartElements().add(ele);
				buildXpath(currentPath, state.getStartElements());
				state.incrementPathCount(currentPath);
			} else if (event.isEndElement()) {
				buildXpath(currentPath, state.getStartElements());
				state.getStartElements().remove(state.getStartElements().size() - 1);
			} else if (event.isEndDocument()) {
				currentPath.setLength(0);
				currentPath.append("/");
			}

			for (StreamingXmlVisitor visitor : visitors) {
				visitor.visit(state, event);
			}

			if (event.isEndElement()) {
				state.prune(currentPath);
			}
		}
	}
}
