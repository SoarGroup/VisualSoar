package edu.umich.soar.visualsoar.operatorwindow;


import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryReader;
import edu.umich.soar.visualsoar.dialogs.FindInProjectDialog;
import edu.umich.soar.visualsoar.dialogs.NameDialog;
import edu.umich.soar.visualsoar.dialogs.ReplaceInProjectDialog;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.graph.SoarVertex;
import edu.umich.soar.visualsoar.misc.FeedbackListObject;
import edu.umich.soar.visualsoar.parser.ParseException;
import edu.umich.soar.visualsoar.parser.SoarProduction;
import edu.umich.soar.visualsoar.parser.TokenMgrError;
import edu.umich.soar.visualsoar.util.ReaderUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * A class to implement the behavior of the operator window
 *
 * @author Brad Jones
 * @author Brian Harleton
 * @version 4.0 15 Jun 2002
 */
@SuppressWarnings("unused")
public class OperatorWindow extends JTree {
    private static final long serialVersionUID = 20221225L;

    int nextId = 1;
///////////////////////////////////////////////////////////////////////////
// Data members
///////////////////////////////////////////////////////////////////////////
    /**
     * @serial a reference to the DragGestureListener for Drag and Drop operations, may be deleted in future
     */
    DragGestureListener dgListener = new OWDragGestureListener();

    /**
     * @serial a reference to the project file
     */
    private SoarWorkingMemoryModel WorkingMemory;
    private final boolean closed = false;
    private static OperatorWindow s_OperatorWindow;


