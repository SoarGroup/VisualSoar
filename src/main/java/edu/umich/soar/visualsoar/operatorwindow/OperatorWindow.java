package edu.umich.soar.visualsoar.operatorwindow;


import edu.umich.soar.visualsoar.ProjectModel;
import edu.umich.soar.visualsoar.dialogs.DialogUtils;
import edu.umich.soar.visualsoar.dialogs.find.FindUtils;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.dialogs.find.FindInProjectDialog;
import edu.umich.soar.visualsoar.dialogs.NameDialog;
import edu.umich.soar.visualsoar.dialogs.find.ReplaceInProjectDialog;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackEntryOpNode;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackManager;
import edu.umich.soar.visualsoar.misc.Prefs;
import edu.umich.soar.visualsoar.misc.Template;
import edu.umich.soar.visualsoar.parser.ParseException;
import edu.umich.soar.visualsoar.parser.SoarProduction;
import edu.umich.soar.visualsoar.parser.TokenMgrError;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
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

///////////////////////////////////////////////////////////////////////////
// Data members
///////////////////////////////////////////////////////////////////////////
    /**
     * a reference to the DragGestureListener for Drag and Drop operations, may be deleted in future
     */
    DragGestureListener dgListener = new OWDragGestureListener();

  private ProjectModel projectModel;

  private final DefaultTreeCellRenderer owCellRenderer = new OperatorWindowRenderer();
  private static OperatorWindow s_OperatorWindow;

  private static final int ROW_TEXT_MARGIN = 7;

  /**
   * Private usage only. Default constructor to do common things such as setting up the mouse and
   * keyboard listeners and backup threads
   *
   * @see BackupThread
   */
  private OperatorWindow() {

    setCellRenderer(owCellRenderer);

    s_OperatorWindow = this;

    toggleClickCount = 3;
    getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    DragSource.getDefaultDragSource()
        .createDefaultDragGestureRecognizer(
            this, DnDConstants.ACTION_MOVE | DnDConstants.ACTION_LINK, dgListener);
    addMouseListener(
        new MouseAdapter() {
          public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
              suggestShowContextMenu(e.getX(), e.getY());
            }
            if ((e.getClickCount() == 2
                && ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK)
                    == InputEvent.BUTTON1_DOWN_MASK))) {
              openRules();
            }
          }

          public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
              suggestShowContextMenu(e.getX(), e.getY());
            }
          }
        });

    registerKeyboardAction(
        e -> delete(),
        KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
        WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    registerKeyboardAction(
        e -> delete(),
        KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
        WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    registerKeyboardAction(
        e -> {
          TreePath tp = getSelectionPath();
          if (tp != null) {
            OperatorNode selNode = (OperatorNode) tp.getLastPathComponent();
            selNode.openRules(MainFrame.getMainFrame());
          }
        },
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
        WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    // Auto-backup
    if (MainFrame.getMainFrame() != null && !MainFrame.getMainFrame().isReadOnly()) {
      new BackupThread().start();
    }
    setFontSize(Prefs.editorFontSize.getInt());
    Prefs.editorFontSize.addChangeListener(
        newValue -> setFontSize((int) newValue));
    FindUtils.registerTextComponentFocus(this);
  }

  private void setFontSize(int fontSize) {
    final Font newSizedFont = new Font(getFont().getName(), getFont().getStyle(), fontSize);
    SwingUtilities.invokeLater(() -> {
      setFont(newSizedFont);
      owCellRenderer.setFont(newSizedFont);
      setRowHeight(fontSize + ROW_TEXT_MARGIN);
    });
  }

    /**
     * Creates an Operator Window given a project name.
     * Creates a default project tree model and a new WorkingMemory for new projects.
     *
     * @param projectName     The name of the project
     * @param projectPath     The full file path of the project's location
     * @param is_new          True if it is a new project
     * @see ProjectModel#newProject(String, Path)
     */
    public OperatorWindow(String projectName, Path projectPath, boolean is_new) {
        this();
        s_OperatorWindow = this;
        if (is_new) {
            projectModel = ProjectModel.newProject(projectName, projectPath);
            setModel(projectModel.operatorHierarchy);
        }
    }

    /**
     * Opens an OperatorWindow for an existing project
     *
     * @param in_file the location of the project to be opened
     * @param readOnly is this project being opened in read-only mode?
     * @see ProjectModel#openExistingProject(Path)
     */
    public OperatorWindow(File in_file, boolean readOnly) throws NumberFormatException, IOException {
        this();
        FeedbackManager feedbackManager = MainFrame.getMainFrame().getFeedbackManager();
        feedbackManager.clearFeedback();
        s_OperatorWindow = this;
        projectModel = ProjectModel.openExistingProject(in_file.toPath());
        feedbackManager.setStatusBarMsg("Opened " + in_file.getName());
        setModel(projectModel.operatorHierarchy);
        Prefs.addRecentProject(in_file, readOnly);
    }

    public static OperatorWindow getOperatorWindow() {
        return s_OperatorWindow;
    }

    public ProjectModel getProjectModel() {
      return projectModel;
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
     * Removes a node from the operator window
     *
     * @param operatorNode the node that is to be removed
     * @see DefaultTreeModel#removeNodeFromParent
     */
    public void removeNode(OperatorNode operatorNode) {
        ((DefaultTreeModel) getModel()).removeNodeFromParent(operatorNode);
    }

    /**
     * Returns the number of children associated with the root node / project node.
     */
    public int getChildCount() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) getModel().getRoot();
        return node.getChildCount();
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
        DialogUtils.closeOnEscapeKey(theDialog, this);
        if (! theDialog.wasApproved()) return;  //User hit Cancel button
        opName = theDialog.getText();

        //Find the new child operator's location in the operator hierarchy
        TreePath tp = getSelectionPath();
        if (tp == null) return; //should never happen
        OperatorNode parent = (OperatorNode) tp.getLastPathComponent();
        OperatorNode child = null;

        //Attempt to add the new operator
        try {
            child = parent.addSubOperator(this, projectModel.swmm, opName);
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
            MainFrame.getMainFrame().getFeedbackManager().setStatusBarMsg("Export Complete");
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
                node.importFunc(new FileReader(file), this, projectModel.swmm);
                r.close();

                //Inform user of success
                MainFrame.getMainFrame().getFeedbackManager().setStatusBarMsg("Import Complete");
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


          //Save the change to the _source files
          DefaultTreeModel tree = (DefaultTreeModel) getModel();
          OperatorRootNode root = (OperatorRootNode) tree.getRoot();
          try {
            // Save the change to the .vsa.json file.
            // TODO: ask user first
            saveHierarchy();
            root.startSourcing();
          } catch (IOException ioe) {
            JOptionPane.showMessageDialog(
                MainFrame.getMainFrame(),
                "Operator file renamed successfully but '_source' file \ncould not be updated.  I recommend you try to save \nyour project manually.",
                "I/O Error",
                JOptionPane.ERROR_MESSAGE);
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
                parent.addFileOperator(this, projectModel.swmm, s);

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
        FolderNode folderNode = projectModel.createFolderNode(folderName, newFolderFile.getName());
        projectModel.addChild(parent, folderNode);

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
            parent = parent.addImpasseOperator(this, projectModel.swmm, s);

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
        selNode.openDataMap(projectModel.swmm, MainFrame.getMainFrame());
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
        return projectModel.swmm;
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
        getModel(); //TODO: unnecessary?

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
     * @see ProjectModel#checkProductions
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
                    MainFrame.getMainFrame().getOperatorWindow().getProjectModel().checkProductions(selNode, currentNode, parsedProductions, vecErrors);
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
        MainFrame.getMainFrame().getFeedbackManager().showFeedback(vecErrors);
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
        SoarIdentifierVertex siv = parentNode.getStateIdVertex(projectModel.swmm);
        if (siv == null) {
            siv = projectModel.swmm.getTopstate();
        }

        //Generate the new datamap entries
        Enumeration<SoarProduction> prodEnum = parsedProds.elements();
        while (prodEnum.hasMoreElements()) {
            SoarProduction sp = prodEnum.nextElement();
            vecGenerations.addAll(projectModel.swmm.checkGenerateProduction(siv, sp, opNode));
        }

        //Verify our changes worked
        projectModel.checkProductions(parentNode, opNode, parsedProds, parseErrors);

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
        SoarIdentifierVertex siv = parentNode.getStateIdVertex(projectModel.swmm);
        if (siv == null) {
            siv = projectModel.swmm.getTopstate();
        }

        //Generate the new datamap entries
        Enumeration<SoarProduction> prodEnum = parsedProds.elements();
        while (prodEnum.hasMoreElements()) {
            SoarProduction sp = prodEnum.nextElement();
            //we only care about the production that caused the error

            if (errToFix.getProdName().equals(sp.getName())) {
                Vector<FeedbackListEntry> errs =
                  projectModel.swmm.checkGenerateSingleEntry(siv, sp, opNode, errToFix);

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
     * Saves project as a new project name
     *
     * @param newName the new name of the project
     * @param newPath the new file path of the project
     * @see #saveHierarchy()
     * @see OperatorRootNode#renameAndBackup(OperatorWindow, String, String)
     */
    public void saveProjectAs(String newName, String newPath) throws IOException {
        //Save the original to file first (TODO: ask user first)
        saveHierarchy();

        //Make a copy here:
        OperatorRootNode orn = (OperatorRootNode) (getModel().getRoot());
        orn.renameAndBackup(this, newName, newPath);
        Prefs.addRecentProject(new File(orn.getProjectFile()), false);
    }

  /**
   * Saves the current project to disk
   *
   * @param inProjFile name of the file to be saved - .vsa file
   * @see TreeSerializer#toJson(DefaultTreeModel)
   * @see SoarWorkingMemoryModel#toJson()
   */
  public void writeOutHierarchy(File inProjFile)
      throws IOException {
    projectModel.writeProject(inProjFile);
  }

  /**
   * Save entire Operator Hierarchy (including datamap)
   *
   * @see #writeOutHierarchy
   */
  public void saveHierarchy() throws IOException {
    OperatorRootNode orn = (OperatorRootNode) (getModel().getRoot());
    boolean formatChanged = orn.setIsJson(true);
    File projectFileName = new File(orn.getProjectFile());

    // User probably already converted to .vsa.json and then forgot to open it up this time.
    // Don't let them save over the other one, in case they put a lot of work into it.
    if (formatChanged && projectFileName.exists()) {
      JOptionPane.showMessageDialog(
          MainFrame.getMainFrame(),
          "You opened the project from "
              + orn.getName()
              + ".vsa and tried to save it to "
              + projectFileName.getName()
              + ",\nbut that file already exists. Did you mean to open "
              + projectFileName.getName()
              + " instead?\n"
              + "If not, delete it or back it up before trying to save this project again.",
          "Project Save Failed",
          JOptionPane.ERROR_MESSAGE);
      orn.setIsJson(false);
      return;
    }

    writeOutHierarchy(projectFileName);
    if (formatChanged) {

      String oldName = projectFileName.getName().replaceAll("\\.vsa\\.json", "");
      Prefs.addRecentProject(projectFileName, MainFrame.getMainFrame().isReadOnly());
      JOptionPane.showMessageDialog(
          MainFrame.getMainFrame(),
          "Your project has been written in a new format in "
              + projectFileName.getName()
              + ". After ensuring you have a backup,\nyou should manually remove the old "
              + orn.getName()
              + ".vsa, "
              + orn.getName()
              + ".dm and comment.dm files.",
          "Project Converted",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

    /**
     * Class used for drag and drop operations
     */
    private class OWDragGestureListener implements DragGestureListener {
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
    private static class OWDragSourceListener implements DragSourceListener {
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

        MainFrame.getMainFrame().getFeedbackManager().showFeedback(vecErrs);
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

  /** Responsible for keeping track of the backup project files */
  private class BackupThread extends Thread {
    Runnable writeOutControl;

    public BackupThread() {
      writeOutControl =
          () -> {
            OperatorRootNode orn = (OperatorRootNode) (getModel().getRoot());
            File projectFile = new File(orn.getProjectFile() + "~");
            try {
              writeOutHierarchy(projectFile);
            } catch (IOException e) {
              // caught in the run() method below
              throw new RuntimeException(e);
            }
          };
    }

    @Override
    public void run() {
      while (true) {
        try {
          sleep(60000 * 3); // 3 minutes
        } catch (InterruptedException e) {
          return;
        }
        try {
          SwingUtilities.invokeAndWait(writeOutControl);
        } catch (InterruptedException | InvocationTargetException ie) {
          // TODO: messing with the feedback manager here could cause other important feedback to disappear, so for now we won't write to it. If we upgrade the feedback UI some day, we should come back and log the errors here.
          // FeedbackManager fbManager = MainFrame.getMainFrame().getFeedbackManager();
          // fbManager.setStatusBarError("Error occurred while backing up project");
          ie.printStackTrace();
        }
      }
    } // run
  } // end of BackupThread class
}   // end of OperatorWindow class
