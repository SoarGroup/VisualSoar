package edu.umich.soar.visualsoar.parser;

@SuppressWarnings("unused")
public final class RHSValue {
	// Data Members
	private Constant d_constant;
	private Pair d_variable;
	private final boolean d_isConstant;
	private final boolean d_isVariable;


	public RHSValue(Constant c) {
		d_constant = c;
		d_isConstant = true;
		d_isVariable = false;
	}
	
	public RHSValue(Pair variable) {
		d_variable = variable;
		d_isConstant = false;
		d_isVariable = true;
	}

	/** functionCall param is needed to support parser even though never used */
	public RHSValue(FunctionCall functionCall) {
		d_isConstant = false;
		d_isVariable = false;
	}
	
	// Member Functions	
	public boolean isConstant() {
		return d_isConstant;
	}
	
	public boolean isVariable() {
		return d_isVariable;
	}
	
	public boolean isFunctionCall() {
		return (!d_isConstant && !d_isVariable);
	}

	public Constant getConstant() {
		if(!isConstant())
			throw new IllegalArgumentException("Not a Constant");
		else
			return d_constant;
	}
	
	public Pair getVariable() {
		if(!isVariable()) 
			throw new IllegalArgumentException("Not a Variable");
		else
			return d_variable;
	}

}
