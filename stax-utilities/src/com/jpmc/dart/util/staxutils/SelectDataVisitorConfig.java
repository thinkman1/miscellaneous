package com.jpmc.dart.util.staxutils;

import javax.xml.stream.EventFilter;

import com.jpmc.dart.util.staxutils.predicate.StreamingXmlPath;

/**
 * Configures a SelectDataVisitor to select data
 * 
 * @author e001668
 * 
 */
public class SelectDataVisitorConfig {
	private String name;
	private String path;
	private EventFilter filter;
	private boolean keepTags;
	private StreamingXmlPath xpath;

	public SelectDataVisitorConfig(String name, StreamingXmlPath xpath) throws Exception {
		this.name = name;
		this.xpath = xpath;
		this.path = xpath.getXpath();
		filter = XmlEventFilters.TEXT_ONLY_FILTER;
	}

	public SelectDataVisitorConfig(String name, String xpath) throws Exception {
		this.name = name;
		this.path = xpath;
		this.xpath = new StreamingXmlPath(xpath);
		filter = XmlEventFilters.TEXT_ONLY_FILTER;
	}

	public SelectDataVisitorConfig(String name, String xpath, boolean keepTags) throws Exception {
		this.name = name;
		this.path = xpath;
		this.keepTags = keepTags;
		if (this.keepTags) {
			filter = XmlEventFilters.NOOP_FILTER;
		} else {
			filter = XmlEventFilters.TEXT_ONLY_FILTER;
		}
		this.xpath = new StreamingXmlPath(xpath);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public EventFilter getFilter() {
		return filter;
	}

	public void setFilter(EventFilter filter) {
		this.filter = filter;
	}

	public boolean isKeepTags() {
		return keepTags;
	}

	public StreamingXmlPath getXpath() {
		return xpath;
	}
}