    /**
     * Private usage only.
     * Default constructor to do common things such as
     * setting up the mouse and keyboard listeners and backup threads
     *
     * @see BackupThread
     */
    private OperatorWindow() {

        setCellRenderer(new OperatorWindowRenderer());

        s_OperatorWindow = this;

        toggleClickCount = 3;
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE | DnDConstants.ACTION_LINK, dgListener);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    suggestShowContextMenu(e.getX(), e.getY());
                }
                if ((e.getClickCount() == 2 && ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK))) {
                    openRules();
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    suggestShowContextMenu(e.getX(), e.getY());
                }
            }
        });

        registerKeyboardAction(new ActionListener() {
                                   public void actionPerformed(ActionEvent e) {
                                       delete();
                                   }
                               },
                KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
                WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        registerKeyboardAction(new ActionListener() {
                                   public void actionPerformed(ActionEvent e) {
                                       delete();
                                   }
                               },
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
                WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        registerKeyboardAction(new ActionListener() {
                                   public void actionPerformed(ActionEvent e) {
                                       TreePath tp = getSelectionPath();
                                       if (tp != null) {
                                           OperatorNode selNode = (OperatorNode) tp.getLastPathComponent();
                                           selNode.openRules(MainFrame.getMainFrame());
                                       }
                                   }
                               },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        new BackupThread().start();
    }

    /**
     * Creates an Operator Window given a project name.
     * Creates a default project tree model and a new WorkingMemory for new projects.
     *
     * @param projectName     The name of the project
     * @param projectFileName The full file path of the projects location
     * @param is_new          True if it is a new project
     * @see SoarWorkingMemoryModel
     * @see OperatorWindow#defaultProject(String, File)
     */
    public OperatorWindow(String projectName, String projectFileName, boolean is_new) {
        this();
        s_OperatorWindow = this;
        if (is_new) {
            setModel(defaultProject(projectName, new File(projectFileName)));
            WorkingMemory = new SoarWorkingMemoryModel(true, projectName);
        }
    }

    /**
     * Opens an OperatorWindow for an existing project
     *
     * @param in_file the location of the project to be opened
     * @see SoarWorkingMemoryModel
     * @see OperatorWindow#openHierarchy(File)
     */
    public OperatorWindow(File in_file) throws NumberFormatException, IOException {
        this();
        s_OperatorWindow = this;
        WorkingMemory = new SoarWorkingMemoryModel(false, null);
        openHierarchy(in_file);
    }

    public static OperatorWindow getOperatorWindow() {
        return s_OperatorWindow;
    }


    /**
     * Checks to see if x,y is a valid location on the screen, if it is then it
     * displays the context menu there
     *
     * @param x the x coordinate of the screen
     * @param y the y coordinate of the screen
     */
    public void suggestShowContextMenu(int x, int y) {
        TreePath treePath = getPathForLocation(x, y);
        if (treePath != null) {
            setSelectionPath(treePath);
            OperatorNode node = (OperatorNode) treePath.getLastPathComponent();
            node.showContextMenu(this, x, y);
        }
    }

    /**
     * Creates a new folder node in the operator window
     *
     * @param inName       Name given to new node
     * @param inFolderName same as inName, name of created folder
     * @see FolderNode
     */
    public FolderNode createFolderNode(String inName, String inFolderName) {
        return new FolderNode(inName, getNextId(), inFolderName);
    }

    /**
     * Creates a new File node in the operator window
     *
     * @param inName name of file node
     * @param inFile name of created rule editor file, same as inName
     * @see FileNode
     */
    public FileNode createFileNode(String inName, String inFile) {
        return new FileNode(inName, getNextId(), inFile);
    }

    /**
     * Creates a new Impasse Operator Node in the operator window
     *
     * @param inName     name of node
     * @param inFileName name of created rule editor file, same as inName
     * @see ImpasseOperatorNode
     */
    public ImpasseOperatorNode createImpasseOperatorNode(String inName, String inFileName) {
        return new ImpasseOperatorNode(inName, getNextId(), inFileName);
    }

    /**
     * Creates a high level Impasse Operator Node in the operator window
     *
     * @param inName            name of node
     * @param inFileName        name of created rule editor file, same as inName
     * @param inFolderName      name of created folder, same as inName
     * @param inDataMapIdNumber integer corresponding to node's datamap
     * @see ImpasseOperatorNode
     */
    public ImpasseOperatorNode createImpasseOperatorNode(String inName, String inFileName, String inFolderName, int inDataMapIdNumber) {
        return new ImpasseOperatorNode(inName, getNextId(), inFileName, inFolderName, inDataMapIdNumber);
    }

    /**
     * Creates a new File Operator Node in the operator window
     *
     * @param inName     name of node
     * @param inFileName name of created rule editor file, same as inName
     * @see FileOperatorNode
     */
    public FileOperatorNode createFileOperatorNode(String inName, String inFileName) {
        return new FileOperatorNode(inName, getNextId(), inFileName);
    }

    /**
     * Creates a high-level File Operator Node in the operator window
     *
     * @param inName       name of node
     * @param inFileName   name of created rule editor file, same as inName
     * @param inFolderName name of created folder, same as inName
     * @param inDataMapId  SoarIdentifierVertex corresponding to node's datamap
     * @see FileOperatorNode
     * @see SoarIdentifierVertex
     */
    public FileOperatorNode createFileOperatorNode(String inName, String inFileName, String inFolderName, SoarIdentifierVertex inDataMapId) {
        return new FileOperatorNode(inName, getNextId(), inFileName, inFolderName, inDataMapId);
    }

    /**
     * Creates a high-level File Operator Node in the operator window
     *
     * @param inName            name of node
     * @param inFileName        name of created rule editor file, same as inName
     * @param inFolderName      name of created folder, same as inName
     * @param inDataMapIdNumber integer corresponding to node's datamap
     * @see FileOperatorNode
     */
    public FileOperatorNode createFileOperatorNode(String inName, String inFileName, String inFolderName, int inDataMapIdNumber) {
        return new FileOperatorNode(inName, getNextId(), inFileName, inFolderName, inDataMapIdNumber);
    }

    /**
     * Creates a new Soar Operator Node in the operator window
     *
     * @param inName     name of the node
     * @param inFileName name of created rule editor file, same as inName
     * @see OperatorOperatorNode
     */
    public OperatorOperatorNode createSoarOperatorNode(String inName, String inFileName) {
        return new OperatorOperatorNode(inName, getNextId(), inFileName);
    }

    /**
     * Creates a high level Soar Operator Node in the operator window
     *
     * @param inName            name of the node
     * @param inFileName        name of created rule editor file, same as inName
     * @param inFolderName      name of created folder, same as inName
     * @param inDataMapIdNumber integer corresponding to node's datamap
     * @see OperatorOperatorNode
     */
    public OperatorOperatorNode createSoarOperatorNode(String inName, String inFileName, String inFolderName, int inDataMapIdNumber) {
        return new OperatorOperatorNode(inName, getNextId(), inFileName, inFolderName, inDataMapIdNumber);
    }

    /**
     * Creates the Root Node of the operator hierarchy.  From here all sub operators
     * branch.  This is the method called when a new project is created
     *
     * @param inName          name of the root node, should be the same as the project name
     * @param inFullPathStart full path of the project
     * @param inFolderName    created folder name, same as project name
     * @see OperatorRootNode
     */
    public OperatorRootNode createOperatorRootNode(String inName, String inFullPathStart, String inFolderName) {
        return new OperatorRootNode(inName, getNextId(), inFullPathStart, inFolderName);
    }

    /**
     * Creates the Root Node of the operator hierarchy.  From here all sub operators
     * branch.  This is the root node method called when opening an existing project.
     *
     * @param inName       name of the node, same as the name of the project
     * @param inFolderName name of the root operator's folder, same as inName
     * @see OperatorRootNode
     */
    public OperatorRootNode createOperatorRootNode(String inName, String inFolderName) {
        return new OperatorRootNode(inName, getNextId(), inFolderName);
    }

    /**
     * LinkNodes not used in this version of Visual Soar
     *
     * @see LinkNode
     */
    public LinkNode createLinkNode(String inName, String inFileName, int inHighLevelId) {
        return new LinkNode(inName, getNextId(), inFileName, inHighLevelId);
    }

    /**
     * Removes a node from the operator window
     *
     * @param operatorNode the node that is to be removed
     * @see DefaultTreeModel#removeNodeFromParent
     */
    public void removeNode(OperatorNode operatorNode) {
        ((DefaultTreeModel) getModel()).removeNodeFromParent(operatorNode);
    }

    /**
     * Returns the next id used for keeping track of each operator's datamap
     */
    final public int getNextId() {
        return nextId++;
    }

    /**
     * Returns the number of children associated with the root node / project node.
     */
    public int getChildCount() {
        DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) getModel().getRoot();
        return dmtn.getChildCount();
    }


    /*
     * Method inserts an Operator Node into the Operator Hierarchy tree in
     * alphabetical order preferenced in order of [FileOperators], [SoarOperators],
     * and [ImpasseOperators].
     *
     * @param parent operator of operator to be inserted
     * @param child operator to be inserted into tree
     * @see DefaultTreeModel#insertNodeInto(MutableTreeNode, MutableTreeNode, int)
     */
    public void addChild(OperatorNode parent, OperatorNode child) {
        // Put in alphabetical order in order of [Files], [Operators], [Impasses]
        boolean found = false;

        for (int i = 0; i < parent.getChildCount() && !found; ++i) {
            String childName = child.toString();
            String sl = childName.toLowerCase();
            String childString = (parent.getChildAt(i)).toString();

            // Check for duplicate
            if (childName.compareTo(parent.getChildAt(i).toString()) == 0) {
                JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                        "Node conflict for " + childName,
                        "Node Conflict",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!(childString.equals("_firstload") || childString.equals("all") || childString.equals("elaborations"))) {
                // Adding an Impasse Node
                if (child instanceof ImpasseOperatorNode) {
                    if ((sl.compareTo(childString.toLowerCase()) <= 0)) {
                        found = true;
                        ((DefaultTreeModel) getModel()).insertNodeInto(child, parent, i);
                    }
                }
                // Adding a SoarOperatorNode
                else if (child instanceof OperatorOperatorNode) {
                    if (parent.getChildAt(i) instanceof OperatorOperatorNode && sl.compareTo(childString.toLowerCase()) <= 0) {
                        found = true;
                        ((DefaultTreeModel) getModel()).insertNodeInto(child, parent, i);
                    }
                }
                // Adding a File
                else {
                    if ((parent.getChildAt(i) instanceof OperatorOperatorNode) || (sl.compareTo(childString.toLowerCase()) <= 0)) {
                        found = true;
                        ((DefaultTreeModel) getModel()).insertNodeInto(child, parent, i);
                    }
                }
            }
        }   // go through all the children until find the proper spot for the new child
        if (!found) {
            ((DefaultTreeModel) getModel()).insertNodeInto(child, parent, parent.getChildCount());
        }
    }   // end of addChild()


    /**
     * Checks name entries for illegal values
     *
     * @param theName the name entered
     * @return true if a valid name false otherwise
     */
    public static boolean operatorNameIsValid(String theName) {

        for (int i = 0; i < theName.length(); i++) {
            char testChar = theName.charAt(i);
            if (!(Character.isLetterOrDigit(testChar) || (testChar == '-') || (testChar == '_'))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks name entries for illegal values
     *
     * @param theName the name entered
     * @return true if a valid name false otherwise
     */
    public static boolean isProjectNameValid(String theName) {
        return operatorNameIsValid(theName);
    }

    /**
     * This prompts the user for a name for the sub-operator, if the user returns a valid name then
     * it inserts a new node into the tree
     */
    public void addSuboperator() {
        String s;
        NameDialog theDialog = new NameDialog(MainFrame.getMainFrame());
        theDialog.setTitle("Enter Operator Name");
        theDialog.setVisible(true);

        if (theDialog.wasApproved()) {
            s = theDialog.getText();

            TreePath tp = getSelectionPath();
            if (tp == null) {
                return; //should never happen
            }
            OperatorNode parent = (OperatorNode) tp.getLastPathComponent();

            try {
                parent = parent.addSuboperator(this, WorkingMemory, s);

                if (parent != null) {
                    tp = new TreePath(parent.getPath());

                    if (parent.getChildCount() != 0) {
                        expandPath(tp);
                    }
                }
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                        "Could not create suboperator, name may be invalid",
                        "I/O Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /*
     * Exports a portion of the Operator hierarchy into a .vse file
     * name of the .vse file is the name of the node
     * @see OperatorNode#export(File)
     * @throws Exception I/O error
     */
    public void export() {
        TreePath tp = getSelectionPath();
        if (tp == null) {
            return; //should never happen
        }
        OperatorNode node = (OperatorNode) tp.getLastPathComponent();
        try {
            String projectFolder = (new File(((OperatorRootNode) getModel().getRoot()).getProjectFile())).getParent();
            node.export(new File(projectFolder + File.separator + node + ".vse"));

            //Let the user know it was successful
            MainFrame.getMainFrame().setStatusBarMsg("Export Complete");
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                    "Error Writing File to Disk",
                    "I/O Error", JOptionPane.ERROR_MESSAGE);
        }
    }//export

    /*
     * Imports a .vse file into the operator hierarchy at the currently selected point
     * @see OperatorNode#importFunc(Reader, OperatorWindow, SoarWorkingMemoryModel)
     * @throws Exception Data Incorrectly Formatted
     * @throws Exception I/O error
     */
    public void importFunc() {
        TreePath tp = getSelectionPath();
        if (tp == null) {
            return; //should never happen
        }
        OperatorNode node = (OperatorNode) tp.getLastPathComponent();
        try {
            JFileChooser fileChooser = new JFileChooser(((OperatorRootNode) getModel().getRoot()).getFullPathStart());
            fileChooser.setFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    return f.getName().endsWith(".vse") || f.isDirectory();
                }

                public String getDescription() {
                    return "Visual Soar Export File";
                }
            });
            int state = fileChooser.showOpenDialog(MainFrame.getMainFrame());
            File file = fileChooser.getSelectedFile();
            if (file != null && state == JFileChooser.APPROVE_OPTION) {
                Reader r = new FileReader(file);
                node.importFunc(new FileReader(file), this, WorkingMemory);
                r.close();

                //Inform user of success
                MainFrame.getMainFrame().setStatusBarMsg("Import Complete");
            }


        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                    "Error Reading Import File",
                    "I/O Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                    "Data Incorrectly Formatted",
                    "Parse Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Renames the selected node
     *
     * @see NameDialog
     */
    public void rename() {
        String s;
        TreePath tp = getSelectionPath();
        if (tp == null) {
            return; //should never happen
        }
        OperatorNode node = (OperatorNode) tp.getLastPathComponent();

        NameDialog nd = new NameDialog(MainFrame.getMainFrame());
        nd.makeVisible(node.toString());
        if (nd.wasApproved()) {
            s = nd.getText();
            getModel();  //unnecessary?

            try {
                node.rename(this, s);
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                        "Could not rename, name may be invalid",
                        "I/O Error", JOptionPane.ERROR_MESSAGE);
            }

            //Save the change to the .vsa file.
            saveHierarchy();

            //Save the change to the _source files
            DefaultTreeModel tree = (DefaultTreeModel) getModel();
            OperatorRootNode root = (OperatorRootNode) tree.getRoot();
            try {

                root.startSourcing();
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                        "Operator file renamed successfully but '_source' file \ncould not be updated.  I recommend you try to save \nyour project manually.",
                        "I/O Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Adds a file object underneath the currently selected node after prompting for the name
     */
    public void addFile() {
        String s;
        NameDialog theDialog = new NameDialog(MainFrame.getMainFrame());
        theDialog.setTitle("Enter File Name");
        theDialog.setVisible(true);

        if (theDialog.wasApproved()) {
            s = theDialog.getText();
            getModel();  //unnecessary?
            TreePath tp = getSelectionPath();
            if (tp == null) {
                return; //should never happen
            }
            OperatorNode parent = (OperatorNode) tp.getLastPathComponent();

            try {
                parent.addFileOperator(this, WorkingMemory, s);

                if (parent.getChildCount() != 0) {
                    expandPath(tp);
                }
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                        "Could not create file, name may be invalid",
                        "I/O Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    /**
     * Adds an Impasse.  An impasse is similar to a Soar Operator Node on all
     * accounts except for user cannot name an impasse, impasses are automatically
     * named based on Soar Convention.
     * Adds a SoarOperatorNode object underneath the currently selected node.
     *
     * @param s impasse type string chosen from file menu, given as name of node
     */
    public void addImpasse(String s) {
        TreePath tp = getSelectionPath();
        if (tp == null) {
            return; //should never happen
        }
        OperatorNode parent = (OperatorNode) tp.getLastPathComponent();

        try {
            parent = parent.addImpasseOperator(this, WorkingMemory, s);

            if (parent != null) {
                tp = new TreePath(parent.getPath());

                if (parent.getChildCount() != 0) {
                    expandPath(tp);
                }
            }
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                    "Could not create file, name may be invalid",
                    "I/O Error", JOptionPane.ERROR_MESSAGE);
        }

    }     // end of addImpasse()


    /**
     * Given the associated Operator Node, a vector of parsed soar productions,
     * and a list to put the errors this function will check the productions
     * consistency across the datamap.
     *
     * @see SoarProduction
     * @see SoarWorkingMemoryModel#checkProduction
     */
    public void checkProductions(OperatorNode parent,
                                 OperatorNode child,
                                 Vector<SoarProduction> productions,
                                 List<FeedbackListObject> errors) {

        // Find the state that these productions should be checked against
        SoarIdentifierVertex siv = parent.getStateIdVertex();
        if (siv == null) {
            siv = WorkingMemory.getTopstate();
        }
        Enumeration<SoarProduction> prodEnum = productions.elements();

        while (prodEnum.hasMoreElements()) {
            SoarProduction sp = prodEnum.nextElement();
            errors.addAll(WorkingMemory.checkProduction(child, siv, sp));
        }
    }

    /**
     * Asks the MainFrame class to open a rule editor with the associated file of the node
     */
    public void openRules() {
        TreePath tp = getSelectionPath();
        if (tp == null) {
            return; //should never happen
        }
        OperatorNode selNode = (OperatorNode) tp.getLastPathComponent();
        selNode.openRules(MainFrame.getMainFrame());
    }

    /**
     * Asks the MainFrame class to open the datamap
     */
    public void openDataMap() {
        TreePath tp = getSelectionPath();
        if (tp == null) {
            return; //should never happen
        }
        OperatorNode selNode = (OperatorNode) tp.getLastPathComponent();
        selNode.openDataMap(WorkingMemory, MainFrame.getMainFrame());
    }

    /**
     * Displays a find dialog to search the subtree of the currently selected
     * node
     */
    public void searchFiles() {
        TreePath tp = getSelectionPath();
        if (tp == null) {
            return; //should never happen
        }
        OperatorNode selNode = (OperatorNode) tp.getLastPathComponent();
        FindInProjectDialog theDialog =
                new FindInProjectDialog(MainFrame.getMainFrame(),
                        this,
                        selNode);
        theDialog.setVisible(true);
    }

    /**
     * Displays a replace dialog to search the subtree of the currently selected
     * node
     */
    public void replaceFiles() {
        TreePath tp = getSelectionPath();
        if (tp == null) {
            return; //should never happen
        }
        OperatorNode selNode = (OperatorNode) tp.getLastPathComponent();
        ReplaceInProjectDialog theDialog =
                new ReplaceInProjectDialog(MainFrame.getMainFrame(),
                        this,
                        selNode);
        theDialog.setVisible(true);
    }


    /**
     * Returns the SoarWorkingMemoryModel
     *
     * @see SoarWorkingMemoryModel
     */
    public SoarWorkingMemoryModel getDatamap() {
        return WorkingMemory;
    }

    /**
     * removes the selected node from the tree
     */
    public void delete() {
        TreePath tp = getSelectionPath();
        if (tp == null) {
            return; //should never happen
        }
        OperatorNode selNode = (OperatorNode) tp.getLastPathComponent();
        getModel(); //unnecessary?

        if (selNode instanceof FileNode) {
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " +
                    selNode + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION)) {
                selNode.delete(this);
            }
        } else if ((selNode instanceof FolderNode) && selNode.toString().equals("common")) {
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " +
                    selNode + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION)) {
                selNode.delete(this);
            }
        } else {
            getToolkit().beep();
        }
    }

    /**
     * For the currently selected node, it will check all the children of this node against the datamap
     *
     * @see #checkProductions
     */
    public void checkChildrenAgainstDataMap() {
        TreePath tp = getSelectionPath();
        if (tp == null) {
            return; //should never happen
        }
        OperatorNode selNode = (OperatorNode) tp.getLastPathComponent();
        Vector<FeedbackListObject> vecErrors = new Vector<>();
        Vector<SoarProduction> parsedProductions;
        Enumeration<TreeNode> prodEnum = selNode.children();
        while (prodEnum.hasMoreElements()) {
            OperatorNode currentNode = (OperatorNode) prodEnum.nextElement();
            try {
                parsedProductions = currentNode.parseProductions();
                if (parsedProductions != null) {
                    MainFrame.getMainFrame().getOperatorWindow().checkProductions(selNode, currentNode, parsedProductions, vecErrors);
                }
            } catch (ParseException pe) {
                vecErrors.add(new FeedbackListObject("Unable to check productions due to parse error"));
                vecErrors.add(currentNode.parseParseException(pe));
            } catch (IOException ioe) {
                String msg = currentNode.getFileName() + "(1): " + " Error reading file.";
                vecErrors.add(new FeedbackListObject(msg));
            }
        }//while
        if (vecErrors.isEmpty()) {
            vecErrors.add(new FeedbackListObject("No errors detected in children."));
        }
        MainFrame.getMainFrame().setFeedbackListData(vecErrors);
    }

    /**
     * This function compares all productions in the file associated
     * with the currently selected node to the project datamap and
     * 'fixes' any discrepancies by adding missing entries to the
     * datamap.  Results and errors are returned to the caller.
     *
     * @param opNode         the operator node to generate a datamap for.  If null is
     *                       passed in then the currently selected node will be used.
     * @param parseErrors    parse errors discovered during generation
     * @param vecGenerations new datamap entries that were generated provided as a
     *                       list of FeedbackListObjects for easy reporting
     */
    public void generateDataMap(OperatorNode opNode,
                                Vector<FeedbackListObject> parseErrors,
                                Vector<FeedbackListObject> vecGenerations) {
        if (opNode == null) {
            TreePath tp = getSelectionPath();
            if (tp == null) {
                return; //should never happen
            }
            opNode = (OperatorNode) tp.getLastPathComponent();
        }

        //Parse all the productions in the file
        Vector<SoarProduction> parsedProds = null;
        try {
            parsedProds = opNode.parseProductions();
        } catch (ParseException pe) {
            parseErrors.add(new FeedbackListObject("Unable to generate datamap due to parse error"));
            parseErrors.add(opNode.parseParseException(pe));
        } catch (TokenMgrError | IOException tme) {
            tme.printStackTrace();
        }

        //Do not continue if there were parse errors
        if (parsedProds == null) {
            return;
        }

        // Find the datamap that these productions should be checked against
        OperatorNode parentNode = (OperatorNode) opNode.getParent();
        SoarIdentifierVertex siv = parentNode.getStateIdVertex();
        if (siv == null) {
            siv = WorkingMemory.getTopstate();
        }

        //Generate the new datamap entries
        Enumeration<SoarProduction> prodEnum = parsedProds.elements();
        while (prodEnum.hasMoreElements()) {
            SoarProduction sp = prodEnum.nextElement();
            vecGenerations.addAll(WorkingMemory.checkGenerateProduction(siv, sp, opNode));
        }

        //Verify our changes worked
        checkProductions(parentNode, opNode, parsedProds, parseErrors);

    }//generateDataMap


    /**
     * generateDataMapForOneError
     * <p>
     * behaves like {@link #generateDataMap} but it "fixes" just a single
     * datamap error by adding a non-validated entry to the datamap.
     *
     * @param errToFix error to fix (user has selected this)
     */
    public void generateDataMapForOneError(FeedbackListObject errToFix,
                                           Vector<FeedbackListObject> vecGenerations) {
        OperatorNode opNode = errToFix.getNode();
        if (opNode == null) {
            vecGenerations.add(new FeedbackListObject("No operator associated with this entry."));
            return;
        }

        //Parse all the productions in the file
        Vector<SoarProduction> parsedProds;
        try {
            parsedProds = opNode.parseProductions();
        } catch (Exception e) {
            //should never happen...
            vecGenerations.add(new FeedbackListObject("Unable to generate datamap entry due to parse error."));
            return;
        }

        // Find the datamap that these productions should be checked against
        OperatorNode parentNode = (OperatorNode) opNode.getParent();
        SoarIdentifierVertex siv = parentNode.getStateIdVertex();
        if (siv == null) {
            siv = WorkingMemory.getTopstate();
        }

        //Generate the new datamap entries
        Enumeration<SoarProduction> prodEnum = parsedProds.elements();
        while (prodEnum.hasMoreElements()) {
            SoarProduction sp = prodEnum.nextElement();
            //we only care about the production that caused the error
            if (errToFix.getMessage().startsWith(sp.getName())) {
                Vector<FeedbackListObject> errs =
                        WorkingMemory.checkGenerateSingleEntry(siv, sp, opNode, errToFix);

                //The errs list should not be empty
                if (errs.size() == 0) {
                    errs.add(new FeedbackListObject(opNode, errToFix.getLine(),
                            "Datamap entry operation failed."));
                }

                vecGenerations.addAll(errs);
                break;
            }
        }

    }//generateDataMap


    /**
     * Opens up an existing operator hierarchy
     *
     * @param in_file the file that describes the operator hierarchy
     * @see #openVersionFour(FileReader, String)
     */
    public void openHierarchy(File in_file) throws IOException, NumberFormatException {
        FileReader fr = new FileReader(in_file);
        String buffer = ReaderUtils.getWord(fr);
        if (buffer.compareToIgnoreCase("VERSION") == 0) {
            int versionId = ReaderUtils.getInteger(fr);


            if (versionId == 5) {
                openVersionFive(fr, in_file.getParent());
            } else if (versionId == 4) {
                openVersionFour(fr, in_file.getParent());
            } else if (versionId == 3) {
                openVersionThree(fr, in_file.getParent());
            } else if (versionId == 2) {
                openVersionTwo(fr, in_file.getParent());
            } else if (versionId == 1) {
                openVersionOne(fr, in_file.getParent());
            } else {
                throw new IOException("Invalid Version Number" + versionId);
            }

        }
    }

    /**
     * Opens a Version One Operator Hierarchy file
     *
     * @see #readVersionOne
     * @see SoarWorkingMemoryReader#read(SoarWorkingMemoryModel, Reader, Reader)
     */
    private void openVersionOne(FileReader fr,
                                String parentPath) throws IOException, NumberFormatException {
        String relPathToDM = ReaderUtils.getWord(fr);

        //If this project was created on one platform and ported to another
        //The wrong file separator might be present
        if (relPathToDM.charAt(0) != File.separatorChar) {
            char c = (File.separatorChar == '/') ? '\\' : '/';
            relPathToDM = relPathToDM.replace(c, File.separatorChar);
        }

        File dataMapFile = new File(parentPath + relPathToDM);
        File commentFile = new File(dataMapFile.getParent() + File.separator + "comment.dm");

        readVersionOne(fr);
        OperatorRootNode root = (OperatorRootNode) getModel().getRoot();
        root.setFullPath(parentPath);
        fr.close();

        Reader r = new FileReader(dataMapFile);

        // If a comment file exists, then make sure that it gets read in by the reader.
        if (commentFile.exists()) {
            Reader rComment = new FileReader(commentFile);
            SoarWorkingMemoryReader.read(WorkingMemory, r, rComment);
            rComment.close();
        } else {
            SoarWorkingMemoryReader.read(WorkingMemory, r, null);
        }
        r.close();
        restoreStateIds();
    }

    /**
     * Opens a Version two Operator Hierarchy file
     *
     * @see #readVersionTwo
     * @see SoarWorkingMemoryReader#read(SoarWorkingMemoryModel, Reader, Reader)
     */
    private void openVersionTwo(FileReader fr, String parentPath) throws IOException, NumberFormatException {
        String relPathToDM = ReaderUtils.getWord(fr);
        //If this project was created on one platform and ported to another
        //The wrong file separator might be present
        if (relPathToDM.charAt(0) != File.separatorChar) {
            char c = (File.separatorChar == '/') ? '\\' : '/';
            relPathToDM = relPathToDM.replace(c, File.separatorChar);
        }

        File dataMapFile = new File(parentPath + relPathToDM);
        File commentFile = new File(dataMapFile.getParent() + File.separator + "comment.dm");

        readVersionTwo(fr);
        OperatorRootNode root = (OperatorRootNode) getModel().getRoot();
        root.setFullPath(parentPath);
        fr.close();

        Reader r = new FileReader(dataMapFile);

        // If a comment file exists, then make sure that it gets read in by the reader.
        if (commentFile.exists()) {
            Reader rComment = new FileReader(commentFile);
            SoarWorkingMemoryReader.read(WorkingMemory, r, rComment);
            rComment.close();
        } else {
            SoarWorkingMemoryReader.read(WorkingMemory, r, null);
        }
        r.close();
        restoreStateIds();
    }

    /**
     * Opens a Version Three Operator Hierarchy file
     *
     * @see #readVersionThree
     * @see SoarWorkingMemoryReader#read(SoarWorkingMemoryModel, Reader, Reader)
     */
    private void openVersionThree(FileReader fr, String parentPath) throws IOException, NumberFormatException {
        String relPathToDM = ReaderUtils.getWord(fr);
        //If this project was created on one platform and ported to another
        //The wrong file separator might be present
        if (relPathToDM.charAt(0) != File.separatorChar) {
            char c = (File.separatorChar == '/') ? '\\' : '/';
            relPathToDM = relPathToDM.replace(c, File.separatorChar);
        }

        File dataMapFile = new File(parentPath + relPathToDM);
        File commentFile = new File(dataMapFile.getParent() + File.separator + "comment.dm");
        readVersionThree(fr);
        OperatorRootNode root = (OperatorRootNode) getModel().getRoot();
        root.setFullPath(parentPath);
        fr.close();

        Reader r = new FileReader(dataMapFile);
        // If a comment file exists, then make sure that it gets read in by the reader.
        if (commentFile.exists()) {
            Reader rComment = new FileReader(commentFile);
            SoarWorkingMemoryReader.read(WorkingMemory, r, rComment);
            rComment.close();
        } else {
            SoarWorkingMemoryReader.read(WorkingMemory, r, null);
        }
        r.close();
        restoreStateIds();
    }

    /**
     * Opens a Version Four Operator Hierarchy file
     *
     * @see #readVersionFour
     * @see SoarWorkingMemoryReader#read(SoarWorkingMemoryModel, Reader, Reader)
     */
    private void openVersionFour(FileReader fr, String parentPath) throws IOException, NumberFormatException {
        String relPathToDM = ReaderUtils.getWord(fr);
        //If this project was created on one platform and ported to another
        //The wrong file separator might be present
        if (relPathToDM.charAt(0) != File.separatorChar) {
            char c = (File.separatorChar == '/') ? '\\' : '/';
            relPathToDM = relPathToDM.replace(c, File.separatorChar);
        }

        File dataMapFile = new File(parentPath + relPathToDM);
        File commentFile = new File(dataMapFile.getParent() + File.separator + "comment.dm");
        readVersionFour(fr);
        OperatorRootNode root = (OperatorRootNode) getModel().getRoot();
        root.setFullPath(parentPath);
        fr.close();

        Reader r = new FileReader(dataMapFile);
        // If a comment file exists, then make sure that it gets read in by the reader.

        if (commentFile.exists()) {
            Reader rComment = new FileReader(commentFile);
            SoarWorkingMemoryReader.read(WorkingMemory, r, rComment);
            rComment.close();
        } else {
            SoarWorkingMemoryReader.read(WorkingMemory, r, null);
        }
        r.close();
        restoreStateIds();

    }

    /**
     * Opens a Version Five Operator Hierarchy file
     *
     * @see #readVersionOne
     * @see SoarWorkingMemoryReader#read(SoarWorkingMemoryModel, Reader, Reader)
     */
    private void openVersionFive(FileReader fr, String parentPath) throws IOException, NumberFormatException {

        String relPathToDM = ReaderUtils.getWord(fr);
        //If this project was created on one platform and ported to another
        //The wrong file separator might be present
        if (relPathToDM.charAt(0) != File.separatorChar) {
            char c = (File.separatorChar == '/') ? '\\' : '/';
            relPathToDM = relPathToDM.replace(c, File.separatorChar);
        }

        File dataMapFile = new File(parentPath + relPathToDM);
        File commentFile = new File(dataMapFile.getParent() + File.separator + "comment.dm");
        readVersionFive(fr);
        OperatorRootNode root = (OperatorRootNode) getModel().getRoot();
        root.setFullPath(parentPath);
        fr.close();

        Reader r = new FileReader(dataMapFile);
        // If a comment file exists, then make sure that it gets read in by the reader.

        if (commentFile.exists()) {
            Reader rComment = new FileReader(commentFile);
            SoarWorkingMemoryReader.read(WorkingMemory, r, rComment);
            rComment.close();
        } else {
            SoarWorkingMemoryReader.read(WorkingMemory, r, null);
        }
        r.close();
        restoreStateIds();

    }


    /**
     * This is a helper function restores the ids to high-level operators
     */
    private void restoreStateIds() {
        Enumeration<TreeNode> nodeEnum = ((OperatorRootNode) getModel().getRoot()).breadthFirstEnumeration();
        while (nodeEnum.hasMoreElements()) {
            Object o = nodeEnum.nextElement();
            if (o instanceof SoarOperatorNode) {
                SoarOperatorNode son = (SoarOperatorNode) o;
                if (son.isHighLevel()) {
                    son.restoreId(WorkingMemory);
                }
            }
        }

    }

    /**
     * Returns a breadth first enumeration of the tree
     */
    public Enumeration<TreeNode> breadthFirstEnumeration() {
        return ((DefaultMutableTreeNode) (treeModel.getRoot())).breadthFirstEnumeration();
    }

    /**
     * Saves project as a new project name
     *
     * @param newName the new name of the project
     * @param newPath the new file path of the project
     * @see #saveHierarchy()
     * @see OperatorRootNode#renameAndBackup(OperatorWindow, String, String)
     */
    public void saveProjectAs(String newName, String newPath) {
        //Save the original to file first
        saveHierarchy();

        //Make a copy here:
        OperatorRootNode orn = (OperatorRootNode) (getModel().getRoot());
        orn.renameAndBackup(this, newName, newPath);
    }


    /**
     * Saves the current hierarchy to disk using Version 4 method
     *
     * @param inFileName    name of the file to be saved - .vsa file
     * @param inDataMapName name of the datamap file - .dm file
     * @see #reduceWorkingMemory()
     * @see TreeFileWriter#write
     * @see SoarWorkingMemoryModel#write
     * @see SoarWorkingMemoryModel#writeComments
     */
    public void writeOutHierarchy(File inFileName, File inDataMapName) {
        reduceWorkingMemory();
        try {
            OperatorRootNode orn = (OperatorRootNode) (getModel().getRoot());
            FileWriter fw = new FileWriter(inFileName);
            fw.write("VERSION 4\n");
            // for Version 5:  fw.write("VERSION 5\n");
            String dataMapRP = orn.getDataMapFile().substring(orn.getFullPathStart().length());
            fw.write(dataMapRP + '\n');
            TreeFileWriter.write(fw, (DefaultTreeModel) getModel());
            // for Version 5:  TreeFileWriter.write5(fw,(DefaultTreeModel)getModel());
            fw.close();
            FileWriter graphWriter = new FileWriter(inDataMapName);
            WorkingMemory.write(graphWriter);
            graphWriter.close();

            File commentFile = new File(inDataMapName.getParent() + File.separator + "comment.dm");
            FileWriter commentWriter = new FileWriter(commentFile);
            WorkingMemory.writeComments(commentWriter);
            commentWriter.close();
        } catch (IOException ioe) {
            System.err.println("An Exception was thrown in OperatorWindow.saveHierarchy");
            ioe.printStackTrace();
        }
    }  // end of writeOutHierarchy()


    /**
     * Save entire Operator Hierarchy (including datamap)
     *
     * @see #writeOutHierarchy(File, File)
     */
    public void saveHierarchy() {
        OperatorRootNode orn = (OperatorRootNode) (getModel().getRoot());
        File projectFileName = new File(orn.getProjectFile());
        File dataMapFile = new File(orn.getDataMapFile());
        writeOutHierarchy(projectFileName, dataMapFile);
    }

    /**
     * Attempts to reduce Working Memory by finding all vertices that are unreachable
     * from a state and adds them to a list of holes so that they can be recycled for later use
     *
     * @see SoarWorkingMemoryModel#reduce(java.util.List)
     */
    private void reduceWorkingMemory() {
        List<SoarVertex> vertList = new LinkedList<>();
        Enumeration<TreeNode> e = ((DefaultMutableTreeNode) getModel().getRoot()).breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            OperatorNode on = (OperatorNode) e.nextElement();
            SoarVertex v = on.getStateIdVertex();
            if (v != null) {
                vertList.add(v);
            }
        }
        WorkingMemory.reduce(vertList);
    }

    /**
     * Class used for drag and drop operations
     */
    class OWDragGestureListener implements DragGestureListener {
        public OWDragGestureListener() {
        }

        // methods
        public void dragGestureRecognized(DragGestureEvent e) {
            int action = e.getDragAction();
            TreePath path = getSelectionPath();

            if (e.getTriggerEvent() instanceof MouseEvent) {
                if (((MouseEvent) e.getTriggerEvent()).isPopupTrigger()) {
                    return;
                }
            }

            if (path == null) {
                System.out.println("Nothing selected - beep");
                getToolkit().beep();
                return;
            }
            OperatorNode selection = (OperatorNode) path.getLastPathComponent();

            Transferable t = new TransferableOperatorNodeLink(selection.getId());
            if (action == DnDConstants.ACTION_LINK) {
                DragSource.getDefaultDragSource().startDrag(e, DragSource.DefaultLinkNoDrop, t, new OWDragSourceListener());
            } else if (action == DnDConstants.ACTION_MOVE) {
                DragSource.getDefaultDragSource().startDrag(e, DragSource.DefaultMoveNoDrop, t, new OWDragSourceListener());
            }
        }
    }

    /**
     * Class used for drag and drop operations
     */
    static class OWDragSourceListener implements DragSourceListener {
        // Methods
        public void dragEnter(DragSourceDragEvent e) {
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

    /**
     * Constructs a DefaultTreeModel exactly the way we decided how to do it
     * Creates a root node named after the project name at the root of the tree.
     * Children of that are an 'all' folder node, a tcl file node called '_firstload'
     * and an 'elaborations' folder node.
     * Children of the elaborations folder include two file operator nodes called
     * '_all' and 'top-state'.
     * Also created is the datamap file called <project name> + '.dm'
     *
     * @param projectName name of the project
     * @param projectFile name of project's .vsa file
     * @see OperatorRootNode
     * @see FileOperatorNode
     * @see FolderNode
     */
    private DefaultTreeModel defaultProject(String projectName, File projectFile) {
        File parent = new File(projectFile.getParent() + File.separator + projectName);
        File dataMapFile = new File(parent.getPath() + File.separator + projectName + ".dm");
        File elabFolder = new File(parent.getPath() + File.separator + "elaborations");
        File allFolder = new File(parent.getPath() + File.separator + "all");
        File initFile = new File(parent.getPath() + File.separator
                + "initialize-" + projectName + ".soar");
        File tclFile = new File(parent.getPath() + File.separator + "_firstload.soar");
        parent.mkdir();

        try {
            dataMapFile.createNewFile();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }


        elabFolder.mkdir();
        allFolder.mkdir();

        try {
            //Root node
            OperatorRootNode root = createOperatorRootNode(projectName, parent.getParent(), parent.getName());

            //Elaborations Folder
            FolderNode elaborationsFolderNode = createFolderNode("elaborations", elabFolder.getName());
            File topStateElabsFile = new File(elabFolder, "top-state.soar");
            File allFile = new File(elabFolder, "_all.soar");
            topStateElabsFile.createNewFile();
            allFile.createNewFile();
            writeOutTopStateElabs(topStateElabsFile, projectName);
            writeOutAllElabs(allFile);

            //Initialize File
            initFile.createNewFile();
            writeOutInitRules(initFile, projectName);

            //TCL file
            tclFile.createNewFile();

            //Construct the tree
            root.add(createFileOperatorNode("_firstload", tclFile.getName()));
            root.add(createFolderNode("all", allFolder.getName()));
            root.add(elaborationsFolderNode);
            elaborationsFolderNode.add(createFileOperatorNode("_all", allFile.getName()));
            elaborationsFolderNode.add(createFileOperatorNode("top-state", topStateElabsFile.getName()));
            root.add(createSoarOperatorNode("initialize-" + projectName,
                    initFile.getName()));

            return new DefaultTreeModel(root);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    /**
     * Searches all files in the project for the specified string and returns a
     * Vector of FindInProjectListObjects of all instances
     *
     * @param opNode the operator subtree (which may be the whole project) to search
     */
    public void findInProject(OperatorNode opNode,
                              String stringToFind,
                              boolean matchCase) {
        Enumeration<TreeNode> bfe = opNode.breadthFirstEnumeration();
        Vector<FeedbackListObject> vecErrs = new Vector<>();

        if (!matchCase) {
            stringToFind = stringToFind.toLowerCase();
        }

        while (bfe.hasMoreElements()) {
            OperatorNode current = (OperatorNode) bfe.nextElement();
            String fn = current.getFileName();

            if (fn != null) {
                try {
                    LineNumberReader lnr = new LineNumberReader(new FileReader(fn));
                    String line = lnr.readLine();
                    while (line != null) {
                        if (!matchCase) {
                            line = line.toLowerCase();
                        }
                        if (line.contains(stringToFind)) {
                            vecErrs.add(new FeedbackListObject(current,
                                    lnr.getLineNumber(),
                                    line,
                                    stringToFind));
                        }
                        line = lnr.readLine();
                    }
                    lnr.close();
                } catch (FileNotFoundException fnfe) {
                    System.err.println("Couldn't find: " + fn);
                } catch (IOException ioe) {
                    System.err.println("Error reading from file " + fn);
                }
            }
        }

        if (vecErrs.isEmpty()) {
            vecErrs.add(new FeedbackListObject(stringToFind + " not found in project"));
        }

        MainFrame.getMainFrame().setFeedbackListData(vecErrs);
    }


    /**
     * Searches all files in the project for the specified string and opens
     * the file containing the first instance of that string.
     *
     * @author ThreePenny
     */
    public void findInProjectAndOpenRule(String stringToFind, boolean matchCase) {
        TreeModel model = getModel();
        VSTreeNode root = (VSTreeNode) model.getRoot();
        Enumeration<TreeNode> bfe = root.breadthFirstEnumeration();

        if (!matchCase) {
            stringToFind = stringToFind.toLowerCase();
        }

        while (bfe.hasMoreElements()) {
            OperatorNode current = (OperatorNode) bfe.nextElement();
            String fn = current.getFileName();

            if (fn != null) {
                try {
                    LineNumberReader lnr = new LineNumberReader(new FileReader(fn));
                    String line = lnr.readLine();
                    while (line != null) {
                        if (!matchCase) {
                            line = line.toLowerCase();
                        }
                        if (line.contains(stringToFind)) {
                            // Open the rule
                            current.openRules(MainFrame.getMainFrame(), lnr.getLineNumber());

                            // All done!
                            return;
                        }
                        line = lnr.readLine();
                    }
                    lnr.close();
                } catch (FileNotFoundException fnfe) {
                    System.err.println("Couldn't find: " + fn);
                } catch (IOException ioe) {
                    System.err.println("Error reading from file " + fn);
                }
            }
        }
    }

    /*
     * STI component used to send productions
     * @author ThreePenny
     */
    public void sendProductions(Writer w) throws IOException {
        TreeModel model = getModel();
        VSTreeNode root = (VSTreeNode) model.getRoot();
        Enumeration<TreeNode> bfe = root.breadthFirstEnumeration();
        while (bfe.hasMoreElements()) {
            OperatorNode current = (OperatorNode) bfe.nextElement();
            String fn = current.getFileName();
            if (fn != null) {
                Reader r = new BufferedReader(new FileReader(fn));
                for (int ch = r.read(); ch != -1; ch = r.read()) {
                    w.write(ch);
                }
                w.write('\n');
                r.close();
            }
        }
        w.close();
    }

    /**
     * Reads a Version One .vsa project file and interprets it to create a Visual
     * Soar project from the file.
     *
     * @param r the Reader of the .vsa project file
     * @see #makeNodeVersionOne(Reader)
     */
    private void readVersionOne(Reader r) throws IOException, NumberFormatException {
        // This hash table has keys of ids and a pointer as a value
        // it is used for parent lookup
        Hashtable<Integer, VSTreeNode> ht = new Hashtable<>();

        // Special Case Root Node
        // tree specific stuff
        int rootId = ReaderUtils.getInteger(r);

        // node stuff
        VSTreeNode root = makeNodeVersionOne(r);

        // add the new node to the hash table
        ht.put(rootId, root);

        // Read in all the other nodes
        while (r.ready()) {
            // again read in the tree specific stuff
            int nodeId = ReaderUtils.getInteger(r);
            int parentId = ReaderUtils.getInteger(r);

            // get the parent
            OperatorNode parent = (OperatorNode) ht.get(parentId);

            // read in the node
            OperatorNode node = makeNodeVersionOne(r);
            addChild(parent, node);

            // add that node to the hash table
            ht.put(nodeId, node);
        }
        setModel(new DefaultTreeModel(root));
    }


    /**
     * Reads a Version Five .vsa project file and interprets it to create a Visual
     * Soar project from the file.
     * This version reads the unique ids which are strings consisting of concatenation
     * of parent names as a value.  This method ensures that every id is unique.
     *
     * @param r the Reader of the .vsa project file
     * @see #makeNodeVersionFive(HashMap, java.util.List, Reader, SoarIdentifierVertex)
     */
    private void readVersionFive(Reader r) throws IOException, NumberFormatException {
        // This hash table has keys of ids which are strings consisting of
        // concatenation of parent names and a pointer as a value.
        // It is used for parent lookup
        Hashtable<Integer, VSTreeNode> ht = new Hashtable<>();
        List<VSTreeNode> linkNodesToRestore = new LinkedList<>();
        HashMap<Integer, VSTreeNode> persistentIdLookup = new HashMap<>();
        // Special Case Root Node
        // tree specific stuff
        int rootId = ReaderUtils.getInteger(r);
        SoarIdentifierVertex sv = new SoarIdentifierVertex(0);

        // node stuff
        VSTreeNode root = makeNodeVersionFive(persistentIdLookup, linkNodesToRestore, r, sv);

        // add the new node to the hash table
        ht.put(rootId, root);

        // Read in all the other nodes
        boolean done = false;
        for (; ; ) {
            // again read in the tree specific stuff
            SoarIdentifierVertex parentDataMapId = new SoarIdentifierVertex(0); //reset datamap id to 0, the top level datamap

            String nodeIdOrEnd = ReaderUtils.getWord(r);
            if (!nodeIdOrEnd.equals("END")) {
                int nodeId = Integer.parseInt(nodeIdOrEnd);
                int parentId = ReaderUtils.getInteger(r);

                // try to get DataMapId from the parent in case it is a high level file operator
                //    high level file operators use the dataMap of the next highest (on the tree) regular operator
                if (ht.get(parentId) instanceof SoarOperatorNode) {
                    SoarOperatorNode soarParent = (SoarOperatorNode) ht.get(parentId);
                    parentDataMapId = soarParent.getStateIdVertex();
                }

                OperatorNode node = makeNodeVersionFive(persistentIdLookup, linkNodesToRestore, r, parentDataMapId);
                OperatorNode parent = (OperatorNode) ht.get(parentId);
                addChild(parent, node);
                // add that node to the hash table
                ht.put(nodeId, node);
            } else {
                for (VSTreeNode vsTreeNode : linkNodesToRestore) {
                    LinkNode linkNodeToRestore = (LinkNode) vsTreeNode;
                    linkNodeToRestore.restore(persistentIdLookup);
                }
                setModel(new DefaultTreeModel(root));
                return;
            }
        }
    }

    /**
     * Reads a Version Four .vsa project file and interprets it to create a Visual
     * Soar project from the file.
     *
     * @param r the Reader of the .vsa project file
     * @see #makeNodeVersionFour(HashMap, java.util.List, Reader)
     */
    private void readVersionFour(Reader r) throws IOException, NumberFormatException {
        // This hash table has keys of ids and a pointer as a value
        // it is used for parent lookup
        Hashtable<Integer, VSTreeNode> ht = new Hashtable<>();
        List<VSTreeNode> linkNodesToRestore = new LinkedList<>();
        HashMap<Integer, VSTreeNode> persistentIdLookup = new HashMap<>();

        // Special Case Root Node
        // tree specific stuff
        int rootId = ReaderUtils.getInteger(r);

        // node stuff
        VSTreeNode root = makeNodeVersionFour(persistentIdLookup, linkNodesToRestore, r);

        // add the new node to the hash table
        ht.put(rootId, root);

        // Read in all the other nodes
        boolean done = false;
        for (; ; ) {
            // again read in the tree specific stuff
            SoarIdentifierVertex parentDataMapId = new SoarIdentifierVertex(0);              //reset datamap id to 0, the top level datamap

            String nodeIdOrEnd = ReaderUtils.getWord(r);
            if (!nodeIdOrEnd.equals("END")) {
                int nodeId = Integer.parseInt(nodeIdOrEnd);
                int parentId = ReaderUtils.getInteger(r);


                OperatorNode node = makeNodeVersionFour(persistentIdLookup, linkNodesToRestore, r);
                OperatorNode parent = (OperatorNode) ht.get(parentId);
                addChild(parent, node);
                // add that node to the hash table
                ht.put(nodeId, node);
            } else {
                for (VSTreeNode vsTreeNode : linkNodesToRestore) {
                    LinkNode linkNodeToRestore = (LinkNode) vsTreeNode;
                    linkNodeToRestore.restore(persistentIdLookup);
                }
                setModel(new DefaultTreeModel(root));
                return;
            }
        }
    }

    /**
     * Reads a Version Three .vsa project file and interprets it to create a Visual
     * Soar project from the file.
     *
     * @param r the Reader of the .vsa project file
     * @see #makeNodeVersionThree(HashMap, java.util.List, Reader)
     */
    private void readVersionThree(Reader r) throws IOException, NumberFormatException {
        // This hash table has keys of ids and a pointer as a value
        // it is used for parent lookup
        Hashtable<Integer, VSTreeNode> ht = new Hashtable<>();
        List<VSTreeNode> linkNodesToRestore = new LinkedList<>();
        HashMap<Integer, VSTreeNode> persistentIdLookup = new HashMap<>();

        // Special Case Root Node
        // tree specific stuff
        int rootId = ReaderUtils.getInteger(r);

        // node stuff
        VSTreeNode root = makeNodeVersionThree(persistentIdLookup, linkNodesToRestore, r);

        // add the new node to the hash table
        ht.put(rootId, root);

        // Read in all the other nodes
        boolean done = false;
        for (; ; ) {
            // again read in the tree specific stuff
            String nodeIdOrEnd = ReaderUtils.getWord(r);
            if (!nodeIdOrEnd.equals("END")) {
                int nodeId = Integer.parseInt(nodeIdOrEnd);
                int parentId = ReaderUtils.getInteger(r);

                // get the parent
                OperatorNode parent = (OperatorNode) ht.get(parentId);

                // read in the node
                OperatorNode node = makeNodeVersionThree(persistentIdLookup, linkNodesToRestore, r);
                addChild(parent, node);

                // add that node to the hash table
                ht.put(nodeId, node);
            } else {
                for (VSTreeNode vsTreeNode : linkNodesToRestore) {
                    LinkNode linkNodeToRestore = (LinkNode) vsTreeNode;
                    linkNodeToRestore.restore(persistentIdLookup);
                }
                setModel(new DefaultTreeModel(root));
                return;
            }
        }
    }

    /**
     * Reads a Version Two .vsa project file and interprets it to create a Visual
     * Soar project from the file.
     *
     * @param r the Reader of the .vsa project file
     * @see #makeNodeVersionTwo(HashMap, java.util.List, Reader)
     */
    private void readVersionTwo(Reader r) throws IOException, NumberFormatException {
        // This hash table has keys of ids and a pointer as a value
        // it is used for parent lookup
        Hashtable<Integer, VSTreeNode> ht = new Hashtable<>();
        List<VSTreeNode> linkNodesToRestore = new LinkedList<>();
        HashMap<Integer, VSTreeNode> persistentIdLookup = new HashMap<>();

        // Special Case Root Node
        // tree specific stuff
        int rootId = ReaderUtils.getInteger(r);

        // node stuff
        VSTreeNode root = makeNodeVersionTwo(persistentIdLookup, linkNodesToRestore, r);

        // add the new node to the hash table
        ht.put(rootId, root);

        // Read in all the other nodes
        while (r.ready()) {
            // again read in the tree specific stuff
            int nodeId = ReaderUtils.getInteger(r);
            int parentId = ReaderUtils.getInteger(r);

            // get the parent
            OperatorNode parent = (OperatorNode) ht.get(parentId);

            // read in the node
            OperatorNode node = makeNodeVersionTwo(persistentIdLookup, linkNodesToRestore, r);

            addChild(parent, node);

            // add that node to the hash table
            ht.put(nodeId, node);
        }
        for (VSTreeNode vsTreeNode : linkNodesToRestore) {
            LinkNode linkNodeToRestore = (LinkNode) vsTreeNode;
            linkNodeToRestore.restore(persistentIdLookup);
        }
        setModel(new DefaultTreeModel(root));
    }


    /**
     * Opens a Visual Soar project by creating the appropriate node
     *
     * @param linkedToMap        hashmap used to keep track of linked nodes, not used
     * @param linkNodesToRestore list of linked nodes needed to restore, not used
     * @param r                  .vsa file that is being read to open project
     * @param parentDataMap      parent of created nodes datamap id
     * @return the created OperatorNode
     * @see OperatorNode
     * @see #readVersionFive(Reader)
     */
    private OperatorNode makeNodeVersionFive(HashMap<Integer, VSTreeNode> linkedToMap, List<VSTreeNode> linkNodesToRestore, Reader r, SoarIdentifierVertex parentDataMap) throws IOException, NumberFormatException {
        OperatorNode retVal;
        String type = ReaderUtils.getWord(r);

        if (type.equals("HLOPERATOR")) {
            retVal = createSoarOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getInteger(r));
        } else if (type.equals("OPERATOR")) {
            retVal = createSoarOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("HLFOPERATOR")) {
            // High level file operators use the parent operators dataMap
            retVal = createFileOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getWord(r), parentDataMap);
            ReaderUtils.getInteger(r);
        } else if (type.equals("FOPERATOR")) {
            retVal = createFileOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("HLIOPERATOR")) {
            retVal = createImpasseOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getInteger(r));
        } else if (type.equals("IOPERATOR")) {
            retVal = createImpasseOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("FOLDER")) {
            retVal = createFolderNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("FILE")) {
            retVal = createFileOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("ROOT")) {
            retVal = createOperatorRootNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("LINK")) {
            retVal = createLinkNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getInteger(r));
            linkNodesToRestore.add(retVal);
        } else {
            throw new IOException("Parse Error");
        }

        if (retVal != null) {
            linkedToMap.put(ReaderUtils.getInteger(r), retVal);
        }
        return retVal;
    }

    /**
     * Opens a Visual Soar project by creating the appropriate node
     *
     * @param linkedToMap        hashmap used to keep track of linked nodes, not used
     * @param linkNodesToRestore list of linked nodes needed to restore, not used
     * @param r                  .vsa file that is being read to open project
     * @return the created OperatorNode
     * @see OperatorNode
     * @see #readVersionFour(Reader)
     */
    private OperatorNode makeNodeVersionFour(HashMap<Integer, VSTreeNode> linkedToMap, List<VSTreeNode> linkNodesToRestore, Reader r) throws IOException, NumberFormatException {
        OperatorNode retVal;
        String type = ReaderUtils.getWord(r);

        if (type.equals("HLOPERATOR")) {
            retVal = createSoarOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getInteger(r));
        } else if (type.equals("OPERATOR")) {
            retVal = createSoarOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("HLFOPERATOR")) {
            retVal = createFileOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getInteger(r));
        } else if (type.equals("FOPERATOR")) {
            retVal = createFileOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("HLIOPERATOR")) {
            retVal = createImpasseOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getInteger(r));
        } else if (type.equals("IOPERATOR")) {
            retVal = createImpasseOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("FOLDER")) {
            retVal = createFolderNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("FILE")) {
            retVal = createFileOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("ROOT")) {
            retVal = createOperatorRootNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("LINK")) {
            retVal = createLinkNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getInteger(r));
            linkNodesToRestore.add(retVal);
        } else {
            throw new IOException("Parse Error");
        }

        if (retVal != null) {
            linkedToMap.put(ReaderUtils.getInteger(r), retVal);
        }
        return retVal;
    }

    /**
     * Opens a Visual Soar project by creating the appropriate node
     *
     * @param linkedToMap        hashmap used to keep track of linked nodes, not used
     * @param linkNodesToRestore list of linked nodes needed to restore, not used
     * @param r                  .vsa file that is being read to open project
     * @return the created OperatorNode
     * @see OperatorNode
     * @see #readVersionThree(Reader)
     */
    private OperatorNode makeNodeVersionThree(HashMap<Integer, VSTreeNode> linkedToMap, List<VSTreeNode> linkNodesToRestore, Reader r) throws IOException, NumberFormatException {
        OperatorNode retVal;
        String type = ReaderUtils.getWord(r);
        if (type.equals("HLOPERATOR")) {
            retVal = createSoarOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getInteger(r));
        } else if (type.equals("OPERATOR")) {
            retVal = createSoarOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("FOLDER")) {
            retVal = createFolderNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("FILE")) {
            retVal = createFileNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("ROOT")) {
            retVal = createOperatorRootNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("LINK")) {
            retVal = createLinkNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getInteger(r));
            linkNodesToRestore.add(retVal);
        } else {
            throw new IOException("Parse Error");
        }

        if (retVal != null) {
            linkedToMap.put(ReaderUtils.getInteger(r), retVal);
        }
        return retVal;
    }

    /**
     * Opens a Visual Soar project by creating the appropriate node
     *
     * @param linkedToMap        hashmap used to keep track of linked nodes, not used
     * @param linkNodesToRestore list of linked nodes needed to restore, not used
     * @param r                  .vsa file that is being read to open project
     * @return the created OperatorNode
     * @see OperatorNode
     * @see #readVersionTwo(Reader)
     */
    private OperatorNode makeNodeVersionTwo(HashMap<Integer, VSTreeNode> linkedToMap, List<VSTreeNode> linkNodesToRestore, Reader r) throws IOException, NumberFormatException {
        OperatorNode retVal;
        String type = ReaderUtils.getWord(r);
        if (type.equals("HLOPERATOR")) {
            retVal = createSoarOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getInteger(r));
        } else if (type.equals("OPERATOR")) {
            retVal = createSoarOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("FOLDER")) {
            retVal = createFolderNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("FILE")) {
            retVal = createFileNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("ROOT")) {
            retVal = createOperatorRootNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("LINK")) {
            retVal = createLinkNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getInteger(r));
            linkNodesToRestore.add(retVal);
        } else {
            throw new IOException("Parse Error");
        }

        if (retVal != null) {
            linkedToMap.put(ReaderUtils.getInteger(r), retVal);
        }
        return retVal;
    }

    /**
     * Opens a Visual Soar project by creating the appropriate node
     *
     * @param r .vsa file that is being read to open project
     * @return the created OperatorNode
     * @see OperatorNode
     * @see #readVersionOne(Reader)
     */
    private OperatorNode makeNodeVersionOne(Reader r) throws IOException, NumberFormatException {
        String type = ReaderUtils.getWord(r);
        if (type.equals("HLOPERATOR")) {
            return createSoarOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getInteger(r));
        } else if (type.equals("OPERATOR")) {
            return createSoarOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("FOLDER")) {
            return createFolderNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("FILE")) {
            return createFileNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else if (type.equals("ROOT")) {
            return createOperatorRootNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        } else {
            throw new IOException("Parse Error");
        }
    }


    private static boolean treePathSubset(TreePath set, TreePath subset) {

        if (subset.getPathCount() > set.getPathCount()) {
            return false;
        }

        boolean difference = false;
        for (int i = 0; i < subset.getPathCount() && !difference; ++i) {
            String stringSet = set.getPathComponent(i).toString();
            String stringSubset = subset.getPathComponent(i).toString();
            if (!stringSet.equals(stringSubset)) {
                difference = true;
            }
        }
        return !difference;
    }

    private OperatorNode getNodeForId(int id) {
        Enumeration<TreeNode> nodeEnum = ((OperatorNode) getModel().getRoot()).breadthFirstEnumeration();
        while (nodeEnum.hasMoreElements()) {
            OperatorNode operatorNode = (OperatorNode) nodeEnum.nextElement();
            if (operatorNode.getId() == id) {
                return operatorNode;
            }
        }
        return null;
    }

    /**
     * Writes out the default productions in the top-state.soar file
     *
     * @param fileToWriteTo the top-state.soar file
     * @param topStateName  the name of the project/top state
     */
    private void writeOutTopStateElabs(File fileToWriteTo, String topStateName) throws IOException {
        Writer w = new FileWriter(fileToWriteTo);
        w.write("sp {elaborate*top-state*top-state\n");
        w.write("   (state <s> ^superstate nil)\n");
        w.write("-->\n");
        w.write("   (<s> ^top-state <s>)\n");
        w.write("}\n");
        w.write("\n");
        w.close();
    }

    /**
     * Writes out the default productions in the _all.soar file
     *
     * @param fileToWriteTo the _all.soar file
     */
    private void writeOutAllElabs(File fileToWriteTo) throws IOException {
        Writer w = new FileWriter(fileToWriteTo);
        w.write("sp {elaborate*state*name\n");
        w.write("   (state <s> ^superstate.operator.name <name>)\n");
        w.write("-->\n");
        w.write("   (<s> ^name <name>)\n");
        w.write("}\n\n");
        w.write("sp {elaborate*state*top-state\n");
        w.write("   (state <s> ^superstate.top-state <ts>)\n");
        w.write("-->\n");
        w.write("   (<s> ^top-state <ts>)\n");
        w.write("}\n\n");
        //w.write();
        w.close();
    }


    /**
     * Writes out the default productions in the _all.soar file
     *
     * @param fileToWriteTo the _all.soar file
     */
    private void writeOutInitRules(File fileToWriteTo, String topStateName) throws IOException {
        Writer w = new FileWriter(fileToWriteTo);
        w.write("sp {propose*initialize-" + topStateName + "\n");
        w.write("   (state <s> ^superstate nil\n");
        w.write("             -^name)\n");
        w.write("-->\n");
        w.write("   (<s> ^operator <o> +)\n");
        w.write("   (<o> ^name initialize-" + topStateName + ")\n");
        w.write("}\n");
        w.write("\n");
        w.write("sp {apply*initialize-" + topStateName + "\n");
        w.write("   (state <s> ^operator <op>)\n");
        w.write("   (<op> ^name initialize-" + topStateName + ")\n");
        w.write("-->\n");
        w.write("   (<s> ^name " + topStateName + ")\n");
        w.write("}\n\n");

        w.close();
    }

    /**
     * Responsible for keeping track of the backup project files
     */
    class BackupThread extends Thread {
        Runnable writeOutControl;

        public BackupThread() {
            writeOutControl = new Runnable() {
                public void run() {
                    if (!closed) {
                        OperatorRootNode orn = (OperatorRootNode) (getModel().getRoot());
                        File projectFileName = new File(orn.getProjectFile() + "~");
                        File dataMapFile = new File(orn.getDataMapFile() + "~");
                        writeOutHierarchy(projectFileName, dataMapFile);
                    }
                }
            };

        }

        public void run() {
            while (!closed) {
                try {
                    sleep(60000 * 3);  // 3 minutes
                    SwingUtilities.invokeAndWait(writeOutControl);
                } catch (InterruptedException | InvocationTargetException ie) { /* don't care */ }
            }
        }//run
    }//end of BackupThread class

}   // end of OperatorWindow class
