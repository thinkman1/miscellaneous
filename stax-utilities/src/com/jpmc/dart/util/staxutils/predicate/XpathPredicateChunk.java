package com.jpmc.dart.util.staxutils.predicate;

import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

class XpathPredicateChunk implements PredicateCondition {
	QName property;
	Pattern valueRegex;

	@Override
	public String toString() {
		return property.toString() + "=" + valueRegex.toString();
	}

	@Override
	public boolean isTrue(StartElement ele) {
		Attribute value = ele.getAttributeByName(property);
		if (value != null) {
			return this.valueRegex.matcher(value.getValue()).matches();
		}
		return false;
	}
}
