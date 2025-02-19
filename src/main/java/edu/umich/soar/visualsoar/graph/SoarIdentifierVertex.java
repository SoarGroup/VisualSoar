package edu.umich.soar.visualsoar.graph;

public class SoarIdentifierVertex extends SoarVertex {
    private static final long serialVersionUID = 20221225L;

///////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////
    public SoarIdentifierVertex(int id) {
        super(id);
    }

  public SoarIdentifierVertex(int id, String serializationId) {
    super(id, serializationId);
  }

    ///////////////////////////////////////////////////
// Accessors
///////////////////////////////////////////////////
    @Override
    public SoarVertex copy(int newId) {
        return new SoarIdentifierVertex(newId);
    }

    @Override
    public boolean allowsEmanatingEdges() {
        return true;
    }

    @Override
    public boolean isValid(String s) {
        return false;
    }

    @Override
    public String typeName() { return "identfier"; }

    @Override
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
