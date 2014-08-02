package com.jpmc.dart.util.staxutils;

import java.io.StringWriter;

import javax.xml.stream.events.XMLEvent;

import com.jpmc.dart.util.staxutils.predicate.StreamingXmlPath;

/**
 * provides a callback when an XML path is hit
 * 
 * @author E001668
 * 
 */
public class CallbackOnMatch extends PathMatchingVisitor {
	private MatchCallback callback;
	private StringWriter writer = new StringWriter();

	public CallbackOnMatch(StreamingXmlPath path, MatchCallback callback) throws Exception {
		super(path);
		this.callback = callback;
	}

	public CallbackOnMatch(String path, MatchCallback callback) throws Exception {
		super(path);
		this.callback = callback;
	}

	@Override
	public void init() {
		writer.getBuffer().setLength(0);
	}

	@Override
	public void match(XmlEventRecorder recorder) throws Exception {
		if (callback instanceof XmlEventRecorderCallback) {
			callback.callback(recorder);
		} else if (callback instanceof StringCallback) {
			writer.getBuffer().setLength(0);
			recorder.write(writer);
			callback.callback(writer.toString());
		}
	}

	@Override
	public void preMatch(CurrentDocumentState state, XMLEvent currentEvent, XmlEventRecorder recorder) throws Exception {

	}
}
