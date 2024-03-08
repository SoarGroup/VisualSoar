package edu.umich.soar.visualsoar.graph;

public class StringVertex extends SoarVertex {
    private static final long serialVersionUID = 20221225L;

//////////////////////////////////
// Constructors
//////////////////////////////////
    public StringVertex(int id) {
        super(id);
    }

    //////////////////////////////////
// Accessors
//////////////////////////////////
    public SoarVertex copy(int newId) {
        return new StringVertex(newId);
    }

    public boolean allowsEmanatingEdges() {
        return false;
    }

    public boolean isValid(String s) {
        return true;
    }


    public String toString() {
        return " : string";
    }

    public String typeName() { return "string"; }

    //////////////////////////////////
// Manipulators
//////////////////////////////////
    public void write(java.io.Writer w) throws java.io.IOException {
        w.write("STRING " + number + '\n');
    }

}
