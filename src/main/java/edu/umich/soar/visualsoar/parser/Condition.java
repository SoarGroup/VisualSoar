package edu.umich.soar.visualsoar.parser;

public final class Condition {
	// Data Members
	private final PositiveCondition d_positiveCondition;
	
	// Constructors
	//Note:  isNegated parm isn't used but needed to support parser
	public Condition(boolean isNegated,PositiveCondition positiveCondition) {
		d_positiveCondition = positiveCondition;
	}

	public PositiveCondition getPositiveCondition() {
		return d_positiveCondition;
	}
}
