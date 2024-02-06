package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.dialogs.FileAlreadyExistsDialog;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.misc.FeedbackListObject;
import edu.umich.soar.visualsoar.parser.ParseException;
import edu.umich.soar.visualsoar.parser.SoarProduction;
import edu.umich.soar.visualsoar.parser.TokenMgrError;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 * This is the basis class for which all operator nodes are
 * derived. Known sub-classes:
 *
 * @author Brad Jones
 * @version 0.5a 5 Aug 1999
 * <p>
 * Class Hierarchy: <br/>
 *    Operator Node                                superclass for all nodes<br/>
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

///////////////////////////////////////////////////////////////////
// Data Members
////////////////////////////////////////////////////////////////////
    static protected JPopupMenu contextMenu = new JPopupMenu();
    static protected JMenuItem addSuboperatorItem = new JMenuItem("Add a Suboperator...");
    static protected JMenuItem addTopFolderItem = new JMenuItem("Add a Top-Level Folder...");
    static protected JMenuItem addFileItem = new JMenuItem("Add a File...");

    static protected JMenu impasseSubMenu = new JMenu("Add an Impasse...");
    static protected JMenuItem tieImpasseItem = new JMenuItem("Operator Tie Impasse");
    static protected JMenuItem conflictImpasseItem = new JMenuItem("Operator Conflict Impasse");
    static protected JMenuItem constraintImpasseItem = new JMenuItem("Operator Constraint-Failure Impasse");
    static protected JMenuItem stateNoChangeImpasseItem = new JMenuItem("State No-Change Impasse");

    static protected JMenuItem openRulesItem = new JMenuItem("Open Rules");
    static protected JMenuItem openDataMapItem = new JMenuItem("Open Datamap");
    static protected JMenuItem searchItem = new JMenuItem("Find...");
    static protected JMenuItem replaceItem = new JMenuItem("Replace...");
    static protected JMenuItem deleteItem = new JMenuItem("Delete");
    static protected JMenuItem renameItem = new JMenuItem("Rename...");
    static protected JMenuItem exportItem = new JMenuItem("Export");
    static protected JMenuItem importItem = new JMenuItem("Import...");
    static protected JMenuItem checkChildrenAgainstDataMapItem = new JMenuItem("Check Children Against Datamap");
    static protected JMenuItem generateDataMapItem = new JMenuItem("Generate Datamap Entries for this File");

    static {
        contextMenu.add(addSuboperatorItem);
        addSuboperatorItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.addSuboperator();
            }
        });

        contextMenu.add(addTopFolderItem);
        addTopFolderItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.addTopFolder();
            }
        });

        contextMenu.add(addFileItem);
        addFileItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.addFile();
            }
        });

        impasseSubMenu.add(tieImpasseItem);
        tieImpasseItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.addImpasse("Impasse__Operator_Tie");
            }
        });

        impasseSubMenu.add(conflictImpasseItem);
        conflictImpasseItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.addImpasse("Impasse__Operator_Conflict");
            }
        });

        impasseSubMenu.add(constraintImpasseItem);
        constraintImpasseItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.addImpasse("Impasse__Operator_Constraint-Failure");
            }
        });

        impasseSubMenu.add(stateNoChangeImpasseItem);
        stateNoChangeImpasseItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.addImpasse("Impasse__State_No-Change");
            }
        });

        contextMenu.add(impasseSubMenu);


        contextMenu.add(openRulesItem);
        openRulesItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.openRules();
            }
        });

        contextMenu.add(openDataMapItem);
        openDataMapItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.openDataMap();
            }
        });

        contextMenu.addSeparator();

        contextMenu.add(searchItem);
        searchItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                        ow.searchFiles();
                    }
                });

        contextMenu.add(replaceItem);
        replaceItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                        ow.replaceFiles();
                    }
                });

        contextMenu.addSeparator();

        contextMenu.add(deleteItem);
        deleteItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.delete();
            }
        });

        contextMenu.add(renameItem);
        renameItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.rename();
            }
        });

        contextMenu.add(exportItem);
        exportItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.export();
            }
        });

        contextMenu.add(importItem);
        importItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.importFunc();
            }
        });

        contextMenu.add(generateDataMapItem);
        generateDataMapItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Vector<FeedbackListObject> parseErrors = new Vector<>();
                Vector<FeedbackListObject> vecGenerations = new Vector<>();

                //Generate the new entries
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.generateDataMap(null, parseErrors, vecGenerations);

                //Report the results
                MainFrame.getMainFrame().setFeedbackListData(vecGenerations);

            }//actionPerformed
        });

        contextMenu.add(checkChildrenAgainstDataMapItem);
        checkChildrenAgainstDataMapItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperatorWindow ow = (OperatorWindow) contextMenu.getInvoker();
                ow.checkChildrenAgainstDataMap();
            }
        });

    }

    protected String name;
    protected int id;

