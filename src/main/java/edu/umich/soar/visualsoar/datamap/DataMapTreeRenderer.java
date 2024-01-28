package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.graph.NamedEdge;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * This class customizes the look of the DataMap Tree.
 * It is responsible for changing the color of the text of nodes
 * generated by the datamap generator.
 */
public class DataMapTreeRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = 20221225L;

    public DataMapTreeRenderer() {
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        if (isGeneratedNode(value)) {
            setForeground(Color.green.darker().darker());
        }

        return this;
    }

    protected boolean isGeneratedNode(Object value) {
        if (value instanceof FakeTreeNode) {
            FakeTreeNode node = (FakeTreeNode) value;
            NamedEdge ne = node.getEdge();
            if (ne != null) {
                return ne.isGenerated();
            }
        }
        return false;
    }   // end of isGeneratedNode() member function


}//class DataMapRenderer
