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
 *
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
     * addForeignSubTree       <!-- RECURSIVE -->
     *
     * creates ForeignVertex objects for a given parentForeignFTN and all its descendants.  These
     * are added to the given result vector _unless_ their id is in the seenSoFar list.
     *
     * @param localSWMM  the SWMM for this project's datamap
     * @param foreignDM  the relative path the .dm file for the foreign datamap
     * @param parentForeignFTN an FTN from the foreign datamap.  The children of this FTN will be added to the local
     *                   datamap as {@link ForeignVertex} objects and their children will be added recursively.
     * @param foreignSV  the local vertex representing the parent FTN's value in the local SWMM.  It's represented
     *                   locally as a ForeignVertex.
     * @param seenSoFar  a mapping of the foreign IDs of all {@link SoarIdentifierVertex} objects seen so far from
     *                   the foreign datamap to the ForeignVertex objects added to this project's datamap.  This is
     *                   used to handle links in the foreign database and to avoid infinite recursion (e.g., base case).
     * @param addedFTNs  as the recursion proceeds, it keeps a list of all new {@link ForeignVertex}-based FTNs that
     *                   have been added to the datamap, so they can be reported to the user at the end.
     *
     * @author Andrew Nuxoll
     * created:  Feb 2024
     */
    private void addForeignSubTree(SoarWorkingMemoryModel localSWMM,
                                   String foreignDM,
                                   FakeTreeNode parentForeignFTN,
                                   ForeignVertex foreignSV,
                                   HashMap<Integer, ForeignVertex> seenSoFar,
                                   Vector<FakeTreeNode> addedFTNs) {

        //For each child FTN of the parentForeignFTN...
        int numChildren = parentForeignFTN.getChildCount();
        for(int i = 0; i < numChildren; ++i) {
            FakeTreeNode childFTN = parentForeignFTN.getChildAt(i);

            //Extract the child SoarVertex from the foreign DM
            NamedEdge neChild = childFTN.getEdge();
            SoarVertex foreignChildSV = neChild.V1();

            //To avoid insanity, skip any foreign vertexes in the foreign datamap.
            if (foreignChildSV instanceof ForeignVertex) continue;

            //If this child has been seen before (copy/link in datamap) retrieve its associated ForeignVertex
            ForeignVertex childFV = null;
            int childId = foreignChildSV.getValue();
            if (foreignChildSV instanceof SoarIdentifierVertex) {
                childFV = seenSoFar.get(childId);
            }

            //If we've not seen this SIV before, create a new ForeignVertex in the local SWMM
            boolean newForeignVertex = (childFV == null);
            if (newForeignVertex) {
                int newId = localSWMM.getNextVertexId();
                childFV = new ForeignVertex(newId, foreignDM, foreignChildSV);
                localSWMM.addVertex(childFV);
            }

            //Add the new entry in the local datamap
            localSWMM.addTriple(foreignSV, neChild.getName(), childFV);

            //Record the addition to report to the user later
            addedFTNs.add(childFTN);

            //If this is a new ForeignVertex, then record the associated foreign SIV and recurse to add its children
            if (newForeignVertex) {
                seenSoFar.put(childId, childFV);

                if (childFTN.getChildCount() > 0) {
                    addForeignSubTree(localSWMM, foreignDM, childFTN, childFV, seenSoFar, addedFTNs);
                }
            }

        }//for

    }//addForeignSubTree

    /**
     * importFromForeignDataMap
     *
     * is called when the user hits the Import button.  It extracts all the
     * entries that the user selected and adds a set of
     * {@link ForeignVertex} objects this project's local datamap.
     *
     */
    public void importFromForeignDataMap() {
        //These are the level 1 datamap entries the user selected for import
        Vector<FakeTreeNode> level1FTNs = this.renderer.getAllSelectedEntries();
        if (level1FTNs.isEmpty()) {
            MainFrame.getMainFrame().setStatusBarMsg("Nothing to import!");
            return;
        }

        //Get the relative path to the foreign datamap file
        if (! (this.parentWindow instanceof CheckBoxDataMap)) {
            System.err.println("Error:  CheckBoxDataMapTree's parent is not a CheckBoxDataMap!");
            return; //should never happen
        }
        CheckBoxDataMap parent = (CheckBoxDataMap)this.parentWindow;
        String foreignDM = parent.getForeignDMFilename();

        //Retrieve the root vertex of the local SWMM
        SoarWorkingMemoryModel localSWMM = MainFrame.getMainFrame().getOperatorWindow().getDatamap();
        SoarVertex localRoot = localSWMM.getTopstate();

        //This is a mapping from foreign SoarIdentifierVertex ids to ForeignVertex objects
        HashMap<Integer, ForeignVertex> seenSoFar = new HashMap<>();

        //We want to track all FTNs that were added, so we can report to the user
        Vector<FakeTreeNode> addedFTNs = new Vector<>(level1FTNs);

        //Create the ForeignVertex objs and add to local SWMM
        for(FakeTreeNode ftn : level1FTNs) {
            //extract the SoarVertex from the foreign datamap
            NamedEdge ne = ftn.getEdge();
            SoarVertex foreignSV = ne.V1();

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
                addForeignSubTree(localSWMM, foreignDM, ftn, fv, seenSoFar, addedFTNs);
            }
        }//for

        //Report the success to the user
        Vector<FeedbackListEntry> msgs = new Vector<>();
        msgs.add(new FeedbackListEntry("The following entries were imported from " + foreignDM));
        for(FakeTreeNode ftn : addedFTNs) {
            msgs.add(new FeedbackListEntry("   " + ftn.stringPath()));
        }
        if (msgs.size() > 4) {
            msgs.add(new FeedbackListEntry("Total: " + seenSoFar.size() + " new entries"));
        }
        MainFrame.getMainFrame().setFeedbackListData(msgs);

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
