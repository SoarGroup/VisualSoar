package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.util.TextFolderIcons;
import edu.umich.soar.visualsoar.util.TextIcons;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;



/*
 *  This class helps render different leaf icons for the three types of
 *  Operators:  Impasse, File and Operators.
 */


public class OperatorWindowRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = 20221225L;

    public OperatorWindowRenderer() {
    }

    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {


        super.getTreeCellRendererComponent(
                tree, value, sel,
                expanded, leaf, row,
                hasFocus);


        if (value instanceof OperatorNode) {
            OperatorNode node = (OperatorNode) value;

            if (node instanceof FolderNode) {
                setIcon(TextFolderIcons.getIcon("file"));
            } else if (node instanceof SoarOperatorNode) {
                SoarOperatorNode soarNode = (SoarOperatorNode) value;
                // Make sure that it is a leaf node
                if (!soarNode.isHighLevel()) {
                    if (node.toString().startsWith("Impasse")) {
                        // Impasse
                        setIcon(TextIcons.getIcon("impasse"));


                    } else if (node instanceof FileOperatorNode) {
                        // FileOperator
                        setIcon(TextIcons.getIcon("file"));
                    } else {
                        // Operator
                        setIcon(TextIcons.getIcon("operator"));
                    }
                } else {
                    if (node.toString().startsWith("Impasse")) {
                        // Impasse
                        setIcon(TextFolderIcons.getIcon("impasse"));
                    } else if (node instanceof FileOperatorNode) {
                        // FileOperator
                        setIcon(TextFolderIcons.getIcon("file"));
                    } else {
                        // Operator
                        setIcon(TextFolderIcons.getIcon("operator"));
                    }
                }     // end of else is a high level operator
            }   // end of if is a SoarOperatorNode
            else if (node instanceof LinkNode) {
                // Link Node
                setIcon(TextIcons.getIcon("link"));
            } else if (node instanceof FileNode) {
                // File
                setIcon(TextIcons.getIcon("file"));
            }
        }

        return this;
    }       // end of getTreeCellRendererComponent
}     // end of Operator Window Tree Renderer class
