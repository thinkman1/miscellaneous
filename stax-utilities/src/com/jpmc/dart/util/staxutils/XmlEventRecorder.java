package com.jpmc.dart.util.staxutils;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.EventFilter;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.jpmc.dart.util.staxutils.predicate.StreamingXmlPath;

/**
 * records XML events so they can be played back to a writer.
 * 
 * @author e001668
 * 
 */
public class XmlEventRecorder {
	private List<XMLEvent> events = new ArrayList<XMLEvent>();
	protected StreamingXmlPath path;
	protected String activationPath;
	protected EventFilter filter = XmlEventFilters.NOOP_FILTER;

	public XmlEventRecorder(StreamingXmlPath path, String activationPath) {
		this.path = path;
		this.activationPath = activationPath;
	}

	public boolean record(XMLEvent event, StringBuilder currentPath) throws Exception {

		if (activationPath.contentEquals(currentPath)) {
			if (event.isStartElement()) {
				if (path.isSelectProperty()) {
					events.add(event);
					// if this is a select property and we've matched the path,
					// add the first start element and return false to stop
					// receiving events
					return false;
				}
			}
			if (event.isEndElement()) {
				if (filter.accept(event)) {
					events.add(event);
				}
				// return false to tell the code you don't want any more events.
				return false;
			}
		}

		if (filter.accept(event)) {
			events.add(event);
		}

		return true;
	}

	/**
	 * visit all the data that has been recorded.
	 * 
	 * @param visitors
	 * @throws Exception
	 */
	public void visitData(StreamingXmlVisitor... visitors) throws Exception {
		CurrentDocumentState state = new CurrentDocumentState();
		state.setCurrentPath(new StringBuilder());

		// visit all the recorded events.
		for (XMLEvent eve : this.events) {
			if (eve.isStartElement()) {
				StartElement ele = (StartElement) eve;
				state.getStartElements().add(ele);
				StreamingXmlReader.buildXpath(state.getCurrentPath(), state.getStartElements());
				state.incrementPathCount(state.getCurrentPath());
			} else if (eve.isEndElement()) {
				StreamingXmlReader.buildXpath(state.getCurrentPath(), state.getStartElements());
				state.getStartElements().remove(state.getStartElements().size() - 1);
			}

			for (StreamingXmlVisitor wiz : visitors) {
				wiz.visit(state, eve);
			}

			if (eve.isEndElement()) {
				state.prune(state.getCurrentPath());
			}
		}
	}

	/**
	 * convert the data to a LDom object. If you intend on using this, don't set
	 * a filter with setFilter()
	 * 
	 * @return
	 */
	public LDom toLdom() {
		ArrayList<LDom> elements = new ArrayList<LDom>();

		for (XMLEvent ev : this.events) {
			if (ev.isStartElement()) {
				LDom domit = new LDom();
				domit.init(ev);
				if (!elements.isEmpty()) {
					elements.get(elements.size() - 1).getChildren().add(domit);
				}
				elements.add(domit);
			} else if (ev.isCharacters()) {
				elements.get(elements.size() - 1).getData().append(((Characters) ev).getData());
			} else if (ev.isEndElement()) {
				elements.remove(elements.size() - 1);
			}
		}

		return elements.get(0);
	}

	/**
	 * Somewhat expensive way query the data using xpaths. see StreamingXmlPath
	 * for supported xpath expressions.
	 * 
	 * NOTE: if you plan on using this, don't specify a filter with setFilter()
	 * 
	 * @param xpath
	 * @return
	 */
	public List<String> selectData(String xpath) throws Exception {
		SimpleDataSelector wiz = new SimpleDataSelector(xpath);
		visitData(wiz);
		return wiz.getData();
	}

	public void write(Writer writer) throws Exception {
		if (this.path.isSelectProperty()) {
			StartElement ele = (StartElement) events.get(0);
			writer.write(ele.getAttributeByName(this.path.getSelectPropertyName()).getValue());
			return;
		}
		for (XMLEvent e : events) {
			e.writeAsEncodedUnicode(writer);
		}
	}

	public StreamingXmlPath getPath() {
		return path;
	}

	public void setFilter(EventFilter filter) {
		this.filter = filter;
	}

	public String getActivationPath() {
		return activationPath;
	}
}
