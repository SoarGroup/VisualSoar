package edu.umich.soar.visualsoar.graph;

/**
 * This class is a vertex in a graph
 *
 * @author Brad Jones
 * @version 0.9 6/5/00
 */
public class Vertex implements java.io.Serializable {
    private static final long serialVersionUID = 20221225L;

    ////////////////////////////////////////
// DataMembers
////////////////////////////////////////
    protected int number;

    ////////////////////////////////////////
// Constructors
////////////////////////////////////////
    public Vertex(int _n) {
        number = _n;
    }

    ////////////////////////////////////////
// Methods
////////////////////////////////////////
    public int getValue() {
        return number;
    }

    /**
     * This is a rather dangerous method, but it is
     * necessary to properly update the SoarWorkingMemoryModel
     * efficiently
     */
    public void setValue(int _n) {
        number = _n;
    }

    public String toString() {
        return "Vertex: " + number;
    }
}
