package edu.umich.soar.visualsoar.operatorwindow;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Arrays;
import java.util.List;

class TransferableOperatorNodeLink implements Transferable {
	public static final DataFlavor[] flavors = { new DataFlavor(Integer.class, "An Id for the Operator Node") };
	private static final List<DataFlavor> flavorList = Arrays.asList(flavors);

	private final Integer soarOperatorNodeId;

	public TransferableOperatorNodeLink(Integer inSoarOperatorNodeId) {
		soarOperatorNodeId = inSoarOperatorNodeId;
	}
	
	public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.equals(flavors[0])) {
			return soarOperatorNodeId;
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
