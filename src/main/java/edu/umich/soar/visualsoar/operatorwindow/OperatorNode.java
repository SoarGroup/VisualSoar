package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.ProjectModel;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.dialogs.FileAlreadyExistsDialog;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackEntryOpNode;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.misc.Prefs;
import edu.umich.soar.visualsoar.parser.ParseException;
import edu.umich.soar.visualsoar.parser.SoarProduction;
import edu.umich.soar.visualsoar.parser.TokenMgrError;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;
import edu.umich.soar.visualsoar.util.IdGenerator;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import static edu.umich.soar.visualsoar.components.FontUtils.setContainerFontSize;


/**
 * This is the basis class for which all operator nodes are
 * derived. Known subclasses:
 *
 * @author Brad Jones
 * @version 0.5a 5 Aug 1999
 * <p>
 * Class Hierarchy: <br/>
 *    OperatorNode                                 (this class) superclass for all nodes<br/>
 *    +-- {@link FolderNode}                       contains a folder<br/>
 *        +-- {@link OperatorRootNode}             root folder of the entire pane<br/>
 *    +-- {@link FileNode}                         superclass for any node with an associated file<br/>
 *        +-- {@link LinkNode}                     (no longer used)<br/>
 *        +-- {@link SoarOperatorNode}             superclass for code-containing nodes<br/>
 *            +-- {@link OperatorOperatorNode}     contains an operator<br/>
 *            +-- {@link ImpasseOperatorNode}      contains an impasse file<br/>
 *            +-- {@link FileOperatorNode}         contains elaborations<br/>
 */
public abstract class OperatorNode extends VSTreeNode implements java.io.Serializable {
    private static final long serialVersionUID = 20221225L;

    //Operator node types are identified with these strings in a .vsa file
    public static final String[] VSA_NODE_TYPES =
            { "OPERATOR", "HLOPERATOR", "HLFOPERATOR", "FOPERATOR", "HLIOPERATOR",
                    "IOPERATOR", "FOLDER", "FILE", "ROOT", "LINK" };

    ///////////////////////////////////////////////////////////////////
// Data Members
////////////////////////////////////////////////////////////////////
//    TODO: wrap menu items into a dedicated class
    static protected final JMenuItem addSubOperatorItem = new JMenuItem("Add a Sub-Operator...");
    static protected final JMenuItem addTopFolderItem = new JMenuItem("Add a Top-Level Folder...");
    static protected final JMenuItem addFileItem = new JMenuItem("Add a File...");

    static protected final JMenu impasseSubMenu = new JMenu("Add an Impasse...");
    static protected final JMenuItem tieImpasseItem = new JMenuItem("Operator Tie Impasse");
    static protected final JMenuItem conflictImpasseItem = new JMenuItem("Operator Conflict Impasse");
    static protected final JMenuItem constraintImpasseItem = new JMenuItem("Operator Constraint-Failure Impasse");
    static protected final JMenuItem stateNoChangeImpasseItem = new JMenuItem("State No-Change Impasse");

    static protected final JMenuItem openRulesItem = new JMenuItem("Open Rules");
    static protected final JMenuItem openDataMapItem = new JMenuItem("Open Datamap");
    static protected final JMenuItem searchItem = new JMenuItem("Find...");
    static protected final JMenuItem replaceItem = new JMenuItem("Replace...");
    static protected final JMenuItem deleteItem = new JMenuItem("Delete");
    static protected final JMenuItem renameItem = new JMenuItem("Rename...");
    static protected final JMenuItem exportItem = new JMenuItem("Export");
    static protected final JMenuItem importItem = new JMenuItem("Import...");
    static protected final JMenuItem checkChildrenAgainstDataMapItem = new JMenuItem("Check Children Against Datamap");
    static protected final JMenuItem generateDataMapItem = new JMenuItem("Generate Datamap Entries for this File");

    protected String name;
    protected final int id;
    // Used for uniquely identifying this node in a project file
    private final String serializationId;

///////////////////////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////////////////////

    /**
     * Constructor
     *
     * Derives serializationId from id.
     *
     * @param inName the name of the operator
     * @param inId   the unique id associated with this operator
     */
    public OperatorNode(String inName, int inId) {
        name = inName;
        id = inId;
        serializationId = IdGenerator.getId();
    }

