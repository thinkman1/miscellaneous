package com.jpmc.dart.util.staxutils.predicate;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.events.StartElement;

import org.apache.commons.lang.StringUtils;

class XpathAndGroup implements CompoundPredicate {
	List<PredicateCondition> conditions = new ArrayList<PredicateCondition>();

	public XpathAndGroup() {
	}

	public void addPredicate(PredicateCondition cond) {
		conditions.add(cond);
	}

	@Override
	public boolean isTrue(StartElement ele) {
		for (PredicateCondition pred : conditions) {
			if (!pred.isTrue(ele)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		String val = StringUtils.EMPTY;
		for (int i = 0; i < conditions.size(); i++) {
			PredicateCondition cond = conditions.get(i);
			val += cond.toString();
			if (i < conditions.size() - 1) {
				val += " AND ";
			}
		}

		return val;
	}
}
