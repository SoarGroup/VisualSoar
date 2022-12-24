package edu.umich.soar.visualsoar.parser;
import java.util.*;

/** This class really does nothing and only exists to support {@link SoarParser} */
public final class FunctionCall {
	private final List<RHSValue> d_rhsValues = new LinkedList<>();

	public FunctionCall(Pair functionName) {
	}
	
	// Member Functions
	public void add(RHSValue rhsValue) {
		d_rhsValues.add(rhsValue);
	}

}
