package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;

/**
 * class CheckBoxDataMapTreeRenderer
 *
 * adds a checkbox to the nodes in a datamap tree node
 *
 * Initial code taken from
 * <a href="http://www.java2s.com/Code/Java/Swing-JFC/CheckBoxNodeTreeSample.htm">this example</a>.
 */
public class CheckBoxDataMapTreeRenderer implements TreeCellRenderer {

    //Each node in the tree has an associated checkbox object stored here
    private final HashMap<FakeTreeNode, JCheckBox> boxes = new HashMap<>();

    //This is a set of named edges that emanate from the root of the current
    // project's top-level datamap. It's populated in the ctor and used to
    // detect items in the foreign datamap that conflict with the project
    private final HashSet<String> nativeEdges = new HashSet<>();

    //These colors are used for highlighting
    Color selectionForeground, selectionBackground;
    //These colors are the regular appearance
    Color textForeground, textBackground;

    /** ctor */
    public CheckBoxDataMapTreeRenderer() {
        //initialize colors
        selectionForeground = UIManager.getColor("Tree.selectionForeground");
        selectionBackground = UIManager.getColor("Tree.selectionBackground");
        textForeground = UIManager.getColor("Tree.textForeground");
        textBackground = UIManager.getColor("Tree.textBackground");

        populateNativeEdgesSet();
    }//ctor

    /** populate this.{@link #nativeEdges } */
    private void populateNativeEdgesSet() {
        //Load the native datamap into a SWMM
        OperatorWindow opWin = MainFrame.getMainFrame().getOperatorWindow();
        SoarWorkingMemoryModel nativeSWMM = opWin.getDatamap();

        //Retrieve the attributes of all WMEs whose parent is the root
        SoarIdentifierVertex root = nativeSWMM.getTopstate();
        Enumeration<NamedEdge> e = nativeSWMM.emanatingEdges(root);
        while (e.hasMoreElements()) {
            String eStr = e.nextElement().getName();
            this.nativeEdges.add(eStr);
        }
    }//populateNativeFTNSet

    //Toggle the checkbox associated with a tree node
    public void toggle(FakeTreeNode ftn) {
        JCheckBox checkBox = this.boxes.get(ftn);
        if (checkBox != null) checkBox.doClick();
    }


    /**
     * getTreeCellRendererComponent
     *
     * displays the node as a labeled checkbox.  This method gets called for each node each time it is drawn.
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {


        //Not sure what to do in this case?  Does it even happen?
        if (! (value instanceof FakeTreeNode)) return null;

        FakeTreeNode ftn = (FakeTreeNode) value;

        //If the node is not at level 1 (child of the root) then it's not allowed to have
        //a checkbox.  This was a simplifying design decision we made at first implementation.
        //In the future, this restriction could be relaxed
        FakeTreeNode parent = ftn.getParent();
        if ((parent == null) || (! parent.isRoot())) {
            return makeJLabelComponent(ftn, false);
        }

        //check to see if this node has a name conflict with the native datamap
        //While it's possible the user intends to import a different entry
        //with the same name it's unlikely and I've made a design decision to
        //ban that here
        if (nativeEdges.contains(ftn.getEdge().toString())) {
            return makeJLabelComponent(ftn, true);
        }

        //Create/find a JCheckBox for this tree node
        return makeJCheckBoxComponent(ftn);

    }//getTreeCellRendererComponent

    /**
     * makeJLabelComponent
     *
     * finds the JCheckBox for a given node or makes a new one if it doesn't exist.
     * The checkbox is highlighted correctly based upon whether it is checked.
     *
     * This is a helper method for {@link #getTreeCellRendererComponent}
     *
     * @param ftn  the node
     */

    private JCheckBox makeJCheckBoxComponent(FakeTreeNode ftn) {
        //Retrieve existing JCheckBox (if it exists)
        JCheckBox checkBox;
        if (this.boxes.containsKey(ftn)) {
            checkBox = this.boxes.get(ftn);
        }

        //Otherwise create a new one
        else {
            checkBox = new JCheckBox();
            checkBox.setText(ftn.toString());
            this.boxes.put(ftn, checkBox);
        }

        //highlight the node if it is checked (mimicking tree selection)
        if (checkBox.isSelected()) {
            checkBox.setForeground(selectionForeground);
            checkBox.setBackground(selectionBackground);
        } else {
            checkBox.setForeground(textForeground);
            checkBox.setBackground(textBackground);
        }
        return checkBox;
    }//makeJCheckBoxComponent

    /**
     * makeJLabelComponent
     *
     * generates a JLabel for a given node
     *
     * This is a helper method for {@link #getTreeCellRendererComponent}
     *
     * @param ftn  the node
     * @param isConflict  whether this node conflicts (name clash) with the current datamap
     */
    private JLabel makeJLabelComponent(FakeTreeNode ftn, boolean isConflict) {

        JLabel label = new JLabel(ftn.toString());

        //If it's a conflict then just make it red text
        if (isConflict) {
            label.setForeground(Color.red.darker().darker());
            return label;
        }

        //determine if this label should be highlighted depending upon whether
        //its level 1 ancestor's checkbox is checked
        boolean highlight = false;
        while(true) {
            FakeTreeNode parent = ftn.getParent();
            if (parent == null) break;  //ftn must have been the root

            //If parent has a checkbox then set highlight
            JCheckBox checkBox = this.boxes.get(parent);
            if (checkBox != null) {
                highlight = checkBox.isSelected();
                break;
            }

            //recurse up the tree
            ftn = parent;
        }//while

        //highlight the node if its parent is checked (mimicking tree selection)
        if (highlight) {
            label.setForeground(selectionForeground);
            label.setOpaque(true); //so background will be visible
            label.setBackground(selectionBackground);
        } else {
            label.setForeground(textForeground);
            label.setBackground(textBackground);
        }

        return label;

    }//makeJLabelComponent

    /** tick all the checkboxes in this.boxes */
    public void selectAll() {
        for(JCheckBox box : this.boxes.values()) {
            box.setSelected(true);
            box.invalidate();
        }
    }

    /** untick all the checkboxes in this.boxes */
    public void selectNone() {
        for(JCheckBox box : this.boxes.values()) {
            box.setSelected(false);
            box.invalidate();
        }
    }

}//class CheckBoxDataMapTreeRenderer
