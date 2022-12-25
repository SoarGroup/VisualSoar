package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;

import javax.swing.tree.TreeNode;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Map;

/**
 * class LinkNode
 * <p>
 * represents a node in the operator hierarchy pane that is linked as
 * a child of this one.  Links can be created via drag and drop.
 * <p>
 * NOTE:  At some point in the past, LinkNodes were disabled (not accessible
 * via the UI).  Older .vsa files (version 5 or before) may still contain them.
 * So, the code remains here to support it. -:AMN: Dec 2022
 */
public class LinkNode extends FileNode {
    SoarOperatorNode linkedToNode;
    int linkedToNodeId;

    public LinkNode(String inName, int inId, String inFileName, int inLinkToNodeId) {
        super(inName, inId, inFileName);
        linkedToNodeId = inLinkToNodeId;
    }

    public void restore(Map<Integer, VSTreeNode> persistentIds) {
        linkedToNode = (SoarOperatorNode) persistentIds.get(linkedToNodeId);
        linkedToNode.registerLink(this);
    }

    public String getFolderName() {
        return linkedToNode.getFolderName();
    }

    public javax.swing.tree.TreeNode getChildAt(int childIndex) {
        return linkedToNode.getChildAt(childIndex);
    }

    public int getChildCount() {
        return linkedToNode.getChildCount();
    }

    public boolean getAllowsChildren() {
        return linkedToNode.getAllowsChildren();
    }

    public boolean isLeaf() {
        return linkedToNode.isLeaf();
    }

    public Enumeration<TreeNode> children() {
        return EMPTY_ENUMERATION;
    }


    public OperatorNode addSuboperator(OperatorWindow operatorWindow, SoarWorkingMemoryModel swmm, String newOperatorName) throws IOException {
        return linkedToNode.addSuboperator(operatorWindow, swmm, newOperatorName);
    }

    public void addFile(OperatorWindow operatorWindow, String newFileName) throws IOException {
        linkedToNode.addFile(operatorWindow, newFileName);
    }

    public void delete(OperatorWindow operatorWindow) {
        OperatorNode parent = (OperatorNode) getParent();
        renameToDeleted(new File(getFileName()));
        operatorWindow.removeNode(this);
        parent.notifyDeletionOfChild(operatorWindow, this);
        linkedToNode.removeLink(this);
    }

    public String toString() {
        if (linkedToNode == null) {
            return name;
        } else {
            return name + " @ " + linkedToNode.getUniqueName();
        }
    }

    public void write(Writer w) throws IOException {
        w.write("LINK " + name + " " + fileAssociation + " " + linkedToNode.getId() + " " + id);
    }

    public void showContextMenu(Component c, int x, int y) {
        addSuboperatorItem.setEnabled(true);
        addFileItem.setEnabled(false);
        openRulesItem.setEnabled(true);
        openDataMapItem.setEnabled(true);
        deleteItem.setEnabled(true);
        renameItem.setEnabled(false);
        exportItem.setEnabled(true);
        impasseSubMenu.setEnabled(false);
        checkChildrenAgainstDataMapItem.setEnabled(true);
        contextMenu.show(c, x, y);
    }

    public void exportDesc(Writer w) throws IOException {
        w.write("OPERATOR " + name);
    }

}
