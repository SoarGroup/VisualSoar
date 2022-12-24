package edu.umich.soar.visualsoar.graph;

import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.util.QueueAsLinkedList;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is a graph class that provides an interface for different kinds of graphs
 * it is based on Object-Oriented Design patterns in C++, later converted to Java
 * @author Brad Jones
 */
public abstract class DirectedGraph extends Graph {

  /**
   *  Function uses a Breadth-first traversal to search
   *  through all the vertices to find all vertices
   *  that connect to the SoarVertex parameter sv.
   *  Returns an enumeration of those vertices.
   */
  public List<SoarVertex> getParentVertices(SoarWorkingMemoryModel swmm, SoarVertex sv) {
    boolean[] visitedVertices = new boolean[numberOfVertices];
    for(int i = 1; i < numberOfVertices; i++)
      visitedVertices[i] = false;
    visitedVertices[0] = true;
    edu.umich.soar.visualsoar.util.Queue queue = new QueueAsLinkedList();
    List<SoarVertex> foundVertices = new LinkedList<>();
    queue.enqueue(selectVertex(0));

    while(!queue.isEmpty()) {
      SoarVertex w = (SoarVertex)queue.dequeue();
      visitedVertices[w.getValue()] = true;

      if(w.allowsEmanatingEdges()) {
        Enumeration<NamedEdge> edges = swmm.emanatingEdges(w);
        while(edges.hasMoreElements()) {
          NamedEdge edge = edges.nextElement();
          if(! visitedVertices[edge.V1().getValue()]) {
            if(edge.V1().equals(sv)) {
              foundVertices.add(w);
            }
            visitedVertices[w.getValue()] = true;
            queue.enqueue(edge.V1());
          }     // if haven't visited this vertex yet, then check if match and add to queue

        }   // while edges have more elements
      }
    }    // while queue is not empty
    return foundVertices;
  }


  /**
   *  Similar to getParentVertices(), but this looks for a
   *  SoarIdentifierVertex that was recently created.
   */
  public SoarVertex getMatchingParent(SoarWorkingMemoryModel swmm, SoarVertex sv) {
    boolean[] visitedVertices = new boolean[numberOfVertices];
    for(int i = 1; i < numberOfVertices; i++)
      visitedVertices[i] = false;
    visitedVertices[0] = true;
    edu.umich.soar.visualsoar.util.Queue queue = new QueueAsLinkedList();
    queue.enqueue(selectVertex(0));

    while(!queue.isEmpty()) {
      SoarVertex w = (SoarVertex)queue.dequeue();
      visitedVertices[w.getValue()] = true;
      if(w.allowsEmanatingEdges()) {
        Enumeration<NamedEdge> edges = swmm.emanatingEdges(w);
        while(edges.hasMoreElements()) {
          NamedEdge edge = edges.nextElement();
          if(! visitedVertices[edge.V1().getValue()]) {
            // Now find the edge that shares the same name, but is of type SoarIdentifierVertex
            if(edge.V1().equals(sv)) {
              Enumeration<NamedEdge> foundEdges = swmm.emanatingEdges(w);
              while(foundEdges.hasMoreElements()) {
                NamedEdge foundEdge = foundEdges.nextElement();
                if(edge.getName().equals(foundEdge.getName()) && (foundEdge.V1() instanceof SoarIdentifierVertex)) {
                  return foundEdge.V1();
                }
              }
            }
            visitedVertices[w.getValue()] = true;
            queue.enqueue(edge.V1());
          }     // if haven't visited this vertex yet, then check if match and add to queue
        }   // while edges have more elements
      }
    }    // while queue is not empty
    return null;


  }

	
	/**
	 * This function finds all vertices that are unreachable from a state
	 * and adds them to a list of holes so that they can be recycled for later
	 * use
	 */
	 public abstract void reduce(List<SoarVertex> listOfStartVertices);
}
