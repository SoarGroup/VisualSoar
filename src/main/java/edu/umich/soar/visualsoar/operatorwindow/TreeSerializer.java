package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.files.projectjson.LayoutNode;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * This class saves the contents of a @see DefaultTreeModel DefaultTreeModel containing DataMap
 * objects to a file to later be read in by DataMapReader
 *
 * @author Brad Jones
 * @author Jon Bauman
 * @version 0.5a 5 Aug 1999
 */
public class TreeSerializer {

  /**
   * Traverses the tree and writes out the data in the VSA file format Format of vsa file is: nodeId
   * parentId NodeType name filename nextId
   *
   * @param treeWriter the writer for the vsa file
   * @param tree the operator hierarchy tree that is being read.
   * @see DefaultTreeModel DefaultTreeModel
   */
  public static void write(Writer treeWriter, DefaultTreeModel tree) throws IOException {

    final String TAB = "\t";

    // This hash table is used to associate pointers with id's
    // given a pointer you can look up the id for that node
    // this is used for parent id lookup
    Hashtable<TreeNode, Integer> ht = new Hashtable<>();
    Integer nodeID = 0;

    DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getRoot();

    // Doing this enumeration guarantees that we will never reach a
    // child without first processing its parent
    Enumeration<TreeNode> e = root.preorderEnumeration();

    VSTreeNode node = (VSTreeNode) e.nextElement();
    ht.put(node, nodeID);

    // special case for the root node
    // write out tree specific stuff
    treeWriter.write(nodeID + TAB);

    // tell the node to write itself
    node.write(treeWriter);

    // terminate the line
    treeWriter.write("\n");

    while (e.hasMoreElements()) {
      nodeID = nodeID + 1;
      node = (VSTreeNode) e.nextElement();
      ht.put(node, nodeID);

      // Again the same technique write out the tree information, then the node specific stuff, then
      // terminate the line
      treeWriter.write(nodeID + TAB + ht.get(node.getParent()) + TAB);
      node.write(treeWriter);
      treeWriter.write("\n");
    }
    treeWriter.write("END\n");
  }

  public static LayoutNode toJson(DefaultTreeModel tree) {
    VSTreeNode root = (VSTreeNode) tree.getRoot();
    return toJson(root);
  }

  private static LayoutNode toJson(VSTreeNode treeNode) {
    List<LayoutNode> jsonChildren = new ArrayList<>();
    treeNode
        .children()
        .asIterator()
        .forEachRemaining(
            tn -> {
              VSTreeNode child = (VSTreeNode) tn;
              LayoutNode jsonChild = toJson(child);
              jsonChildren.add(jsonChild);
            });

    switch (treeNode.getType()) {
      case FILE:
        FileNode fn = (FileNode) treeNode;
        return new LayoutNode.File(
            jsonChildren, fn.getName(), String.valueOf(fn.getId()), fn.getFileAssociation());
      case FILE_OPERATOR:
        FileOperatorNode fon = (FileOperatorNode) treeNode;
        if (fon.isHighLevel()) {
          return new LayoutNode.HighLevelFileOperator(
              jsonChildren,
              fon.getName(),
              String.valueOf(fon.getId()),
              fon.getFileAssociation(),
              fon.getRelativeFolderName(),
              String.valueOf(fon.getDataMapIdNumber()));
        } else {
          return new LayoutNode.FileOperator(
              jsonChildren, fon.getName(), String.valueOf(fon.getId()), fon.getFileAssociation());
        }
      case FOLDER:
        FolderNode folderNode = (FolderNode) treeNode;
        return new LayoutNode.Folder(
            jsonChildren,
            folderNode.getName(),
            String.valueOf(folderNode.getId()),
            folderNode.folderName);
      case LINK:
        LinkNode linkNode = (LinkNode) treeNode;
        return new LayoutNode.Link(
            jsonChildren,
            linkNode.getName(),
            String.valueOf(linkNode.getId()),
            linkNode.fileAssociation,
            String.valueOf(linkNode.linkedToNodeId));
      case OPERATOR:
        OperatorOperatorNode operatorNode = (OperatorOperatorNode) treeNode;
        if (operatorNode.isHighLevel()) {
          return new LayoutNode.HighLevelOperator(
              jsonChildren,
              operatorNode.getName(),
              String.valueOf(operatorNode.getId()),
              operatorNode.getFileAssociation(),
              operatorNode.getRelativeFolderName(),
              String.valueOf(operatorNode.getDataMapIdNumber()));
        } else {
          return new LayoutNode.Operator(
              jsonChildren,
              operatorNode.getName(),
              String.valueOf(operatorNode.getId()),
              operatorNode.getFileAssociation());
        }
      case OPERATOR_ROOT:
        OperatorRootNode opRootNode = (OperatorRootNode) treeNode;
        return new LayoutNode.OperatorRoot(
            jsonChildren,
            opRootNode.getName(),
            String.valueOf(opRootNode.getId()),
            opRootNode.folderName);
      case IMPASSE_OPERATOR:
        ImpasseOperatorNode impasseNode = (ImpasseOperatorNode) treeNode;
        if (impasseNode.isHighLevel()) {
          return new LayoutNode.HighLevelImpasseOperator(
              jsonChildren,
              impasseNode.getName(),
              String.valueOf(impasseNode.getId()),
              impasseNode.getFileAssociation(),
              impasseNode.getRelativeFolderName(),
              String.valueOf(impasseNode.getDataMapIdNumber()));
        } else {
          return new LayoutNode.ImpasseOperator(
              jsonChildren,
              impasseNode.getName(),
              String.valueOf(impasseNode.getId()),
              impasseNode.getFileAssociation());
        }
      default:
        throw new IllegalArgumentException(
            "Unknown VSTreeNode class found while writing project JSON: "
                + treeNode.getClass().getName());
    }
  }
}
