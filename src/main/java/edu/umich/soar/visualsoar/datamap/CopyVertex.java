package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.graph.SoarVertex;

/**
 * class CopyVertex
 *
 * is a simple data containing class for storing a vertex in the datamap
 * along with the name of the attribute that attaches it to a parent node.
 * This is used for copying datmap entries.
 *
 * @see CopyVertexVector
 */

public class CopyVertex {
    public String name;
    public SoarVertex  vertex;

    public CopyVertex(String inName, SoarVertex inVertex) {
        name = inName;
        vertex = inVertex;
    }
}
