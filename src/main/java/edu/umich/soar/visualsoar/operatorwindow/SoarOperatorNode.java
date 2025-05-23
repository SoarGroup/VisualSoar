package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.graph.SoarVertex;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.io.*;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Vector;

public abstract class SoarOperatorNode extends FileNode {
    private static final long serialVersionUID = 20221225L;

    /////////////////////////////////////////
    // DataMembers
    /////////////////////////////////////////

    // this member tells us whether this Operator is high-level or not
    protected boolean isHighLevel = false;

    // if this SoarOperatorNode is high-level then this is a string representing
    // just the folder name, else it is null
    protected String folderName = null;

    // if this SoarOperatorNode is high-level then this is the Associated state
    // in the datamap, else it is null
    protected SoarIdentifierVertex dataMapId;

    // this is the number associated with the dataMapId, if the dataMapId is
    // null, then this is left uninitialized, or 0
    protected int dataMapIdNumber;

    //Linked child nodes of this one
    private final Vector<LinkNode> linkNodes = new Vector<>();

    /////////////////////////////////////////
    // Constructors
    /////////////////////////////////////////

    /**
     * this creates a low-level operator with the given name and file
     */
    public SoarOperatorNode(String inName, int inId, String inFileName) {
        super(inName, inId, inFileName);
    }

  /**
   * this creates a low-level operator with the given name and file
   */
  public SoarOperatorNode(String inName, int inId, String serializationId, String inFileName) {
    super(inName, inId, serializationId, inFileName);
  }

    public boolean isHighLevel() {
        return isHighLevel;
    }

    @Override
    protected void enableContextMenuItems() {
        super.enableContextMenuItems();

        addSubOperatorItem.setEnabled(true);
        addFileItem.setEnabled(true);
        deleteItem.setEnabled(true);
        renameItem.setEnabled(true);
        impasseSubMenu.setEnabled(true);

        if (isHighLevel) {
            openDataMapItem.setEnabled(true);
            checkChildrenAgainstDataMapItem.setEnabled(true);
        }

    }//enableContextMenuItems


    /**
     * Given a Writer this writes out a description of the soar operator node
     * that can be read back in later
     *
     * @param w the writer
     * @throws IOException if there is an error writing to the writer
     */

    public void exportDesc(Writer w) throws IOException {

        if (isHighLevel) {
            w.write("HLOPERATOR " + getName() + " " + dataMapIdNumber);
        } else {
            w.write("OPERATOR " + getName());
        }
    }

    /*
     * This represents the set of actions that should be preformed when an
     * operator becomes a high-level operator.  This function is overridden
     * in FileOperatorNode since it does not represent a Soar state.
     *
     * @return true on success
     */
    public boolean firstTimeAdd(OperatorWindow operatorWindow,
                             SoarWorkingMemoryModel swmm) throws IOException {

        // Create the Folder
        File folder = new File(((OperatorNode) getParent()).getFullPathName() + File.separator + getName());

        if (creationConflict(folder, true)) {
            return false;
        }

        if (!folder.mkdir()) {
            throw new IOException();
        }


        // Create the elaborations file
        File elaborationsFile = new File(folder.getPath() + File.separator + "elaborations.soar");
        elaborationsFile.createNewFile();

        // Create the elaborations node
        OperatorNode elaborationNode = operatorWindow.getProjectModel().createFileOperatorNode("elaborations", elaborationsFile.getName());

        // Create the datamap id
        dataMapId = swmm.createNewStateId(((OperatorNode) getParent()).getStateIdVertex(swmm), getName());
        dataMapIdNumber = dataMapId.getValue();

        // Make this node high-level
        isHighLevel = true;

        //Add the elaborations node and folder
        operatorWindow.getProjectModel().addChild(this, elaborationNode);
        folderName = folder.getName();

        return true;
    }

    public void importFunc(Reader r,
                           OperatorWindow operatorWindow,
                           SoarWorkingMemoryModel swmm) throws IOException, NumberFormatException {

        if (!isHighLevel) {
            if (! firstTimeAdd(operatorWindow, swmm)) {
                throw new IOException("firstTimeAdd() failed in SoarOperatorNode.importFunc()");
            }
        }
        VSEImporter.read(r, this, operatorWindow, swmm, VSEImporter.HLOPERATOR | VSEImporter.OPERATOR);
    }

