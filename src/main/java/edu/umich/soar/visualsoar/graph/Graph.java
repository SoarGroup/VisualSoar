package edu.umich.soar.visualsoar.graph;

import edu.umich.soar.visualsoar.util.PrePostVisitor;

import java.util.Enumeration;

/**
 * This class is a graph class that provides an interface for different kinds of graphs
 * it is based on Object-Oriented Design patterns in C++, later converted to Java
 *
 * @author Brad Jones
 */
public abstract class Graph {
    ///////////////////////////////////////////////////////////////////
// Data Members
///////////////////////////////////////////////////////////////////
    protected int numberOfVertices = 0;
    protected int numberOfEdges = 0;

    ///////////////////////////////////////////////////////////////////
// Methods
///////////////////////////////////////////////////////////////////
    protected void depthFirstTraversal(PrePostVisitor visitor, SoarVertex vertex, boolean[] visited) {
		if (visitor.isDone()) {
			return;
		}
        visitor.preVisit(vertex);
        visited[vertex.getValue()] = true;
        Enumeration<NamedEdge> e = emanatingEdges(vertex);
        while (e.hasMoreElements()) {
            NamedEdge edge = e.nextElement();
            SoarVertex to = edge.mate(vertex);
            if (!visited[to.getValue()]) {
              depthFirstTraversal(visitor, to, visited);
            }
        }
        visitor.postVisit(vertex);
    }

    /**
     * @return the numberOfEdges
     */
    public int numberOfEdges() {
        return numberOfEdges;
    }

    /**
     * @return the numberOfVertices
     */
    public int numberOfVertices() {
        return numberOfVertices;
    }

    /**
     * Adds a vertex to the graph
     */
    public abstract void addVertex(SoarVertex v);

    public abstract SoarVertex selectVertex(int id);

    public SoarVertex get(int id) {
        return selectVertex(id);
    }

    public abstract void addEdge(NamedEdge e);

    public abstract void removeEdge(NamedEdge e);

    /**
     * If you have two vertices, get the edge between them
     * if the edge exists in the graph it returns that edge
     * otherwise it returns null
     */
    public abstract NamedEdge selectEdge(int v0, int v1);

    public abstract Enumeration<SoarVertex> vertices();

    public abstract Enumeration<NamedEdge> edges();

    public abstract Enumeration<NamedEdge> emanatingEdges(SoarVertex v);

}
