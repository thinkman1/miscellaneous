package com.jpmc.dart.util.staxutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.events.StartElement;

/**
 * stores the current state of where we are in a document. This include the
 * xpath that indicates where we are, as well as the number of times each path
 * has been accessed.
 * 
 * @author e001668
 * 
 */
public class CurrentDocumentState {
	private Map<String, Integer> pathCounts = new HashMap<String, Integer>();
	private StringBuilder currentPath;
	private List<StartElement> startElements = new ArrayList<StartElement>();

	public void setCurrentPath(StringBuilder builder) {
		this.currentPath = builder;
	}

	public StringBuilder getCurrentPath() {
		return currentPath;
	}

	public void incrementPathCount(StringBuilder path) {
		String ppath = path.toString();
		if (!pathCounts.containsKey(ppath)) {
			pathCounts.put(ppath, new Integer(0));
		}
		Integer count = Integer.valueOf(pathCounts.get(ppath).intValue() + 1);
		pathCounts.put(ppath, count);
	}

	public void prune(StringBuilder path) {
		String ppath = path.toString();
		Set<String> removes = new HashSet<String>();
		for (String key : pathCounts.keySet()) {
			if (key.startsWith(ppath) && (!key.equals(ppath))) {
				removes.add(key);
			}
		}
		for (String removeKey : removes) {
			pathCounts.remove(removeKey);
		}
	}

	public int getCountForPath(StringBuilder path) {
		String ppath = path.toString();
		if (!pathCounts.containsKey(ppath)) {
			return 0;
		}
		return pathCounts.get(ppath).intValue();
	}

	public List<StartElement> getStartElements() {
		return startElements;
	}
}
