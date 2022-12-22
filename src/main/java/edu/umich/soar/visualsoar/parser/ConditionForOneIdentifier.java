package edu.umich.soar.visualsoar.parser;

import java.util.*;

public final class ConditionForOneIdentifier {
	// Data Members
	private final boolean d_hasState;
	private final Pair d_variable;
	private final List<AttributeValueTest> d_attributeValueTests = new LinkedList<>();
	
	// Constructor
	public ConditionForOneIdentifier(boolean hasState,Pair variable) {
		d_variable = variable;
		d_hasState = hasState;
	}
		
	// Accessor
	public boolean hasState() {
		return d_hasState;
	}
	
	public Pair getVariable() {
		return d_variable;
	}
	
	public void add(AttributeValueTest avt) {
		d_attributeValueTests.add(avt);
	}
	
	public Iterator<AttributeValueTest> getAttributeValueTests() {
		return d_attributeValueTests.iterator();
	}
}