    public void exportType(Writer w) throws IOException {

        if (isHighLevel) {
            w.write("IMPORT_TYPE " + VSEImporter.HLOPERATOR + "\n");
        } else {
            w.write("IMPORT_TYPE " + VSEImporter.OPERATOR + "\n");
        }
    }

    /**
     * Use this getter function to get the path to the folder
     *
     * @return the path to the folder
     */
    public String getFolderName() {

        return getFullPathName();
    }

    /**
     * Use this getter function to get the path to the rule file
     *
     * @return the path to the rule file
     */
    public String getFileName() {

        OperatorNode parent = (OperatorNode) getParent();
        return parent.getFullPathName() + File.separator + fileAssociation;
    }

    protected String getFullPathName() {

        if (isHighLevel) {
            return ((OperatorNode) getParent()).getFullPathName() + File.separator + folderName;
        } else {
            return ((OperatorNode) getParent()).getFullPathName();
        }
    }

    public SoarIdentifierVertex getStateIdVertex(SoarWorkingMemoryModel swmm) {
        return dataMapId;
    }

    public int getDataMapIdNumber() { return this.dataMapIdNumber; }

    /**
     * @return whether an openDataMap() call on this node will work
     */
    public boolean noDataMap() {
        return !isHighLevel;
    }

    public void restoreId(SoarWorkingMemoryModel swmm) {
    SoarVertex sv = swmm.getVertexForId(dataMapIdNumber);
    Objects.requireNonNull(sv, "Operator node's datamap ID, " + dataMapIdNumber + ", does not point to any existing DM vertex.");
    if (!(sv instanceof SoarIdentifierVertex)) {
      throw new IllegalStateException(
          "Operator node's datamap ID should point to a Soar ID, but found vertex '"
              + sv.getSerializationId()
              + "' (or "
              + sv.getValue()
              + "), which is a "
              + sv.getClass().getName());
    }
    dataMapId = (SoarIdentifierVertex) sv;
  }

    /** helper method for {@link #rename} that renames a source file
     * @return a File object for the renamed file */
    private File fileRename(String newName, File oldFile) throws IOException {
        //Check for filename conflict
        File newFile = new File(oldFile.getParent() + File.separator + newName + ".soar");
        if (creationConflict(newFile)) {
            throw new IOException();
        }

        if (!oldFile.renameTo(newFile)) {
            //Caller must catch this and report fail to user
            throw new IOException();
        }

        return newFile;
    }


    /**
     * helper method for {@link #rename} that renames a high-level operator
     */
    private void highLevelRename(String newName, File oldFile) throws IOException {
        // Check for folder name conflict
        File oldFolder = new File(getFolderName());
        File newFolder = new File(oldFolder.getParent() + File.separator + newName);
        if (creationConflict(newFolder)) {
            throw new IOException("Creation conflict with new folder name: " + newFolder.getPath());
        }

        //Check for name conflict with associated "_source" file
        String oldSrcFileName = oldFolder.getPath() + File.separator + oldFile.getName();
        oldSrcFileName = oldSrcFileName.replace(".soar","_source.soar");
        String newSrcFileName = oldFolder.getPath() + File.separator + newName + "_source.soar";
        File oldSrcFile = new File(oldSrcFileName);
        File newSrcFile = new File(newSrcFileName);
        if (newSrcFile.exists()) newSrcFile.delete();  //shouldn't happen but nbd if it does

        //Rename the "_source" file inside the old folder
        if (!oldSrcFile.renameTo(newSrcFile)) {
            throw new IOException("Unable to rename" + oldSrcFileName + " to " + newSrcFileName);
        }

        // Rename old folder to the new folder
        if (!oldFolder.renameTo(newFolder)) {
            //If folder rename failed change file back to old name for the "_source" file
            //(This should never fail since we just renamed it...)
            newSrcFile.renameTo(oldSrcFile);
            throw new IOException("Unable to rename folder" + oldFolder.getPath() + " to " + newFolder.getPath());
        }

        folderName = newFolder.getName();
    }//highLevelRename


    /**
     * The user wants to rename this node
     *
     * @param newName the new name that the user wants this node to be called
     */

