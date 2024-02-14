package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.graph.EnumerationVertex;
import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.graph.SoarVertex;
import edu.umich.soar.visualsoar.misc.FeedbackListEntry;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Vector;


/**
 * OperatorOperatorNode class is for regular Operators.
 * Similar to SoarOperatorNode in every way other than
 * writing to disk.
 **/
public class OperatorOperatorNode extends SoarOperatorNode {
    private static final long serialVersionUID = 20221225L;


    /////////////////////////////////////////
    // Constructors
    /////////////////////////////////////////

    /**
     * this creates a low-level operator with the given name and file
     */
    public OperatorOperatorNode(String inName, int inId, String inFileName) {
        super(inName, inId, inFileName);
    }

    /**
     * This will construct a high-level operator node, this one supports serialization,
     * restoreId must be called to get this object into a good state
     */
    public OperatorOperatorNode(String inName, int inId, String inFileName, String inFolderName, int inDataMapIdNumber) {
        this(inName, inId, inFileName);
        folderName = inFolderName;
        dataMapIdNumber = inDataMapIdNumber;
        isHighLevel = true;
    }

    public void searchTestDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListEntry> errors) {
        // if high-level, then search datamap
        if (isHighLevel()) {
            DataMap dataMap = new DataMap(swmm, dataMapId, toString());
            errors.addAll(dataMap.searchTestDataMap(dataMapId, toString()));
        }
    }

    public void searchCreateDataMap(SoarWorkingMemoryModel swmm,
                                    Vector<FeedbackListEntry> errors) {
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
            errors.addAll(dataMap.searchTestNoCreateDataMap(dataMapId, toString()));
        }
    }

    public void searchCreateNoTestDataMap(SoarWorkingMemoryModel swmm,
                                          Vector<FeedbackListEntry> errors) {
        // if high-level, then search datamap
        if (isHighLevel()) {
            DataMap dataMap = new DataMap(swmm, dataMapId, toString());
            errors.addAll(dataMap.searchCreateNoTestDataMap(dataMapId, toString()));
        }
    }

    public void searchNoTestNoCreateDataMap(SoarWorkingMemoryModel swmm,
                                            Vector<FeedbackListEntry> errors) {
        // if high-level, then search datamap
        if (isHighLevel()) {
            DataMap dataMap = new DataMap(swmm, dataMapId, toString());
            errors.addAll(dataMap.searchNoTestNoCreateDataMap(dataMapId, toString()));
        }
    }

    public void write(Writer w) throws IOException {
        if (isHighLevel) {
            w.write("HLOPERATOR " + name + " " + fileAssociation + " " + folderName + " " + dataMapId.getValue() + " " + id);
        } else {
            w.write("OPERATOR " + name + " " + fileAssociation + " " + id);
        }
    }

    /**
     * The user wants to rename this node
     *
     * @param newName the new name that the user wants this node to be called
     */
    public void rename(OperatorWindow operatorWindow,
                       String newName) throws IOException {
        String oldName = name;

        //This will throw an IOException if it fails
        super.rename(operatorWindow, newName);

        /*=====================================================================
         * Attempt to update the operator's name in the datamap
         *---------------------------------------------------------------------
         */
        //Find the correct datamap
        OperatorNode node = (OperatorNode) getParent();
        while (node.noDataMap()) {
            node = (OperatorNode) node.getParent();
        }

        //Find the datamap for this operator
        SoarWorkingMemoryModel swmm = operatorWindow.getDatamap();

        //Search for all operators in the datamap
        Enumeration<NamedEdge> enumOper;
        if (node instanceof SoarOperatorNode) {
            enumOper = swmm.emanatingEdges(((SoarOperatorNode) node).dataMapId);
        } else {
            enumOper = swmm.emanatingEdges(swmm.getTopstate());
        }
        while (enumOper.hasMoreElements()) {
            NamedEdge ne = enumOper.nextElement();
            if (ne.getName().equals("operator")) {
                //Search this operator for the old name
                SoarVertex svOper = ne.V1();
                Enumeration<NamedEdge> enumName = swmm.emanatingEdges(svOper);
                while (enumName.hasMoreElements()) {
                    ne = enumName.nextElement();
                    if (ne.getName().equals("name")) {
                        SoarVertex svName = ne.V1();
                        if (svName instanceof EnumerationVertex) {
                            EnumerationVertex evName = (EnumerationVertex) svName;
                            if (evName.contains(oldName)) {
                                evName.add(newName);
                                evName.remove(oldName);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

} // end of OperatorOperatorNode
