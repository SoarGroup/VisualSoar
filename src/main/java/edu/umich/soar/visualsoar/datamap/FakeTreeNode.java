package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.graph.EnumerationVertex;
import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.graph.SoarVertex;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TreeModelEvent;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This class takes graph nodes and cleverly (or not so cleverly) disguises
 * as tree nodes, to prevent infinite recursion, the children are loaded when
 * needed
 *
 * @author Brad Jones
 */

public class FakeTreeNode {
/////////////////////////////////////////
// Data Members
/////////////////////////////////////////

    // a flag noting whether this node has been loaded
    private boolean hasLoaded = false;

    // a string of how this node should be represented
    private String representation;

    // the vertex from which emanating edges are considered children
    private final SoarVertex enumeratingVertex;

    // a reference to the graph structure, so we can extract the information as needed
    private final SoarWorkingMemoryModel swmm;

    // the parent of this node, can be null
    private FakeTreeNode parent;

    // the children for this node
    private final Vector<FakeTreeNode> children = new Vector<>();

    // the associated edge for this node, can be null
    private NamedEdge theEdge = null;

    /////////////////////////////////////////
// Constructors
/////////////////////////////////////////
    public FakeTreeNode(SoarWorkingMemoryModel in_swmm, SoarIdentifierVertex siv, String s) {
        representation = s;
        enumeratingVertex = siv;
        swmm = in_swmm;
    }

    public FakeTreeNode(SoarWorkingMemoryModel in_swmm, NamedEdge ne) {

        representation = ne.toString();
        enumeratingVertex = ne.V1();
        swmm = in_swmm;
        theEdge = ne;

        if (representation.equals("operator")) {
            boolean foundName = false;
            Enumeration<NamedEdge> e = swmm.emanatingEdges(enumeratingVertex);
            NamedEdge edge = null;
            while (e.hasMoreElements() && !foundName) {
                edge = e.nextElement();
                if (((edge.getName()).equals("name"))
                        && (edge.V1() instanceof EnumerationVertex)) {
                    foundName = true;
                }
            } // while looking through enumeration

            if (foundName) {
                EnumerationVertex ev = (EnumerationVertex) edge.V1();
                if (ev != null) {
                    representation = "operator " + ev;
                }
            }

        }   // end of if the current node is an operator node

        // Add any possible comments to the representation of the fake node
        if (ne.hasComment()) {
            representation = representation + "          * " + ne.getComment() + " *";
        }
    }

    //////////////////////////////////////////
// Accessors
//////////////////////////////////////////
    public FakeTreeNode getChildAt(int index) {
        return children.get(index);
    }

    public int getChildCount() {
        if (!hasLoaded) {
            int count = 0;
            Enumeration<NamedEdge> e = swmm.emanatingEdges(enumeratingVertex);
            while (e.hasMoreElements()) {
                ++count;
                NamedEdge edge = e.nextElement();
                FakeTreeNode aChild = new FakeTreeNode(swmm, edge);
                aChild.setParent(this);
                children.add(aChild);
            }
            hasLoaded = true;
            return count;
        }
        return children.size();
    }

    @Nullable
    public NamedEdge getEdge() {
        return theEdge;
    }

    public SoarVertex getEnumeratingVertex() {
        return enumeratingVertex;
    }

    public int getIndex(FakeTreeNode ftn) {
        return children.indexOf(ftn);
    }

    public FakeTreeNode getParent() {
        return parent;
    }

    public Vector<FakeTreeNode> getTreePath() {
        Vector<FakeTreeNode> v = new Vector<>();
        if (parent != null) {
            v = parent.getTreePath();
        }
        v.add(this);
        return v;
    }

    public boolean hasLoaded() {
        return hasLoaded;
    }

    public boolean isLeaf() {
        return !enumeratingVertex.allowsEmanatingEdges();
    }

    public boolean isRoot() {
        return (parent == null);
    }

    public String toString() {
        return representation;
    }

    /**
     * @return a String representing this node in the tree via a path to root.
     * Example:  "<s> ^io.input-link.block.on-top"
     */
    public String stringPath() {
        NamedEdge myEdge = getEdge();
        if (myEdge == null) return "<s>";
        String result = getParent().stringPath();
        if (result.equals("<s>")) {
            result += " ^";
        }
        else {
            result += ".";
        }

        result = result + myEdge.getName();

        if (this.isLeaf()) {
            result += myEdge.V1().toString();
        }

        return result;
    }//stringPath


    //////////////////////////////////////////
// Manipulators
//////////////////////////////////////////
    /** Adds a new child to this node and generates a TreeModelEvent that results the addition */
    public TreeModelEvent add(NamedEdge ne) {
        int[] indices = new int[1];
        FakeTreeNode aChild = new FakeTreeNode(swmm, ne);
        aChild.setParent(this);
        indices[0] = insertSorted(ne, aChild);

        return new TreeModelEvent(swmm, getTreePath().toArray(), indices, children.toArray());
    }//add

    /**
     * insertSorted
     *
     * is a helper method for {@link #add(NamedEdge)}.  It inserts a new child in sorted order */
    private int insertSorted(NamedEdge ne, FakeTreeNode aChild) {
        boolean found = false;
        int foundAt = 0;
        for (int i = 0; i < children.size(); ++i) {
            NamedEdge current = getChildAt(i).getEdge();
            if (current.getName().compareTo(ne.getName()) >= 0) {
                found = true;
                foundAt = i;
                break;
            }
        }
        if (found) {
            children.add(foundAt, aChild);
            return foundAt;
        } else {
            children.add(aChild);
            return children.size() - 1;
        }
    }//insertSorted


    public void setParent(FakeTreeNode ftn) {
        parent = ftn;
    }

    public TreeModelEvent remove(NamedEdge ne) {
        int[] indices = new int[1];
        boolean found = false;
        int count = 0;
        Enumeration<FakeTreeNode> e = children.elements();
        while (!found && e.hasMoreElements()) {
            FakeTreeNode currentChild = e.nextElement();
            if (ne.equals(currentChild.getEdge())) {
                found = true;
                indices[0] = count;
            }
            ++count;
        }
        children.removeElementAt(indices[0]);
        return new TreeModelEvent(swmm, getTreePath().toArray(), indices, children.toArray());
    }

    public void visitChildren(edu.umich.soar.visualsoar.util.Visitor v) {
        Enumeration<FakeTreeNode> e = children.elements();
        while (e.hasMoreElements()) {
            FakeTreeNode currentChild = e.nextElement();
            v.visit(currentChild);
            currentChild.visitChildren(v);
        }
    }
}