  /**
   * Constructor
   *
   * Derives serializationId from id.
   *
   * @param inName the name of the operator
   * @param inId   the unique id associated with this operator
   * @param serializationId string unique ID for this operator (used when writing project files)
   */
  public OperatorNode(String inName, int inId, String serializationId) {
    name = inName;
    id = inId;
    this.serializationId = serializationId;
  }

///////////////////////////////////////////////////////////////////
// Methods
///////////////////////////////////////////////////////////////////

    /**
     * This is a getter method for the file name
     * if the node supports this operation it returns the
     * true path if it doesn't this returns null
     *
     * @return null
     */
    public String getFileName() {
        //System.err.println("This should never get called");
        return null;
    }

    /**
     * @return the unique id associated with this operator
     */
    public final int getId() {
        return id;
    }

  public String getSerializationId() {
    return serializationId;
  }

    /**
     * @return whether an openDataMap() call on this node will work
     */
    public boolean noDataMap() {
        return true;
    }

    /**
     * getLineNumFromErr
     * <p>
     * helper for the exception parsers (below) that extracts the
     * line number from an error string
     */
    private static int getLineNumFromErr(String errMsg) {
        int i = errMsg.lastIndexOf("line ");
        String lineNumStr = errMsg.substring(i + 5);
        i = lineNumStr.indexOf(',');
        return Integer.parseInt(lineNumStr.substring(0, i));
    }

    /**
     * Given a parse exception discovered in this node, this function converts
     * it into a FeedbackListEntry that can be placed in the feedback window.
     *
     * @param node the OperatorNode that contains the parse error
     * @param pe   the ParseException to parse
     * @return the generated FeedbackListEntry
     */
    public static FeedbackListEntry parseParseException(OperatorNode node,
                                                        ParseException pe) {
        String parseError = pe.toString();
        //Strip away unnecessary full qualifiers on class name
        parseError = parseError.replace("edu.umich.soar.visualsoar.", "");
        int lineNum = getLineNumFromErr(parseError);
        return new FeedbackEntryOpNode(node, lineNum, parseError, true);
    }

    /**
     * non-static version of the above that uses 'this' for the opNode
     */
    public FeedbackListEntry parseParseException(ParseException pe) {
        return parseParseException(this, pe);
    }

    /**
     * Given a lexical error discovered in this node, this function converts
     * it into a FeedbackListEntry that can be placed in the feedback window.
     *
     * @param tme the TokeMgrError to parse
     * @return the generated FeedbackListEntry
     */
    public FeedbackListEntry parseTokenMgrError(TokenMgrError tme) {
        String parseError = tme.toString();

        //Extract the line number
        int lineNum = getLineNumFromErr(parseError);
        String lineNumStr = "(" + lineNum + "): ";

        //Extract the offending characters
        int i = parseError.lastIndexOf("Encountered: \"");
        String tokenString = parseError.substring(i + 14);
        i = tokenString.indexOf('\"');
        tokenString = tokenString.substring(0, i);

        //Build the full error string
        String errString = getFileName() + lineNumStr + parseError;

        return new FeedbackEntryOpNode(this, lineNum, errString, tokenString);
    }


    /**
     * overloaded by subclasses
     */
    public Vector<SoarProduction> parseProductions() throws ParseException, java.io.IOException {
        return null;
    }

    public boolean checkAgainstDatamap(Vector<FeedbackListEntry> vecErrors, ProjectModel pm) throws IOException {
        return false;           // no datamap errors found
    }

    public Vector<String> getProdNames() {
        return new Vector<>();
    }

    public int getLineNumForString(String target) {
        return 0;
    }

    /**
     * This is a getter method for the folder name
     * if the node supports this operation it returns the
     * true path if it doesn't this returns null
     *
     * @return null
     */
    public String getFolderName() {
        //System.err.println("This should never get called");
        return null;
    }

    /**
     * Returns a unique name based of the path of the node in the operator
     * hierarchy.
     *
     * @return a String that is a unique name
     */
    public String getUniqueName() {
        StringBuilder uniqueName = new StringBuilder();
        javax.swing.tree.TreeNode[] path = getPath();

        for (int i = 1; i < path.length; i++) {
            uniqueName.append(path[i].toString());
            int atPos = uniqueName.indexOf("@");
            if (atPos != -1) {
                uniqueName = new StringBuilder(uniqueName.substring(0, atPos - 1));
            }
            if ((i + 1) < path.length) {
                uniqueName.append(File.separator);
            }
        }

        return uniqueName.toString();
    }

