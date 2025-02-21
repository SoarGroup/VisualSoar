package edu.umich.soar.visualsoar.graph;

import edu.umich.soar.visualsoar.util.*;

import java.util.*;

/**
 * This class is an implementation of the DirectedGraph class, using Adjacency Lists
 * it is based on Object-Oriented Design patterns in C++, later converted to Java
 *
 * @author Brad Jones
 */

public class DirectedGraphAsAdjacencyLists extends DirectedGraph {
    //////////////////////////////////////////////////////////
// Data Members
//////////////////////////////////////////////////////////
    protected Vector<SoarVertex> vertices = new Vector<>();
    protected Vector<Vector<NamedEdge>> adjacencyLists = new Vector<>();

    /////////////////////////////////////////////////////////
// Methods
/////////////////////////////////////////////////////////
    public void addVertex(SoarVertex v) {
        if (v.getValue() == numberOfVertices) {
            vertices.add(v);
            adjacencyLists.add(new Vector<NamedEdge>());
            ++numberOfVertices;
        } else {
            vertices.set(v.getValue(), v);
            adjacencyLists.set(v.getValue(), new Vector<NamedEdge>());
        }

    }

    public SoarVertex selectVertex(int id) {
        return vertices.get(id);
    }

    public void addEdge(NamedEdge e) {
        Vertex start = e.V0();
        Vector<NamedEdge> emanatingEdges = adjacencyLists.get(start.getValue());
        insertSorted(e, emanatingEdges);
        ++numberOfEdges;
    }

    /**
     * insertSorted
     *
     * a helper method for {@link #addEdge(NamedEdge)} that adds
     * a given NamedEdge to a Vector of same in a position that keeps
     * it in sorted order.
     *
     * Side effect:  also verifies that the list is already sorted
     */
    private void insertSorted(NamedEdge ne, Vector<NamedEdge> vec) {
        boolean found = false;
        int foundAt = 0;
        boolean isSorted = true;  //innocent until proven guilty
        NamedEdge prev = null;
        for (int i = 0; i < vec.size(); ++i) {
            NamedEdge current = vec.get(i);
            if ((!found) && (current.compareTo(ne) >= 0)) {
                found = true;
                foundAt = i;
            }

            //check for unsorted list (just in case)
            if ((prev != null) && (prev.compareTo(ne) > 0)) {
                isSorted = false;  //this will trigger a full sort below
            }
            prev = current;
        }//for

        if (found) {
            vec.insertElementAt(ne, foundAt);
        } else {
            vec.add(ne);  //add to end
        }

        //this should never be needed but it was fairly cheap to double-check
        if (! isSorted) Collections.sort(vec);
    }//insertSorted

    public void removeEdge(NamedEdge e) {
        Vertex start = e.V0();
        Vector<NamedEdge> emanatingEdges = adjacencyLists.get(start.getValue());
        emanatingEdges.remove(e);
        --numberOfEdges;
    }

    public NamedEdge selectEdge(int v0, int v1) {
        Vector<NamedEdge> emanatingEdges = adjacencyLists.get(v0);
        for (NamedEdge edge : emanatingEdges) {
            if (edge.V1().getValue() == v1) {
                return edge;
            }
        }
        return null;
    }

    public Enumeration<SoarVertex> vertices() {
        return this.vertices.elements();
    }

    public Enumeration<NamedEdge> edges() {
        Vector<NamedEdge> allEdges = new Vector<>();
        for (int i = 0; i < numberOfVertices; ++i) {
            allEdges.addAll(adjacencyLists.get(i));
        }
        return allEdges.elements();
    }

    public Enumeration<NamedEdge> emanatingEdges(SoarVertex v) {
        Vector<NamedEdge> emanatingEdges = adjacencyLists.get(v.getValue());
        return emanatingEdges.elements();
    }

    public void reduce(List<SoarVertex> listOfStartVertices) {
        // This code finds all the unvisited nodes
        boolean[] visited = new boolean[numberOfVertices()];
        Visitor doNothing = new DoNothingVisitor();
        PrePostVisitor dnPreVisitor = new PreOrder(doNothing);
        EnumerationIteratorWrapper vertEnum = new EnumerationIteratorWrapper(listOfStartVertices.iterator());
        while (vertEnum.hasMoreElements()) {
            SoarVertex startVertex = (SoarVertex) vertEnum.nextElement();
            if (!visited[startVertex.getValue()]) {
                depthFirstTraversal(dnPreVisitor, startVertex, visited);
            }
        }

        //This code maps visited nodes to new ids
        Hashtable<Integer, Integer> ht = new Hashtable<>();
        int newNumberOfVertices = 0;
        for (int i = 0; i < visited.length; ++i) {
            if (visited[i]) {
                ht.put(i, newNumberOfVertices++);
            }
        }

        // Make up the new vertices
        Vector<SoarVertex> newVertices = new Vector<>();
        for (int i = 0; i < numberOfVertices(); ++i) {
            Integer newId = ht.get(i);
            if (newId != null) {
                SoarVertex vertex = vertices.get(i);
                vertex.setValue(newId);
                newVertices.add(vertex);
            }
        }

        // Make up the new edges
        Vector<Vector<NamedEdge>> newAdjacencyLists = new Vector<>();
        int newNumberOfEdges = 0;
        for (int i = 0; i < numberOfVertices; ++i) {
            Integer newId = ht.get(i);
            if (newId != null) {
                newAdjacencyLists.add(adjacencyLists.get(i));
                newNumberOfEdges += adjacencyLists.get(i).size();
            }
        }

        // Update the Working Memory
        vertices = newVertices;
        adjacencyLists = newAdjacencyLists;
        numberOfVertices = newNumberOfVertices;
        numberOfEdges = newNumberOfEdges;
    }

}