    public void rename(OperatorWindow operatorWindow,
                       String newName) throws IOException {
        File oldFile = new File(getFileName());
        File newFile = fileRename(newName, oldFile);

        //For high-level operators, the folder must also be renamed
        if (isHighLevel) {
            try {
                highLevelRename(newName, oldFile);
            }
            catch(IOException ioe) {
                //On failure, undo the .soar file rename
                newFile.renameTo(oldFile);

                throw ioe;
            }
        }


        //Update this object's instance variables to reflect the successful rename
        this.name = newName;
        fileAssociation = newFile.getName();
        if (ruleEditor != null) {
            ruleEditor.fileRenamed(newFile.getPath());
        }

        //notify the Tree widget that this node has changed
        DefaultTreeModel model = (DefaultTreeModel) operatorWindow.getModel();
        model.nodeChanged(this);

    }//rename
    /**
     * This is the function that gets called when you want to add a sub-operator to this node
     *
     * @param newOperatorName the name of the new operator to add
     *
     * @return the newly created child operator node (or null on failure)
     */
    public OperatorNode addSubOperator(OperatorWindow operatorWindow,
                                       SoarWorkingMemoryModel swmm,
                                       String newOperatorName) throws IOException {
        if (!isHighLevel) {
            if (!firstTimeAdd(operatorWindow, swmm)) {
                return null;
            }
        }

        File rules = new File(getFullPathName() + File.separator + newOperatorName + ".soar");
        if (creationConflict(rules)) {
            return null;
        }

        if (!rules.createNewFile()) {
            throw new IOException();
        }

        OperatorNode child = operatorWindow.getProjectModel().createSoarOperatorNode(newOperatorName,
                rules.getName());
        operatorWindow.getProjectModel().addChild(this, child);

        SoarVertex opName = swmm.createNewEnumeration(newOperatorName);
        SoarVertex operId = swmm.createNewSoarId();
        swmm.addTriple(dataMapId, "operator", operId);
        swmm.addTriple(operId, "name", opName);

        notifyLinksOfUpdate(operatorWindow);
        sourceChildren();
        return child;
    }//addSubOperator

    /**
     * This is the function that gets called when you want to add a sub Impasse Operator to this node
     *
     * @param newOperatorName the name of the new operator to add
     */
    public OperatorNode addImpasseOperator(OperatorWindow operatorWindow,
                                           SoarWorkingMemoryModel swmm,
                                           String newOperatorName) throws IOException {
        if (!isHighLevel) firstTimeAdd(operatorWindow, swmm);

        File rules = new File(getFullPathName() + File.separator + newOperatorName + ".soar");
        if (creationConflict(rules)) {
            return this;
        }

        if (!rules.createNewFile()) {
            throw new IOException();
        }

        SoarOperatorNode ion = operatorWindow.getProjectModel().createImpasseOperatorNode(newOperatorName,
                rules.getName());
        operatorWindow.getProjectModel().addChild(this, ion);
        sourceChildren();


        //Automatically create an elaborations file.  Impasses do not have files
        //associated with them directly so, we have to add an elaborations file
        //to the impasse (making it a high level operator) immediately.  I'm not
        //sure if this is the best place for this code design-wise.  But it does
        //work so, I'm leaving it here for the time being.  -:AMN: 20 Oct 03
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

    /**
     * This is the function that gets called when you want to add a sub file
     * operator to this node
     *
     * @param newFileName the name of the new operator to add
     */
    public void addFileOperator(OperatorWindow operatorWindow, SoarWorkingMemoryModel swmm, String newFileName) throws IOException {
        if (!isHighLevel) {
            if (!firstTimeAdd(operatorWindow, swmm)) return;
        }

        File file = new File(getFullPathName() + File.separator + newFileName + ".soar");
        if (checkCreateReplace(file)) return;

        FileOperatorNode fon = operatorWindow.getProjectModel().createFileOperatorNode(newFileName, file.getName());
        operatorWindow.getProjectModel().addChild(this, fon);
        sourceChildren();
    }//addFileOperator


    /**
     * Removes the selected operator from the tree if it is allowed
     */
    public void delete(OperatorWindow operatorWindow) {
        if (isHighLevel) {
            if (!linkNodes.isEmpty()) {
                int selOption =
                        JOptionPane.showConfirmDialog(MainFrame.getMainFrame(),
                                "This Operator has links, which will be deleted do you want to continue?",
                                "Link Deletion Confirmation",
                                JOptionPane.YES_NO_OPTION);
                if (selOption == JOptionPane.NO_OPTION) return;
                while (!linkNodes.isEmpty()) {
                    LinkNode linkNodeToDelete = linkNodes.get(0);
                    linkNodeToDelete.delete(operatorWindow);
                }
            }
            renameToRemove(new File(getFileName()));
            renameToRemove(new File(getFolderName()));
            OperatorNode parent = (OperatorNode) getParent();
            operatorWindow.removeNode(this);
            parent.notifyDeletionOfChild(operatorWindow, this);

        } else //not a HL operator
        {
            renameToRemove(new File(getFileName()));
            OperatorNode parent = (OperatorNode) getParent();
            operatorWindow.removeNode(this);
            parent.notifyDeletionOfChild(operatorWindow, this);
        }
    }   //delete


    /**
     * A child has been deleted from this node, so check if this node has
     * become a low-level operator now
     */
    public void notifyDeletionOfChild(OperatorWindow operatorWindow,
                                      OperatorNode child) {

        if (getChildCount() == 0) {
            renameToRemove(new File(getFolderName()));
            isHighLevel = false;
            folderName = null;
            dataMapId = null;

            if (!linkNodes.isEmpty()) {
                JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                        "This node is no longer high-level, links will be deleted.",
                        "Link deletions",
                        JOptionPane.INFORMATION_MESSAGE);
                while (!linkNodes.isEmpty()) {
                    LinkNode linkNodeToDelete = linkNodes.get(0);
                    linkNodeToDelete.delete(operatorWindow);
                }
            }
        }
        notifyLinksOfUpdate(operatorWindow);
        try {
            sourceChildren();
        } catch (IOException ioe) { /* TODO: something other than quiet fail */ }
    }

