package edu.umich.soar.visualsoar.parser;
import java.util.*;

public final class AttributeValueMake {
	// Data Members
	private final List<ValueMake> d_valueMakes = new LinkedList<>();
	private final List<RHSValue> d_rhsValues = new LinkedList<>();
	
	// Constructors
	public AttributeValueMake() {}
	
	
	// Accessors
	public void add(RHSValue rhsValue) {
		d_rhsValues.add(rhsValue);
	}
	
	public void add(ValueMake vm) {
		d_valueMakes.add(vm);
	}
	
	public Iterator<RHSValue> getRHSValues() {
		return d_rhsValues.iterator();
	}
	
	public Iterator<ValueMake> getValueMakes() {
		return d_valueMakes.iterator();
	}
	
}
