package edu.umich.soar.visualsoar.parser;
import java.util.*;

public final class VarAttrValMake {
	private final Pair d_variable;
	private final List<AttributeValueMake> d_attributeValueMakes = new LinkedList<>();
	
	public VarAttrValMake(Pair variable) {
		d_variable = variable;
	}
	
	// Member Functions
	public void add(AttributeValueMake avm) {
		d_attributeValueMakes.add(avm);
	}
	
	public Iterator<AttributeValueMake> getAttributeValueMakes() {
		return d_attributeValueMakes.iterator();
	}
	
	public Pair getVariable() {
		return d_variable;
	}
}
