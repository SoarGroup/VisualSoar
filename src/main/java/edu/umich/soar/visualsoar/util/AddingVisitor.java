package edu.umich.soar.visualsoar.util;
import edu.umich.soar.visualsoar.datamap.FakeTreeNode;
import edu.umich.soar.visualsoar.graph.NamedEdge;

import javax.swing.event.TreeModelEvent;
import java.util.*;

/**
 * This class follows the visitor pattern
 * it is based on Object-Oriented Design patterns in C++, later converted to Java
 * Something has been added to working memory so add the edge to the datamap models
 * and produce the proper change event that can be iterated through later
 * @author Brad Jones
 */

public class AddingVisitor extends Visitor {
///////////////////////////////////////////
// Data Members
///////////////////////////////////////////
	private final NamedEdge edge;
	private final LinkedList<TreeModelEvent> changeEvents = new LinkedList<>();

	public AddingVisitor(NamedEdge ne) {
		edge = ne;
	}
	
///////////////////////////////////////////
// Manipulators
///////////////////////////////////////////
	public void visit(Object o) {
		if(o instanceof FakeTreeNode) {
			FakeTreeNode ftn = (FakeTreeNode)o;
			if (!ftn.hasLoaded()) {
				return;
      }
			if (edge.V0().getValue() == ftn.getEnumeratingVertex().getValue()) {
				changeEvents.add(ftn.add(edge));
			}
		}
	}
	
	public EnumerationIteratorWrapper changeEvents() {
		return new EnumerationIteratorWrapper(changeEvents.iterator());
	}
}
