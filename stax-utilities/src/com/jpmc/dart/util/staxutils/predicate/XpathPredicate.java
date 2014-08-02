package com.jpmc.dart.util.staxutils.predicate;

import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

class XpathPredicate {
	public static final String ANY = ".*";
	
	PredicateCondition predicate;

	@Override
	public String toString() {
		return predicate.toString();
	}

	public XpathPredicate(List<String> tokens) {
		Stack<PredicateCondition> conditions = new Stack<PredicateCondition>();

		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);

			if (token.startsWith("@")) {
				int equalsIndex = token.indexOf("=");
				XpathPredicateChunk chunk = new XpathPredicateChunk();
				if (equalsIndex > -1) {
					chunk.property = new QName(token.substring(1,
							equalsIndex));
					String value = token.substring(token.indexOf('\'') + 1,
							token.lastIndexOf('\''));
					chunk.valueRegex = Pattern.compile(value);
				} else {
					chunk.property = new QName(token.substring(1));
					chunk.valueRegex = Pattern.compile(ANY);
				}

				if (!conditions.isEmpty()
						&& (conditions.peek() instanceof CompoundPredicate)) {
					((CompoundPredicate) conditions.peek())
							.addPredicate(chunk);
				} else {
					conditions.push(chunk);
				}
			} else if (token.equalsIgnoreCase("AND")) {
				XpathAndGroup and = new XpathAndGroup();
				and.addPredicate(conditions.pop());
				conditions.push(and);
			} else if (token.equalsIgnoreCase("OR")) {
				XpathOrGroup or = new XpathOrGroup();
				or.addPredicate(conditions.pop());
				conditions.push(or);
			}
		}
		this.predicate = conditions.firstElement();
	}

}
