package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.misc.FeedbackListEntry;

import javax.swing.*;
import java.io.*;
import java.util.Vector;

/**
 * This is the Folder node for the operator window
 *
 * @author Brad Jones
 * @version 0.5a 5 Aug 1999
 */
public class FolderNode extends OperatorNode implements java.io.Serializable {
    private static final long serialVersionUID = 20221225L;

///////////////////////////////////////////////////////////////////
// Data Members
///////////////////////////////////////////////////////////////////
    /**
     * a string that is the path to the folder which is associated with this
     * node
     */
    protected String folderName;

///////////////////////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////////////////////

    /**
     * This constructs a folder node for the Operator Window
     *
     * @param inName       the name of the node
     * @param inFolderName the folder for which this node is associated
     */
    public FolderNode(String inName, int inId, String inFolderName) {

        super(inName, inId);
        folderName = inFolderName;
    }

///////////////////////////////////////////////////////////////////
// Accessors
///////////////////////////////////////////////////////////////////

    /**
     * Use this getter function to get the path to the folder
     *
     * @return the path to the folder
     */
    public String getFolderName() {

        return getFullPathName();
    }

    /**
     * This returns the full path from the parent
     */
    protected String getFullPathName() {

        OperatorNode parent = (OperatorNode) getParent();
        return parent.getFullPathName() + File.separator + folderName;
    }


    /**
     * this tells the JTree to always render this like it
     * has children
     *
     * @return false
     */
    public boolean isLeaf() {

        return false;
    }


///////////////////////////////////////////////////////////////////
// Modifiers
///////////////////////////////////////////////////////////////////

    /**
     * This is the function that gets called when you want to add a sub file
     * operator to this node
     *
     * @param newFileName the name of the new operator to add
     */
    public void addFileOperator(OperatorWindow operatorWindow, SoarWorkingMemoryModel swmm, String newFileName) throws IOException {

        File file = new File(getFullPathName() + File.separator + newFileName + ".soar");

        if (checkCreateReplace(file)) {

            return;
        }
        //FileNode fn = operatorWindow.createFileNode(newFileName,file.getName());
        FileOperatorNode fon = operatorWindow.createFileOperatorNode(newFileName, file.getName());
        operatorWindow.addChild(this, fon);
        sourceChildren();
    }

    /**
     * This is the function that gets called when you want to add a sub-operator
     * to this node
     *
     * @param swmm            the tree model for which this node is currently a member
     * @param newOperatorName the name of the new operator to add
     *
     * @return the newly created child operator node (or null on failure)
     */
    public OperatorNode addSubOperator(OperatorWindow operatorWindow,
                                       SoarWorkingMemoryModel swmm,
                                       String newOperatorName) throws IOException {

        File rules = new File(getFullPathName() + File.separator + newOperatorName + ".soar");

        if (creationConflict(rules)) {

            return null;
        }

        if (!rules.createNewFile()) {

            throw new IOException();
        }

        OperatorNode child = operatorWindow.createSoarOperatorNode(newOperatorName,
                rules.getName());
        operatorWindow.addChild(this, child);
        sourceChildren();
        return child;
    }

