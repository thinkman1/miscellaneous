package com.jpmc.dart.util.staxutils;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.events.XMLEvent;

import com.jpmc.dart.util.staxutils.predicate.StreamingXmlPath;

/**
 * Set the properties of T based on the paths set in @SetFromXpath annotated
 * properties.
 * 
 * This class is reusable, but not thread safe.
 * 
 * @author e001668
 * 
 * @param <T>
 *            Type to map to.
 */
public class StreamingXmlToObject<T> extends PathMatchingVisitor {

	private Class<T> typeToCreate;
	private Map<String, List<Field>> xpathToFields = new HashMap<String, List<Field>>();
	private StringWriter buffer = new StringWriter();
	private List<T> target = new ArrayList<T>();
	private String newObjectPath;

	public StreamingXmlToObject(Class<T> type) throws Exception {
		this.typeToCreate = type;

		CreateOnXPath cpath = this.typeToCreate.getAnnotation(CreateOnXPath.class);
		if (cpath != null) {
			this.newObjectPath = cpath.xpath();
		} else {
			// since it's not annotated to create on a path, all will get
			// funneled though
			// one.
			this.target.add(this.typeToCreate.newInstance());
		}

		for (Field f : type.getDeclaredFields()) {
			f.setAccessible(true);
			if (f.isAnnotationPresent(SetFromXpath.class)) {
				String xpath = f.getAnnotation(SetFromXpath.class).xpath();
				if (!xpathToFields.containsKey(xpath)) {
					xpathToFields.put(xpath, new ArrayList<Field>());
				}
				xpathToFields.get(xpath).add(f);
			}
		}

		StreamingXmlPath[] paths = new StreamingXmlPath[xpathToFields.keySet().size()];
		int index = 0;
		// now set up the paths to listen for
		for (String path : xpathToFields.keySet()) {
			paths[index] = new StreamingXmlPath(path);
			index++;
		}
		setPaths(paths);
	}

	public XmlEventRecorder getRecorder(StreamingXmlPath matchingPath, String activationPath) {
		XmlEventRecorder rec = new XmlEventRecorder(matchingPath, activationPath);
		rec.setFilter(XmlEventFilters.TEXT_ONLY_FILTER);
		return rec;
	}

	@Override
	public void init() {
		target.clear();
	}

	@Override
	public void match(XmlEventRecorder recorder) throws Exception {

		if (recorder.getPath().getXpath().equals(this.newObjectPath)) {
			// put a new object on the stack
			target.add(typeToCreate.newInstance());
		}

		buffer.getBuffer().setLength(0);
		recorder.write(buffer);
		String val = buffer.toString();

		// find the path you hit in the conf and set any field that has it
		// configured.
		if (xpathToFields.containsKey(recorder.getPath().getXpath())) {
			for (Field f : xpathToFields.get(recorder.getPath().getXpath())) {
				// if there isn't a value in here to set, do it now.
				if (target.isEmpty()) {
					target.add(typeToCreate.newInstance());
				}

				f.set(target.get(target.size() - 1), val);
			}
		}
	}

	public List<T> getObject() {
		return target;
	}

	@Override
	public void preMatch(CurrentDocumentState state, XMLEvent currentEvent, XmlEventRecorder rec) throws Exception {
		// don't care
	}

}
