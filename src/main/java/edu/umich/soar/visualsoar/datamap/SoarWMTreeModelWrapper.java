package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.util.AddingVisitor;
import edu.umich.soar.visualsoar.util.EnumerationIteratorWrapper;
import edu.umich.soar.visualsoar.util.RemovingVisitor;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.LinkedList;

/**
 * This is a wrapper class
 * that makes SoarWorkingMemory behave like a Tree Model
 *
 * @author Brad Jones
 */
public class SoarWMTreeModelWrapper implements TreeModel, WorkingMemoryListener {
    private final LinkedList<TreeModelListener> listeners = new LinkedList<>();
    private final FakeTreeNode root;

    /**
     * Creates a tree model from working memory
     * by creating a FakeTreeNode as the root of the tree
     *
     * @param swmm working memory
     * @param siv  the soar vertex which the root is based on
     * @param name name of the tree
     * @see FakeTreeNode
     */
    public SoarWMTreeModelWrapper(SoarWorkingMemoryModel swmm, SoarIdentifierVertex siv, String name) {
        swmm.addWorkingMemoryListener(this);
        root = new FakeTreeNode(swmm, siv, name);
    }

    /**
     * Adds a listener for the TreeModelEvent posted after the tree changes.
     */
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    /**
     * Get a particular child of a tree object
     *
     * @param parent the parent
     * @param index  which child of the parent
     */
    public Object getChild(Object parent, int index) {
        return ((FakeTreeNode) parent).getChildAt(index);
    }


    public int getChildCount(Object parent) {
        return ((FakeTreeNode) parent).getChildCount();
    }

    /**
     * Given a parent and a child, returns an integer describing which
     * index that child is located at in the tree model.
     */
    public int getIndexOfChild(Object parent, Object child) {
        return ((FakeTreeNode) parent).getIndex(((FakeTreeNode) child));
    }

    public Object getRoot() {
        return root;
    }

    public boolean isLeaf(Object node) {
        return ((FakeTreeNode) node).isLeaf();
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    public void WMEAdded(WorkingMemoryEvent wme) {
        NamedEdge triple = wme.getTriple();
        AddingVisitor av = new AddingVisitor(triple);

        av.visit(root);
        root.visitChildren(av);
        EnumerationIteratorWrapper e = av.changeEvents();
        while (e.hasMoreElements()) {
            TreeModelEvent tme = (TreeModelEvent) e.nextElement();
            notifyListenersOfAdd(tme);
        }
    }

    public void WMERemoved(WorkingMemoryEvent wme) {
        NamedEdge triple = wme.getTriple();
        RemovingVisitor rv = new RemovingVisitor(triple);
        rv.visit(root);
        root.visitChildren(rv);
        EnumerationIteratorWrapper e = rv.changeEvents();
        while (e.hasMoreElements()) {
            TreeModelEvent tme = (TreeModelEvent) e.nextElement();
            notifyListenersOfRemove(tme);
        }

    }

    protected void notifyListenersOfAdd(TreeModelEvent tme) {
        EnumerationIteratorWrapper e = new EnumerationIteratorWrapper(listeners.iterator());
        while (e.hasMoreElements()) {
            TreeModelListener tml = (TreeModelListener) e.nextElement();
            tml.treeNodesInserted(tme);
        }
    }

    protected void notifyListenersOfRemove(TreeModelEvent tme) {
        EnumerationIteratorWrapper e = new EnumerationIteratorWrapper(listeners.iterator());
        while (e.hasMoreElements()) {
            TreeModelListener tml = (TreeModelListener) e.nextElement();
            tml.treeNodesRemoved(tme);
        }
    }

}
