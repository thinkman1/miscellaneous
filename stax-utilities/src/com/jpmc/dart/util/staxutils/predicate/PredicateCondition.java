package com.jpmc.dart.util.staxutils.predicate;

import javax.xml.stream.events.StartElement;

interface PredicateCondition {
	public boolean isTrue(StartElement ele);
}
