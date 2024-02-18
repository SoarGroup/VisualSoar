package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.graph.ForeignVertex;
import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.graph.SoarVertex;
import edu.umich.soar.visualsoar.misc.FeedbackListEntry;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Vector;

/**
 * class CheckBoxDataMapTree
 * <p>
 * is a special variant of the DataMapTree that displays a checkbox
 * next to every entry in the tree.  This is used for the functionality
 * that allows the user to link items from a foreign datamap.  The
 * checkboxes allow the user to select the desired items.
 *
 * @author Andrew Nuxoll
 * @version Created: Jan 2024
 */
public class CheckBoxDataMapTree extends DataMapTree implements MouseListener {

    //Used to display the node as a labeled checkbox
    CheckBoxDataMapTreeRenderer renderer = new CheckBoxDataMapTreeRenderer();

    /**
     * ctor
     *
     * @param initParent datamap window that displays this tree
     * @param model      the model which specifies the contents of the tree.
     * @param _swmm      datamap data (loaded from .dm file)
     */
    public CheckBoxDataMapTree(DataMap initParent, TreeModel model, SoarWorkingMemoryModel _swmm) {
        super(initParent, model, _swmm, false);

        setCellRenderer(this.renderer);
        addMouseListener(this);
    }//ctor

    /** tick all checkboxes in this tree */
    public void selectAll() {
        renderer.selectAll();
    }//selectAll

    /** un-tick all checkboxes in this tree */
    public void selectNone() {
        renderer.selectNone();
    }//selectNone

    /**
     * importFromForeignDataMap
     * <p>
     * is called when the user hits the Import button.  It extracts all the
     * entries that the user selected and adds a set of
     * {@link ForeignVertex} objects this project's local datamap.
     */
    public void importFromForeignDataMap() {
        //These are the level 1 datamap entries the user selected for import
        Vector<FakeTreeNode> level1FTNs = this.renderer.getAllSelectedEntries();
        if (level1FTNs.isEmpty()) {
            MainFrame.getMainFrame().setStatusBarMsg("Nothing to import!");
            return;
        }

        //Get the relative path to the foreign datamap file
        if (!(this.parentWindow instanceof CheckBoxDataMap)) {
            System.err.println("Error:  CheckBoxDataMapTree's parent is not a CheckBoxDataMap!");
            return; //should never happen
        }
        CheckBoxDataMap parent = (CheckBoxDataMap) this.parentWindow;
        String foreignDM = parent.getForeignDMFilename();

        //Retrieve the root vertex of the local SWMM
        SoarWorkingMemoryModel localSWMM = MainFrame.getMainFrame().getOperatorWindow().getDatamap();
        SoarVertex localRoot = localSWMM.getTopstate();

        //This is a mapping from foreign SoarIdentifierVertex ids to ForeignVertex objects
        HashMap<Integer, ForeignVertex> seenSoFar = new HashMap<>();

        //We want to track all entries that were added, so we can report to the user
        Vector<String> addedEntries = new Vector<>();

        //Create the ForeignVertex objs and add to local SWMM
        for (FakeTreeNode ftn : level1FTNs) {
            //extract the SoarVertex from the foreign datamap
            NamedEdge ne = ftn.getEdge();
            SoarVertex foreignSV = ne.V1();

            //Record the addition
            String rep = "    <s> ^" + ne.getName();  //added spaces aid readability later
            addedEntries.add(rep);

            //To avoid insanity, skip any foreign vertexes.
            if (foreignSV instanceof ForeignVertex) continue;

            //Graft a new foreign vertex attached to the local top-state
            int newId = localSWMM.getNextVertexId();
            ForeignVertex fv = new ForeignVertex(newId, foreignDM, foreignSV);
            localSWMM.addVertex(fv);
            localSWMM.addTriple(localRoot, ne.getName(), fv);

            //record the mapping from foreign SIV id to ForeignVertex object
            if (foreignSV instanceof SoarIdentifierVertex) {
                seenSoFar.put(foreignSV.getValue(), fv);
            }

            //Build out the entire foreign subtree recursively
            if (ftn.getChildCount() > 0) {
                addForeignSubTree(localSWMM, this.swmm, foreignSV, fv, seenSoFar, rep, addedEntries);
            }
        }//for

        //Report the success to the user
        addedEntries.add(0, "The following " + addedEntries.size() + " entries were imported from " + foreignDM + ":");
        MainFrame.getMainFrame().setFeedbackListWithStrings(addedEntries);

    }//importFromForeignDataMap




    /** When the user clicks on a node in the tree, toggle its checkbox */
    @Override
    public void mousePressed(MouseEvent e) {
        TreePath path = getSelectionPath();
        if (path == null) return;
        FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
        if (ftn != null) {
            renderer.toggle(ftn);
        }

        //This is a bit of a kludge.  When you click on a tree node you are
        //also clicking on a JCheckBox.  So one click generates two events.
        //If the node you are clicking on is currently selected, then
        //those events both translate into clicks on the JCheckBox so,
        //in effect, the checkbox is clicked twice yielding a null result!
        //To prevent this, the currently selected node is kept at the root.
        this.setSelectionPath(new TreePath(this.treeModel.getRoot()));
        this.repaint();
    }//mousePressed

    //Ignore these events
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) { }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }

}//class CheckBoxDataMapTree
