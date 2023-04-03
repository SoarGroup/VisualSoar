package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.misc.CustomInternalFrame;
import edu.umich.soar.visualsoar.misc.FeedbackListObject;
import edu.umich.soar.visualsoar.misc.Prefs;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Vector;

/**
 * This class is the internal frame in which the DataMap resides
 *
 * @author Brad Jones
 * @see DataMapTree
 */

public class DataMap extends CustomInternalFrame {
    private static final long serialVersionUID = 20221225L;

    private DataMapTree dataMapTree;
    private int id;

////////////////////////////////////////
// Constructors
////////////////////////////////////////

    /**
     * Create a DataMap in an internal frame
     *
     * @param swmm  Working Memory - SoarWorkingMemoryModel
     * @param siv   the vertex that is the root of datamap
     * @param title the name of the datamap window, generally the name of the selected operator node
     * @see SoarWMTreeModelWrapper
     * @see DataMapTree
     */
    public DataMap(SoarWorkingMemoryModel swmm, SoarIdentifierVertex siv, String title) {
        super("Datamap " + title, true, true, true, true);
        setType(DATAMAP);
        setBounds(0, 0, 250, 100);
        id = siv.getValue();

        // Retile the internal frames after closing a window
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addInternalFrameListener(
                new InternalFrameAdapter() {
                    public void internalFrameClosing(InternalFrameEvent e) {

                        MainFrame mf = MainFrame.getMainFrame();
                        mf.getDesktopPane().dmRemove(id);
                        dispose();

                        if (Prefs.autoTileEnabled.getBoolean()) {
                            mf.getDesktopPane().performTileAction();
                        }
                        mf.selectNewInternalFrame();
                    }
                });

        TreeModel soarTreeModel = new SoarWMTreeModelWrapper(swmm, siv, title);
        soarTreeModel.addTreeModelListener(new DataMapListenerModel());
        dataMapTree = new DataMapTree(this, soarTreeModel, swmm);
        getContentPane().add(new JScrollPane(dataMapTree));

        dataMapTree.setCellRenderer(new DataMapTreeRenderer());

        JMenuBar menuBar = new JMenuBar();
        JMenu editMenu = new JMenu("Edit");
        JMenu validateMenu = new JMenu("Validation");

/*		Too Dangerous, see DataMapTree.java

		JMenuItem cutItem = new JMenuItem("Cut");		
		editMenu.add(cutItem);
		cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,Event.CTRL_MASK));
		cutItem.addActionListener(dataMapTree.cutAction);
*/

        JMenuItem copyItem = new JMenuItem("Copy");
        editMenu.add(copyItem);
        copyItem.setAccelerator(KeyStroke.getKeyStroke("control C"));
        copyItem.addActionListener(dataMapTree.copyAction);


        JMenuItem pasteItem = new JMenuItem("Paste");
        editMenu.add(pasteItem);
        pasteItem.setAccelerator(KeyStroke.getKeyStroke("control V"));
        pasteItem.addActionListener(dataMapTree.pasteAction);


        JMenuItem linkItem = new JMenuItem("Paste as Link");
        editMenu.add(linkItem);
        linkItem.setAccelerator(KeyStroke.getKeyStroke("control L"));
        linkItem.addActionListener(dataMapTree.linkAction);

        JMenuItem searchItem = new JMenuItem("Search Datamap");
        editMenu.add(searchItem);
        searchItem.setAccelerator(KeyStroke.getKeyStroke("control F"));
        searchItem.addActionListener(dataMapTree.searchAction);

        JMenuItem validateItem = new JMenuItem("Validate Datamap");
        validateMenu.add(validateItem);
        validateItem.addActionListener(dataMapTree.validateDataMapAction);

        JMenuItem removeItem = new JMenuItem("Remove Non-Validated");
        validateMenu.add(removeItem);
        removeItem.addActionListener(dataMapTree.removeInvalidAction);

        menuBar.add(editMenu);
        menuBar.add(validateMenu);

        setJMenuBar(menuBar);
    }

    public int getId() {
        return id;
    }

    /** Since the datamap contents are auto-saved it can't remain modified for long.
     */
    @Override
    public void setModified(boolean b) {
        if (b) {
            //briefly set modified so that app knows project has changed and auto-save will begin operation
            super.setModified(true);
            super.setModified(false);
        }
    }

    public Vector<FeedbackListObject> searchTestDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        return dataMapTree.searchTestDataMap(in_siv, dataMapName);
    }

    public Vector<FeedbackListObject> searchCreateDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        return dataMapTree.searchCreateDataMap(in_siv, dataMapName);
    }

    public Vector<FeedbackListObject> searchTestNoCreateDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        return dataMapTree.searchTestNoCreateDataMap(in_siv, dataMapName);
    }

    public Vector<FeedbackListObject> searchCreateNoTestDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        return dataMapTree.searchCreateNoTestDataMap(in_siv, dataMapName);
    }

    public Vector<FeedbackListObject> searchNoTestNoCreateDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        return dataMapTree.searchNoTestNoCreateDataMap(in_siv, dataMapName);
    }

    public void displayGeneratedNodes() {
        dataMapTree.displayGeneratedNodes();
    }

    /**
     * Selects (highlights and centers) the requested edge within the datamap.
     *
     * @param edge the requested NamedEdge to select
     * @return true if success, false if could not find edge
     */
    public boolean selectEdge(NamedEdge edge) {
        FakeTreeNode node = dataMapTree.selectEdge(edge);

        if (node != null) {
            TreePath path = new TreePath(node.getTreePath().toArray());
            dataMapTree.scrollPathToVisible(path);
            dataMapTree.setSelectionPath(path);

            return true;
        } else {
            JOptionPane.showMessageDialog(null, "Could not find a matching FakeTreeNode in the datamap");
            return false;
        }
    }

    /**
     * This class customizes the look of the DataMap Tree.
     * It is responsible for changing the color of the text of nodes
     * generated by the datamap generator.
     */
    private static class DataMapTreeRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 20221225L;

        public DataMapTreeRenderer() {
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


    }   // end of DataMapRenderer class

    private class DataMapListenerModel implements TreeModelListener {
        public void treeNodesChanged(TreeModelEvent e) {

        }

        public void treeNodesInserted(TreeModelEvent e) {
            // Make sure to expand path to display created node
            dataMapTree.expandPath(e.getTreePath());


        }

        public void treeNodesRemoved(TreeModelEvent e) {

        }

        public void treeStructureChanged(TreeModelEvent e) {
        }

    }
} // end of DataMap class

