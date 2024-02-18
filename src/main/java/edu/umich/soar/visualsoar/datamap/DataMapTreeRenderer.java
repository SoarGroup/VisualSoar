package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.graph.ForeignVertex;
import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.graph.SoarVertex;

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

    public static final Color brown = new Color(153, 77, 0);

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

        //Foreign nodes are brown
        if (isForeignNode(value)) {
            setForeground(brown);
        }

        //Un-validated nodes are dark green
        else if (isGeneratedNode(value)) {
            setForeground(Color.green.darker().darker());
        }


        return this;
    }

    protected boolean isForeignNode(Object value) {
        if (value instanceof FakeTreeNode) {
            FakeTreeNode ftn = (FakeTreeNode) value;
            NamedEdge ne = ftn.getEdge();

            //check for root node
            if (ne == null) {
                return false;
            }

            SoarVertex sv = ne.V1();
            return (sv instanceof ForeignVertex);
        }
        return false;
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
