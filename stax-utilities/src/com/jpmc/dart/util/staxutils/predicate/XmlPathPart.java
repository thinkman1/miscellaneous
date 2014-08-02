package com.jpmc.dart.util.staxutils.predicate;

import javax.xml.namespace.QName;
import javax.xml.stream.events.StartElement;

class XmlPathPart {
	QName tagName;
	boolean any = false;
	XpathPredicate predicateCondition;
	boolean selectProperty;
	int count=-1;

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();

		ret.append("/");
		if (any) {
			ret.append("/");
		}

		if (tagName != null) {
			ret.append(tagName.toString());
			ret.append(" ");
		}

		if (predicateCondition != null) {
			ret.append(predicateCondition.toString());
		}

		return ret.toString();
	}

	public boolean equals(Object obj) {
		if (obj instanceof StartElement) {
			StartElement start = (StartElement) obj;
			if ((any) || ((tagName.getLocalPart().equals(start.getName().getLocalPart()))&&
					(tagName.getPrefix().equals(start.getName().getPrefix())))){
				if (predicateCondition != null) {
					return predicateCondition.predicate.isTrue((StartElement) obj);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Please note this is not a good hashcode - though it does follow the contract.
	 */
	public int hashCode() {
		return 1;
	}
}
