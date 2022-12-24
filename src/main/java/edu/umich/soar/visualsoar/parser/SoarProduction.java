package edu.umich.soar.visualsoar.parser;

/**
 * @author Brad Jones
 * @version 0.75 3 Mar 2000
 */
public final class SoarProduction {
	// Data Members
	private String d_name;
	private int d_startLine;
	private String d_comment;
	private ConditionSide d_conditionSide;
	private ActionSide d_actionSide;
	
	// Constructors
	public SoarProduction() {}
		
	// Accessors
	public void setName(String name) {
		d_name = name;
	}
	
	public void setComment(String comment) {
		d_comment = comment;
	}

	//parameter never used but left here to support parser
	public void setProductionType(String productionType) {
	}
	
	public void setStartLine(int startLine) {
		d_startLine = startLine;
	}
	
	public void setConditionSide(ConditionSide cs) {
		d_conditionSide = cs;
	}
	
	public void setActionSide(ActionSide as) {
		d_actionSide = as;
	}
	
	public int getStartLine() {
		return d_startLine;
	}
	
	public String getName() {
		return d_name;
	}
	
	public String getComment() {
		return d_comment;
	}

	public ConditionSide getConditionSide() {
		return d_conditionSide;
	}
	
	public ActionSide getActionSide() {
		return d_actionSide;
	}
}
