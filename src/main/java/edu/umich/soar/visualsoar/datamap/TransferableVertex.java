package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.graph.Edge;
import edu.umich.soar.visualsoar.graph.Vertex;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * This allows vertexes to be used in drag and drop operations
 * @author Brad Jones
 */


public class TransferableVertex implements Transferable {
	public static final DataFlavor[] flavors = { new DataFlavor(Vector.class, "Visual Soar Working Memory Id, and Name") };
	private static final List flavorList = Arrays.asList(flavors);
	
	private final Integer id;
	private final String rep;
	private Edge edge;
	
	public TransferableVertex(Vertex v, String _rep) {
		id = v.getValue();
		rep = _rep;
	}
	
	public TransferableVertex(Vertex v, String inRep, Edge e) {
		this(v,inRep);
		edge = e;
	}
	
	public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.equals(flavors[0])) {
			//FIXME:  This is a poor way to combine heterogeneous data
			Vector v = new Vector(3);
			v.add(id);
			v.add(rep);
			v.add(edge);
			return v;
		}
		throw new UnsupportedFlavorException(flavor);
	}	

	/**
	 * @return a reference to the dataflavors
	 */
	public synchronized DataFlavor[] getTransferDataFlavors() {
		return flavors;
	}
	
	/**
	 * @param flavor the data flavor to check if it is supported 
	 * @return true if the data flavor is supported false otherwise
	 */
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return (flavorList.contains(flavor));
	}
}