///////////////////////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////////////////////

    /**
     * Constructor
     *
     * @param inName the name of the operator
     * @param inId   the unique id associated with this operator
     */
    public OperatorNode(String inName, int inId) {
        name = inName;
        id = inId;
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
     * it into a FeedbackListObject that can be placed in the feedback window.
     *
     * @param node the OperatorNode that contains the parse error
     * @param pe   the ParseException to parse
     * @return the generated FeedbackListObject
     */
    public static FeedbackListObject parseParseException(OperatorNode node,
                                                         ParseException pe) {
        String parseError = pe.toString();
        //Strip away unncessary full qualifiers on class name
        parseError = parseError.replace("edu.umich.soar.visualsoar.", "");
        int lineNum = getLineNumFromErr(parseError);
        return new FeedbackListObject(node, lineNum, parseError, true);
    }

    /**
     * non-static version of the above that uses 'this' for the opNode
     */
    public FeedbackListObject parseParseException(ParseException pe) {
        return parseParseException(this, pe);
    }

    /**
     * Given a lexical error discovered in this node, this function converts
     * it into a FeedbackListObject that can be placed in the feedback window.
     *
     * @param tme the TokeMgrError to parse
     * @return the generated FeedbackListObject
     */
    public FeedbackListObject parseTokenMgrError(TokenMgrError tme) {
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

        return new FeedbackListObject(this, lineNum, errString, tokenString);
    }


    /**
     * overloaded by subclasses
     */
    public Vector<SoarProduction> parseProductions() throws ParseException, java.io.IOException {
        return null;
    }

    public boolean CheckAgainstDatamap(Vector<FeedbackListObject> vecErrors) throws IOException {
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

        addSuboperatorItem.setEnabled(true);
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
        addSuboperatorItem.setEnabled(false);
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
        enableContextMenuItems();
        if (MainFrame.getMainFrame().isReadOnly()) disableContextMenuItemsForReadOnlyMode();
        contextMenu.show(c, x, y);
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
        if ((children != null) && (children.length > 0)) {
            for (File child : children) {
                recursiveDelete(child);
            }
        }

        theFile.delete();
    }

    protected void renameToDeleted(File oldFile) {
        File newFile;

        newFile = new File(oldFile.getPath() + "~");

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
     */
    public OperatorNode addSubOperator(OperatorWindow operatorWindow, SoarWorkingMemoryModel swmm, String newOperatorName) throws IOException {
        System.err.println("addSuboperator: This should never get called");
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
     */
    public void openRules(MainFrame pw) {
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

    public SoarIdentifierVertex getStateIdVertex() {
        return null;
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
        // given a pointer you can lookup the id for the that node
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

    public abstract void searchTestDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListObject> errors);

    public abstract void searchCreateDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListObject> errors);

    public abstract void searchTestNoCreateDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListObject> errors);

    public abstract void searchCreateNoTestDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListObject> errors);

    public abstract void searchNoTestNoCreateDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListObject> errors);

    public abstract void source(Writer w) throws IOException;

    public abstract void sourceChildren() throws IOException;

    public abstract void sourceRecursive() throws IOException;


}//class OperatorNode
