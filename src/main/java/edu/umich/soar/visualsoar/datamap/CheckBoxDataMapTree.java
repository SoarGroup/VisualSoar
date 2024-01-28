package edu.umich.soar.visualsoar.datamap;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * class CheckBoxDataMapTree
 *
 * is a special variant of the DataMapTree that displays a checkbox
 * next to every entry in the tree.  This is used for the functionality
 * that allows the user to link items from a foreign datamap.  The
 * checkboxes allow the user to select the desired items.
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