    /**
     * This is the function that gets called when you want to add a sub Impasse
     * Operator to this node
     *
     * @param newOperatorName the name of the new operator to add
     */
    public OperatorNode addImpasseOperator(OperatorWindow operatorWindow, SoarWorkingMemoryModel swmm, String newOperatorName) throws IOException {
        File rules = new File(getFullPathName() + File.separator + newOperatorName + ".soar");

        if (creationConflict(rules)) {

            return this;
        }

        if (!rules.createNewFile()) {

            throw new IOException();
        }

        SoarOperatorNode ion = operatorWindow.createImpasseOperatorNode(newOperatorName,
                rules.getName());
        operatorWindow.addChild(this, ion);
        sourceChildren();

        //Automatically create an elaborations file.  Impasses do not have files
        //associated with them directly, so we have to add an elaborations file
        //to the impasse (making it a high level operator) immediately.  I'm not
        //sure if this is the best place for this code design-wise.  But it does
        //work, so I'm leaving it here for the time being.  -:AMN: 20 Oct 03
        try {

            ion.firstTimeAdd(operatorWindow, swmm);
        } catch (IOException ioe) {

            JOptionPane.showMessageDialog(
                    MainFrame.getMainFrame(),
                    "IOException adding elaborations file to impasse.",
                    "IOException",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        return this;
    }//addImpasseOperator


    public void notifyDeletionOfChild(OperatorWindow operatorWindow,
                                      OperatorNode child) {

        try {

            sourceChildren();
        } catch (IOException ioe) { /* shrug */ }
    }

    @Override
    protected void enableContextMenuItems() {
        super.enableContextMenuItems();

        exportItem.setEnabled(false);
        renameItem.setEnabled(false);
        addTopFolderItem.setEnabled(false);
        openRulesItem.setEnabled(false);
        openDataMapItem.setEnabled(false);
        deleteItem.setEnabled(false);

        if (name.equals("elaborations")) {
            addSubOperatorItem.setEnabled(false);
        }

        //non-special folders may be deleted
        if ( (!name.equals("elaborations")) && (!name.equals("all")) ) {
            deleteItem.setEnabled(true);
        }

        addTopFolderItem.setVisible(false);

    }//enableContextMenuItems

    /**
     * Given a Writer this writes out a description of the folder node
     * that can be read back in later
     *
     * @param w the writer
     * @throws IOException if there is an error writing to the writer
     */
    public void write(Writer w) throws IOException {

        w.write("FOLDER " + name + " " + folderName + " " + id);
    }

    public void exportDesc(Writer w) throws IOException {

        w.write("FOLDER " + name);
    }

    public void exportFile(Writer w, int id) {
    }

    public void exportDataMap(Writer w) throws IOException {

        w.write("DATAMAP\n");
        MainFrame.getMainFrame().getOperatorWindow().getDatamap().write(w);
    }

    public void exportType(Writer w) throws IOException {

        w.write("Not expecting to export this\n");
    }

    public void delete(OperatorWindow operatorWindow) {

        renameToRemove(new File(getFolderName()));
        OperatorNode parent = (OperatorNode) getParent();
        operatorWindow.removeNode(this);
        parent.notifyDeletionOfChild(operatorWindow, this);
    }


    public void importFunc(Reader r,
                           OperatorWindow operatorWindow,
                           SoarWorkingMemoryModel swmm) throws IOException, NumberFormatException {

        if (name.equals("common") || name.equals("all")) {
            VSEImporter.read(r,
                    this,
                    operatorWindow,
                    swmm,
                    VSEImporter.HLOPERATOR | VSEImporter.OPERATOR);
        } else {
            VSEImporter.read(r, this, operatorWindow, swmm, VSEImporter.FILE);
        }
    }

    public void copyStructures(File folderToWriteTo) throws IOException {

        File copyOfFolder = new File(folderToWriteTo.getPath() + File.separator + folderName);
        copyOfFolder.mkdir();
        for (int i = 0; i < getChildCount(); ++i) {

            ((OperatorNode) getChildAt(i)).copyStructures(copyOfFolder);
        }
    }

    public void source(Writer w) throws IOException {

        String LINE = System.lineSeparator();
        w.write("pushd " + folderName + LINE +
                "source " + folderName + "_source.soar" + LINE +
                "popd" + LINE);
    }

    public void sourceChildren() throws IOException {
        String filename = getFullPathName() + File.separator + folderName + "_source.soar";
        Writer w = new FileWriter(filename);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {

            OperatorNode child = (OperatorNode) getChildAt(i);
            child.source(w);
        }
        w.close();
    }

    public void sourceRecursive() throws IOException {

        sourceChildren();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {

            OperatorNode child = (OperatorNode) getChildAt(i);
            child.sourceRecursive();
        }
    }

    /**
     * I've implemented this but I note that VS doesn't allow the user
     * to close non-empty folders so I don't think this method ever
     * really gets called.  It's here just in case.  -:AMN: 20 Apr 2024
     */
    @Override
    public void closeEditors() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            OperatorNode child = (OperatorNode) getChildAt(i);
            child.closeEditors();
        }
    }

    public void searchTestDataMap(SoarWorkingMemoryModel swmm,
                                  Vector<FeedbackListEntry> errors) {
    }

    public void searchCreateDataMap(SoarWorkingMemoryModel swmm,
                                    Vector<FeedbackListEntry> errors) {
    }

    public void searchTestNoCreateDataMap(SoarWorkingMemoryModel swmm,
                                          Vector<FeedbackListEntry> errors) {
    }

    public void searchCreateNoTestDataMap(SoarWorkingMemoryModel swmm,
                                          Vector<FeedbackListEntry> errors) {
    }

    public void searchNoTestNoCreateDataMap(SoarWorkingMemoryModel swmm,
                                            Vector<FeedbackListEntry> errors) {
    }

}
