package com.jpmc.dart.util.staxutils.predicate;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.events.StartElement;

import org.apache.commons.lang.StringUtils;

class XpathOrGroup implements CompoundPredicate {
	List<PredicateCondition> conditions = new ArrayList<PredicateCondition>();

	public XpathOrGroup() {
	}

	public void addPredicate(PredicateCondition cond) {
		conditions.add(cond);
	}

	@Override
	public boolean isTrue(StartElement ele) {
		for (PredicateCondition pred : conditions) {
			if (pred.isTrue(ele)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		String val = StringUtils.EMPTY;
		for (int i = 0; i < conditions.size(); i++) {
			PredicateCondition cond = conditions.get(i);
			val += cond.toString();
			if (i < conditions.size() - 1) {
				val += " OR ";
			}
		}

		return val;
	}

}
