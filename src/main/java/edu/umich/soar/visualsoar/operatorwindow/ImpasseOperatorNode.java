package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;

import java.io.IOException;
import java.io.Writer;
import java.util.Vector;


/**
 * ImpasseOperatorNode class is for Impasse Operators.
 * Similar to SoarOperatorNode in every way other than
 * writing to disk.
 */
public class ImpasseOperatorNode extends SoarOperatorNode {
    private static final long serialVersionUID = 20221225L;

    /////////////////////////////////////////
    // Constructors
    /////////////////////////////////////////

    /**
     * this creates a low-level operator with the given name and file
     */
    public ImpasseOperatorNode(String inName, int inId, String inFileName) {

        super(inName, inId, inFileName);
    }

    /**
     * This will construct a high-level operator node, this one supports
     * serialization, restoreId must be called to get this object into a good
     * state
     */
    public ImpasseOperatorNode(String inName, int inId, String inFileName, String inFolderName, int inDataMapIdNumber) {

        this(inName, inId, inFileName);
        folderName = inFolderName;
        dataMapIdNumber = inDataMapIdNumber;
        isHighLevel = true;
    }


    /**
     * Searches the datamap associated with this node if it owns one, looking
     * for any portions of the datamap that were not tested by any productions
     * and are not located within the output-link.
     */
    public void searchTestDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListEntry> errors) {
        // if high-level, then search datamap
        if (isHighLevel()) {

            DataMap dataMap = new DataMap(swmm, dataMapId, toString());
            errors.addAll(dataMap.searchTestDataMap(dataMapId, toString()));
        }
    }

    public void searchCreateDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListEntry> errors) {

        // if high-level, then search datamap
        if (isHighLevel()) {

            DataMap dataMap = new DataMap(swmm, dataMapId, toString());
            errors.addAll(dataMap.searchCreateDataMap(dataMapId, toString()));
        }
    }

    public void searchTestNoCreateDataMap(SoarWorkingMemoryModel swmm,
                                          Vector<FeedbackListEntry> errors) {

        // if high-level, then search datamap
        if (isHighLevel()) {

            DataMap dataMap = new DataMap(swmm, dataMapId, toString());
            errors.addAll(dataMap.searchTestNoCreateDataMap(dataMapId,
                    toString()));
        }
    }

    public void searchCreateNoTestDataMap(SoarWorkingMemoryModel swmm,
                                          Vector<FeedbackListEntry> errors) {

        // if high-level, then search datamap
        if (isHighLevel()) {

            DataMap dataMap = new DataMap(swmm, dataMapId, toString());
            errors.addAll(dataMap.searchCreateNoTestDataMap(dataMapId,
                    toString()));
        }
    }

    public void searchNoTestNoCreateDataMap(SoarWorkingMemoryModel swmm,
                                            Vector<FeedbackListEntry> errors) {

        // if high-level, then search datamap
        if (isHighLevel()) {

            DataMap dataMap = new DataMap(swmm, dataMapId, toString());
            errors.addAll(dataMap.searchNoTestNoCreateDataMap(dataMapId,
                    toString()));
        }
    }

    @Override
    protected void enableContextMenuItems() {
        super.enableContextMenuItems();
        renameItem.setEnabled(false);
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
            w.write("HLIOPERATOR " + name + " " + dataMapIdNumber);
        } else {
            w.write("IOPERATOR " + name);
        }
    }

    public void write(Writer w) throws IOException {

        if (isHighLevel) {

            w.write("HLIOPERATOR " + name + " " + fileAssociation + " " + folderName + " " + dataMapId.getValue() + " " + id);
        } else {

            w.write("IOPERATOR " + name + " " + fileAssociation + " " + id);
        }
    }


} // end of ImpasseOperatorNode
