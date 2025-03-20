package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Vector;


/**
 * FileOperatorNode class is for File Operators.
 * Similar to SoarOperatorNode in every way other than
 * writing to disk and does not have a datamap associated
 * with itself, instead, it uses the datamap of its Operator parent
 * Supports sub-filing.
 */
@SuppressWarnings("unused")
public class FileOperatorNode extends SoarOperatorNode {
    private static final long serialVersionUID = 20221225L;

    /////////////////////////////////////////
    // Constructors
    /////////////////////////////////////////

    /**
     * this creates a low-level operator with the given name and file
     */
    public FileOperatorNode(String inName, int inId, String inFileName) {
        super(inName, inId, inFileName);
    }

  /**
   * this creates a low-level operator with the given name and file
   */
  public FileOperatorNode(String inName, int inId, String serializationId, String inFileName) {
    super(inName, inId, serializationId, inFileName);
  }

    /**
     * this creates a high level operator with the given name, file, folder and
     * dataMapId
     */
    public FileOperatorNode(String inName, int inId, String inFileName, String inFolderName, SoarIdentifierVertex inDataMapId) {
        this(inName, inId, inFileName);
        folderName = inFolderName;
        dataMapId = inDataMapId;
        dataMapIdNumber = inDataMapId.getValue();
        isHighLevel = true;
    }

    /**
     * This will construct a high-level operator node, this one supports serialization,
     * restoreId must be called to get this object into a good state
     */
    public FileOperatorNode(String inName, int inId, String inFileName, String inFolderName, int inDataMapIdNumber) {
        this(inName, inId, inFileName);
        folderName = inFolderName;
        dataMapIdNumber = inDataMapIdNumber;
        isHighLevel = true;
    }

  /**
   * This will construct a high-level operator node, this one supports serialization,
   * restoreId must be called to get this object into a good state
   */
  public FileOperatorNode(String inName, int inId, String serializationId, String inFileName, String inFolderName, int inDataMapIdNumber) {
    this(inName, inId, serializationId, inFileName);
    folderName = inFolderName;
    dataMapIdNumber = inDataMapIdNumber;
    isHighLevel = true;
  }


    /**
     * File Operator Nodes do not own their own datamaps, therefore, it is
     * redundant to search the datamap associated with a FileOperatorNode.
     */
    public void searchTestDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListEntry> errors) {
    }

    public void searchCreateDatamap(SoarWorkingMemoryModel swmm, Vector<FeedbackListEntry> errors) {
    }

    public void searchTestNoCreateDatamap(SoarWorkingMemoryModel swmm, Vector<FeedbackListEntry> errors) {
    }

    public void searchCreateNoTestDatamap(SoarWorkingMemoryModel swmm, Vector<FeedbackListEntry> errors) {
    }

    public void searchNoTestNoCreateDatamap(SoarWorkingMemoryModel swmm, Vector<FeedbackListEntry> errors) {
    }

    /*
     * This represents the set of actions that should be preformed when an
     * operator becomes a high-level operator
     */
    public boolean firstTimeAdd(OperatorWindow operatorWindow,
                             SoarWorkingMemoryModel swmm) throws IOException {
        OperatorNode parent = (OperatorNode) getParent();

        // Create the Folder
        File folder = new File(parent.getFullPathName() + File.separator + getName());
        if (creationConflict(folder, true)) {
            return false;
        }
        if (!folder.mkdir()) throw new IOException();

        // Determine the datamap id
        if (parent instanceof SoarOperatorNode) {
            dataMapId = (parent).getStateIdVertex(swmm);
        } else {
            dataMapId = swmm.getTopstate();
        }
        dataMapIdNumber = dataMapId.getValue();

        // Make this node high level
        isHighLevel = true;

        //Add the folder
        folderName = folder.getName();

        return true;
    }


    /**
     * Given a Writer this writes out a description of the soar operator node
     * that can be read back in later
     *
     * @param w the writer
     * @throws IOException if there is an error writing to the writer
     */
    public void exportDesc(Writer w) throws IOException {
		if (isHighLevel) {
			w.write("HLFOPERATOR " + getName() + " " + dataMapIdNumber);
		} else {
			w.write("FOPERATOR " + getName());
		}
    }

    @Override
    public void write(Writer w) throws IOException {
        if (isHighLevel) {
            w.write("HLFOPERATOR " + getName() + " " + fileAssociation + " " + folderName + " " + dataMapId.getValue() + " " + id);
        } else {
            w.write("FOPERATOR " + getName() + " " + fileAssociation + " " + id);
        }
    }

    @Override
    public NodeType getType() {
      return NodeType.FILE_OPERATOR;
    }

  public String getRelativeFolderName() {
      return folderName;
  }
} // end of FileOperatorNode
