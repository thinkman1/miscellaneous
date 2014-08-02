package com.jpmc.dart.util.staxutils;

import javax.xml.stream.EventFilter;
import javax.xml.stream.events.XMLEvent;

public class XmlEventFilters {
	public static class TextOnlyFilter implements EventFilter {
		@Override
		public boolean accept(XMLEvent event) {
			if (event.isCharacters()){
				return true;
			}
			return false;
		}
	}

	public static class AllowAllFilter implements EventFilter {
		@Override
		public boolean accept(XMLEvent event) {
			return true;
		}
	}

	public static class ThrowawayFilter implements EventFilter {
		@Override
		public boolean accept(XMLEvent event) {
			return false;
		}
	}
	
	public static final EventFilter TEXT_ONLY_FILTER = new TextOnlyFilter();
	public static final EventFilter THROWAWAY = new ThrowawayFilter();
	public static final EventFilter NOOP_FILTER = new AllowAllFilter();
}
