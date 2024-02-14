package edu.umich.soar.visualsoar.misc;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;

/**
 * class FeedbackEntryDatamap
 * <p>
 * is a subclass of {@link FeedbackListEntry} that identifies an entry in the
 * datamap that has a mismatch with the code (or similar issue). It reacts by
 * displaying this code file to the user and highlighting the associated line.
 */
public class FeedbackEntryDatamap extends FeedbackListEntry {

    ///////////////////////////////////////////////////////////////////
    // Data Members
    ///////////////////////////////////////////////////////////////////
    private NamedEdge edge;
    private SoarIdentifierVertex siv;
    private String dataMapName;

    ///////////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////////
    public FeedbackEntryDatamap(NamedEdge in_edge,
                                SoarIdentifierVertex in_siv,
                                String inDataMapName,
                                String msg) {
        super(msg);
        edge = in_edge;
        siv = in_siv;
        dataMapName = inDataMapName;
    }

    ///////////////////////////////////////////////////////////////////
    // Accessor Methods
    ///////////////////////////////////////////////////////////////////

    public int getDataMapId() {
        if (siv == null) return -1;
        return siv.getValue();
    }

    public NamedEdge getEdge() { return edge; }

    @Override
    public String toString() { return dataMapName + ":  " + edge.toString(); }

    ///////////////////////////////////////////////////////////////////
    // Methods
    ///////////////////////////////////////////////////////////////////
    /**
     * Creates a temporary DataMap from the datamap information in this object.
     * This is a helper method for {@link #react()}
     *
     * @return null on failure
     */
    private DataMap createDataMap(SoarWorkingMemoryModel swmm) {
        //sanity check
        if (siv == null) return null;

        DataMap dm;
        if (siv.getValue() != 0) {
            dm = new DataMap(swmm, siv, dataMapName);
        } else {
            dm = new DataMap(swmm, swmm.getTopstate(), dataMapName);
        }
        return dm;
    }//createDataMap

    /**
     * Displays the datamap file associated with this object
     */
    @Override
    public void react() {
        // Only open a new window if the window does not already exist
        DataMap dm = MainFrame.getMainFrame().getDesktopPane().dmGetDataMap(this.getDataMapId());
        if (dm != null) {
            try {
                if (dm.isIcon()) {
                    dm.setIcon(false);
                }
                dm.setSelected(true);
                dm.moveToFront();
            } catch (java.beans.PropertyVetoException pve) {
                System.err.println("Guess we can't do that");
            }
        } else {
            dm = this.createDataMap(MainFrame.getMainFrame().getOperatorWindow().getDatamap());
            MainFrame mf = MainFrame.getMainFrame();
            mf.addDataMap(dm);
            mf.getDesktopPane().dmAddDataMap(this.getDataMapId(), dm);
            try {
                dm.setVisible(true);
            }
            catch(NullPointerException npe) {
                //this should never happen
                return;
            }
        }

        // Highlight the proper node within the datamap
        dm.selectEdge(this.getEdge());
    }//react




}//class FeedbackEntryDatamap
