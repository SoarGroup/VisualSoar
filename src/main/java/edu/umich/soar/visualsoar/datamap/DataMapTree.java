package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.dialogs.searchdm.SearchDataMapDialog;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.dialogs.*;
import edu.umich.soar.visualsoar.graph.*;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackEntryDatamap;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackEntryForeignDatamap;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackEntryOpNode;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorRootNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;
import edu.umich.soar.visualsoar.parser.ParseException;
import edu.umich.soar.visualsoar.parser.SoarProduction;
import edu.umich.soar.visualsoar.parser.TokenMgrError;
import edu.umich.soar.visualsoar.parser.Triple;
import edu.umich.soar.visualsoar.util.QueueAsLinkedList;
import edu.umich.soar.visualsoar.util.VSQueue;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * class DataMapTree
 * <p>
 * A class to implement the behavior of the DataMap.  This class has
 * these inner classes for responding to these events:
 * - DMTDragGestureListener implements DragGestureListener
 * - DMTDropGestureListener implements DropGestureListener
 * - DMTDragSourceListener implements DragSourceListener
 * . provides "drag over" feedback during a drag and drop operation
 * - CopyAction extends AbstractAction
 * - PasteAction extends AbstractAction
 * - SearchAction extends AbstractAction
 * - ValidateDataMapAction extends AbstractAction
 * - RemoveInvalidAction extends AbstractAction
 *
 * @author Jon Bauman
 * @author Brad Jones
 * @author Andrew Nuxoll
 */
public class DataMapTree extends JTree implements ClipboardOwner, PopupMenuListener {
    private static final long serialVersionUID = 20221225L;


    ////////////////////////////////////////
    // DataMembers
    ////////////////////////////////////////
    protected final DataMap parentWindow;  //the window displaying the contents of this tree to the user
    public static Clipboard clipboard = new Clipboard("Datamap Clipboard");
    private static DataMapTree s_DataMapTree;

    //public Action cutAction = new CutAction();  //removed because too dangerous
    public Action copyAction = new CopyAction();
    public Action pasteAction = new PasteAction();
    public Action linkAction = new LinkAction();
    public Action searchAction = new SearchAction();
    public Action validateDataMapAction = new ValidateDataMapAction();
    public Action removeInvalidAction = new RemoveInvalidAction();

    /**
     * Reference to the DragGestureListener for Drag and Drop operations, may be deleted in the future.
     */
    DragGestureListener dgListener = new DMTDragGestureListener();

    /**
     * Reference to the DropTargetListener for Drag and Drop operations, may be deleted in the future.
     */
    DropTargetListener dtListener = new DMTDropTargetListener();

    /**
     * Reference to the DropTarget for Drag and Drop operations, may be deleted in the future.
     */
    @SuppressWarnings("unused")  //not sure why IntelliJ thinks this is "unused"
    private final DropTarget dropTarget = new DropTarget(this, DnDConstants.ACTION_LINK | DnDConstants.ACTION_COPY_OR_MOVE, dtListener, true);

    /**
     * to support Read-Only mode
     */
    public boolean isReadOnly = false;

    protected final SoarWorkingMemoryModel swmm;
    private static final JPopupMenu contextMenu = new JPopupMenu();
    private static final JMenuItem AddIdentifierItem = new JMenuItem("Add Identifier...");
    private static final JMenuItem AddEnumerationItem = new JMenuItem("Add Enumeration...");
    private static final JMenuItem AddIntegerItem = new JMenuItem("Add Integer...");
    private static final JMenuItem AddFloatItem = new JMenuItem("Add Float...");
    private static final JMenuItem AddStringItem = new JMenuItem("Add String...");

    private static final JMenuItem CopyItem = new JMenuItem("Copy");
    private static final JMenuItem PasteItem = new JMenuItem("Paste");
    private static final JMenuItem LinkItem = new JMenuItem("Paste as Link");

    private static final JMenuItem SearchForItem = new JMenuItem("Search For...");
    private static final JMenuItem FindUsingProdsItem = new JMenuItem("Find Productions that Create or Test this WME");
    private static final JMenuItem FindTestingProdsItem = new JMenuItem("Find Productions that Test this WME");
    private static final JMenuItem FindCreatingProdsItem = new JMenuItem("Find Productions that Create this WME");

    private static final JMenuItem RemoveAttributeItem = new JMenuItem("Delete Attribute...");
    private static final JMenuItem RenameAttributeItem = new JMenuItem("Rename Attribute...");
    private static final JMenuItem EditValueItem = new JMenuItem("Edit Value(s)...");

    private static final JMenuItem EditCommentItem = new JMenuItem("Add/Edit Comment...");
    private static final JMenuItem RemoveCommentItem = new JMenuItem("Remove Comment");

    private static final JMenuItem ValidateEntryItem = new JMenuItem("Validate Entry");
    private static final JMenuItem ValidateAllItem = new JMenuItem("Validate All");
    private static final JMenuItem ReimportSubtree = new JMenuItem("Re-Import Subtree");
    private static final JMenuItem VerifySubtree = new JMenuItem("Verify Imported Subtree");


    private static final JMenu ChangeTypeSubMenu = new JMenu("Change Datamap Type...");
    private static final JMenuItem ChangeToIdentifierItem = new JMenuItem("to Identifier");
    private static final JMenuItem ChangeToEnumerationItem = new JMenuItem("to Enumeration");
    private static final JMenuItem ChangeToIntegerItem = new JMenuItem("to Integer");
    private static final JMenuItem ChangeToFloatItem = new JMenuItem("to Float");
    private static final JMenuItem ChangeToStringItem = new JMenuItem("to String");

    public static TreePath OriginalSelectionPath;

