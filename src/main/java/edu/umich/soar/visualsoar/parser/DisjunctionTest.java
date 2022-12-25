package edu.umich.soar.visualsoar.parser;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class DisjunctionTest {
    // Data Members
    private final List<Constant> d_constants = new LinkedList<>();

    // Constructors
    public DisjunctionTest() {
    }

    // Accessors
    public void add(Constant constant) {
        d_constants.add(constant);
    }

    public Iterator<Constant> getConstants() {
        return d_constants.iterator();
    }
}
	