    /**
     * This opens/shows a dataMap with the node's associated Data Map File
     */
    public void openDataMap(SoarWorkingMemoryModel swmm, MainFrame pw) {

        DataMap dataMap;


        // Check to see if Operator uses top-state datamap.
        if (dataMapIdNumber != 0) {

            // If File Operator, get the datamap of the first OperatorOperatorNode above it
            if (this instanceof FileOperatorNode) {
                OperatorNode parent = (OperatorNode) this.getParent();
                SoarIdentifierVertex dataMapParent = parent.getStateIdVertex(swmm);
                while (((parent.getStateIdVertex(swmm)).getValue() != 0)
                        && (!(parent instanceof OperatorOperatorNode))) {
                    parent = (OperatorNode) parent.getParent();
                    dataMapParent = parent.getStateIdVertex(swmm);
                }
                dataMap = new DataMap(swmm, dataMapParent, parent.toString());
                pw.addDataMap(dataMap);
                pw.getDesktopPane().dmAddDataMap(dataMapParent.getValue(), dataMap);
            } else {
                dataMap = new DataMap(swmm, dataMapId, toString());
                pw.addDataMap(dataMap);
                pw.getDesktopPane().dmAddDataMap(dataMapId.getValue(), dataMap);
            }
        } else {
            dataMap = new DataMap(swmm, swmm.getTopstate(), toString());
            pw.addDataMap(dataMap);
            pw.getDesktopPane().dmAddDataMap(swmm.getTopstate().getValue(),
                    dataMap);
        }

        dataMap.setVisible(true);
    }

    public SoarIdentifierVertex getState() {

        return dataMapId;
    }

    /**
     * exportDataMap
     * <p>
     * writes current datamap content to a given Writer object
     */
    public void exportDataMap(Writer w) throws IOException {
        w.write("DATAMAP\n");
        MainFrame.getMainFrame().getOperatorWindow().getDatamap().write(w);
    }

    public void registerLink(LinkNode inLinkNode) {
        linkNodes.add(inLinkNode);
    }

    public void removeLink(LinkNode inLinkNode) {

        linkNodes.remove(inLinkNode);
    }