    /////////////////////////////////////////////////////////////////////
    //This block constructs the context menu
    /////////////////////////////////////////////////////////////////////
    static {

        contextMenu.add(AddIdentifierItem);
        contextMenu.add(AddEnumerationItem);
        contextMenu.add(AddIntegerItem);
        contextMenu.add(AddFloatItem);
        contextMenu.add(AddStringItem);

        contextMenu.addSeparator();
        contextMenu.add(CopyItem);
        contextMenu.add(PasteItem);
        contextMenu.add(LinkItem);

        contextMenu.addSeparator();
        contextMenu.add(SearchForItem);
        contextMenu.add(FindUsingProdsItem);
        contextMenu.add(FindTestingProdsItem);
        contextMenu.add(FindCreatingProdsItem);
        contextMenu.addSeparator();

        contextMenu.add(ChangeTypeSubMenu);
        contextMenu.add(RemoveAttributeItem);
        contextMenu.add(RenameAttributeItem);
        contextMenu.add(EditValueItem);

        contextMenu.addSeparator();

        contextMenu.add(EditCommentItem);
        contextMenu.add(RemoveCommentItem);

        contextMenu.addSeparator();

        contextMenu.add(ValidateEntryItem);
        contextMenu.add(ValidateAllItem);

        contextMenu.addSeparator();

        contextMenu.add(ReimportSubtree);
        contextMenu.add(VerifySubtree);


        ChangeTypeSubMenu.add(ChangeToIdentifierItem);
        ChangeToIdentifierItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.changeTypeTo(NodeType.IDENTIFIER);
          });

        ChangeTypeSubMenu.add(ChangeToEnumerationItem);
        ChangeToEnumerationItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.changeTypeTo(NodeType.ENUMERATION);
          });

        ChangeTypeSubMenu.add(ChangeToIntegerItem);
        ChangeToIntegerItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.changeTypeTo(NodeType.INTEGER);
          });

        ChangeTypeSubMenu.add(ChangeToFloatItem);
        ChangeToFloatItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.changeTypeTo(NodeType.FLOAT);
          });

        ChangeTypeSubMenu.add(ChangeToStringItem);
        ChangeToStringItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.changeTypeTo(NodeType.STRING);
          });


        AddIdentifierItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.addIdentifier();
          });

        AddEnumerationItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.addEnumeration();
          });

        AddIntegerItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.addInteger();
          });

        AddFloatItem.addActionListener(e -> {
            DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
            dmt.addFloat();
        });

        AddStringItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.addString();
          });

        CopyItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.copyAction.actionPerformed(e);
          });

        PasteItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.pasteAction.actionPerformed(e);
          });

        LinkItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.linkAction.actionPerformed(e);
          });

        SearchForItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.searchFor();
          });


        FindUsingProdsItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.findProds(true, true);
          });

        FindTestingProdsItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.findProds(true, false);
          });

        FindCreatingProdsItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.findProds(false, true);
          });

        RemoveAttributeItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.removeEdge();
          });

        RenameAttributeItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.renameEdge();
          });

        EditValueItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.editValue();
          });

        EditCommentItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.editComment();
          });

        RemoveCommentItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.removeComment();
          });

        ValidateEntryItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.validateEntry();
          });

        ValidateAllItem.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.validateAll();
          });

        ReimportSubtree.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.reimportSubtree();
          });

        VerifySubtree.addActionListener(
          e -> {
              DataMapTree dmt = (DataMapTree) contextMenu.getInvoker();
              dmt.verifySubtree();
          });

    }//static


    /**
     * The lone constructor. Creating new DataMaps and reading in saved DataMaps
     * are the same operation, so there is only one constructor. This constructor
     * sets some data fields as well as specifying the custom cell renderer for
     * italicizing links and add the mouse adapter for right-clicking.
     *
     * @param initParent datamap window that displays this tree
     * @param model      the model which specifies the contents of the tree.
     * @param _swmm      datamap data (loaded from .dm file)
     * @param forEditing set to 'true' to allow the user to edit the datamap
     */
    public DataMapTree(DataMap initParent, TreeModel model, SoarWorkingMemoryModel _swmm, boolean forEditing) {
        super(model);
        parentWindow = initParent;
        swmm = _swmm;
        s_DataMapTree = this;

        contextMenu.addPopupMenuListener(this);

        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        //When a datamap tree is being used to select nodes (i.e., import from foreign datamap)
        //it should not respond to editing related events.
        if (forEditing) {
            setupEditingEventHandling();
            displayGeneratedNodes();
        }

    }//ctor

    /**
     * setupEditingEventHandling
     * <p>
     * activates mouse and keyboard event handling for editing the tree
     */
    public void setupEditingEventHandling() {
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_LINK | DnDConstants.ACTION_COPY_OR_MOVE, dgListener);
        setAutoscrolls(true);

        registerKeyboardAction(
          e -> removeEdge(),
                KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
                WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        registerKeyboardAction(
          e -> removeEdge(),
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
                WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        registerKeyboardAction(
          e -> {
              TreePath path = getSelectionPath();
              if (path == null) return;
              FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
              if (ftn == null) return;
              SoarVertex theVertex = ftn.getEnumeratingVertex();
              if (theVertex.isEditable()) {
                  editValue();
              }
          },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        addMouseListener(new MouseAdapter() {

            // Open the rules associated with invalid generated node
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = getSelectionPath();
                    if (path == null) return;
                    FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
                    if (ftn == null) return;
                    NamedEdge ne = ftn.getEdge();
                    if (ne != null && ne.isGenerated() && (ne.getNode() != null)) {
                        (ne.getNode()).openRules(MainFrame.getMainFrame(), ne.getLine());
                    }
                }
            }

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    suggestShowContextMenu(e.getX(), e.getY());
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    suggestShowContextMenu(e.getX(), e.getY());
                }
            }
        });
    }//setupEditingEventHandling


    /**
     * static accessor for the datamap tree.
     */
    public static DataMapTree getDataMapTree() {
        return s_DataMapTree;
    }

    /**
     * helper method to en/disable all context menu items that modify the datamap and,
     * thus, should not be accessible in read-only mode.
     */
    private static void setContextMenuItemsToAllowChanges(boolean changeOk) {
        AddIdentifierItem.setEnabled(changeOk);
        AddEnumerationItem.setEnabled(changeOk);
        AddIntegerItem.setEnabled(changeOk);
        AddFloatItem.setEnabled(changeOk);
        AddStringItem.setEnabled(changeOk);
        CopyItem.setEnabled(changeOk);
        PasteItem.setEnabled(changeOk);
        LinkItem.setEnabled(changeOk);
        RemoveAttributeItem.setEnabled(changeOk);
        RenameAttributeItem.setEnabled(changeOk);
        EditValueItem.setEnabled(changeOk);
        EditCommentItem.setEnabled(changeOk);
        RemoveCommentItem.setEnabled(changeOk);
        ChangeTypeSubMenu.setEnabled(changeOk);
        ChangeToIdentifierItem.setEnabled(changeOk);
        ChangeToEnumerationItem.setEnabled(changeOk);
        ChangeToIntegerItem.setEnabled(changeOk);
        ChangeToFloatItem.setEnabled(changeOk);
        ChangeToStringItem.setEnabled(changeOk);
    }//setContextMenuItemsToAllowChanges

    /**
     * @return true if the contents of the clipboard can be pasted or linked to the datamap
     */
    public boolean clipboardIsPasteable() {
        Transferable tr = clipboard.getContents(this);
        if (tr != null) {
            DataFlavor dataFlavor = TransferableVertex.flavors[0];
            return tr.isDataFlavorSupported(dataFlavor);
        }
        return false;
    }

    /**
     * Checks to see if x,y is a valid location on the screen, if it is then it
     * displays the context menu there.
     *
     * @param x the x coordinate of the screen
     * @param y the y coordinate of the screen
     */
    public void suggestShowContextMenu(int x, int y) {
        TreePath path = getPathForLocation(x, y);
        if (path == null) return;
        setSelectionPath(path);
        FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
        if (ftn == null) return;
        SoarVertex theVertex = ftn.getEnumeratingVertex();
        NamedEdge ne = ftn.getEdge();

        ChangeTypeSubMenu.setEnabled(ftn.getChildCount() == 0);

        //Start out with these default settings
        setContextMenuItemsToAllowChanges(true);
        ReimportSubtree.setEnabled(false);
        VerifySubtree.setEnabled(false);


        //Most restrictive:  read-only
        if (isReadOnly) {
            setContextMenuItemsToAllowChanges(false);
        }

        //almost as restrictive as read-only:  foreign nodes
        else if (theVertex instanceof ForeignVertex) {
            setContextMenuItemsToAllowChanges(false);
            RemoveAttributeItem.setEnabled(true); //the only edit allowed

            //If this is a level-1 foreign subtree it can be re-imported
            ReimportSubtree.setEnabled(ftn.getParent().isRoot());

            //Any foreign subtree can be verified
            VerifySubtree.setEnabled(true);
        }

        //some restrictions:  leaf nodes
        else if (!theVertex.allowsEmanatingEdges()) {
            // can't add any edges
            AddIdentifierItem.setEnabled(false);
            AddEnumerationItem.setEnabled(false);
            AddIntegerItem.setEnabled(false);
            AddFloatItem.setEnabled(false);
            AddStringItem.setEnabled(false);
        }

        // un-editable item
        EditValueItem.setEnabled(theVertex.isEditable());

        if (ne != null) {
            if (ne.isGenerated()) {
                ValidateEntryItem.setEnabled(true);
                ValidateAllItem.setEnabled(true);
            } else {
                ValidateEntryItem.setEnabled(false);
                ValidateAllItem.setEnabled(false);
            }
        }   // end of if has a named edge
        else {
            ValidateEntryItem.setEnabled(false);
            ValidateAllItem.setEnabled(false);
        }

        contextMenu.show(this, x, y);
    }//suggestShowContextMenu

    //TODO:  All the addXXX methods below are very similar.  Could they be combined?

    /**
     * Add a Soar Identifier Attribute to the dataMap
     *
     * @see IdentifierDialog
     */
    public void addIdentifier() {
        IdentifierDialog theDialog = new IdentifierDialog(MainFrame.getMainFrame());

        theDialog.setVisible(true);

        if (theDialog.wasApproved()) {
            String attribute = theDialog.getText();

            TreePath path = getSelectionPath();
            if (path == null) return;
            FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
            if (ftn == null) return;
            SoarVertex v0 = ftn.getEnumeratingVertex();
            SoarVertex v1 = swmm.createNewSoarId();
            swmm.addTriple(v0, attribute, v1);
            if (ftn.getChildCount() != 0) {
                expandPath(path);
            } else {
                System.err.println("I am barren");  //should never happen
            }
            parentWindow.setModified(true);
        }//if approved
    }//addIdentifier

    /**
     * Add a Soar Enumeration Attribute to the datamap
     *
     * @see EnumerationDialog
     */
    public void addEnumeration() {
        EnumerationDialog theDialog = new EnumerationDialog(MainFrame.getMainFrame());

        theDialog.setVisible(true);

        if (theDialog.wasApproved()) {
            String attribute = theDialog.getText();
            Vector<String> enumVal = theDialog.getVector();

            TreePath path = getSelectionPath();
            if (path == null) return;
            FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
            if (ftn == null) return;
            SoarVertex parent = ftn.getEnumeratingVertex();
            SoarVertex child = swmm.createNewEnumeration(enumVal);
            swmm.addTriple(parent, attribute, child);
            if (ftn.getChildCount() != 0) {
                expandPath(path);
            } else {
                System.err.println("I am barren"); //should never happen
            }
            parentWindow.setModified(true);
        }//if
    }//addEnumeration

    /**
     * Add a Soar Integer Attribute to the datamap
     *
     * @see NumberDialog
     */
    public void addInteger() {
        NumberDialog theDialog = new NumberDialog(MainFrame.getMainFrame(), "Integer");

        theDialog.setVisible(true);

        if (theDialog.wasApproved()) {
            String attribute = theDialog.getText();
            Number low = theDialog.getLow(), high = theDialog.getHigh();

            TreePath path = getSelectionPath();
            if (path == null) return;
            FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
            if (ftn == null) return;
            SoarVertex parent = ftn.getEnumeratingVertex();
            SoarVertex child;
            if ((low.intValue() == Integer.MIN_VALUE) && (high.intValue() == Integer.MAX_VALUE)) {
                child = swmm.createNewInteger();
            } else {
                child = swmm.createNewIntegerRange(low.intValue(), high.intValue());
            }
            swmm.addTriple(parent, attribute, child);
            if (ftn.getChildCount() != 0) {
                expandPath(path);
            } else {
                System.err.println("I am barren");  //should never happen
            }
            parentWindow.setModified(true);
        }//if approved
    }//addInteger

    /**
     * Add a Soar Float Attribute to the datamap
     *
     * @see NumberDialog
     */
    public void addFloat() {
        NumberDialog theDialog = new NumberDialog(MainFrame.getMainFrame(), "Float");

        theDialog.setVisible(true);

        if (theDialog.wasApproved()) {
            String attribute = theDialog.getText();
            Number low = theDialog.getLow(), high = theDialog.getHigh();

            TreePath path = getSelectionPath();
            if (path == null) return;
            FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
            if (ftn == null) return;
            SoarVertex parent = ftn.getEnumeratingVertex();
            SoarVertex child;
            if ((low.floatValue() == Float.NEGATIVE_INFINITY) && (high.floatValue() == Float.POSITIVE_INFINITY)) {
                child = swmm.createNewFloat();
            } else {
                child = swmm.createNewFloatRange(low.floatValue(), high.floatValue());
            }
            swmm.addTriple(parent, attribute, child);
            if (ftn.getChildCount() != 0) {
                expandPath(path);
            } else {
                System.err.println("I am barren");  //should never happen
            }
            parentWindow.setModified(true);
        }//if approved
    }//addFloat

    /**
     * Add a Soar String Attribute to the Datamap
     */
    public void addString() {
        IdentifierDialog theDialog = new IdentifierDialog(MainFrame.getMainFrame());

        theDialog.setTitle("Enter String");
        theDialog.setVisible(true);

        if (theDialog.wasApproved()) {
            String attribute = theDialog.getText();

            TreePath path = getSelectionPath();
            if (path == null) return;
            FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
            if (ftn == null) return;
            SoarVertex parent = ftn.getEnumeratingVertex();
            SoarVertex child = swmm.createNewString();
            swmm.addTriple(parent, attribute, child);
            if (ftn.getChildCount() != 0) {
                expandPath(path);
            } else {
                System.err.println("I am barren");  //should never happen
            }
            parentWindow.setModified(true);
        }//if approved
    }//addString


    /**
     * Opens the SearchDataMapDialog to search the datamap beginning at the
     * currently selected node.
     *
     * @see SearchDataMapDialog
     */
    public void searchFor() {
        TreePath thePath = getSelectionPath();
        FakeTreeNode fake = null;
        if (thePath != null) {
            fake = ((FakeTreeNode) thePath.getLastPathComponent());
        } else {
            if (DataMapTree.getDataMapTree().getModel().getRoot() instanceof FakeTreeNode) {
                fake = (FakeTreeNode) DataMapTree.getDataMapTree().getModel().getRoot();
            } else {
                System.err.println("No node selected for search option");
            }
        }
        SearchDataMapDialog searchDialog = new SearchDataMapDialog(MainFrame.getMainFrame(), this, fake);
        searchDialog.setVisible(true);
    }

    /**
     * Finds all productions that test or create the currently selected
     * vertex in the tree.
     *
     * @param bTest   if this boolean is set to false, this function will
     *                ignore matches that test the WME
     * @param bCreate if this boolean is set to false, this function will
     *                ignore matches that create the WME
     */
    public void findProds(boolean bTest, boolean bCreate) {
        TreePath path = getSelectionPath();
        if (path == null) return;
        //Must use 'Object' because this vector will contain both Strings and
        //FeedbackListEntry objects
        Vector<FeedbackListEntry> vecErrors = new Vector<>();

        OperatorWindow operatorWindow = MainFrame.getMainFrame().getOperatorWindow();
        Enumeration<TreeNode> bfe = operatorWindow.getProjectModel().breadthFirstEnumeration();
        while (bfe.hasMoreElements()) {
            OperatorNode opNode = (OperatorNode) bfe.nextElement();
            Vector<SoarProduction> parsedProds = null;
            try {
                parsedProds = opNode.parseProductions();
            } catch (ParseException pe) {
                vecErrors.add(new FeedbackListEntry("Unable to search productions due to parse error"));
                vecErrors.add(opNode.parseParseException(pe));
            } catch (TokenMgrError | IOException tme) {
                tme.printStackTrace();
            }

            if (parsedProds == null) continue;


            Enumeration<SoarProduction> enumProds = parsedProds.elements();
            while (enumProds.hasMoreElements()) {
                SoarProduction sp = enumProds.nextElement();
                Vector<Triple> vecMatches =
                        DataMapMatcher.pathMatchesProduction(path, sp);
                Enumeration<Triple> enumMatches = vecMatches.elements();
                while (enumMatches.hasMoreElements()) {
                    Triple trip = enumMatches.nextElement();

                    //Make sure the caller has requested this match
                    if (((bTest) && (trip.isCondition()))
                            || ((bCreate) && (!trip.isCondition()))) {
                        vecErrors.add(new FeedbackEntryOpNode(opNode,
                                trip.getLine(),
                                sp.getName()));
                    }
                }//while
            }//while
        }//while

        if (vecErrors.isEmpty()) {
            vecErrors.add(new FeedbackListEntry("No matches found."));
        }

        MainFrame.getMainFrame().getFeedbackManager().showFeedback(vecErrors);


    }//findProds


    private enum NodeType {
        IDENTIFIER, ENUMERATION, INTEGER, FLOAT, STRING;
    }

    /**
     * Function changeTypeTo() changes the selected DataMap item to
     * the specified type with the same name.
     */
    private void changeTypeTo(NodeType type) {
        TreePath[] paths = getSelectionPaths();
        FakeTreeNode ftn;
        NamedEdge ne;
        String componentName;
        SoarVertex v1;

        if (paths == null) return;
        for (TreePath path : paths) {
            ftn = (FakeTreeNode) path.getLastPathComponent();
            ne = ftn.getEdge();
            componentName = ne.getName();

            swmm.removeTriple(ne.V0(), ne.getName(), ne.V1());
            if (type == NodeType.IDENTIFIER) {
                v1 = swmm.createNewSoarId();
            } else if (type == NodeType.ENUMERATION) {
                v1 = swmm.createNewEnumeration("nil");
            } else if (type == NodeType.INTEGER) {
                v1 = swmm.createNewInteger();
            } else if (type == NodeType.FLOAT) {
                v1 = swmm.createNewFloat();
            } else if (type == NodeType.STRING) {
                v1 = swmm.createNewString();
            } else {
                throw new IllegalArgumentException("Unknown node type " + type);
            }

            swmm.addTriple(ne.V0(), componentName, v1);
            parentWindow.setModified(true);
        }
    }   // end of changeTypeTo()

    /**
     * Removes an attribute from the datamap
     */
    public void removeEdge() {
        TreePath[] paths = getSelectionPaths();
        FakeTreeNode ftn;
        NamedEdge ne;

        if (paths == null) return;
        for (TreePath path : paths) {
            ftn = (FakeTreeNode) path.getLastPathComponent();
            ne = ftn.getEdge();

            if (ne != null) {
                switch (JOptionPane.showConfirmDialog(this, "Do you really want to delete the attribute named \"" +
                                ne.getName() + "\"?", "Confirm Delete",
                        JOptionPane.YES_NO_CANCEL_OPTION)) {
                    case JOptionPane.YES_OPTION:
                        swmm.removeTriple(ne.V0(), ne.getName(), ne.V1());
                        parentWindow.setModified(true);
                        break;
                    case JOptionPane.CANCEL_OPTION:
                        return;
                }
            } else {
                JOptionPane.showMessageDialog(this, "This is not an attribute", "Cannot remove", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void lostOwnership(Clipboard clipboard, Transferable t) {
        System.out.println("Dang, lost ownership");
    }

    /**
     * Attempts to rename an attribute on the datamap
     */
    public void renameEdge() {
        // Get what the user clicked on
        TreePath path = getSelectionPath();
        if (path == null) return;
        FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
        if (ftn == null) return;

        // Get the edge associated with what the user clicked on
        NamedEdge ne = ftn.getEdge();
        if (ne == null) {
            JOptionPane.showMessageDialog(this, "This is not an edge", "Cannot remove", JOptionPane.ERROR_MESSAGE);
            return;
        }
        IdentifierDialog namedDialog = new IdentifierDialog(MainFrame.getMainFrame());
        namedDialog.makeVisible(ne.getName());
        if (namedDialog.wasApproved()) {
            String newAttributeName = namedDialog.getText();
            swmm.removeTriple(ne.V0(), ne.getName(), ne.V1());
            swmm.addTriple(ne.V0(), newAttributeName, ne.V1());
            parentWindow.setModified(true);
        }
    }

    /**
     * Selects (highlights and centers) the requested edge within the DataMapTree.
     *
     * @param ftn the string name of the edge to be highlighted
     */
    public void highlightEdge(FakeTreeNode ftn) {
        if (ftn != null) {
            TreePath path = new TreePath(ftn.getTreePath().toArray());
            scrollPathToVisible(path);
            setSelectionPath(path);
        } else {
            JOptionPane.showMessageDialog(MainFrame.getMainFrame(), "Could not find a matching wme in the datamap");
        }
    }

    /**
     * Copies a piece of the datamap to the clipboard
     */
    private void copy() {
        clipboard.setContents(getCopyVertices(), this);
    }

    private CopyVertexVector getCopyVertices() {
        TreePath[] paths = getSelectionPaths();
        FakeTreeNode ftn;
        NamedEdge edge;
        SoarVertex vertex;
        String name;
        if (paths == null) return null;
        CopyVertexVector copyVertices = new CopyVertexVector(paths.length);

        for (TreePath path : paths) {
            ftn = (FakeTreeNode) path.getLastPathComponent();
            vertex = ftn.getEnumeratingVertex();
            edge = ftn.getEdge();
            name = edge.getName();

            copyVertices.add(name, vertex);
        }
        return copyVertices;
    }

    private void pasteCopyVertices(CopyVertexVector data) {
        TreePath path = getSelectionPath();
        FakeTreeNode ftn;

        if (data == null) return;
        if (path == null) {
            return;
        }

        ftn = (FakeTreeNode) path.getLastPathComponent();
        for (int j = 0; j < data.size(); j++) {
            SoarVertex parent = ftn.getEnumeratingVertex();
            SoarVertex child = swmm.createVertexCopy(data.getVertex(j));
            swmm.addTriple(parent, data.getName(j), child);
        }
    }

    /**
     * Paste a portion of the datamap from the clipboard
     */
    private void paste() {
        TreePath[] paths = getSelectionPaths();
        FakeTreeNode ftn;
        Transferable transferable;
        DataFlavor dataFlavor;
        CopyVertexVector data;

        if (paths == null) {
            return;
        }

        transferable = clipboard.getContents(this);
        dataFlavor = TransferableVertex.flavors[0];

        try {
            data = (CopyVertexVector) transferable.getTransferData(dataFlavor);

        } catch (UnsupportedFlavorException | IOException ufe) {
            ufe.printStackTrace();
            return;
        }


        //Copy the data to the destination
        for (TreePath path : paths) {
            ftn = (FakeTreeNode) path.getLastPathComponent();
            if (transferable.isDataFlavorSupported(dataFlavor)) {
                for (int j = 0; j < data.size(); j++) {
                    SoarVertex parent = ftn.getEnumeratingVertex();
                    SoarVertex child = swmm.createVertexCopy(data.getVertex(j));

                    //Is this a legal paste?
                    if (! parent.allowsEmanatingEdges()) {
                        String msg = "Paste of " + child + " to " + parent + " is illegal.  " + parent + " is not an identifier.";
                        MainFrame.getMainFrame().getFeedbackManager().setStatusBarError(msg);
                        return;
                    }

                    swmm.addTriple(parent, data.getName(j), child);
                }
                parentWindow.setModified(true);
            }
        }
    }

    /**
     * link a portion of the datamap from the clipboard
     */
    private void link() {
        TreePath[] paths = getSelectionPaths();
        FakeTreeNode ftn;
        Transferable transferable;
        DataFlavor dataFlavor;
        CopyVertexVector data;

        if (paths == null) {
            return;  //nothing to link
        }

        //Retrieve the data
        transferable = clipboard.getContents(this);
        dataFlavor = TransferableVertex.flavors[0];

        try {
            data = (CopyVertexVector) transferable.getTransferData(dataFlavor);

        } catch (UnsupportedFlavorException | IOException ufe) {
            ufe.printStackTrace();
            return;
        }


        //Copy the data to the destination
        for (TreePath path : paths) {
            ftn = (FakeTreeNode) path.getLastPathComponent();
            if (transferable.isDataFlavorSupported(dataFlavor)) {
                //Note:  only the first item in the clipboard is linked
                //       Perhaps we should allow multiple links at once?
                //       Easy enough to do this:  wrap the code below in
                //       a for-loop and replace the '0' below with loop var
                SoarVertex parent = ftn.getEnumeratingVertex();
                SoarVertex child = data.getVertex(0);

                //Is this a legal link?
                if (! parent.allowsEmanatingEdges()) {
                    String msg = "Link of " + child + " to " + parent + " is illegal.  " + parent + " is not an identifier.";
                    MainFrame.getMainFrame().getFeedbackManager().setStatusBarError(msg);
                    return;
                }

                swmm.addTriple(parent, data.getName(0), child);
                parentWindow.setModified(true);
            }
        }
    }//link


    /**
     * Can change the values for an enumeration or number attribute
     */
    public void editValue() {
        TreePath path = getSelectionPath();
        if (path == null) return;
        FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
        if (ftn == null) return;
        NamedEdge ne = ftn.getEdge();
        if (ne == null) {
            JOptionPane.showMessageDialog(this, "This is not an edge", "Cannot Edit Value", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (ne.V1().edit(MainFrame.getMainFrame())) {
            swmm.removeTriple(ne.V0(), ne.getName(), ne.V1());
            swmm.addTriple(ne.V0(), ne.getName(), ne.V1());
        }
    }


    /**
     * This allows the user to add a comment to an edge on the datamap.
     * This comment has no bearing on working memory, only to allow the
     * user to comment their datamap.
     *
     * @see CommentDialog
     */
    public void editComment() {
        TreePath path = getSelectionPath();
        if (path == null) return;
        FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
        if (ftn == null) return;
        NamedEdge ne = ftn.getEdge();
        if (ne == null) {
            JOptionPane.showMessageDialog(this, "This is not an edge", "Cannot Edit Value", JOptionPane.ERROR_MESSAGE);
            return;
        }

        CommentDialog theDialog = new CommentDialog(MainFrame.getMainFrame(), ne.getComment());
        theDialog.setTitle("Enter Comment");
        theDialog.setVisible(true);

        if (theDialog.wasApproved()) {
            String newComment = theDialog.getText();

            swmm.notifyListenersOfRemove(ne);
            ne.setComment(newComment);
            swmm.notifyListenersOfAdd(ne);
            parentWindow.setModified(true);
        }
    }

    /**
     * This removes a comment from an edge on the datamap.
     */
    public void removeComment() {
        TreePath path = getSelectionPath();
        if (path == null) return;
        FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
        if (ftn == null) return;
        NamedEdge ne = ftn.getEdge();
        if (ne == null) {
            JOptionPane.showMessageDialog(this, "This is not an edge", "Cannot Edit Value", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int ret = JOptionPane.showConfirmDialog(
                this,
                "Do you really want to delete this comment?",
                "Confirm Delete",
                JOptionPane.YES_NO_CANCEL_OPTION);
        if (ret == JOptionPane.YES_OPTION) {
            swmm.notifyListenersOfRemove(ne);
            ne.setComment("");
            swmm.notifyListenersOfAdd(ne);
            parentWindow.setModified(true);
        }


    }//removeComment

    /**
     * This function validates the selected node of the datamap,
     * meaning that it makes a single node generated by the datamap
     * generator a valid node and restores its original color.
     */
    public void validateEntry() {
        TreePath path = getSelectionPath();
        if (path == null) return;
        FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
        if (ftn == null) return;
        NamedEdge ne = ftn.getEdge();
        ne.validate();
        swmm.notifyListenersOfRemove(ne);
        swmm.notifyListenersOfAdd(ne);
        parentWindow.setModified(true);
        if (ftn.getChildCount() != 0) {
            expandPath(path);
        }
    } // end of validateEntry()


    /**
     * Similar to validateEntry(), but this function validates not
     * only the selected node, but all of that nodes children.
     */
    public void validateAll() {
        VSQueue<SoarVertex> queue = new QueueAsLinkedList<>();
        int numberOfVertices = swmm.getNumberOfVertices();
        boolean[] visitedVertices = new boolean[numberOfVertices];

        TreePath path = getSelectionPath();
        if (path == null) return;
        FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
        if (ftn == null) return;
        NamedEdge ne = ftn.getEdge();
        queue.enqueue(ne.V1());
        ne.validate();
        swmm.notifyListenersOfRemove(ne);
        swmm.notifyListenersOfAdd(ne);
        parentWindow.setModified(true);

        while (!queue.isEmpty()) {
            SoarVertex w = queue.dequeue();
            visitedVertices[w.getValue()] = true;
            if (w.allowsEmanatingEdges()) {
                Enumeration<NamedEdge> edges = swmm.emanatingEdges(w);
                while (edges.hasMoreElements()) {
                    NamedEdge theEdge = edges.nextElement();
                    theEdge.validate();
                    if (!visitedVertices[theEdge.V1().getValue()]) {
                        visitedVertices[w.getValue()] = true;
                        queue.enqueue(theEdge.V1());
                    }   // if haven't visited this vertex, add to the queue
                } // while looking at all of the edges of the vertex
            }
        }   // while queue is not empty, examine each vertex in it
    } // end of validateAll()

    /**
     * check that the SWMM's root note is a FakeTreeNode and, if not, report this to the user.
     * @return false if the SWMM's root is invalid  */
    private boolean verifyRoot() {
        //This should never happen
        if (! (this.getModel().getRoot() instanceof FakeTreeNode)) {
            JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                    "Working Memory Model is corrupted.  Validation impossible",
                    "DataMap Generator",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }//verifyRoot

    /**
     * findOrLoadForeignDatamap
     *
     * retrieves a {@link SoarWorkingMemoryModel} object for the V1() vertex on a given {@link NamedEdge}.
     * If it's already been loaded it's returned, otherwise it's loaded and added to a cache.
     *
     * Caveat: theEdge.V1() must be a {@link ForeignVertex}!  The method does not double-check this.
     *
     * @param theEdge       the NamedEdge that refers to the external datamap
     * @param cache         external datamaps that have already been loaded
     * @param vecIssues     if the load fails, a message is placed here as a side effect
     *
     * @return the loaded datamap or null on failure
     *
     * @see #reimportSubtree()
     * @see #verifySubtree()
     */
    private SoarWorkingMemoryModel findOrLoadForeignDatamap(NamedEdge theEdge,
                                                            HashMap<String, SoarWorkingMemoryModel> cache,
                                                            Vector<FeedbackListEntry> vecIssues) {
        //Try to retrieve it from the HashMap
        ForeignVertex fv = (ForeignVertex)theEdge.V1();
        SoarWorkingMemoryModel fSWMM = cache.get(fv.getForeignDMName());
        if (fSWMM != null) return fSWMM;

        //SWMM needs to be loaded.  Since the path is relative, we need to append it to the location of our .vsa file
        String projRoot = ((OperatorRootNode) MainFrame.getMainFrame().getOperatorWindow().getModel().getRoot()).getFullPathStart();
        String foreignDMFullPath = projRoot + File.separator + fv.getForeignDMName();

        //Does the foreign .dm file still exist?
        File foreignDMFile = new File(foreignDMFullPath);
        if (! foreignDMFile.exists()) {
            String msg = foreignDMFullPath + " does not exist.";
            vecIssues.add(new FeedbackEntryForeignDatamap(theEdge, FeedbackEntryForeignDatamap.ERR_DM_FILE_UNREADABLE, msg));
            return null;
        }

        //The .dm file exists, can it be read?
        fSWMM = SoarWorkingMemoryReader.loadSWMM(foreignDMFile);
        if (fSWMM == null) {
            String msg = foreignDMFullPath + " can not be read.";
            vecIssues.add(new FeedbackEntryForeignDatamap(theEdge, FeedbackEntryForeignDatamap.ERR_DM_FILE_UNREADABLE, msg));
            return null;
        }

        //Hooray!  Cache our successful read for future use
        cache.put(fv.getForeignDMName(), fSWMM);

        return fSWMM;

    }//findOrLoadForeignDatamap

    /** overloaded version of the above that needs fewer params */
    private SoarWorkingMemoryModel findOrLoadForeignDatamap(NamedEdge theEdge) {
        HashMap<String, SoarWorkingMemoryModel> cache = new HashMap<>();
        Vector<FeedbackListEntry> vecIssues = new Vector<>();
        return findOrLoadForeignDatamap(theEdge, cache, vecIssues);
    }

    /**
     * addForeignSubTree       <!-- RECURSIVE -->
     * <p>
     * creates ForeignVertex objects for a given vertex in a foreign datamap and all its descendants.  These
     * are added to the given result vector _unless_ their id is in the seenSoFar list (to prevent loops).
     *
     * @param localSWMM        the SWMM for this project's datamap
     * @param foreignSWMM     the SWMM from the other (external) project's datamap
     * @param foreignSV       a SoarVertex foreign datamap.  The emanating edge of this vertex will be added to the local
     *                         datamap as {@link ForeignVertex} objects and its children will be added recursively.
     * @param localSV          the local ForesignVertex object that is linked to the given foreignSV
     * @param seenSoFar        a mapping of the foreign IDs of all {@link SoarIdentifierVertex} objects seen so far from
     *                         the foreign datamap to the ForeignVertex objects added to this project's datamap.  This is
     *                         used to handle links in the foreign database and to avoid infinite recursion (e.g., base case).
     * @param stringRep        is a String representation of foreignSV as a path from the root.
     *                                Example:  "<s> ^io.input-link.block.on-top"
     *                         When making the initial call to this method use a string of the form:  "<s> ^foo"
     * @param addedEntries     as the recursion proceeds, it keeps a list of all new {@link ForeignVertex}-based
     *                         entries that have been added to the datamap, so they can be reported to the
     *                         user at the end.
     * @author Andrew Nuxoll
     * created:  Feb 2024
     */
    protected void addForeignSubTree(SoarWorkingMemoryModel localSWMM,
                                     SoarWorkingMemoryModel foreignSWMM,
                                     SoarVertex foreignSV,
                                     ForeignVertex localSV,
                                     HashMap<Integer, ForeignVertex> seenSoFar,
                                     String stringRep,
                                     Vector<FeedbackListEntry> addedEntries) {

        //base case: foreign vertex is a leaf node
        if (! foreignSV.allowsEmanatingEdges()) {
            return;
        }

        Enumeration<NamedEdge> iter = foreignSWMM.emanatingEdges(foreignSV);
        while(iter.hasMoreElements()) {
            //Extract the child SoarVertex from the foreign DM
            NamedEdge neChild = iter.nextElement();
            SoarVertex foreignChildSV = neChild.V1();

            //To avoid insanity, skip any foreign vertexes in the foreign datamap.
            if (foreignChildSV instanceof ForeignVertex) continue;

            //If this child has been seen before (due to copy/link in datamap) retrieve its associated ForeignVertex
            ForeignVertex childFV = null;
            int childId = foreignChildSV.getValue();
            if (foreignChildSV instanceof SoarIdentifierVertex) {
                childFV = seenSoFar.get(childId);  //this will most often return null
            }

            //If we've not seen this SoarIdVertex before, create a new ForeignVertex in the local SWMM
            boolean newForeignVertex = (childFV == null);  //this will usually be true
            if (newForeignVertex) {
                int newId = localSWMM.getNextVertexId();
                childFV = new ForeignVertex(newId, localSV.getForeignDMName(), foreignChildSV);
                localSWMM.addVertex(childFV);
            }

            //Add the new entry in the local datamap
            NamedEdge newLocalNE = localSWMM.addTriple(localSV, neChild.getName(), childFV);

            //Record the addition to report to the user later
            String childRep = stringRep + "." + neChild;
            addedEntries.add(new FeedbackEntryForeignDatamap(newLocalNE, FeedbackEntryForeignDatamap.ADDED_FOREIGN_VERTEX, childRep));

            //If this is a new ForeignVertex, then record the associated foreign SIV and recurse to add its children
            if (newForeignVertex) {
                seenSoFar.put(childId, childFV);

                if (foreignChildSV.allowsEmanatingEdges()) {
                    addForeignSubTree(localSWMM, foreignSWMM, neChild.V1(), childFV, seenSoFar, childRep, addedEntries);
                }
            }

        }//for

    }//addForeignSubTree

    /**
     * compareForeignSubTree       <!-- RECURSIVE -->
     * <p>
     * compares a foreign subtree in the local datamap to the source subtree in the foreign datamap and compiles a
     * list of differences between them.
     *
     * @param localSWMM        the SWMM for this project's datamap
     * @param foreignSWMM      the SWMM from the other (external) project's datamap
     * @param foreignSV        a SoarVertex in the foreign datamap.
     * @param localNE          the local NamedEdge whose .V1() is linked to the given foreignSV
     *                         CAVEAT:  call must guarantee that localNE.V1() is a ForeignVertex
     * @param stringRep        is a String representation of foreignSV as a path from the root.
     *                                Example:  "<s> ^io.input-link.block.on-top"
     *                         When making the initial call to this method use a string of the form:  "<s> ^foo"
     * @param seenSoFar        a mapping of the foreign IDs of all {@link SoarIdentifierVertex} objects seen so far from
     *                         the foreign datamap to the ForeignVertex objects added to this project's datamap.  This is
     *                         used to handle links in the foreign database and to avoid infinite recursion (e.g., base case).
     *                         You should pass in an empty set the first time you call this method.
     * @param issuesList        as the recursion proceeds, it keeps a list of all differences, so they can be reported
     *                         to the user at the end.
     *
     * CAVEAT:  This method assumes the foreign subtree has no loops as it does not check for infinite recursion.
     *          If the subtree was imported via {@link #addForeignSubTree} then there will be no loops.
     *
     * @author Andrew Nuxoll
     * created:  Mar 2024
     */
    protected void compareForeignSubTree(SoarWorkingMemoryModel localSWMM,
                                         SoarWorkingMemoryModel foreignSWMM,
                                         SoarVertex foreignSV,
                                         NamedEdge localNE,
                                         String stringRep,
                                         HashMap<Integer, ForeignVertex> seenSoFar,
                                         Vector<FeedbackListEntry> issuesList) {


        //base case #1:  this foreign vertex has already been seen
        if (seenSoFar.get(foreignSV.getValue()) != null) return;
        ForeignVertex localSV = (ForeignVertex) localNE.V1();
        seenSoFar.put(foreignSV.getValue(), localSV);  //record for next time

        //base case #2:  mismatch  (no point in recursing further)
        FeedbackEntryForeignDatamap mismatch =
                FeedbackEntryForeignDatamap.compareForeignVertex(localNE, foreignSV, stringRep);
        if (mismatch != null) {
            issuesList.add(mismatch);
            return;
        }

        //base case #3: local vertex is a leaf node; no need to recurse further
        if (! localSV.allowsEmanatingEdges()) {
            return;
        }

        //We'll need a list of edges emanating from the foreign vertex, so we can search it for matches
        ArrayList<NamedEdge> foreignNEs = Collections.list(foreignSWMM.emanatingEdges(foreignSV));

        //Iterate over all the named edges in the local SWMM to compare
        Enumeration<NamedEdge> iter = localSWMM.emanatingEdges(localSV);
        while(iter.hasMoreElements()) {
            //Extract the child SoarVertex from the foreign DM
            NamedEdge neLocal = iter.nextElement();

            //If it's not a ForeignVertex then skip it as it's not relevant to this comparison
            SoarVertex sv = neLocal.V1();
            if (! (sv instanceof ForeignVertex)) continue;
            ForeignVertex localChildSV = (ForeignVertex)sv;
            SoarVertex efcSV = localChildSV.getForeignSoarVertex();  //expected foreign child SoarVertex

            //Find the corresponding foreign NamedEdge
            NamedEdge neForeign = null;
            int loc = -1;
            for(int i = 0; i < foreignNEs.size(); ++i) {
                NamedEdge neTmp = foreignNEs.get(i);
                //check: name matches
                if (neTmp.getName().equals(neLocal.getName())) {
                    //check: child SV matches
                    if (efcSV.getValue() == neTmp.V1().getValue()) {
                        loc = i;
                        neForeign = neTmp;
                        break;
                    }
                }
            }

            if (neForeign == null) {
                //Report not found
                String childRep = stringRep + "." + neLocal;
                String msg = "    " + childRep + " refers to an entry in " + localSV.getForeignDMName() + " that no longer exists.";
                issuesList.add(new FeedbackEntryForeignDatamap(localNE, FeedbackEntryForeignDatamap.ERR_FOREIGN_VERTEX_MISSING, msg));
                continue;
            }


            //Remove from list, so we know at the end if there are any vertices
            // in the foreign datamap that aren't in the local datamap
            foreignNEs.remove(loc);

            //Recursive call to compare these children
            String childRep = stringRep + "." + neForeign.getName();
            compareForeignSubTree(localSWMM, foreignSWMM, neForeign.V1(), neLocal, childRep, seenSoFar, issuesList);

        }//for

        //After the checks are completed, see if there are any foreign SVs that are unaccounted for
        for(NamedEdge ne : foreignNEs) {
            String childRep = stringRep + "." + ne;
            String msg = "    " + childRep + " is in " + localSV.getForeignDMName() + " but it is missing in this project's datamap.";
            issuesList.add(new FeedbackListEntry(msg));
        }

    }//compareForeignSubTree


    /**
     * getForeignSoarVertex
     *
     * attempts to retrieve the SoarVertex in a foreign datamap thar
     * corresponds to a ForeignVertex object in the local datamap
     *
     * @return null on failure
     */
    protected SoarVertex getForeignSoarVertex(SoarWorkingMemoryModel fSWMM, ForeignVertex fv) {
        SoarVertex foreignSV = fSWMM.getVertexForId(fv.getForeignSoarVertex().getValue());
        boolean found = false;
        if (foreignSV != null) {
            //Verify that this foreign vertex still has the right name and is at level 1
            SoarVertex root = fSWMM.getTopstate();
            Enumeration<NamedEdge> foreignLevel1Edges = fSWMM.emanatingEdges(root);
            while (foreignLevel1Edges.hasMoreElements()) {
                NamedEdge foreignNE = foreignLevel1Edges.nextElement();
                SoarVertex cand = foreignNE.V1();
                if (cand.getValue() == foreignSV.getValue()) {
                    found = true;
                }
            }
        }
        if (!found) {
            return null;
        }
        return foreignSV;
    }//getForeignSoarVertex

    /**
     * This method re-imports a subtree of ForeignVertex objects from the associated database
     * by simply deleting it and reloading it.
     */
    public void reimportSubtree() {
        //Get the FTN and NamedEdge that correspond to the user's selection
        TreePath path = getSelectionPath();
        if (path == null) return;
        FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
        if (ftn == null) return;
        NamedEdge ne = ftn.getEdge();

        //Sanity check
        if ( (ne == null) || (! (ne.V1() instanceof ForeignVertex)) ){
            //This should never happen
            JOptionPane.showMessageDialog(this, "Can not re-import non-foreign vertex", "Cannot re-import", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Load the foreign datamap that it refers to
        ForeignVertex fv = (ForeignVertex)ne.V1();
        SoarWorkingMemoryModel fSWMM = findOrLoadForeignDatamap(ne);
        if (fSWMM == null) {
            JOptionPane.showMessageDialog(this, "Unable to load foreign datamap.  Can not find: " + fv.getForeignDMName() + ".  Has it been moved or deleted?", "Cannot Re-Import", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Retrieve the SoarVertex that fv is referring to
        SoarVertex foreignSV = getForeignSoarVertex(fSWMM, fv);
        if (foreignSV == null) {
            JOptionPane.showMessageDialog(this, "Unable to find the corresponding entry in" + fv.getForeignDMName() +".  Has it been deleted?" , "Cannot Re-Import", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Delete the old subtree
        this.swmm.removeTriple(ne.V0(), ne.getName(), ne.V1());
        this.parentWindow.setModified(true);
        OperatorWindow operatorWindow = MainFrame.getMainFrame().getOperatorWindow();
        operatorWindow.getProjectModel().reduceWorkingMemory();

        //Re-add the subtree's root as a new vertex
        fv = new ForeignVertex(this.swmm.getNextVertexId(), fv.getForeignDMName(), foreignSV);
        this.swmm.addVertex(fv);
        this.swmm.addTriple(ne.V0(), ne.getName(), fv);

        //re-load the subtree
        HashMap<Integer, ForeignVertex> seenSoFar = new HashMap<>();
        seenSoFar.put(foreignSV.getValue(), fv);
        String rep = "    <s> ^" + ne.getName(); //added spaces help formatting later
        Vector<FeedbackListEntry> addedEntries = new Vector<>();
        addedEntries.add(new FeedbackEntryForeignDatamap(ne, FeedbackEntryForeignDatamap.ADDED_FOREIGN_VERTEX, rep));
        addForeignSubTree(this.swmm,  fSWMM, foreignSV, fv, seenSoFar, rep, addedEntries);

        //Report the results to the user
        addedEntries.add(0, new FeedbackListEntry("The following " + addedEntries.size() + " entries were imported from " + fv.getForeignDMName() + ":"));
        MainFrame.getMainFrame().getFeedbackManager().showFeedback(addedEntries);

    }//reimportSubtree



    /**
     * This method verifies that a ForeignVertex object and all its descendents still match the
     * corresponding entries in the foreign datamap
     *
     * Note:  a lot of this code is copied from {@link #reimportSubtree}
     *        but I don't see any easy way to refactor it into a shared helper.
     */
    public void verifySubtree() {
        //Get the FTN and NamedEdge that correspond to the user's selection
        TreePath path = getSelectionPath();
        if (path == null) return;
        FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
        if (ftn == null) return;
        NamedEdge ne = ftn.getEdge();

        //Sanity check
        if ( (ne == null) || (! (ne.V1() instanceof ForeignVertex)) ){
            //This should never happen
            JOptionPane.showMessageDialog(this, "Can not verify a non-foreign vertex", "Cannot verify", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Load the foreign datamap that it refers to
        ForeignVertex fv = (ForeignVertex)ne.V1();
        SoarWorkingMemoryModel fSWMM = findOrLoadForeignDatamap(ne);
        if (fSWMM == null) {
            JOptionPane.showMessageDialog(this, "Unable to load foreign datamap.  Can not find: " + fv.getForeignDMName() + ".  Has it been moved or deleted?", "Cannot Re-Import", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Retrieve the SoarVertex that fv is referring to
        SoarVertex foreignSV = getForeignSoarVertex(fSWMM, fv);
        if (foreignSV == null) {
            JOptionPane.showMessageDialog(this, "Unable to find the corresponding entry in" + fv.getForeignDMName() +".  Has it been deleted?" , "Cannot Re-Import", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //verify the subtree via each child
        Vector<FeedbackListEntry> diffsList = new Vector<>();
        HashMap<Integer, ForeignVertex> seenSoFar = new HashMap<>();
        compareForeignSubTree(this.swmm,  fSWMM, foreignSV, ne, ne.getName(), seenSoFar, diffsList);

        //Report the results to the user
        if (! diffsList.isEmpty()) {
            diffsList.add(0, new FeedbackListEntry("The following " + diffsList.size() + " mismatches were found between the imported datamap entries and their original imported from " + fv.getForeignDMName() + ":"));
        }
        else {
            diffsList.add(new FeedbackListEntry("No mismatches found."));
        }
        MainFrame.getMainFrame().getFeedbackManager().showFeedback(diffsList);
    }//verifySubtree



    /**
     * Validates the entire datamap
     */
    public void validateDataMap() {
        if (! verifyRoot()) return;
        VSQueue<SoarVertex> queue = new QueueAsLinkedList<>();
        int numberOfVertices = swmm.getNumberOfVertices();
        boolean[] visitedVertices = new boolean[numberOfVertices];

        //Iterate over every vertex that can be reached from the root
        FakeTreeNode root = (FakeTreeNode) getModel().getRoot();
        queue.enqueue(root.getEnumeratingVertex());
        while (!queue.isEmpty()) {
            SoarVertex w = queue.dequeue();

            visitedVertices[w.getValue()] = true;
            if (w.allowsEmanatingEdges()) {
                Enumeration<NamedEdge> edges = swmm.emanatingEdges(w);
                while (edges.hasMoreElements()) {
                    NamedEdge theEdge = edges.nextElement();
                    theEdge.validate();
                    swmm.notifyListenersOfRemove(theEdge);
                    swmm.notifyListenersOfAdd(theEdge);
                    if (!visitedVertices[theEdge.V1().getValue()]) {
                        visitedVertices[w.getValue()] = true;
                        queue.enqueue(theEdge.V1());
                    }   // if haven't visited this vertex, add to the queue
                } // while looking at all of the edges of the vertex
            } // if this vertex has children
        }   // while queue is not empty, examine each vertex in it

        JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                "Validation of DataMap completed", "DataMap Generator", JOptionPane.INFORMATION_MESSAGE);
    }     // end of validateDataMap()


    /**
     * Deletes all invalid datamap entries from the datamap
     */
    public void removeInvalid() {
        VSQueue<SoarVertex> queue = new QueueAsLinkedList<>();
        int numberOfVertices = swmm.getNumberOfVertices();
        boolean[] visitedVertices = new boolean[numberOfVertices];

        if (this.getModel().getRoot() instanceof FakeTreeNode) {
            FakeTreeNode root = (FakeTreeNode) getModel().getRoot();
            queue.enqueue(root.getEnumeratingVertex());

            while (!queue.isEmpty()) {
                SoarVertex w = queue.dequeue();
                visitedVertices[w.getValue()] = true;
                if (w.allowsEmanatingEdges()) {
                    Enumeration<NamedEdge> edges = swmm.emanatingEdges(w);
                    while (edges.hasMoreElements()) {
                        NamedEdge ne = edges.nextElement();
                        if (ne.isGenerated()) {
                            swmm.removeTriple(ne.V0(), ne.getName(), ne.V1());
                            edges = swmm.emanatingEdges(w);
                        } else {
                            // Valid node, keep it and look at children
                            if (!visitedVertices[ne.V1().getValue()]) {
                                visitedVertices[w.getValue()] = true;
                                queue.enqueue(ne.V1());
                            }  // if haven't visited this vertex, add to the queue
                        }
                    } // while looking at all of the edges of the vertex
                }
            }   // while queue is not empty, examine each vertex in it
        }
        JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                "Removal of all invalid datamap entries completed",
                "DataMap Generator",
                JOptionPane.INFORMATION_MESSAGE);
    }     // end of removeInvalid()


    //TODO:  All the searchXXX methods below are very similar.  Could they be combined?


    /**
     * Searches the entire dataMap looking for any edges that were not tested
     * by a production and that are not in the output link.
     * Returns feedback list information
     */
    public Vector<FeedbackListEntry> searchTestDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        Vector<FeedbackListEntry> errors = new Vector<>();
        VSQueue<SoarVertex> queue = new QueueAsLinkedList<>();
        int numberOfVertices = swmm.getNumberOfVertices();
        boolean[] visitedVertices = new boolean[numberOfVertices];

        if (this.getModel().getRoot() instanceof FakeTreeNode) {
            FakeTreeNode root = (FakeTreeNode) getModel().getRoot();
            queue.enqueue(root.getEnumeratingVertex());
            while (!queue.isEmpty()) {
                SoarVertex w = queue.dequeue();

                visitedVertices[w.getValue()] = true;
                if (w.allowsEmanatingEdges()) {
                    Enumeration<NamedEdge> edges = swmm.emanatingEdges(w);
                    while (edges.hasMoreElements()) {
                        NamedEdge theEdge = edges.nextElement();
                        String edgeName = theEdge.getName();

                        // if the edge isn't tested and not the output-link, add to error list
                        if ((!theEdge.isTested()) && !edgeName.equals("output-link") && !theEdge.getErrorNoted()
                                && !edgeName.equals("top-state") && !edgeName.equals("operator") && !edgeName.equals("input-link") && !edgeName.equals("item")
                                && !edgeName.equals("impasse") && !edgeName.equals("superstate") && !edgeName.equals("io") && !edgeName.equals("attribute")
                                && !edgeName.equals("choices") && !edgeName.equals("type") && !edgeName.equals("quiescence")) {
                            errors.add(new FeedbackEntryDatamap(theEdge, in_siv, dataMapName, ", was never tested in the productions of this agent."));
                            theEdge.setErrorNoted();
                        }

                        // Do not check edges on the output-link
                        if ((!visitedVertices[theEdge.V1().getValue()]) && (!theEdge.getName().equals("output-link"))) {
                            visitedVertices[w.getValue()] = true;
                            queue.enqueue(theEdge.V1());
                        }   // if haven't visited this vertex, add to the queue
                    } // while looking at all of the edges of the vertex
                }
            }   // while queue is not empty, examine each vertex in it
        }
        return errors;
    }     // end of searchTestDataMap()

    /**
     * Searches the entire dataMap looking for any edges that were not created
     * by a production and that are not in the input link.
     * Returns feedback list information
     */
    public Vector<FeedbackListEntry> searchCreateDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        Vector<FeedbackListEntry> errors = new Vector<>();
        VSQueue<SoarVertex> queue = new QueueAsLinkedList<>();
        int numberOfVertices = swmm.getNumberOfVertices();
        boolean[] visitedVertices = new boolean[numberOfVertices];

        if (this.getModel().getRoot() instanceof FakeTreeNode) {
            FakeTreeNode root = (FakeTreeNode) getModel().getRoot();
            queue.enqueue(root.getEnumeratingVertex());
            while (!queue.isEmpty()) {
                SoarVertex w = queue.dequeue();

                visitedVertices[w.getValue()] = true;
                if (w.allowsEmanatingEdges()) {
                    Enumeration<NamedEdge> edges = swmm.emanatingEdges(w);
                    while (edges.hasMoreElements()) {
                        NamedEdge theEdge = edges.nextElement();
                        String edgeName = theEdge.getName();

                        // if the edge isn't created and not the input-link, add to error list
                        if ((!theEdge.isCreated()) && !edgeName.equals("input-link") && !theEdge.getErrorNoted()
                                && !edgeName.equals("top-state") && !edgeName.equals("operator") && !edgeName.equals("output-link") && !edgeName.equals("item")
                                && !edgeName.equals("impasse") && !edgeName.equals("superstate") && !edgeName.equals("io") && !edgeName.equals("attribute")
                                && !edgeName.equals("choices") && !edgeName.equals("type") && !edgeName.equals("quiescence")) {
                            errors.add(new FeedbackEntryDatamap(theEdge, in_siv, dataMapName, ", was never created by the productions of this agent."));
                            theEdge.setErrorNoted();
                        }

                        // Do not check edges on the input-link
                        if ((!visitedVertices[theEdge.V1().getValue()]) && (!theEdge.getName().equals("input-link"))) {
                            visitedVertices[w.getValue()] = true;
                            queue.enqueue(theEdge.V1());
                        }   // if haven't visited this vertex, add to the queue
                    } // while looking at all of the edges of the vertex
                }
            }   // while queue is not empty, examine each vertex in it
        }
        return errors;
    }     // end of searchCreateDataMap()

    /**
     * Searches the entire dataMap looking for any edges that were tested but not created
     * by a production and that are not in the input link.
     * Returns feedback list information
     */
    public Vector<FeedbackListEntry> searchTestNoCreateDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        Vector<FeedbackListEntry> errors = new Vector<>();
        VSQueue<SoarVertex> queue = new QueueAsLinkedList<>();
        int numberOfVertices = swmm.getNumberOfVertices();
        boolean[] visitedVertices = new boolean[numberOfVertices];

        if (this.getModel().getRoot() instanceof FakeTreeNode) {
            FakeTreeNode root = (FakeTreeNode) getModel().getRoot();
            queue.enqueue(root.getEnumeratingVertex());
            while (!queue.isEmpty()) {
                SoarVertex w = queue.dequeue();

                visitedVertices[w.getValue()] = true;
                if (w.allowsEmanatingEdges()) {
                    Enumeration<NamedEdge> edges = swmm.emanatingEdges(w);
                    while (edges.hasMoreElements()) {
                        NamedEdge theEdge = edges.nextElement();
                        String edgeName = theEdge.getName();

                        // if the edge isn't created and not the input-link, add to error list
                        if (theEdge.isTestedNoCreate() && !edgeName.equals("input-link") && !theEdge.getErrorNoted()
                                && !edgeName.equals("top-state") && !edgeName.equals("operator") && !edgeName.equals("output-link") && !edgeName.equals("item")
                                && !edgeName.equals("impasse") && !edgeName.equals("superstate") && !edgeName.equals("io") && !edgeName.equals("attribute")
                                && !edgeName.equals("choices") && !edgeName.equals("type") && !edgeName.equals("quiescence")) {
                            errors.add(new FeedbackEntryDatamap(theEdge, in_siv, dataMapName, ", was tested but never created by the productions of this agent."));
                            theEdge.setErrorNoted();
                        }

                        // Do not check edges on the input-link or output-link
                        if ((!visitedVertices[theEdge.V1().getValue()]) && (!theEdge.getName().equals("input-link")) && (!theEdge.getName().equals("output-link"))) {
                            visitedVertices[w.getValue()] = true;
                            queue.enqueue(theEdge.V1());
                        }   // if haven't visited this vertex, add to the queue
                    } // while looking at all of the edges of the vertex
                }
            }   // while queue is not empty, examine each vertex in it
        }
        return errors;
    }     // end of searchTestNoCreateDataMap()


    /**
     * Searches the entire dataMap looking for any edges that were created but not tested
     * by a production and that are not in the input link.
     * Returns feedback list information
     */
    public Vector<FeedbackListEntry> searchCreateNoTestDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        Vector<FeedbackListEntry> errors = new Vector<>();
        VSQueue<SoarVertex> queue = new QueueAsLinkedList<>();
        int numberOfVertices = swmm.getNumberOfVertices();
        boolean[] visitedVertices = new boolean[numberOfVertices];

        if (this.getModel().getRoot() instanceof FakeTreeNode) {
            FakeTreeNode root = (FakeTreeNode) getModel().getRoot();
            queue.enqueue(root.getEnumeratingVertex());
            while (!queue.isEmpty()) {
                SoarVertex w = queue.dequeue();

                visitedVertices[w.getValue()] = true;
                if (w.allowsEmanatingEdges()) {
                    Enumeration<NamedEdge> edges = swmm.emanatingEdges(w);
                    while (edges.hasMoreElements()) {
                        NamedEdge theEdge = edges.nextElement();
                        String edgeName = theEdge.getName();

                        // if the edge is created, not tested, add to error list
                        if (theEdge.isCreatedNoTest() && !edgeName.equals("input-link") && !theEdge.getErrorNoted()
                                && !edgeName.equals("top-state") && !edgeName.equals("operator") && !edgeName.equals("output-link") && !edgeName.equals("item")
                                && !edgeName.equals("impasse") && !edgeName.equals("superstate") && !edgeName.equals("io") && !edgeName.equals("attribute")
                                && !edgeName.equals("choices") && !edgeName.equals("type") && !edgeName.equals("quiescence")) {
                            errors.add(new FeedbackEntryDatamap(theEdge, in_siv, dataMapName, ", was tested but never created by the productions of this agent."));
                            theEdge.setErrorNoted();
                        }

                        // Do not check edges on the output-link
                        if ((!visitedVertices[theEdge.V1().getValue()]) && (!theEdge.getName().equals("output-link")) && (!theEdge.getName().equals("input-link"))) {
                            visitedVertices[w.getValue()] = true;
                            queue.enqueue(theEdge.V1());
                        }   // if haven't visited this vertex, add to the queue
                    } // while looking at all of the edges of the vertex
                }
            }   // while queue is not empty, examine each vertex in it
        }
        return errors;
    }     // end of searchCreateNoTestDataMap()

    /**
     * Searches the entire dataMap looking for any edges that were not tested AND not created
     * by a production and that are not in the input link.
     * Returns feedback list information
     */
    public Vector<FeedbackListEntry> searchNoTestNoCreateDataMap(SoarIdentifierVertex in_siv, String dataMapName) {
        Vector<FeedbackListEntry> errors = new Vector<>();
        VSQueue<SoarVertex> queue = new QueueAsLinkedList<>();
        int numberOfVertices = swmm.getNumberOfVertices();
        boolean[] visitedVertices = new boolean[numberOfVertices];

        if (this.getModel().getRoot() instanceof FakeTreeNode) {
            FakeTreeNode root = (FakeTreeNode) getModel().getRoot();
            queue.enqueue(root.getEnumeratingVertex());
            while (!queue.isEmpty()) {
                SoarVertex w = queue.dequeue();

                visitedVertices[w.getValue()] = true;
                if (w.allowsEmanatingEdges()) {
                    Enumeration<NamedEdge> edges = swmm.emanatingEdges(w);
                    while (edges.hasMoreElements()) {
                        NamedEdge theEdge = edges.nextElement();
                        String edgeName = theEdge.getName();

                        // if the edge isn't created and not the input-link, add to error list
                        if (theEdge.notMentioned() && !edgeName.equals("input-link") && !theEdge.getErrorNoted()
                                && !edgeName.equals("top-state") && !edgeName.equals("operator") && !edgeName.equals("output-link") && !edgeName.equals("item")
                                && !edgeName.equals("impasse") && !edgeName.equals("superstate") && !edgeName.equals("io") && !edgeName.equals("attribute")
                                && !edgeName.equals("choices") && !edgeName.equals("type") && !edgeName.equals("quiescence")) {
                            errors.add(new FeedbackEntryDatamap(theEdge, in_siv, dataMapName, ", was tested but never created by the productions of this agent."));
                            theEdge.setErrorNoted();
                        }

                        // Do not check edges on the input-link or output-link
                        if ((!visitedVertices[theEdge.V1().getValue()]) && (!theEdge.getName().equals("input-link")) && (!theEdge.getName().equals("output-link"))) {
                            visitedVertices[w.getValue()] = true;
                            queue.enqueue(theEdge.V1());
                        }   // if haven't visited this vertex, add to the queue
                    } // while looking at all of the edges of the vertex
                }
            }   // while queue is not empty, examine each vertex in it
        }
        return errors;
    }     // end of searchNoTestNoCreateDataMap()


    /**
     * Selects (highlights and centers) the requested edge within the datamap.
     *
     * @param desiredEdge the requested NamedEdge to select
     * @return the FakeTreeNode that contains the desired edge,  null if could not find
     */
    public FakeTreeNode selectEdge(NamedEdge desiredEdge) {
        VSQueue<FakeTreeNode> queue = new QueueAsLinkedList<>();
        FakeTreeNode foundftn = null;
        int numberOfVertices = swmm.getNumberOfVertices();
        boolean[] visitedVertices = new boolean[numberOfVertices];
        boolean edgeNotFound = true;
        int children;

        if (this.getModel().getRoot() instanceof FakeTreeNode) {
            FakeTreeNode root = (FakeTreeNode) getModel().getRoot();
            queue.enqueue(root);

            while ((!queue.isEmpty()) && edgeNotFound) {
                FakeTreeNode ftn = queue.dequeue();
                int rootValue = ftn.getEnumeratingVertex().getValue();
                visitedVertices[rootValue] = true;
                children = ftn.getChildCount();

                // See if current FakeTreeNode's edge match desired edge
                if (ftn.getEdge() != null) {
                    if ((ftn.getEdge()).equals(desiredEdge)) {
                        edgeNotFound = false;
                        foundftn = ftn;
                    }
                }

                // Examine children of ftn
                if ((children != 0) && edgeNotFound) {
                    for (int i = 0; i < children; i++) {
                        FakeTreeNode childftn = ftn.getChildAt(i);
                        int vertexValue = childftn.getEnumeratingVertex().getValue();
                        if (!visitedVertices[vertexValue]) {
                            visitedVertices[vertexValue] = true;
                            queue.enqueue(childftn);
                        }   // if never visited vertex
                        else {
                            // Check this edge since it won't be added to the queue
                            if (childftn.getEdge() != null) {
                                if ((childftn.getEdge()).equals(desiredEdge)) {
                                    edgeNotFound = false;
                                    foundftn = childftn;
                                }
                            }
                        }    // end of else already visited this vertex
                    }   // for checking all of ftn's children
                }   // if ftn has children

            }   // while queue is not empty, examine each vertex in it
        } // if root is a valid ftn

        return foundftn;
    }//selectEdge

    /**
     * Opens all the paths leading to generated nodes that have not been validated
     */
    public void displayGeneratedNodes() {
        VSQueue<FakeTreeNode> queue = new QueueAsLinkedList<>();
        int numberOfVertices = swmm.getNumberOfVertices();
        boolean[] visitedVertices = new boolean[numberOfVertices];
        int children;

        if (this.getModel().getRoot() instanceof FakeTreeNode) {
            FakeTreeNode root = (FakeTreeNode) getModel().getRoot();
            queue.enqueue(root);

            while (!queue.isEmpty()) {
                FakeTreeNode ftn = queue.dequeue();
                int rootValue = ftn.getEnumeratingVertex().getValue();
                visitedVertices[rootValue] = true;
                children = ftn.getChildCount();

                // See if current FakeTreeNode's edge match desired edge
                NamedEdge ne = ftn.getEdge();
                if ((ne != null) && (ne.isGenerated())) {
                    TreePath path = new TreePath(ftn.getTreePath().toArray());
                    expandPath(path);
                }

                // Examine children of ftn
                for (int i = 0; i < children; i++) {
                    FakeTreeNode childftn = ftn.getChildAt(i);
                    int vertexValue = childftn.getEnumeratingVertex().getValue();
                    if (!visitedVertices[vertexValue]) {
                        visitedVertices[vertexValue] = true;
                        queue.enqueue(childftn);
                    }   // if never visited vertex
                    else {
                        // Check this edge since it won't be added to the queue
                        ne = childftn.getEdge();
                        if ((ne != null) && (ne.isGenerated())) {
                            TreePath path = new TreePath(childftn.getTreePath().toArray());
                            expandPath(path);
                        }
                    }    // end of else already visited this vertex
                }   // for checking all of ftn's children
            }   // while queue is not empty, examine each vertex in it
        } // if root is a valid ftn

    }//displayGeneratedNodes

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        //Only enable paste/link menu items if there is appropriate data in the clipboard
        if (! isReadOnly) {
            boolean canPaste = clipboardIsPasteable();
            LinkItem.setEnabled(canPaste);
            PasteItem.setEnabled(canPaste);
        }
    }
    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { /* ignore */  }
    @Override
    public void popupMenuCanceled(PopupMenuEvent e) { /* ignore */  }


    /**
     * class DMTDragGestureListener
     */
    class DMTDragGestureListener implements DragGestureListener {
        public void dragGestureRecognized(DragGestureEvent dge) {
            int action = dge.getDragAction();
            TreePath path = getLeadSelectionPath();
            if (path == null) {
                //getToolkit().beep();
                return;
            }
            if (dge.getTriggerEvent() instanceof MouseEvent) {
                if (((MouseEvent) dge.getTriggerEvent()).isPopupTrigger()) {
                    return;
                }
            }

            if (getSelectionCount() > 1) {
                MainFrame.getMainFrame().getFeedbackManager().setStatusBarError("Only one item may be dragged at a time");
                return;
            }

            FakeTreeNode ftn = (FakeTreeNode) path.getLastPathComponent();
            NamedEdge e = ftn.getEdge();
            Transferable t;
            if (e == null) {
                t = new TransferableVertex(ftn.getEnumeratingVertex(), ftn.toString());
            } else {
                t = new TransferableVertex(ftn.getEnumeratingVertex(), e.getName(), e);
            }
            if (action == DnDConstants.ACTION_LINK) {
                DragSource.getDefaultDragSource().startDrag(dge, DragSource.DefaultLinkNoDrop, t, new DMTDragSourceListener());
            } else if (action == DnDConstants.ACTION_COPY) {
                DragSource.getDefaultDragSource().startDrag(dge, DragSource.DefaultCopyNoDrop, t, new DMTDragSourceListener());
            } else if (action == DnDConstants.ACTION_MOVE) {
                DragSource.getDefaultDragSource().startDrag(dge, DragSource.DefaultMoveNoDrop, t, new DMTDragSourceListener());
            }

            OriginalSelectionPath = path;
        }
    }//class DMTDragGestureListener

    /**
     * class DMTDropTargetListener
     */
    class DMTDropTargetListener implements DropTargetListener {
        public void dragEnter(DropTargetDragEvent dtde) {
        }

        public void dragExit(DropTargetEvent dte) {
            // reset cursor back to normal
            Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
            DataMapTree.getDataMapTree().setCursor(defaultCursor);
        }

        public void dragOver(DropTargetDragEvent dtde) {

            int action = dtde.getDropAction();
            Point loc = dtde.getLocation();
            int x = (int) loc.getX(), y = (int) loc.getY();
            TreePath path = getPathForLocation(x, y);
            if (path != null) {
                clearSelection();
                setSelectionPath(path);
                if (isDropOK(x, y, action)) {
                    if (action == DnDConstants.ACTION_LINK) {
                        Cursor cursor = DragSource.DefaultLinkDrop;
                        DataMapTree.getDataMapTree().setCursor(cursor);
                        dtde.acceptDrag(DnDConstants.ACTION_LINK);
                    } else if (action == DnDConstants.ACTION_COPY) {
                        Cursor cursor = DragSource.DefaultCopyDrop;
                        DataMapTree.getDataMapTree().setCursor(cursor);
                        dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    } else {
                        Cursor cursor = DragSource.DefaultMoveDrop;
                        DataMapTree.getDataMapTree().setCursor(cursor);
                        dtde.acceptDrag(DnDConstants.ACTION_MOVE);
                    }
                }   // if drop ok
                else {
                    Cursor cursor;
                    if (action == DnDConstants.ACTION_LINK) {
                        cursor = DragSource.DefaultLinkNoDrop;
                    } else if (action == DnDConstants.ACTION_COPY) {
                        cursor = DragSource.DefaultCopyNoDrop;
                    } else {
                        cursor = DragSource.DefaultMoveNoDrop;
                    }
                    DataMapTree.getDataMapTree().setCursor(cursor);
                    dtde.rejectDrag();
                }
            }   // if path ok
            else {
                Cursor cursor = DragSource.DefaultCopyNoDrop;
                DataMapTree.getDataMapTree().setCursor(cursor);
                dtde.rejectDrag();
            }
        }

        public void dropActionChanged(DropTargetDragEvent dtde) {
        }

        /**
         * drop
         * <p>
         * Gets called when the user releases the mouse in a drag-and-drop
         * operation in a datamap window.
         */
        public void drop(DropTargetDropEvent e) {
            //Verify that drop is acceptable
            Point loc = e.getLocation();
            int x = (int) loc.getX(), y = (int) loc.getY();

            //action will be one of:  copy, move or link
            int action = e.getDropAction();
            if (isDropOK(x, y, action)) {
                e.acceptDrop(action);
            } else {
                e.rejectDrop();
                return;
            }

            // reset cursor back to normal
            Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
            DataMapTree.getDataMapTree().setCursor(defaultCursor);


            if (action == DnDConstants.ACTION_COPY) {
                System.out.println("dndcopy!");
                TreePath currentPath = getPathForLocation(x, y);
                setSelectionPath(OriginalSelectionPath);
                CopyVertexVector copyVerticies = getCopyVertices();
                setSelectionPath(currentPath);
                pasteCopyVertices(copyVerticies);
            } else {  //action is either MOVE or LINK

                //Extract the WME data from the event
                DataFlavor[] flavors = e.getCurrentDataFlavors();
                DataFlavor chosen = flavors[0];
                Vector data;  //This must be a raw vector (see TransferrableVertex.getTransferData)
                try {
                    data = (Vector) e.getTransferable().getTransferData(chosen);
                } catch (Throwable t) {
                    t.printStackTrace();
                    e.dropComplete(false);
                    return;
                }

                //Get the target for the drop
                TreePath path = getPathForLocation(x, y);
                if (path == null) return;
                FakeTreeNode ftn = ((FakeTreeNode) path.getLastPathComponent());
                if (ftn == null) return;
                SoarVertex vertex = ftn.getEnumeratingVertex();

                //Get the thing we're dropping
                SoarVertex dataVertex =
                        swmm.getVertexForId((Integer) data.get(0));

                //If we are moving a node, we have to first make sure that we're
                //not creating a loop.
                if ((action & DnDConstants.ACTION_MOVE) != 0) {
                    for (int i = 0; i < path.getPathCount(); i++) {
                        SoarVertex v = ((FakeTreeNode) path.getPath()[i]).getEnumeratingVertex();
                        if (dataVertex.equals(v)) {
                            e.rejectDrop();
                            return;
                        }
                    }
                }

                //Perform the drop
                swmm.addTriple(vertex, (String) data.get(1), dataVertex);
                if (action == DnDConstants.ACTION_MOVE) {
                    NamedEdge ne = (NamedEdge) data.get(2);
                    swmm.removeTriple(ne.V0(), ne.getName(), ne.V1());
                }

            }
            e.dropComplete(true);

        }

        /**
         * helper method for {@link #dragOver} and {@link #drop} to
         * determine if a given coordinate is a valid drop destination
         */
        boolean isDropOK(int x, int y, int action) {
            TreePath path = getPathForLocation(x, y);
            if (path == null) {
                return false;
            }
            if (path.equals(OriginalSelectionPath)) {
                return false;
            }

            if (action == DnDConstants.ACTION_LINK || action == DnDConstants.ACTION_MOVE || action == DnDConstants.ACTION_COPY) {
                FakeTreeNode ftn = (FakeTreeNode) path.getLastPathComponent();
                return !ftn.isLeaf();
            }
            return false;

        }//isDropOK
    }//class DMTDropTargetListener


    static class DMTDragSourceListener implements DragSourceListener {

        public void dragEnter(DragSourceDragEvent e) {
            //DragSourceContext context = e.getDragSourceContext();

            //intersection of the users selected action, and the source and target actions

            int myaction = e.getDropAction();

            Cursor cursor;
            if ((myaction & DnDConstants.ACTION_COPY) != 0) {
                cursor = DragSource.DefaultCopyDrop;
            } else if ((myaction & DnDConstants.ACTION_LINK) != 0) {
                cursor = DragSource.DefaultLinkDrop;
            } else if ((myaction & DnDConstants.ACTION_MOVE) != 0) {
                cursor = DragSource.DefaultMoveDrop;
            } else {
                cursor = DragSource.DefaultMoveNoDrop;
            }
            DataMapTree.getDataMapTree().setCursor(cursor);
        }


        public void dragOver(DragSourceDragEvent e) {
            e.getDragSourceContext().setCursor(null);
        }


        public void dragExit(DragSourceEvent e) {

        }

        public void dragDropEnd(DragSourceDropEvent e) {

        }

        public void dropActionChanged(DragSourceDragEvent e) {

        }
    }

    class CopyAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public void actionPerformed(ActionEvent e) {
            copy();
        }
    }//class DMTDragSourceListener

    class PasteAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public void actionPerformed(ActionEvent e) {
            paste();
        }
    }

    class LinkAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public void actionPerformed(ActionEvent e) {
            link();
        }
    }

    class SearchAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public SearchAction() {
            super("Search DataMap");
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            TreePath thePath = getSelectionPath();
            FakeTreeNode fake = null;
            if (thePath != null) {
                fake = ((FakeTreeNode) thePath.getLastPathComponent());
            } else {
                if (DataMapTree.getDataMapTree().getModel().getRoot() instanceof FakeTreeNode) {
                    fake = (FakeTreeNode) DataMapTree.getDataMapTree().getModel().getRoot();
                }
            }
            SearchDataMapDialog searchDialog = new SearchDataMapDialog(MainFrame.getMainFrame(), DataMapTree.getDataMapTree(), fake);
            searchDialog.setVisible(true);
        }
    }//class SearchAction

    class ValidateDataMapAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public void actionPerformed(ActionEvent e) {
            validateDataMap();
        }
    }

    class RemoveInvalidAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public void actionPerformed(ActionEvent e) {
            removeInvalid();
        }
    }

}

