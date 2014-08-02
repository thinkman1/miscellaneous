package com.jpmc.dart.util.staxutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * a really lightweight DOM like object.
 * 
 * @author E001668
 * 
 */
public class LDom {
	private String tagName;
	private List<LDom> children = new ArrayList<LDom>();
	private Map<String, String> attributes = new HashMap<String, String>();
	private StringBuilder data;

	public LDom() {
	}

	public void init(XMLEvent event) {
		tagName = null;
		children.clear();
		attributes.clear();
		data.setLength(0);

		if (event.isCharacters()) {
			data.append(((Characters) event).getData());
		} else if (event.isStartElement()) {
			StartElement ele = (StartElement) event;
			if (ele.getName().getPrefix() != null) {
				data.append(ele.getName().getPrefix());
				data.append(":");
			}
			data.append(ele.getName().getLocalPart());
			tagName = data.toString();
			data.setLength(0);
			for (Iterator it = ele.getAttributes(); it.hasNext();) {
				Attribute attr = (Attribute) it.next();
				attributes.put(attr.getName().getLocalPart(), attr.getValue());
			}
		}

	}

	public List<LDom> getChildren() {
		return children;
	}

	public String getTagName() {
		return tagName;
	}

	public StringBuilder getData() {
		return data;
	}
}
