package com.jpmc.dart.util.staxutils;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.events.XMLEvent;

import com.jpmc.dart.util.staxutils.predicate.StreamingXmlPath;

/**
 * Selects data from an XML document. This object is reusable but not thread
 * safe.
 * 
 * @author e001668
 * 
 */
public class SelectDataVisitor extends PathMatchingVisitor {
	private StringWriter buffer = new StringWriter();
	private SelectDataVisitorConfig selectConf[];
	private Map<String, List<String>> selectedData = new HashMap<String, List<String>>();
	boolean convertXmlReservedCharacters = false;

	public SelectDataVisitor(List<SelectDataVisitorConfig> confs) throws Exception {
		this.selectConf = new SelectDataVisitorConfig[confs.size()];
		confs.toArray(this.selectConf);
		configurePaths();
	}

	public SelectDataVisitor(SelectDataVisitorConfig... confs) throws Exception {
		this.selectConf = confs;
		configurePaths();
	}

	public SelectDataVisitor(boolean convertXmlReservedCharacters, List<SelectDataVisitorConfig> confs)
			throws Exception {
		this.selectConf = new SelectDataVisitorConfig[confs.size()];
		confs.toArray(this.selectConf);
		configurePaths();
		this.convertXmlReservedCharacters = convertXmlReservedCharacters;
	}

	public SelectDataVisitor(boolean convertXmlReservedCharacters, SelectDataVisitorConfig... confs) throws Exception {
		this.selectConf = confs;
		configurePaths();
		this.convertXmlReservedCharacters = convertXmlReservedCharacters;
	}

	private void configureDataVistorConfig() throws Exception {
		this.selectConf = new SelectDataVisitorConfig[this.paths.length];
		for (int i = 0; i < this.paths.length; i++) {
			StreamingXmlPath p = this.paths[i];
			SelectDataVisitorConfig conf = new SelectDataVisitorConfig("DATA", p);
			this.selectConf[i] = conf;
		}
	}

	private void configurePaths() throws Exception {
		paths = new StreamingXmlPath[selectConf.length];
		for (int i = 0; i < selectConf.length; i++) {
			paths[i] = selectConf[i].getXpath();
		}
	}

	public SelectDataVisitor(String... path) throws Exception {
		super(path);
		configureDataVistorConfig();
	}

	public SelectDataVisitor(StreamingXmlPath... path) throws Exception {
		super(path);
		configureDataVistorConfig();
	}

	@Override
	public XmlEventRecorder getRecorder(StreamingXmlPath matchingPath, String activationPath) {
		XmlEventRecorder recorder = new XmlEventRecorder(matchingPath, activationPath);
		// see which config matched so we can set the filter
		for (SelectDataVisitorConfig conf : selectConf) {
			if (conf.getXpath() == matchingPath) {
				recorder.setFilter(conf.getFilter());
			}
		}
		return recorder;
	}

	@Override
	public void init() {
		this.selectedData.clear();
	}

	@Override
	public void preMatch(CurrentDocumentState state, XMLEvent currentEvent, XmlEventRecorder rec) throws Exception {
		// don't care
	}

	@Override
	public void match(XmlEventRecorder recorder) throws Exception {
		this.buffer.getBuffer().setLength(0);
		recorder.write(this.buffer);

		if (this.convertXmlReservedCharacters) {
			int indexOfOther = this.buffer.getBuffer().indexOf("&amp;");
			while (indexOfOther > -1) {
				this.buffer.getBuffer().delete(indexOfOther, indexOfOther + 5);
				this.buffer.getBuffer().insert(indexOfOther, "&");
				indexOfOther = this.buffer.getBuffer().indexOf("&amp;", indexOfOther);
			}
			indexOfOther = this.buffer.getBuffer().indexOf("&lt;");
			while (indexOfOther > -1) {
				this.buffer.getBuffer().delete(indexOfOther, indexOfOther + 4);
				this.buffer.getBuffer().insert(indexOfOther, "<");
				indexOfOther = this.buffer.getBuffer().indexOf("&lt;", indexOfOther);
			}
			indexOfOther = this.buffer.getBuffer().indexOf("&gt;");
			while (indexOfOther > -1) {
				this.buffer.getBuffer().delete(indexOfOther, indexOfOther + 4);
				this.buffer.getBuffer().insert(indexOfOther, ">");
				indexOfOther = this.buffer.getBuffer().indexOf("&gt;", indexOfOther);
			}
		}

		String val = this.buffer.toString();
		for (SelectDataVisitorConfig c : selectConf) {
			if (c.getPath().equals(recorder.getPath().getXpath())) {
				if (!this.selectedData.containsKey(c.getName())) {
					this.selectedData.put(c.getName(), new ArrayList<String>());
				}
				this.selectedData.get(c.getName()).add(val);
			}
		}
	}

	public Map<String, List<String>> getSelectedData() {
		return selectedData;
	}
}
