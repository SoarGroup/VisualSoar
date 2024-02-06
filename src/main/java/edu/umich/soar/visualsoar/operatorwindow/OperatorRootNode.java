package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.graph.SoarVertex;
import edu.umich.soar.visualsoar.misc.FeedbackListObject;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.io.*;
import java.util.Vector;

/**
 * This is the root node for the operator window
 *
 * @author Brad Jones
 * @version 0.5a 5 Aug 1999
 */
public class OperatorRootNode extends FolderNode implements java.io.Serializable {

    private static final long serialVersionUID = 20221225L;

///////////////////////////////////////////////////////////////////
// Data Members
///////////////////////////////////////////////////////////////////
    /**
     * A that represents the file path to the datamap, must be initialized
     * in the constructor
     */
    private String fullPathStart;

///////////////////////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////////////////////

    /**
     * This constructs the normal OperatorRootNode object
     *
     * @param inName the name of the node
     */
    public OperatorRootNode(String inName, int inId, String inFullPathStart, String inFolder) {
        super(inName, inId, inFolder);
        fullPathStart = inFullPathStart;
    }

    public OperatorRootNode(String inName, int inId, String inFolder) {
        super(inName, inId, inFolder);
    }

///////////////////////////////////////////////////////////////////
// Methods
///////////////////////////////////////////////////////////////////

    /**
     * @return whether an openDataMap() call on this node will work
     */
    public boolean noDataMap() {
        return false;
    }

    /**
     * Given a Writer this writes out a description of the root node
     * that can be read back in later
     *
     * @param w the writer
     * @throws IOException if there is an error writing to the writer
     */
    public void write(Writer w) throws IOException {
        w.write("ROOT " + name + " " + folderName + " " + id);
    }

    public void exportDesc(Writer w) throws IOException {
        w.write("ROOT " + name);
    }

    public void setFullPath(String s) {
        fullPathStart = s;
    }

    public String getFullPathStart() {
        return fullPathStart;
    }

    /**
     * Adds a sub-operator underneath this root node
     *
     * @param swmm            the Working Memory Model so that we can add corresponding entries to the datamap
     * @param newOperatorName the name of the operator being added
     */
    public OperatorNode addSubOperator(OperatorWindow operatorWindow, SoarWorkingMemoryModel swmm, String newOperatorName) throws IOException {
        super.addSubOperator(operatorWindow, swmm, newOperatorName);

        SoarVertex oper = swmm.createNewSoarId();
        SoarVertex operName = swmm.createNewEnumeration(newOperatorName);
        swmm.addTriple(swmm.getTopstate(), "operator", oper);
        swmm.addTriple(oper, "name", operName);

        return this;
    }

    public String getName() {
        return this.name;
    }

    public String getProjectFile() {
        return fullPathStart + File.separator + name + ".vsa";
    }

    public String getDataMapFile() {
        return getFolderName() + File.separator + name + ".dm";
    }

    /**
     * This returns the path of the project so that children can
     * determine the full path
     *
     * @return the path of the project
     */
    protected String getFullPathName() {
        return fullPathStart + File.separator + folderName;
    }

    @Override
    protected void enableContextMenuItems() {
        super.enableContextMenuItems();
        addTopFolderItem.setVisible(true);
        addTopFolderItem.setEnabled(true);

    }//enableContextMenuItems

    /**
     * This opens/shows a dataMap with this node's associated Data Map File
     *
     * @param pw the MainFrame
     */
    public void openDataMap(SoarWorkingMemoryModel swmm, MainFrame pw) {
        DataMap dataMap = new DataMap(swmm, swmm.getTopstate(), toString());
        dataMap.setVisible(true);
        pw.addDataMap(dataMap);
        pw.getDesktopPane().dmAddDataMap(swmm.getTopstate().getValue(), dataMap);
    }

    /**
     * This returns the associated datamap entry for the root node
     * which is going to be the top-state
     */
    public SoarIdentifierVertex getStateIdVertex() {
        return MainFrame.getMainFrame().getOperatorWindow().getDatamap().getTopstate();
    }

