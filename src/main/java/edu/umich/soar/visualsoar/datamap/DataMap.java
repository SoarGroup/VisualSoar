package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.misc.CustomInternalFrame;
import edu.umich.soar.visualsoar.misc.FeedbackListEntry;
import edu.umich.soar.visualsoar.misc.Prefs;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.Vector;

/**
 * This class is the internal frame in which the DataMap resides
 *
 * @author Brad Jones
 * @see DataMapTree
 */

public class DataMap extends CustomInternalFrame implements MenuListener {
    private static final long serialVersionUID = 20221225L;

    protected DataMapTree dataMapTree;
    private final int id;

    //These menu items must be instance variable since they might or might not be enabled
    private JMenuItem pasteItem;
    private JMenuItem linkItem;

////////////////////////////////////////
// Constructors
////////////////////////////////////////

    /**
     * base ctor is only for use by child classes for minimal initialization
     */
    protected DataMap(SoarIdentifierVertex siv, String title) {
        super("Datamap " + title, true, true, true, true);
        setType(DATAMAP);
        setBounds(0, 0, 250, 100);
        id = siv.getValue();

        // re-tile the internal frames after closing a window
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addInternalFrameListener(
                new InternalFrameAdapter() {
                    public void internalFrameClosing(InternalFrameEvent e) {
                        closeMyselfAndReTile();
                    }
                });


    }//base ctor

    /**
     * Create a DataMap in an internal frame
     *
     * @param swmm  Working Memory - SoarWorkingMemoryModel
     * @param siv   the vertex that is the root of datamap
     * @param title the name of the datamap window, generally the name of the selected operator node
     *
     * @see SoarWMTreeModelWrapper
     * @see DataMapTree
     */
    public DataMap(SoarWorkingMemoryModel swmm, SoarIdentifierVertex siv, String title) {
        this(siv, title);

        TreeModel soarTreeModel = new SoarWMTreeModelWrapper(swmm, siv, title);
        soarTreeModel.addTreeModelListener(new DataMapListenerModel());

        dataMapTree = new DataMapTree(this, soarTreeModel, swmm, true);
        dataMapTree.setCellRenderer(new DataMapTreeRenderer());

        //setup window controls
        getContentPane().add(new JScrollPane(dataMapTree));
        setupMenuBar();

        //apply read-only mode if it's on for the project
        setReadOnly(MainFrame.getMainFrame().isReadOnly());
    }//ctor

    /** this datamap window removes itself from the main frame and causes the remaining windows to be re-tiled */
    protected void closeMyselfAndReTile() {
        MainFrame mf = MainFrame.getMainFrame();
        mf.getDesktopPane().dmRemove(id);
        dispose();

        if (Prefs.autoTileEnabled.getBoolean()) {
            mf.getDesktopPane().performTileAction();
        }
        mf.selectNewInternalFrame();
    }//closeMyselfAndReTile



    /**
     * setupMenuBar
     *
     * called by the ctor to initialize the menu for a datamap window
     */
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu editMenu = new JMenu("Edit");
        editMenu.addMenuListener(this);
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


        this.pasteItem = new JMenuItem("Paste");
        editMenu.add(pasteItem);
        pasteItem.setAccelerator(KeyStroke.getKeyStroke("control V"));
        pasteItem.addActionListener(dataMapTree.pasteAction);


        this.linkItem = new JMenuItem("Paste as Link");
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

        //These are menu-items that are disabled in read-only mode
        readOnlyDisabledMenuItems.add(copyItem);
        readOnlyDisabledMenuItems.add(pasteItem);
        readOnlyDisabledMenuItems.add(linkItem);
        readOnlyDisabledMenuItems.add(removeItem);
    }//setupMenuBar

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

    /**
     * configures the editor in/out of read-only mode
     * @param isReadOnly  read-only=true  editable=false
     */
    @Override
    public void setReadOnly(boolean isReadOnly) {

        //enable/disable menu actions that change the contents
        for(JMenuItem item : readOnlyDisabledMenuItems) {
            item.setEnabled(! isReadOnly);
        }

        //set same isReadOnly for the associated soar document
        dataMapTree.isReadOnly =  isReadOnly;

        //Update the title bar to show the isReadOnly
        String title = getTitle();
        if (isReadOnly) {
            title = MainFrame.RO_LABEL + title;
        }
        else {
            title = title.replace(MainFrame.RO_LABEL, "");
        }
        setTitle(title);
    }//setReadOnly



    public Vector<FeedbackListEntry> searchTestDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        return dataMapTree.searchTestDataMap(in_siv, dataMapName);
    }

    public Vector<FeedbackListEntry> searchCreateDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        return dataMapTree.searchCreateDataMap(in_siv, dataMapName);
    }

    public Vector<FeedbackListEntry> searchTestNoCreateDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        return dataMapTree.searchTestNoCreateDataMap(in_siv, dataMapName);
    }

    public Vector<FeedbackListEntry> searchCreateNoTestDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        return dataMapTree.searchCreateNoTestDataMap(in_siv, dataMapName);
    }

    public Vector<FeedbackListEntry> searchNoTestNoCreateDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        return dataMapTree.searchNoTestNoCreateDataMap(in_siv, dataMapName);
    }

    public void displayGeneratedNodes() {
        dataMapTree.displayGeneratedNodes();
    }

    /**
     * Selects (highlights and centers) the requested edge within the datamap.
     *
     * @param edge the requested NamedEdge to select
     */
    public void selectEdge(NamedEdge edge) {
        FakeTreeNode node = dataMapTree.selectEdge(edge);

        if (node != null) {
            TreePath path = new TreePath(node.getTreePath().toArray());
            dataMapTree.scrollPathToVisible(path);
            dataMapTree.setSelectionPath(path);
        } else {
            JOptionPane.showMessageDialog(MainFrame.getMainFrame(), "Could not find a matching FakeTreeNode in the datamap");
        }
    }

    /**
     * the Paste and Paste as Link menu items should not be active
     * if the clipboard is empty or unusable.  This method is called
     * to activate or deactivate those menu items as appropriate
     */
    @Override
    public void menuSelected(MenuEvent e) {
        if (! dataMapTree.isReadOnly) {
            boolean canPaste = dataMapTree.clipboardIsPasteable();
            pasteItem.setEnabled(canPaste);
            linkItem.setEnabled(canPaste);
        }
    }
    @Override
    public void menuDeselected(MenuEvent e) { /* ignore */ }
    @Override
    public void menuCanceled(MenuEvent e) { /* ignore */ }


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