    void notifyLinksOfUpdate(OperatorWindow operatorWindow) {
        DefaultTreeModel model = (DefaultTreeModel) operatorWindow.getModel();
        for (LinkNode nodeToUpdate : this.linkNodes) {
            model.nodeStructureChanged(nodeToUpdate);
        }
    }


    public void copyStructures(File folderToWriteTo) throws IOException {
        super.copyStructures(folderToWriteTo);

        if (isHighLevel) {
            File copyOfFolder = new File(folderToWriteTo.getPath() + File.separator + folderName);
            copyOfFolder.mkdir();
            for (int i = 0; i < getChildCount(); ++i) {
                ((OperatorNode) getChildAt(i)).copyStructures(copyOfFolder);
            }
        }
    }

    /**
     * move
     * <p>
     * moves this operator to a new position in the operator tree
     */
    public boolean move(OperatorWindow operatorWindow, OperatorNode newParent) {
        //move the node to its new parent
        if ((newParent instanceof SoarOperatorNode)
                && !((SoarOperatorNode) newParent).isHighLevel()) {
            try {
                SoarOperatorNode son = ((SoarOperatorNode) newParent);
                SoarWorkingMemoryModel swmm = operatorWindow.getDatamap();
                if (!son.firstTimeAdd(operatorWindow,swmm)) {
                    return false;
                }
            } catch (IOException ioe) {
                System.out.println("Move failed, because firstTimeAdd on parent failed");
                return false;
            }
        }

        //check for name conflict (why don't we do this first??)
        if (newParent.creationConflict(new File(newParent.getFolderName() + File.separator + fileAssociation))) {
            return false;
        }

        //If moving a high-level operator its folder also needs to be renamed
        if (isHighLevel) {
            // Rename Folder
            File oldFolder = new File(getFolderName());
            File newFolder = new File(newParent.getFolderName() + File.separator + folderName);
            oldFolder.renameTo(newFolder);

            // Remove old superstate links
            SoarWorkingMemoryModel swmm = operatorWindow.getDatamap();
            Enumeration<NamedEdge> emanEnum = swmm.emanatingEdges(dataMapId);
            while (emanEnum.hasMoreElements()) {
                NamedEdge ne = emanEnum.nextElement();
                if (ne.getName().equals("superstate")) {
                    swmm.removeTriple(ne.V0(),
                            ne.getName(),
                            ne.V1());
                }
            }

            // Add new ^superstate link
            SoarVertex soarVertex = newParent.getStateIdVertex(swmm);
            if (soarVertex == null) soarVertex = swmm.getTopstate();
            swmm.addTriple(dataMapId, "superstate", soarVertex);
        }//if HL operator

        // Rename File
        File oldFile = new File(getFileName());
        File newFile = new File(newParent.getFolderName() + File.separator + fileAssociation);
        oldFile.renameTo(newFile);

        DefaultTreeModel model = (DefaultTreeModel) operatorWindow.getModel();
        OperatorNode oldParent = (OperatorNode) getParent();
        model.removeNodeFromParent(this);
        oldParent.notifyDeletionOfChild(operatorWindow, this);

        operatorWindow.getProjectModel().addChild(newParent, this);
        // Adjust rule editor if one is open
        if (ruleEditor != null) {
            ruleEditor.fileRenamed(newFile.getPath());
        }

        return true;
    }

    public void source(Writer w) throws IOException {
        super.source(w);
        if (isHighLevel) {
            String LINE = System.lineSeparator();
            w.write("pushd " + folderName + LINE +
                    "source " + folderName + "_source.soar" + LINE +
                    "popd" + LINE);
        }
    }

    public void sourceChildren() throws IOException {

        if (isHighLevel) {
            Writer w = new FileWriter(getFullPathName() + File.separator + folderName + "_source.soar");
            int childCount = getChildCount();
            for (int i = 0; i < childCount; ++i) {
                OperatorNode child = (OperatorNode) getChildAt(i);
                child.source(w);
            }
            w.close();
        }
    }

    public void sourceRecursive() throws IOException {

        if (isHighLevel) {
            sourceChildren();
            int childCount = getChildCount();
            for (int i = 0; i < childCount; ++i) {
                OperatorNode child = (OperatorNode) getChildAt(i);
                child.sourceRecursive();
            }
        }
    }

}
