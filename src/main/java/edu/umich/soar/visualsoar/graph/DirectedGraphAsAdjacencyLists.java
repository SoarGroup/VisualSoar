package edu.umich.soar.visualsoar.graph;

import edu.umich.soar.visualsoar.util.*;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

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
        emanatingEdges.add(e);
        ++numberOfEdges;
    }

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
