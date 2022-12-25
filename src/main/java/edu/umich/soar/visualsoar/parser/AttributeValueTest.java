package edu.umich.soar.visualsoar.parser;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class AttributeValueTest {
    // Data Members
    boolean d_isNegated = false;
    List<AttributeTest> d_attributeTests = new LinkedList<>();
    List<ValueTest> d_valueTests = new LinkedList<>();

    // Accessors
    public void negate() {
        d_isNegated = true;
    }

    public void add(AttributeTest at) {
        d_attributeTests.add(at);
    }

    public void add(ValueTest vt) {
        d_valueTests.add(vt);
    }

    public Iterator<AttributeTest> getAttributeTests() {
        return d_attributeTests.iterator();
    }

    public Iterator<ValueTest> getValueTests() {
        return d_valueTests.iterator();
    }

}
