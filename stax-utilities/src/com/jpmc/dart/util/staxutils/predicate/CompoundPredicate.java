package com.jpmc.dart.util.staxutils.predicate;

interface CompoundPredicate extends PredicateCondition {
	public void addPredicate(PredicateCondition cond);
}

