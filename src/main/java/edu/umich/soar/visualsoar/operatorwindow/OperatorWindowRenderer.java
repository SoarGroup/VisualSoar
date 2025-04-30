package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.components.TreeIcons;

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
                setIcon(TreeIcons.getFolderIcon(TreeIcons.IconType.FILE));
            } else if (node instanceof SoarOperatorNode) {
                SoarOperatorNode soarNode = (SoarOperatorNode) value;
                // Make sure that it is a leaf node
                if (!soarNode.isHighLevel()) {
                    if (node.toString().startsWith("Impasse")) {
                        // Impasse
                        setIcon(TreeIcons.getFileIcon(TreeIcons.IconType.IMPASSE));
                    } else if (node instanceof FileOperatorNode) {
                        // FileOperator
                      setIcon(TreeIcons.getFileIcon(TreeIcons.IconType.FILE));
                    } else {
                        // Operator
                      setIcon(TreeIcons.getFileIcon(TreeIcons.IconType.OPERATOR));
                    }
                } else {
                    if (node.toString().startsWith("Impasse")) {
                        // Impasse
                        setIcon(TreeIcons.getFolderIcon(TreeIcons.IconType.IMPASSE));
                    } else if (node instanceof FileOperatorNode) {
                        // FileOperator
                        setIcon(TreeIcons.getFolderIcon(TreeIcons.IconType.FILE));
                    } else {
                        // Operator
                        setIcon(TreeIcons.getFolderIcon(TreeIcons.IconType.OPERATOR));
                    }
                }     // end of else is a high level operator
            }   // end of if is a SoarOperatorNode
            else if (node instanceof LinkNode) {
                // Link Node
              setIcon(TreeIcons.getFileIcon(TreeIcons.IconType.LINK));
            } else if (node instanceof FileNode) {
                // File
              setIcon(TreeIcons.getFileIcon(TreeIcons.IconType.FILE));
            }
        }

        return this;
    }       // end of getTreeCellRendererComponent
}     // end of Operator Window Tree Renderer class