    /**
     * rename
     * is used by {@link #renameAndBackup}
     */
    public void rename(OperatorWindow operatorWindow, String newName, String newPath) throws IOException {
        DefaultTreeModel model = (DefaultTreeModel) operatorWindow.getModel();
        File newFolder = new File(newPath + File.separator + newName);

        this.folderName = newFolder.getName();
        this.name = newName;
        this.fullPathStart = newPath;

        model.nodeChanged(this);
    }//rename

    public void importFunc(Reader r, OperatorWindow operatorWindow, SoarWorkingMemoryModel swmm) throws IOException, NumberFormatException {
        VSEImporter.read(r, this, operatorWindow, swmm, VSEImporter.HLOPERATOR | VSEImporter.OPERATOR);
    }

    public void renameAndBackup(OperatorWindow operatorWindow, String newName, String newPath) {

        if (new File(newPath + File.separator + newName + ".vsa").exists()) {
            JOptionPane.showMessageDialog(MainFrame.getMainFrame(), "An agent with this name already exists at this location.", "Naming Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Create the parent folder if it doesn't exist
        File parentFolder = new File(newPath);
        if (!parentFolder.exists()) {
            parentFolder.mkdir();
        }

        //Create the new project folder if it doesn't exist
        File newFolder = new File(newPath + File.separator + newName);
        if (!newFolder.exists()) {
            newFolder.mkdir();
        }

        try {
            //Update the instance variables identifying this project
            rename(operatorWindow, newName, newPath);

            //Create a sub-folder for the .dm file
            String newDataFolderName = newPath + File.separator + newName;
            File newDataFolder = new File(newDataFolderName);
            if (!newDataFolder.exists()) {
                newDataFolder.mkdir();
            }

            //Create the new datamap file
            String newGraphFileName = newDataFolderName + File.separator + newName + ".dm";
            FileWriter graphWriter = new FileWriter(newGraphFileName);
            operatorWindow.getDatamap().write(graphWriter);
            graphWriter.close();

            for (int i = 0; i < getChildCount(); ++i) {
                OperatorNode child = (OperatorNode) getChildAt(i);
                child.copyStructures(newDataFolder);
            }
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(MainFrame.getMainFrame(), "Save As Failed!", "Save As Failed", JOptionPane.ERROR_MESSAGE);
        }
    }//renameAndBackup

    public void startSourcing() throws IOException {
        String filename = fullPathStart + File.separator + folderName + ".soar";
        Writer w = new FileWriter(filename);
        source(w);
        w.close();
        sourceRecursive();
    }

    public void searchTestDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListObject> errors) {
        DataMap dataMap = new DataMap(swmm, swmm.getTopstate(), "");
        errors.addAll(dataMap.searchTestDataMap(swmm.getTopstate(), toString()));
    }

    public void searchCreateDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListObject> errors) {
        DataMap dataMap = new DataMap(swmm, swmm.getTopstate(), "");
        errors.addAll(dataMap.searchCreateDataMap(swmm.getTopstate(), toString()));
    }

    public void searchTestNoCreateDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListObject> errors) {
        DataMap dataMap = new DataMap(swmm, swmm.getTopstate(), "");
        errors.addAll(dataMap.searchTestNoCreateDataMap(swmm.getTopstate(), toString()));
    }

    public void searchCreateNoTestDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListObject> errors) {
        DataMap dataMap = new DataMap(swmm, swmm.getTopstate(), "");
        errors.addAll(dataMap.searchCreateNoTestDataMap(swmm.getTopstate(), toString()));
    }

    public void searchNoTestNoCreateDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListObject> errors) {
        DataMap dataMap = new DataMap(swmm, swmm.getTopstate(), "");
        errors.addAll(dataMap.searchNoTestNoCreateDataMap(swmm.getTopstate(), toString()));
    }
}
	
