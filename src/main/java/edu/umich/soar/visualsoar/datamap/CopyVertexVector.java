package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.graph.SoarVertex;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * This allows vertices to be copied in a shallow manner
 *
 * @author Jon Bauman
 */

public class CopyVertexVector extends Vector<CopyVertex> implements Transferable {
    public static final DataFlavor[] flavors =
            {
                    new DataFlavor(Vector.class, "Visual Soar Vertex Type and Name")
            };
    private static final List<DataFlavor> flavorList = Arrays.asList(flavors);

    public CopyVertexVector(int capacity) {
        super(capacity);
    }

    public void add(String name, SoarVertex vertex) {
        add(new CopyVertex(name, vertex));
    }

    public String getName(int index) {
        CopyVertex v = get(index);
        return v.name;
    }

    public SoarVertex getVertex(int index) {
        CopyVertex v = get(index);
        return v.vertex;
    }

    public synchronized Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException {
        if (flavor.equals(flavors[0])) {
            return this;
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
