package edu.umich.soar.visualsoar.parser;

public final class Action {
	// Data Members
	private VarAttrValMake d_varAttrValMake;
	private final boolean d_isVarAttrValMake;

	// Constructors
	public Action(VarAttrValMake varAttrValMake) {
		d_varAttrValMake = varAttrValMake;
		d_isVarAttrValMake = true;
	}

	/** This ctor is used to support the parser the given value isn't used */
	public Action(FunctionCall functionCall) {
		d_isVarAttrValMake = false;
	}
	// Accessors
	public boolean isVarAttrValMake() {
		return d_isVarAttrValMake;
	}
	
	public VarAttrValMake getVarAttrValMake() {
		if(!d_isVarAttrValMake)
			throw new IllegalArgumentException("Not Variable Attribute Value Make");
		else
			return d_varAttrValMake;
	}

}
