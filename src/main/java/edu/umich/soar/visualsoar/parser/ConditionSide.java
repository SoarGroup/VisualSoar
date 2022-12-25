package edu.umich.soar.visualsoar.parser;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

public final class ConditionSide {
	private final List<Condition> d_conditions = new LinkedList<>();
	
	// Constructor
	public ConditionSide() {
	}
	
	// Accessors
	public void add(Condition condition) {
		d_conditions.add(condition);
	}
	
	public Iterator<Condition> getConditions() {
		return d_conditions.iterator();
	}
}
