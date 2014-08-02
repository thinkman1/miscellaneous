package com.jpmc.dart.util.staxutils;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang.Validate;

import com.jpmc.dart.util.staxutils.predicate.StreamingXmlPath;

/**
 * provides a way to get a callback when the current state of the document
 * matches a(n) xpath(s). Any class that extends this one is NOT thread safe
 * 
 * @author E001668
 * 
 */
public abstract class PathMatchingVisitor implements StreamingXmlVisitor {
	protected StreamingXmlPath paths[];
	private List<XmlEventRecorder> recorders = new ArrayList<XmlEventRecorder>();

	public PathMatchingVisitor(String... xpaths) throws Exception {
		paths = new StreamingXmlPath[xpaths.length];
		for (int i = 0; i < xpaths.length; i++) {
			String p = xpaths[i];
			paths[i] = new StreamingXmlPath(p);
		}
	}

	public PathMatchingVisitor() {
	}

	public PathMatchingVisitor(StreamingXmlPath... paths) {
		this.paths = paths;
	}

	/**
	 * only allow child classes to set this
	 * 
	 * @param paths
	 */
	protected void setPaths(StreamingXmlPath[] paths) {
		this.paths = paths;
	}

	/**
	 * only allow child classes to set this
	 * 
	 * @param paths
	 */
	protected void setPaths(String[] xpaths) throws Exception {
		this.paths = new StreamingXmlPath[xpaths.length];
		for (int i = 0; i < paths.length; i++) {
			String p = xpaths[i];
			this.paths[i] = new StreamingXmlPath(p);
		}
	}

	public abstract void match(XmlEventRecorder recorder) throws Exception;

	public abstract void preMatch(CurrentDocumentState state, XMLEvent currentEvent, XmlEventRecorder recorder)
			throws Exception;

	public XmlEventRecorder getRecorder(StreamingXmlPath matchingPath, String activationPath) {
		return new XmlEventRecorder(matchingPath, activationPath);
	}

	@Override
	public void visit(CurrentDocumentState state, XMLEvent currentEvent) throws Exception {
		if (currentEvent.isStartElement()) {
			for (StreamingXmlPath xp : paths) {
				if (xp.matches(state)) {
					XmlEventRecorder rec = getRecorder(xp, state.getCurrentPath().toString());
					recorders.add(rec);
					preMatch(state, currentEvent, rec);
				}
			}
		}

		ListIterator<XmlEventRecorder> recordIt = recorders.listIterator();
		while (recordIt.hasNext()) {
			XmlEventRecorder recorder = recordIt.next();
			if (!recorder.record(currentEvent, state.getCurrentPath())) {
				recordIt.remove();
				match(recorder);
			}
		}

		if (currentEvent.isEndDocument()) {
			Validate.isTrue(recorders.isEmpty(),
					"there's a bug, there should be no active records when the document is done");
		}
	}
}
