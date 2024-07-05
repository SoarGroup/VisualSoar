package edu.umich.soar.visualsoar.operatorwindow;


import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryReader;
import edu.umich.soar.visualsoar.dialogs.FindInProjectDialog;
import edu.umich.soar.visualsoar.dialogs.NameDialog;
import edu.umich.soar.visualsoar.dialogs.ReplaceInProjectDialog;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.graph.SoarVertex;
import edu.umich.soar.visualsoar.misc.FeedbackEntryOpNode;
import edu.umich.soar.visualsoar.misc.FeedbackListEntry;
import edu.umich.soar.visualsoar.misc.Prefs;
import edu.umich.soar.visualsoar.misc.Template;
import edu.umich.soar.visualsoar.parser.ParseException;
import edu.umich.soar.visualsoar.parser.SoarProduction;
import edu.umich.soar.visualsoar.parser.TokenMgrError;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;
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
    private SoarWorkingMemoryModel workingMemory;
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

        //Autobackup
        if (! MainFrame.getMainFrame().isReadOnly()) {
            new BackupThread().start();
        }
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
            workingMemory = new SoarWorkingMemoryModel(true, projectName);
        }
    }

    /**
     * Opens an OperatorWindow for an existing project
     *
     * @param in_file the location of the project to be opened
     * @param readOnly is this project being opened in read-only mode?
     * @see SoarWorkingMemoryModel
     * @see OperatorWindow#openHierarchy(File)
     */
    public OperatorWindow(File in_file, boolean readOnly) throws NumberFormatException, IOException {
        this();
        s_OperatorWindow = this;
        workingMemory = new SoarWorkingMemoryModel(false, null);
        openHierarchy(in_file);
        Prefs.addRecentProject(in_file, readOnly);
        MainFrame.getMainFrame().setStatusBarMsg("Opened " + in_file.getName());
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
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) getModel().getRoot();
        return node.getChildCount();
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
    public void addSubOperator() {
        //Ask the user for the new operator's name
        String opName;
        NameDialog theDialog = new NameDialog(MainFrame.getMainFrame());
        theDialog.setTitle("Enter Operator Name");
        theDialog.setVisible(true);
        if (! theDialog.wasApproved()) return;  //User hit Cancel button
        opName = theDialog.getText();

        //Find the new child operator's location in the operator hierarchy
        TreePath tp = getSelectionPath();
        if (tp == null) return; //should never happen
        OperatorNode parent = (OperatorNode) tp.getLastPathComponent();
        OperatorNode child = null;

        //Attempt to add the new operator
        try {
            child = parent.addSubOperator(this, workingMemory, opName);
        } catch (IOException ioe) {
            //error will be reported below
        }

        //Report failure to user.
        if (child == null) {
            JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                    "Could not create sub-operator, name may be invalid",
                    "I/O Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Expand to the parent so child is visible.  (How could it not be?)
        tp = new TreePath(parent.getPath());
        if (parent.getChildCount() != 0) {  //should always be true
            expandPath(tp);
        }

        //Create a rule editor for the new, empty operator
        RuleEditor re = child.openRules(MainFrame.getMainFrame());

        //Insert the 'propose' and 'apply' templates
        Template parentTemplate = MainFrame.getMainFrame().getTemplateManager().getRootTemplate();
        Template proposeTemplate = null;
        Template applyTemplate = null;
        Iterator<Template> iter = parentTemplate.getChildTemplates();
        while (iter.hasNext()) {
            Template t = iter.next();
            if (t.getName().equals("propose-operator")) proposeTemplate = t;
            if (t.getName().equals("apply-operator")) applyTemplate = t;
        }
        if (proposeTemplate != null) re.insertTemplate(proposeTemplate);
        if (applyTemplate != null) re.insertTemplate(applyTemplate);

    }//addSubOperator

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
                node.importFunc(new FileReader(file), this, workingMemory);
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
                parent.addFileOperator(this, workingMemory, s);

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
     * Adds a deletable folder object.  The files/operators within it are tested
     * against the top-state data map.
     *
     * @author Andrew Nuxoll
     * added 07 Feb 2024
     */
    public void addTopFolder() {
        //Ask the user to enter a name for this folder
        NameDialog theDialog = new NameDialog(MainFrame.getMainFrame());
        theDialog.setTitle("Enter Folder Name");
        theDialog.setVisible(true);
        if (! theDialog.wasApproved()) return;

        //get the node in the operator pane tree that this was invoked upon
        //Note:  at the time this code was written, it should only be invoked
        //       on the root node, but I'm trying to be flexible for possible
        //       future expansion.
        String folderName = theDialog.getText();
        TreePath tp = getSelectionPath();
        if (tp == null) {
            return; //should never happen
        }
        OperatorNode parent = (OperatorNode) tp.getLastPathComponent();

        //Try to create the new folder on the drive
        String folderPath = parent.getFullPathName() + File.separator + folderName;
        File newFolderFile = new File(folderPath);
        if (! newFolderFile.mkdir()) {
            JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                    "Unable to create folder: " + folderPath,
                    "Creation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Now add the folder node to the Operator Pane
        FolderNode folderNode = createFolderNode(folderName, newFolderFile.getName());
        this.addChild(parent, folderNode);

        //TODO: redraw the operator pane so the new folder appears


    }//addTopFolder


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
            parent = parent.addImpasseOperator(this, workingMemory, s);

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
                                 List<FeedbackListEntry> errors) {

        // Find the state that these productions should be checked against
        SoarIdentifierVertex siv = parent.getStateIdVertex();
        if (siv == null) {
            siv = workingMemory.getTopstate();
        }
        Enumeration<SoarProduction> prodEnum = productions.elements();

        while (prodEnum.hasMoreElements()) {
            SoarProduction sp = prodEnum.nextElement();
            errors.addAll(workingMemory.checkProduction(child, siv, sp));
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
        selNode.openDataMap(workingMemory, MainFrame.getMainFrame());
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
        return workingMemory;
    }

    /**
     * removes the selected node from the tree
     */
    public void delete() {
        //Figure out which node is currently selected
        TreePath tp = getSelectionPath();
        if (tp == null) {
            return; //should never happen
        }
        OperatorNode selNode = (OperatorNode) tp.getLastPathComponent();
        getModel(); //unnecessary?

        //Delete procedure varies by node type
        if (selNode instanceof FileNode) {
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " +
                    selNode + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION)) {
                selNode.closeEditors();
                selNode.delete(this);
            }
        } else if ((selNode instanceof FolderNode) && selNode.toString().equals("common")) {
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " +
                    selNode + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION)) {
                selNode.delete(this);
            }
        }
        else if ((selNode instanceof FolderNode)) {
            //Refuse to delete folders that contain files
            if (selNode.children().hasMoreElements()) {
                JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                        "A non-empty folder may not be deleted.",
                        "Can't Delete",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            selNode.delete(this);
        }
        else {
            getToolkit().beep();
        }



    }//delete

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
        Vector<FeedbackListEntry> vecErrors = new Vector<>();
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
                vecErrors.add(new FeedbackListEntry("Unable to check productions due to parse error"));
                vecErrors.add(currentNode.parseParseException(pe));
            } catch (IOException ioe) {
                String msg = currentNode.getFileName() + "(1): " + " Error reading file.";
                vecErrors.add(new FeedbackListEntry(msg));
            }
        }//while
        if (vecErrors.isEmpty()) {
            vecErrors.add(new FeedbackListEntry("No errors detected in children."));
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
     *                       list of FeedbackListEntry objects for easy reporting
     */
    public void generateDataMap(OperatorNode opNode,
                                Vector<FeedbackListEntry> parseErrors,
                                Vector<FeedbackListEntry> vecGenerations) {
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
            parseErrors.add(new FeedbackListEntry("Unable to generate datamap due to parse error"));
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
            siv = workingMemory.getTopstate();
        }

        //Generate the new datamap entries
        Enumeration<SoarProduction> prodEnum = parsedProds.elements();
        while (prodEnum.hasMoreElements()) {
            SoarProduction sp = prodEnum.nextElement();
            vecGenerations.addAll(workingMemory.checkGenerateProduction(siv, sp, opNode));
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
    public void generateDataMapForOneError(FeedbackEntryOpNode errToFix,
                                           Vector<FeedbackListEntry> vecGenerations) {
        OperatorNode opNode = errToFix.getNode();
        if (opNode == null) {
            vecGenerations.add(new FeedbackListEntry("No operator associated with this entry."));
            return;
        }

        //Parse all the productions in the file
        Vector<SoarProduction> parsedProds;
        try {
            parsedProds = opNode.parseProductions();
        } catch (Exception e) {
            //should never happen...
            vecGenerations.add(new FeedbackListEntry("Unable to generate datamap entry due to parse error."));
            return;
        }

        // Find the datamap that these productions should be checked against
        OperatorNode parentNode = (OperatorNode) opNode.getParent();
        SoarIdentifierVertex siv = parentNode.getStateIdVertex();
        if (siv == null) {
            siv = workingMemory.getTopstate();
        }

        //Generate the new datamap entries
        Enumeration<SoarProduction> prodEnum = parsedProds.elements();
        while (prodEnum.hasMoreElements()) {
            SoarProduction sp = prodEnum.nextElement();
            //we only care about the production that caused the error

            if (errToFix.getProdName().equals(sp.getName())) {
                Vector<FeedbackListEntry> errs =
                        workingMemory.checkGenerateSingleEntry(siv, sp, opNode, errToFix);

                //The errs list should not be empty
                if (errs.isEmpty()) {
                    FeedbackEntryOpNode entry = new FeedbackEntryOpNode(opNode, errToFix.getLine(),
                            "Datamap entry operation failed.");
                    entry.setCanFix(false);
                    errs.add(entry);
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
            SoarWorkingMemoryReader.read(workingMemory, r, rComment);
            rComment.close();
        } else {
            SoarWorkingMemoryReader.read(workingMemory, r, null);
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
            SoarWorkingMemoryReader.read(workingMemory, r, rComment);
            rComment.close();
        } else {
            SoarWorkingMemoryReader.read(workingMemory, r, null);
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
            SoarWorkingMemoryReader.read(workingMemory, r, rComment);
            rComment.close();
        } else {
            SoarWorkingMemoryReader.read(workingMemory, r, null);
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
            SoarWorkingMemoryReader.read(workingMemory, r, rComment);
            rComment.close();
        } else {
            SoarWorkingMemoryReader.read(workingMemory, r, null);
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
            SoarWorkingMemoryReader.read(workingMemory, r, rComment);
            rComment.close();
        } else {
            SoarWorkingMemoryReader.read(workingMemory, r, null);
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
                    son.restoreId(workingMemory);
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
     * @param inProjFile    name of the file to be saved - .vsa file
     * @param inDataMapFile name of the datamap file - .dm file
     * @param inCommentFile name of the datamap comment file - comment.dm
     *
     * @see #reduceWorkingMemory()
     * @see TreeFileWriter#write
     * @see SoarWorkingMemoryModel#write
     * @see SoarWorkingMemoryModel#writeComments
     */
    public void writeOutHierarchy(File inProjFile, File inDataMapFile, File inCommentFile) {
        reduceWorkingMemory();
        try {
            OperatorRootNode orn = (OperatorRootNode) (getModel().getRoot());
            FileWriter fw = new FileWriter(inProjFile);
            fw.write("VERSION 4\n");
            // for Version 5:  fw.write("VERSION 5\n");
            String dataMapRP = orn.getDataMapFile().substring(orn.getFullPathStart().length());
            fw.write(dataMapRP + '\n');
            TreeFileWriter.write(fw, (DefaultTreeModel) getModel());
            // for Version 5:  TreeFileWriter.write5(fw,(DefaultTreeModel)getModel());
            fw.close();
            FileWriter graphWriter = new FileWriter(inDataMapFile);
            workingMemory.write(graphWriter);
            graphWriter.close();

            FileWriter commentWriter = new FileWriter(inCommentFile);
            workingMemory.writeComments(commentWriter);
            commentWriter.close();
        } catch (IOException ioe) {
            System.err.println("An Exception was thrown in OperatorWindow.saveHierarchy");
            ioe.printStackTrace();
        }
    }  // end of writeOutHierarchy()


    /**
     * Save entire Operator Hierarchy (including datamap)
     *
     * @see #writeOutHierarchy
     */
    public void saveHierarchy() {
        OperatorRootNode orn = (OperatorRootNode) (getModel().getRoot());
        File projectFileName = new File(orn.getProjectFile());
        File dataMapFile = new File(orn.getDataMapFile());
        String commentFN = dataMapFile.getParent() + File.separator + "comment.dm";
        File commentFile = new File(commentFN);
        writeOutHierarchy(projectFileName, dataMapFile, commentFile);
    }

    /**
     * Attempts to reduce Working Memory by finding all vertices that are unreachable
     * from a state and adds them to a list of holes so that they can be recycled for later use
     *
     * @see SoarWorkingMemoryModel#reduce(java.util.List)
     */
    public void reduceWorkingMemory() {
        List<SoarVertex> vertList = new LinkedList<>();
        Enumeration<TreeNode> e = ((DefaultMutableTreeNode) getModel().getRoot()).breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            OperatorNode on = (OperatorNode) e.nextElement();
            SoarVertex v = on.getStateIdVertex();
            if (v != null) {
                vertList.add(v);
            }
        }
        workingMemory.reduce(vertList);
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
        Vector<FeedbackListEntry> vecErrs = new Vector<>();

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
                            vecErrs.add(new FeedbackEntryOpNode(current,
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
            vecErrs.add(new FeedbackListEntry(stringToFind + " not found in project"));
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
     * helper method for integrityCheck() methods to extract the highest
     * numbered operator node id from the lines of the file.
     *
     * @param lines a list of lines that may contain operator ids.  There are
     *              presumed to be no blank lines and all lines should already
     *              be trimmed.
     *
     * @return the highest node found or -1 if none found
     */
    private int getLastOpIdFromLines(Vector<String> lines) {
        int lastId = -1;
        for(String line : lines) {
            String[] words = line.split("[ \\t]");  //split on spaces and tabs
            int nodeId = -1;
            try {
                nodeId = Integer.parseInt(words[0]);
            } catch (NumberFormatException nfe) {
                //just ignore this line.  This method does not report parse errors
            }

            if (nodeId > lastId) lastId = nodeId;
        }//for

        return lastId;

    }//getLastOpIdFromLines


    /**
     * helper method for integrityCheckV4.  It reads through a list of lines
     * that contain operator node definitions and places them in the proper
     * slots in an array.  Dummy entries are inserted for missing nodes
     *
     * @param blankless  original lines from the file that should contain
     *                   operator node definitions
     * @param errors    Any missing or duplicate nodes are reported here.
     *
     *
     * @return an array with exactly one entry for each node
     */
    private String[] fillOperatorNodeLineArray(Vector<String> blankless,
                                               Vector<FeedbackListEntry> errors ) {
        //Get the id number of the last valid line in the file.  This should also tell us the number of nodes
        int lastId = getLastOpIdFromLines(blankless);
        if (lastId < 0) return null;  //no operator node ids found!

        //Since we now have an exact number of nodes, create an array to store each associated line
        //This will allow us to detect duplicate and missing operator nodes
        String[] nodeLines = new String[lastId + 1];  //+1 because id numbers start at zero
        for(int i = 0; i < blankless.size(); ++i) {
            String line = blankless.get(i);

            //Extract the node id from the line
            String[] words = line.split("[ \\t]");  //split on spaces and tabs
            int foundNodeId = -1;
            boolean errFlag = false;
            try {
                foundNodeId = Integer.parseInt(words[0]);
            } catch (NumberFormatException nfe) {
                errFlag = true;
            }

            //Check for mismatch or invalid id number
            if (foundNodeId != i) {
                errFlag = true;
            }

            //report any unexpected node id
            if (errFlag) {
                String err = "Found invalid operator node id: " + foundNodeId;
                err += " from this entry: " + line;
                errors.add(new FeedbackListEntry(err));
            }

            //Check for duplicate id
            if (nodeLines[foundNodeId] != null) {
                String err = "Skipping duplicate entry for operator node with id: " + foundNodeId;
                err += " original entry: " + nodeLines[foundNodeId];
                err += " duplicate entry: " + line;
                errors.add(new FeedbackListEntry(err));
            }
            else {
                nodeLines[foundNodeId] = line;
            }
        }//for

        //Detect if any node ids are missing
        for(int i = 0; i < nodeLines.length; ++i) {
            if (nodeLines[i] == null) {
                String err = "No entry found for operator node with id: " + i + ".";
                err += " A dummy entry will be substituted.";
                errors.add(new FeedbackListEntry(err));

                nodeLines[i] = "" + i + "\t0\tFOPERATOR dummy" + i + "  dummy" + i + ".soar 0";
            }
        }

        return nodeLines;
    }//fillOperatorNodeLineArray

    /** given a mal-formatted operator node string, this method tries to find
     * a valid identifier string in it
     */
    private String findIdentifier(String nodeLine) {
        String[] words = nodeLine.split("[ \\t]");
        for(String word : words) {
            //doesn't begin with a letter
            if (! Character.isAlphabetic(word.charAt(0))) continue;

            //check for invalid letters
            boolean invalidChar = false;
            for(int i = 0; i < word.length(); ++i) {
                char c = word.charAt(i);
                if (Character.isLetterOrDigit(c)) continue;
                if (c == '_') continue;
                invalidChar = true;
                break;
            }
            if (invalidChar) continue;

            //check for reserved words
            boolean conflict = false;
            for(String nodeTypeStr : OperatorNode.VSA_NODE_TYPES) {
                if (word.equals(nodeTypeStr)) {
                    conflict = true;
                    break;
                }
            }
            if (conflict) continue;

            //If we get this far it's a valid identifier string.
            // Heuristically, the first one we find is likely correct so use it
            return word;
        }//for

        //no valid id name found
        return null;
    }//findIdentifier



    /**
     * scans each node line of a given version 4 .vsa file for integrity issues
     *
     * @param lines  the lines of the .vsa file. The first two lines of the
     *               file (version number and relative path to .dm file) are
     *               presumed to be absent.
     * @param errors any errors found will be placed in this vector
     *
     * @return the valid operator node lines of the file (or null on unrecoverable failure)
     */
    private String[] integrityCheckV4(Vector<String> lines, Vector<FeedbackListEntry> errors) {
        //The last line should be 'END'.
        String lastLine = lines.lastElement().trim();
        if (! lastLine.equals("END")) {
            errors.add(new FeedbackListEntry("[line " + lines.size() + "] Project file (.vsa) is truncated.  Some operator nodes may be missing."));
        }
        lines.remove(lines.size() - 1);  //remove "END" so remaining lines are consistent

        //Remove any blank lines
        int skipped = 2; //number of lines skipped so far (counting the version num and .dm file name)
        Vector<String> blankless = new Vector<>();
        for(int i = 0; i < lines.size(); ++i) {
            String line = lines.get(i).trim();
            if (line.length() == 0) {
                errors.add(new FeedbackListEntry("[line " + (i+skipped+1) + "] illegal blank line ignored."));
                continue;
            }
            blankless.add(line);
            skipped++;
        }

        //The lines should be numbered sequentially with no blank lines but things may be jumbled.
        //Sort things out and make sure there is exactly one line for each id
        String[] nodeLines = fillOperatorNodeLineArray(blankless, errors);
        if (nodeLines == null) return null;

        //Verify the ROOT node's format
        String[] words = nodeLines[0].split("[ \\t]");
        if ( (words.length != 5)
                || (! words[0].trim().equals("0"))
                || (! words[1].trim().equals("ROOT"))
                || (! words[2].equals(words[3])) ) {
            String err = "Root node has improper format.";
            err += "  Expecting '0\\tROOT <name> <name> 1'";
            err += "  Received '" + nodeLines[0] + "' instead.";
            errors.add(new FeedbackListEntry(err));

            //Replace this node with something in the correct format
            String projName = findIdentifier(nodeLines[0]);
            if (projName == null) projName = "Unknown_Project";
            nodeLines[0] = "0\tROOT " + projName + " " + projName + " 1";
        }

        return nodeLines;

    }//integrityCheckV4

    /**
     * readVersionFourSafe
     *
     * is a "safe" version of {@link #readVersionFour} that is better able to recover from corrupted
     * input files
     *
     * @param r the Reader of the .vsa project file
     * @see #makeNodeVersionFour(HashMap, java.util.List, Reader)
     */
    private void readVersionFourSafe(Reader r) {
        // This hash table has keys of ids and a pointer as a value
        // it is used for parent lookup
        Hashtable<Integer, VSTreeNode> nodeTable = new Hashtable<>();

        //Read in the entire file
        Vector<String> lines = new Vector<>();
        Scanner scan = new Scanner(r);
        while(scan.hasNextLine()) {
            lines.add(scan.nextLine());
        }

        //Report any file format problems.  This will also return an
        //array of all lines that describe an operator node
        Vector<FeedbackListEntry> errors = new Vector<>();
        String[] nodeLines = integrityCheckV4(lines, errors);
        if (errors.size() > 0) {
            MainFrame.getMainFrame().setFeedbackListData(errors);
        }
        if (nodeLines == null) return;

        //Special Case: Root Node
        String[] words = nodeLines[0].split("[ \\t]");
        VSTreeNode root = createOperatorRootNode(words[2], words[2]);
        nodeTable.put(0, root);

        //Parse all the other nodes
        for(int i = 1; i < nodeLines.length; ++i) {
            OperatorNode node = makeNodeVersionFourSafe(i, nodeLines[i], errors);

            //add the node to the tree
            int parentId = Integer.parseInt(words[1]);
            OperatorNode parent = (OperatorNode) nodeTable.get(parentId);
            addChild(parent, node);

            // add the node to the hash table
            nodeTable.put(i, node);
        }

        setModel(new DefaultTreeModel(root));

    }//readVersionFourSafe

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
    }//makeNodeVersionFour


    /**
     * extract the node type from an operator node line in a .vsa file that's
     * mis-formatted in some way.  If no node type is found, OPERATOR is returned.
     * This is a helper method for {@link #reconstructOpNodeLine}
     *
     * @return one member of OperatorNode.VSA_NODE_TYPES.
     */
    public String extractNodeType(String line) {
        String[] words = line.split("[ \\t]");  //split on spaces and tabs

        //If we reach this point, something is amiss
        //Search for the node type in a more robust manner
        String nodeType = OperatorNode.VSA_NODE_TYPES[0];  //default is "OPERATOR"
        int typeIndex = -1;
        String matchLevel = "None";  //how good a match was found so far
        for(String possibleType : OperatorNode.VSA_NODE_TYPES) {
            //Skip types that shouldn't be there in V4 .vsa file
            if (possibleType.equals("ROOT")) continue;
            if (possibleType.equals("LINK")) continue;
            if (possibleType.equals("FILE")) continue;

            //See if this type is present as a separate word (wrong index)
            for(String word : words) {
                if (word.equals(possibleType)) {
                    nodeType = possibleType;
                    typeIndex = line.indexOf(possibleType);
                    matchLevel = "CorrectCaseWord";
                    break;
                }

                //perhaps it was accidentally lowercased
                if (word.equalsIgnoreCase(possibleType)) {
                    nodeType = possibleType;
                    typeIndex = line.indexOf(possibleType);
                    matchLevel = "WrongCaseWord";
                }
            }
            if (matchLevel.contains("Word")) break;  //a better match won't be found

            //Perhaps it's mashed in with different words (no space separation)
            typeIndex = line.indexOf(possibleType);
            if (typeIndex != -1) {
                nodeType = possibleType;
                matchLevel = "mashed";
                break;
            }

            //Note:  not checking for words that are both lowercased and smashed as
            //"folder" is rather general and might yield a gross misinterpretation
        }//for

        //TODO:  could do partial matching.  This seems like overkill atm

        return nodeType;
    }//extractNodeType

    /**
     * try to find the node contents filename in a mis-formatted operator node line from a .vsa file
     *
     * @param nodeType the expected node type of this line
     * @param after  the content of the line after the nodetype
     */
    private String extractNodeContentsFilename(String nodeType, String after) {
        //Bad input yields a default
        if ((after == null) || (after.trim().length() == 0)) {
            if (nodeType.equals("HLIOPERATOR")) {
                return "Impasse__Operator_Tie.soar";  //tie is most frequently used in .soar agents
            }
            else {
                return "unknown.soar";
            }
        }

        //If it's a folder, take the first reasonable string you can find
        String[] words = after.split("[ \\t]");  //split on spaces and tabs
        if (nodeType.equals("FOLDER")) {
            for(String word : words) {
                if (operatorNameIsValid(word)) {
                    return word;
                }
            }
        }

        //look for a tell-tale ".soar" extension
        for(int i = 0; i < words.length; ++i) {
            String word = words[i];

            int soarIndex = word.indexOf(".soar");
            if (soarIndex != -1) {
                //If this word appears to be a valid filename, return it
                if (soarIndex > 0) {
                    String fn = word.substring(0, soarIndex);
                    if (operatorNameIsValid(fn)) {
                        return word;
                    }
                }

                //Perhaps there is valid content in the previous word?
                if ((i > 0) && (operatorNameIsValid(words[i - 1]))) {
                    return words[i - 1] + ".soar";
                }
            }
        }//for

        //Ok, find the first valid name you can and use that
        for(String word : words) {
            if (operatorNameIsValid(word)) {
                return word + ".soar";
            }
        }

        //failure.  make a recursive call with 'null' to get a default response
        return extractNodeContentsFilename(nodeType, null);

    }//extractNodeContentsFilename

    /**
     * try to find the datamap id in a mis-formatted operator node line from a .vsa file
     *
     * @param nodeId the expected id of this node
     * @param after  the content of the line after the node type
     *
     * @return the datamap id or -1 on failure
     */
    private int extractDataMapId(int nodeId, String after) {
        //check for bad input
        if ((after == null) || (after.trim().length() == 0)) return -1;

        //Any standalone number that doesn't match the node id seems legit
        String[] words = after.split("[ \\t]");  //split on spaces and tabs
        int nodeIdCount = 0;
        for(String word : words) {
            try {
                int candId = Integer.parseInt(word);
                if (candId == nodeId) nodeIdCount++;
                else if (candId > 0) return candId;
            }
            catch(NumberFormatException nfe) {
                /* no need to do anything here */
            }
        }

        //if the node id was seen twice then it's probably the datamap id
        if (nodeIdCount == 2) return nodeId;

        //TODO:  Could search the datamap for clues.  That seems like overkill atm.

        //failure
        return -1;

    }//extractDataMapId



    /**
     * attempt to reconstruct a node definition from a mis-formatted operator node line from a .vsa file
     */
    private String reconstructOpNodeLine(int nodeId, String line) {
        //Extract a node type (see OperatorNode.VSA_NODE_TYPES)
        String nodeType = extractNodeType(line);
        int nodeTypeIndex = line.indexOf(nodeType);

        //extract text after the node type
        String after = line;
        if (nodeTypeIndex != -1) {
            after = line.substring(nodeTypeIndex + nodeType.length());
        }

        //Get the contents filename
        String contentsFN = extractNodeContentsFilename(nodeType, after);

        //get the base name from the contents filename
        String baseName = contentsFN;
        int soarIndex = contentsFN.indexOf(".soar");
        if (soarIndex != -1) {
            baseName = baseName.substring(0, soarIndex);
        }



        //Build a valid line using these data
        if (nodeType.equals("FOLDER")) {
            return "" + nodeId + "\t0\tFOLDER " + baseName + " " + contentsFN + " " + nodeId;
        }
        if ( (nodeType.equals("HLOPERATOR")) || (nodeType.equals("HLFOPERATOR")) ||(nodeType.equals("HLIOPERATOR")) ) {
            //Must find a datamap id
            int dataMapId = extractDataMapId(nodeId, after);
            if (dataMapId == -1) {
                nodeType = "OPERATOR"; //can't create a high-level operator
            }
            else {
                return "" + nodeId + "\t0\t" + nodeType + " " + baseName + " " + contentsFN + " " + baseName + " " + dataMapId + " " + nodeId;
            }
        }

        //default:  non-high-level operator or file of some sort
        return "" + nodeId + "\t0\t" + nodeType + " " + baseName + " " + contentsFN + " " + nodeId;


    }//reconstructOpNodeLine


    /**
     * verifies that a given line from a .vsa file that describes a node
     * is valid.  If it's invalid, it attempts to recreate the line as best it can
     *
     * @param line  to verify
     * @return  the verified line or a valid, best-guess substitute
     */
    private String verifyV4OperatorLine(String line, Vector<FeedbackListEntry> errors) {
        //Sanity check: no null input!
        if (line == null) {
            //Note:  this should never happen
            errors.add(new FeedbackListEntry("Error!  Received null operator node line."));
            return null;
        }
        String[] words = line.split("[ \\t]");  //split on spaces and tabs

        //--------------------------------------------------------------------
        //1. Node id (should already have been checked)
        if (words.length < 1) {
            errors.add(new FeedbackListEntry("Error!  Operator node line has missing id number."));
            return null;
        }
        int nodeId = -1;
        try {
            nodeId = Integer.parseInt(words[0]);
        }
        catch(NumberFormatException nfe) {
            errors.add(new FeedbackListEntry("Error!  Operator node line has invalid id number."));
        }

        //Try to

        //--------------------------------------------------------------------
        //2. Parent id
        int parentId = -1;
        if (words.length < 2) {
            errors.add(new FeedbackListEntry("Error!  Operator node line has missing parent id number."));
            parentId = 0;
        }
        else {
            try {
                parentId = Integer.parseInt(words[1]);
                if (parentId < 0) throw new NumberFormatException();

            } catch (NumberFormatException nfe) {
                errors.add(new FeedbackListEntry("Error!  Operator node line has invalid parent id number."));
                parentId = 0; //fallback to a default
            }
        }

        //--------------------------------------------------------------------
        //3. Node Type
        if (words.length < 3) {
            errors.add(new FeedbackListEntry("Error!  Operator node line has missing node type."));
            return null;
        }
        String nodeType = words[2];



        //----STOPPED HERE ---


        //verify the first two words are id numbers


        if (words.length >= 6) {
            //TODO: more tests
            return line;
        }
        //TODO rebuild the line
        return null;

    }




    /**
     * Creates an operator node from a given specification line taken from a .vsa file.
     * Unlike its predecessor, this "safe" version does not throw exceptions but, instead,
     * checks for errors and tries to recover when they are found.
     *
     * Valid Format is:
     *
     * <node_id>(TAB)<parent_node_id>(TAB)<node_type> [node_name] [node_contents_filename] <type_sepcific_contents>
     * where:
     *    <node_id>        is a unique integer.  These ids should appear in
     *                     sequential, numerical order in the file
     *    <parent_mode_id> is the id of the parent node or ROOT if this is the
     *                     root node.  The operator tree has only one root.
     *    <node_type>      is one of the types in {@link OperatorNode#VSA_NODE_TYPES}
     *
     * @param nodeId       the expected id number of this line
     * @param line         a line from a .vsa file that describes an operator node
     * @param errors       any errors found are reported here
     *
     * @return the created OperatorNode
     * @see OperatorNode
     * @see #readVersionFourSafe
     */
    private OperatorNode makeNodeVersionFourSafe(int nodeId, String line, Vector<FeedbackListEntry> errors) {
        //ensure the line is valid
        String verifiedLine = verifyV4OperatorLine(line, errors);
        if (verifiedLine == null) return null;

        //split into parts
        OperatorNode retVal;
        String[] words = verifiedLine.split("[ \\t]");  //split on spaces and tabs
        String type = words[2];

        if (type.equals("HLOPERATOR")) {
            retVal = createSoarOperatorNode(words[3], words[4], words[5], Integer.parseInt(words[6]));
        } else if (type.equals("OPERATOR")) {
            retVal = createSoarOperatorNode(words[3], words[4]);
        } else if (type.equals("HLFOPERATOR")) {
            retVal = createFileOperatorNode(words[3], words[4], words[5], Integer.parseInt(words[6]));
        } else if (type.equals("FOPERATOR")) {
            retVal = createFileOperatorNode(words[3], words[4]);
        } else if (type.equals("HLIOPERATOR")) {
            retVal = createImpasseOperatorNode(words[3], words[4], words[5], Integer.parseInt(words[6]));
        } else if (type.equals("IOPERATOR")) {
            retVal = createImpasseOperatorNode(words[3], words[4]);
        } else if (type.equals("FOLDER")) {
            retVal = createFolderNode(words[3], words[4]);
        } else {
            //This should never happen...
            return null;
        }

        return retVal;
    }//makeNodeVersionFourSafe

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
     * Writes out the default productions in the "top-state.soar" file
     *
     * @param fileToWriteTo the "top-state.soar" file
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
                        File projectFile = new File(orn.getProjectFile() + "~");
                        File dataMapFile = new File(orn.getDataMapFile() + "~");
                        File commentFile = new File(dataMapFile.getParent() + File.separator + "comment.dm~");
                        writeOutHierarchy(projectFile, dataMapFile, commentFile);
                    }
                }
            };

        }

        public void run() {
            while (!closed) {
                try {
                    SwingUtilities.invokeAndWait(writeOutControl);
                    sleep(60000 * 3);  // 3 minutes
                } catch (InterruptedException | InvocationTargetException ie) { /* don't care */ }
            }
        }//run
    }//end of BackupThread class

}   // end of OperatorWindow class
