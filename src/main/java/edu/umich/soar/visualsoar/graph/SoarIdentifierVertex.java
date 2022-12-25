package edu.umich.soar.visualsoar.graph;

public class SoarIdentifierVertex extends SoarVertex {
    private static final long serialVersionUID = 20221225L;

///////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////
    public SoarIdentifierVertex(int id) {
        super(id);
    }

    ///////////////////////////////////////////////////
// Accessors
///////////////////////////////////////////////////	
    public SoarVertex copy(int newId) {
        return new SoarIdentifierVertex(newId);
    }

    public boolean allowsEmanatingEdges() {
        return true;
    }

    public boolean isValid(String s) {
        return false;
    }

    public String toString() {
        return "";
    }

    ///////////////////////////////////////////////////
// Manipulators
///////////////////////////////////////////////////	
    public void write(java.io.Writer w) throws java.io.IOException {
        w.write("SOAR_ID " + number + '\n');
    }

}