    /**
     * enableContextMenuItems
     *
     * is a helper method to enable or disable the relevant context menu items.
     * It's up to each subclass to override this method to adjust the settings
     * as needed.
     *
     * Here in the root class (OperatorNode) everything is turned on
     * by default.
     */
    protected void enableContextMenuItems() {
        //This gets turned invisible sometimes so have it visible by default here
        addTopFolderItem.setVisible(true);

        addSubOperatorItem.setEnabled(true);
        addFileItem.setEnabled(true);
        addTopFolderItem.setEnabled(true);
        impasseSubMenu.setEnabled(true);
        openRulesItem.setEnabled(true);
        openDataMapItem.setEnabled(true);
        searchItem.setEnabled(true);
        replaceItem.setEnabled(true);
        deleteItem.setEnabled(true);
        renameItem.setEnabled(true);
        exportItem.setEnabled(true);
        importItem.setEnabled(true);
        generateDataMapItem.setEnabled(true);
        checkChildrenAgainstDataMapItem.setEnabled(true);
    }//enableContextMenuItems

    /**
     * disables context menu items that shouldn't be available when project is read-only
     */
    private void disableContextMenuItemsForReadOnlyMode() {
        addSubOperatorItem.setEnabled(false);
        addFileItem.setEnabled(false);
        addTopFolderItem.setEnabled(false);
        impasseSubMenu.setEnabled(false);
        //openRulesItem is ok
        //openDataMapItem is ok
        //searchItem is ok
        replaceItem.setEnabled(false);
        deleteItem.setEnabled(false);
        renameItem.setEnabled(false);
        //exportItem is ok
        importItem.setEnabled(false);
        generateDataMapItem.setEnabled(false);
        //checkChildrenAgainstDataMapItem is ok
    }

    /**
     * displays the context menu
     *
     * @param c the owner of the context menu, should be the OperatorWindow
     * @param x the horizontal position on the screen where the context menu should
     *          be displayed
     * @param y the vertical position on the screen where the context menu should
     *          be displayed
     */
    public void showContextMenu(Component c, int x, int y) {
      JPopupMenu contextMenu = createContextMenu();

      enableContextMenuItems();
      if (MainFrame.getMainFrame().isReadOnly()) {
        disableContextMenuItemsForReadOnlyMode();
      }

    setContainerFontSize(contextMenu, Prefs.editorFontSize.getInt());

      // Show the context menu
      contextMenu.show(c, x, y);
    }

  private JPopupMenu createContextMenu() {
    JPopupMenu contextMenu = new JPopupMenu();
    contextMenu.add(addSubOperatorItem);
    setMenuItemAction(addSubOperatorItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.addSubOperator();
      });

