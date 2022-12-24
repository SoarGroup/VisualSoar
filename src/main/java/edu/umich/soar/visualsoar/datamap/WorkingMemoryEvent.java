package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.graph.NamedEdge;

/**
 * A Working Memory Element is basically an edge in the graph
 * when asked it will tell you the WME that changed, imagined
 * use is through the WorkingMemoryListener interface, which
 * will give you the context of how this WME changed
 * @author Brad Jones
 * @version 0.5a Oct 1999
 */

public class WorkingMemoryEvent {
///////////////////////////////////////////////////////////////////
// Data Members
///////////////////////////////////////////////////////////////////
	/**
	 * The edge that changed
	 */
	private final NamedEdge edge;

	/**
	 * Constructs the Feedback List
	 * @param e the WME that changed 
	 */
	public WorkingMemoryEvent(NamedEdge e) {
		edge = e;
	}
	
	/**
     * @return the working element that changed, don't mess around
     * with it
	 */
	public NamedEdge getTriple() {
		return edge;
	}
}
