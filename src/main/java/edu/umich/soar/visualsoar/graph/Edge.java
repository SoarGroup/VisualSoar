package edu.umich.soar.visualsoar.graph;

/**
 * This class represents an edge in a graph
 * it is based on Object-Oriented Design patterns in C++, later converted to Java
 * <p>
 * TODO:  Why does this class exist?  The only sub-class is NamedEdge and
 *        this class is never used directly.
 *
 * @author Brad Jones
 * @see NamedEdge
 */

public class Edge implements java.io.Serializable {

    private static final long serialVersionUID = 20221225L;

////////////////////////////////////////
// DataMembers
////////////////////////////////////////
    /**
     * v0 is the starting edge
     * v1 is the ending edge
     */
    protected SoarVertex v0;
    protected SoarVertex v1;

////////////////////////////////////////
// Constructors
////////////////////////////////////////

    /**
     * constructs and edge between the first
     * vertex and the second one
     */
    public Edge(SoarVertex _v0, SoarVertex _v1) {
        v0 = _v0;
        v1 = _v1;
    }

////////////////////////////////////////
// Methods
////////////////////////////////////////

    /**
     * @return the starting edge
     */
    public SoarVertex V0() {
        return v0;
    }

    /**
     * @return the ending edge
     */
    public SoarVertex V1() {
        return v1;
    }

    /**
     * if v equals the starting edge then return the ending edge
     * if v equal the ending edge then return the starting edge
     * else throw and exception
     *
     * @return the starting edge
     */
    public SoarVertex mate(SoarVertex v) {
        if (v.equals(v0)) {
            return v1;
        }
        if (v.equals(v1)) {
            return v0;
        }
        throw new IllegalArgumentException("Vertex passed in is not part of this edge");
    }

    /**
     * Produces string representation of the edge
     *
     * @return the representation
     */
    public String toString() {
        return "This edge connects " + v0.toString() + " to " + v1.toString();
    }
}
