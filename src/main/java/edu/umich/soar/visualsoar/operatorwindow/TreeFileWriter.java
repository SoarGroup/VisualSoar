package edu.umich.soar.visualsoar.operatorwindow;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This class saves the contents of a @see DefaultTreeModel DefaultTreeModel containing
 * DataMap objects to a file to later be read in by DataMapReader
 *
 * @author Brad Jones
 * @author Jon Bauman
 * @version 0.5a 5 Aug 1999
 */
public class TreeFileWriter {


    /**
     * Traverses the tree and writes out the data in the VSA file format
     * Format of vsa file is:
     * nodeId  parentId  NodeType  name  filename  nextId
     *
     * @param treeWriter the writer for the vsa file
     * @param tree       the operator hierarchy tree that is being read.
     * @see DefaultTreeModel DefaultTreeModel
     */
    public static void write(Writer treeWriter, DefaultTreeModel tree) throws IOException {

        final String TAB = "\t";

        // This hash table is used to associate pointers with id's
        // given a pointer you can lookup the id for the that node
        // this is used for parent id lookup
        Hashtable<TreeNode, Integer> ht = new Hashtable<>();
        Integer nodeID = 0;

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getRoot();

        // Doing this enumeration guarentees that we will never reach a
        // child without first processing its parent
        Enumeration<TreeNode> e = root.preorderEnumeration();

        VSTreeNode node = (VSTreeNode) e.nextElement();
        ht.put(node, nodeID);

        // special case for the root node
        // write out tree specific stuff
        treeWriter.write(nodeID + TAB);

        // tell the node to write it self
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


}  // end of TreeFileWriter
