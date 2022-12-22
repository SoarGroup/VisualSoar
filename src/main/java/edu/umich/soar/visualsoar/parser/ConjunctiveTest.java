package edu.umich.soar.visualsoar.parser;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;

public final class ConjunctiveTest {
	// Data Members
	private final List<SimpleTest> d_simpleTests = new LinkedList<>();
	
	// Constructors
	public ConjunctiveTest() {}
	
	// Methods
	public void add(SimpleTest simpleTest) {
		d_simpleTests.add(simpleTest);
	}
	
	public Iterator<SimpleTest> getSimpleTests() {
		return d_simpleTests.iterator();
	}
}