    contextMenu.add(addTopFolderItem);
    setMenuItemAction(addTopFolderItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.addTopFolder();
      });

    contextMenu.add(addFileItem);
    setMenuItemAction(addFileItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.addFile();
      });

    impasseSubMenu.add(tieImpasseItem);
    setMenuItemAction(tieImpasseItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.addImpasse("Impasse__Operator_Tie");
      });

    impasseSubMenu.add(conflictImpasseItem);
    setMenuItemAction(conflictImpasseItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.addImpasse("Impasse__Operator_Conflict");
      });

    impasseSubMenu.add(constraintImpasseItem);
    setMenuItemAction(constraintImpasseItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.addImpasse("Impasse__Operator_Constraint-Failure");
      });

    impasseSubMenu.add(stateNoChangeImpasseItem);
    setMenuItemAction(stateNoChangeImpasseItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.addImpasse("Impasse__State_No-Change");
      });

    contextMenu.add(impasseSubMenu);

    contextMenu.add(openRulesItem);
    setMenuItemAction(openRulesItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.openRules();
      });

    contextMenu.add(openDataMapItem);
    setMenuItemAction(openDataMapItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.openDataMap();
      });

    contextMenu.addSeparator();

    contextMenu.add(searchItem);
    setMenuItemAction(searchItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.searchFiles();
      });

    contextMenu.add(replaceItem);
    setMenuItemAction(replaceItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.replaceFiles();
      });

    contextMenu.addSeparator();

    contextMenu.add(deleteItem);
    setMenuItemAction(deleteItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.delete();
      });

    contextMenu.add(renameItem);
    setMenuItemAction(renameItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.rename();
      });

    contextMenu.add(exportItem);
    setMenuItemAction(exportItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.export();
      });

    contextMenu.add(importItem);
    setMenuItemAction(importItem,
      e -> {
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.importFunc();
      });

    contextMenu.add(generateDataMapItem);
    // actionPerformed
    setMenuItemAction(generateDataMapItem,
      ae -> {
        Vector<FeedbackListEntry> parseErrors = new Vector<>();
        Vector<FeedbackListEntry> vecGenerations = new Vector<>();

        // Generate the new entries
        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
        ow.generateDataMap(null, parseErrors, vecGenerations);

        // Report the results
        MainFrame.getMainFrame().getFeedbackManager().showFeedback(vecGenerations);
      });

    contextMenu.add(checkChildrenAgainstDataMapItem);
    setMenuItemAction(checkChildrenAgainstDataMapItem,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
            ow.checkChildrenAgainstDataMap();
          }
        });
    return contextMenu;
  }

  private void setMenuItemAction(JMenuItem menuItem, ActionListener newListener) {
    for (ActionListener listener : menuItem.getActionListeners()) {
      menuItem.removeActionListener(listener);
    }
    menuItem.addActionListener(newListener);
  }

    /**
     * just returns the name of the node
     *
     * @return the name of the node
     */
    public String toString() {
        return name;
    }

    /**
     * Recursively Deletes the operator and  any children it may have
     */
    static void recursiveDelete(File theFile) {
        File[] children = theFile.listFiles();

        //Recursive case:
        if (children != null) {
            for (File child : children) {
                recursiveDelete(child);
            }
        }

        theFile.delete();
    }

    /**
     * renames a file or folder to have a succeeding '~' indicating it's
     * been removed from the project.  By not actually deleting the file
     * you make it possible for the user to recover the data if needed.
     *
     * This method properly handles folders by recursively calling itself
     * on their contents.
     */
    protected void renameToRemove(File oldFile) {
        File newFile = new File(oldFile.getPath() + "~");

        //If the name is already in use then it's presumably
        // a previously removed version, so it's safe to delete
        if (newFile.exists()) {
            if (!newFile.delete()) {
                if (newFile.isDirectory()) {
                    recursiveDelete(newFile);
                }
            }
        }

        oldFile.renameTo(newFile);
    }


    /**
     * checkCreateReplace
     * <p>
     * is called when operator window is requested to add a file.
     * Method checks to see if a file with that name already exists.  If so,
     * allows the user to use the existing file or create a new file.
     *
     * @return true if the file can be created, otherwise return false
     */

    public boolean checkCreateReplace(File newFile) throws IOException {
        String nodeName = newFile.getName();
        int endPos = nodeName.indexOf('.');
        if (endPos != -1) {
            nodeName = nodeName.substring(0, endPos);
        }
        // check that no conflicting files already exist
        if (newFile.exists()) {
            FileAlreadyExistsDialog faeDialog = new FileAlreadyExistsDialog(MainFrame.getMainFrame(), nodeName);
            faeDialog.setVisible(true);

            switch (faeDialog.wasApproved()) {
                case 0:     // Cancel add file command
                    return true;
                case 1:     // Use Existing file
                    return false;
                case 2:     // Replace with new file
                    newFile.delete(); // eliminate old file
                    newFile.createNewFile();
                    return false;
            }
        }
        newFile.createNewFile();
        return false;
    }

    /**
     * This method is called by subclasses when they want to add files or folders
     * This performs a check for conflicts and prompts the user appropriately
     *
     * @return true if the file or folder can not be created, otherwise return false
     */
    public boolean creationConflict(File newFile, boolean creatingHLOp) {
        String temp;
        String nodeName = newFile.getName();
        int endPos = nodeName.indexOf('.');
        Enumeration<? extends TreeNode> kids;

        if (!creatingHLOp) {
            // check that no conflicting nodes already exist
            if (endPos != -1) {
                nodeName = nodeName.substring(0, endPos);
            }
            if (newFile.getParentFile().getName().equals(toString())) { // this is a new, check against children
                kids = children();
            } else { // this is a rename operation, check against siblings
                kids = getParent().children();
            }
            while (kids.hasMoreElements()) {
                temp = kids.nextElement().toString();

                if (temp.equals(nodeName)) {
                    JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                            "A node with name \"" + temp + "\" already exists",
                            "Node Conflict", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        // check that no conflicting files already exist
        if (newFile.exists()) {

            if (newFile.isDirectory()) {

                File[] deleteThese = newFile.listFiles();
                if ((deleteThese != null) && (deleteThese.length == 0)) { // empty folder
                    newFile.delete(); // to eliminate rename conflicts
                } else {
                    JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                            "A non-empty folder \n\"" + newFile + "\"\n already exists",
                            "Folder Conflict", JOptionPane.ERROR_MESSAGE);
                    return true;
                }
            } else if (newFile.length() == 0) { // empty file
                newFile.delete(); // to eliminate rename conflicts
            } else {
                int result = JOptionPane.showConfirmDialog(MainFrame.getMainFrame(),
                        "A file with name \"" + newFile.getName() + "\" already exists\nReplace the file?",
                        "File Conflict", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);

                switch (result) {
                    case JOptionPane.OK_OPTION:
                        newFile.delete();
                        return false;
                    case JOptionPane.CANCEL_OPTION:
                    case JOptionPane.CLOSED_OPTION:
                        return true;
                }
            }
        }

        return false;
    }

    /**
     * convenience overload for the above
     */
    public boolean creationConflict(File newFile) {
        return creationConflict(newFile, false);
    }

    /**
     * If the node supports this operation it should be overloaded in the subclass
     * if this function gets called it means that the node did not properly overload
     * the function, so the user just told the program to do something that it cannot
     * all this function does is print out an error message to that effect
     *
     * @param newOperatorName the name of the new operator to add
     *
     * @return the newly created child operator node (or null on failure)
     */
    public OperatorNode addSubOperator(OperatorWindow operatorWindow, SoarWorkingMemoryModel swmm, String newOperatorName) throws IOException {
        System.err.println("OperatorNode.addSubOperator: This should never get called");
        return null;
    }

    /**
     * If the node supports this operation it should be overloaded in the subclass
     * if this function gets called it means that the node did not properly overload
     * the function, so the user just told the program to do something that it cannot
     * all this function does is print out an error message to that effect
     *
     * @param newOperatorName the name of the new operator to add
     */
    public OperatorNode addImpasseOperator(OperatorWindow operatorWindow, SoarWorkingMemoryModel swmm, String newOperatorName) throws IOException {
        System.err.println("addImpasseOperator:  This should never get called");
        return null;
    }

    /**
     * If the node supports this operation it should be overloaded in the subclass
     * if this function gets called it means that the node did not properly overload
     * the function, so the user just told the program to do something that it cannot
     * all this function does is print out an error message to that effect
     */
    public void addFileOperator(OperatorWindow operatorWindow, SoarWorkingMemoryModel swmm, String newFileName) throws IOException {
        System.err.println("addFileOperator: This should never get called");
    }

    /**
     * Overloaded operation
     */
    public void addFile(OperatorWindow operatorWindow, String newFileName) throws IOException {
        System.err.println("addFile: This should never get called");
    }


    /**
     * If the node supports this operation it should be overloaded in the subclass
     * if this function gets called it means that the node did not properly overload
     * the function, so the user just told the program to do something that it cannot
     * all this function does is print out an error message to that effect
     */
    public void delete(OperatorWindow operatorWindow) {
    }

    /**
     * Tell the parent that a node has been deleted
     * a node should do when a child is deleted - nothing
     */
    public void notifyDeletionOfChild(OperatorWindow operatorWindow, OperatorNode child) {
    }

    /**
     * If the node supports this operation it should be overloaded in the subclass
     * if this function gets called it means that the node did not properly overload
     * the function, so the user just told the program to do something that it cannot
     * all this function does is print out an error message to that effect
     *
     * @param pw the MainFrame
     *
     * @return the RuleEditor object created (or null on failure)
     */
    public RuleEditor openRules(MainFrame pw) {
        return null;
    }


    /**
     * Overloaded operation
     */
    public void openDataMap(SoarWorkingMemoryModel swmm, MainFrame pw) {
        System.err.println("openDataMap: This should never get called");
    }


    /**
     * If the node supports this operation it should be overloaded in the subclass
     * if this function gets called it means that the node did not properly overload
     * the function, so the user just told the program to do something that it cannot
     * all this function does is print out an error message to that effect
     *
     * @param pw   the Project window
     * @param line the line number to place the caret on
     */
    public void openRules(MainFrame pw, int line) {
    }

    /**
     * If the node supports this operation it should be overloaded in the subclass
     * if this function gets called it means that the node did not properly overload
     * the function, so the user just told the program to do something that it cannot
     * all this function does is print out an error message to that effect
     *
     * @param pw          the Project window
     * @param line        the line number to place the caret on
     * @param assocString the substring to place the caret on
     */
    public void openRulesToString(MainFrame pw, int line, String assocString) {
    }

    /**
     * If the node supports this operation it should be overloaded in the subclass
     * if this function gets called it means that the node did not properly overload
     * the function, so the user just told the program to do something that it cannot
     * all this function does is print out an error message to that effect
     *
     * @param newName the new name that the user wants this node to be called
     */
    public void rename(OperatorWindow operatorWindow, String newName) throws IOException {
        System.err.println("rename: This operation is not supported on this node");
    }

    public SoarIdentifierVertex getStateIdVertex(SoarWorkingMemoryModel swmm) {
        return null;
    }

  public String getName() {
      return this.name;
  }

  protected abstract String getFullPathName();

    /**
     * Exports the operator and sub operators to a .vse file
     *
     * @param fileName the .vse file that is being written too
     */
    public void export(File fileName) throws IOException {
        Writer w = new FileWriter(fileName);

        int childCount = 0;
        boolean linkNodeFound = false;
        Enumeration<TreeNode> nodes = preorderEnumeration();
        while (nodes.hasMoreElements()) {
            OperatorNode node = (OperatorNode) nodes.nextElement();
            if (node instanceof LinkNode) {
                linkNodeFound = true;
            }
            ++childCount;
        }

        if (linkNodeFound) {
            JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                    "A Linked Operator has been found in your export, and will be exported as a low-level operator."
                    , "Link Information will be lost",
                    JOptionPane.WARNING_MESSAGE);
        }

        w.write("VERSION 2\n");
        exportType(w);
        w.write("LAYOUT " + childCount + "\n");

        final String TAB = "\t";

        // This hash table is used to associate pointers with id's
        // given a pointer you can look up the id for the that node
        // this is used for parent id lookup
        Hashtable<OperatorNode, Integer> ht = new Hashtable<>();
        int nodeID = 0;

        // Doing this enumeration guarantees that we will never reach a
        // child without first processing its parent
        nodes = preorderEnumeration();

        OperatorNode node = (OperatorNode) nodes.nextElement();
        ht.put(node, nodeID);

        // special case for the root node
        // write out tree specific stuff
        w.write("-1" + TAB);

        // tell the node to write itself
        node.exportDesc(w);

        // terminate the line
        w.write("\n");

        while (nodes.hasMoreElements()) {
            nodeID++;
            node = (OperatorNode) nodes.nextElement();
            ht.put(node, nodeID);

            // Again the same technique write out the tree information, then the node specific stuff, then
            // terminate the line
            OperatorNode parent = (OperatorNode) node.getParent();
            w.write(ht.get(parent) + TAB);
            node.exportDesc(w);
            w.write("\n");
        }

        w.write("RULES\n");
        nodes = preorderEnumeration();
        while (nodes.hasMoreElements()) {
            node = (OperatorNode) nodes.nextElement();
            node.exportFile(w, ht.get(node));
        }

        exportDataMap(w);
        w.close();
    }

    /*
     * this function is called when you want to move this child underneath a new parent
     */

    public boolean move(OperatorWindow operatorWindow, OperatorNode newParent) {
        return false;
    }

    public abstract void exportDesc(Writer w) throws IOException;

    public abstract void exportFile(Writer w, int id) throws IOException;

    public abstract void exportDataMap(Writer w) throws IOException;

    public abstract void exportType(Writer w) throws IOException;

    public void importFunc(Reader r, OperatorWindow operatorWindow, SoarWorkingMemoryModel swmm) throws IOException, NumberFormatException {
    }

    public abstract void copyStructures(File folderToWriteTo) throws IOException;

    public abstract void searchTestDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListEntry> errors);

    public abstract void searchCreateDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListEntry> errors);

    public abstract void searchTestNoCreateDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListEntry> errors);

    public abstract void searchCreateNoTestDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListEntry> errors);

    public abstract void searchNoTestNoCreateDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListEntry> errors);

    public abstract void source(Writer w) throws IOException;

    public abstract void sourceChildren() throws IOException;

    public abstract void sourceRecursive() throws IOException;

    /** close any open editor windows associated with this node */
    public abstract void closeEditors();

}//class OperatorNode
