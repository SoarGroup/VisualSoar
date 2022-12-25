package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.graph.EnumerationVertex;
import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.graph.SoarVertex;
import edu.umich.soar.visualsoar.misc.FeedbackListObject;

import java.io.*;
import java.awt.Component;
import java.util.*;


/**
 *  OperatorOperatorNode class is for regular Operators.
 *  Similar to SoarOperatorNode in every way other than
 *  writing to disk.
 **/
class OperatorOperatorNode extends SoarOperatorNode {

	/////////////////////////////////////////
	// Constructors
	/////////////////////////////////////////
	/**
	 * this creates a low-level operator with the given name and file
	 */
	public OperatorOperatorNode(String inName,int inId,String inFileName) {
		super(inName,inId,inFileName);
	}

    /**
	 * This will construct a high-level operator node, this one supports serialization,
	 * restoreId must be called to get this object into a good state
	 */
	public OperatorOperatorNode(String inName,int inId,String inFileName,String inFolderName,int inDataMapIdNumber) {
		this(inName,inId,inFileName);
		folderName = inFolderName;
		dataMapIdNumber = inDataMapIdNumber;
		isHighLevel = true;
	}
  
  public void searchTestDataMap(SoarWorkingMemoryModel swmm, Vector<FeedbackListObject> errors) {
    // if high-level, then search datamap
    if(isHighLevel()) {
      DataMap dataMap = new DataMap(swmm,dataMapId,toString());
      errors.addAll( dataMap.searchTestDataMap(dataMapId, toString()) );
    }
  }
  public void searchCreateDataMap(SoarWorkingMemoryModel swmm, 
                                  Vector<FeedbackListObject> errors) {
    // if high-level, then search datamap
    if(isHighLevel()) {
      DataMap dataMap = new DataMap(swmm,dataMapId,toString());
      errors.addAll( dataMap.searchCreateDataMap(dataMapId, toString()) );
    }
  }

  public void searchTestNoCreateDataMap(SoarWorkingMemoryModel swmm, 
                                        Vector<FeedbackListObject> errors) {
    // if high-level, then search datamap
    if(isHighLevel()) {
      DataMap dataMap = new DataMap(swmm, dataMapId, toString());
      errors.addAll( dataMap.searchTestNoCreateDataMap(dataMapId, toString()) );
    }
  }

  public void searchCreateNoTestDataMap(SoarWorkingMemoryModel swmm, 
                                        Vector<FeedbackListObject> errors) {
    // if high-level, then search datamap
    if(isHighLevel()) {
      DataMap dataMap = new DataMap(swmm, dataMapId, toString());
      errors.addAll( dataMap.searchCreateNoTestDataMap(dataMapId, toString()) );
    }
  }

  public void searchNoTestNoCreateDataMap(SoarWorkingMemoryModel swmm, 
                                          Vector<FeedbackListObject> errors) {
    // if high-level, then search datamap
    if(isHighLevel()) {
      DataMap dataMap = new DataMap(swmm, dataMapId, toString());
      errors.addAll( dataMap.searchNoTestNoCreateDataMap(dataMapId, toString()) );
    }
  }

	/**
	 * This adjusts the context menu so that only the valid commands
	 * are displayed
	 * @param c the owner of the context menu, should be the OperatorWindow
	 * @param x the horizontal position on the screen where the context menu should
	 * be displayed
	 * @param y the vertical position on the screen where the context menu should
	 * be displayed
	 */
	public void showContextMenu(Component c, int x, int y) {
		if(isHighLevel) {
			addSuboperatorItem.setEnabled(true);
			addFileItem.setEnabled(true);
			openRulesItem.setEnabled(true);
			openDataMapItem.setEnabled(true);
			deleteItem.setEnabled(true);
			renameItem.setEnabled(true);
			exportItem.setEnabled(true);
      impasseSubMenu.setEnabled(true);
			checkChildrenAgainstDataMapItem.setEnabled(true);
		}
		else {
			addSuboperatorItem.setEnabled(true);
			addFileItem.setEnabled(true);
			openRulesItem.setEnabled(true);
			openDataMapItem.setEnabled(false);
			deleteItem.setEnabled(true);
			renameItem.setEnabled(true);
			exportItem.setEnabled(true);
      impasseSubMenu.setEnabled(true);
			checkChildrenAgainstDataMapItem.setEnabled(false);
		}
		contextMenu.show(c,x,y);
	}

	
	/**
	 * Given a Writer this writes out a description of the soar operator node
	 * that can be read back in later
	 * @param w the writer 
	 * @throws IOException if there is an error writing to the writer
	 */
	public void exportDesc(Writer w) throws IOException {
		if(isHighLevel) 
			w.write("HLOPERATOR " + name + " " + dataMapIdNumber);
		else
			w.write("OPERATOR " + name);
	}

	public void write(Writer w) throws IOException {
		if(isHighLevel) {
			w.write("HLOPERATOR " + name + " " + fileAssociation + " " + folderName + " " + dataMapId.getValue() + " " + id);
		}
		else {
			w.write("OPERATOR " + name  + " " + fileAssociation + " " + id);
		}
	}

    /**
     * The user wants to rename this node
     * 
     * @param newName the new name that the user wants this node to be called
     */
    public void rename(OperatorWindow operatorWindow,
                       String newName) throws IOException 
    {
        String oldName = name;
        
        //This will throw an IOException if it fails
        super.rename(operatorWindow, newName);

        /*=====================================================================
         * Attempt to update the operator's name in the datamap
         *---------------------------------------------------------------------
         */
        //Find the correct datamap
        OperatorNode node = (OperatorNode)getParent();
        while(node.noDataMap())
        {
            node = (OperatorNode)node.getParent();
        }

        //Find the datamap for this operator
        SoarWorkingMemoryModel swmm = operatorWindow.getDatamap();

        //Search for all operators in the datamap
        Enumeration<NamedEdge> enumOper;
        if (node instanceof SoarOperatorNode)
        {
            enumOper = swmm.emanatingEdges(((SoarOperatorNode)node).dataMapId);
        }
        else
        {
            enumOper = swmm.emanatingEdges(swmm.getTopstate());
        }
        while(enumOper.hasMoreElements())
        {
            NamedEdge ne = enumOper.nextElement();
            if (ne.getName().equals("operator"))
            {
                //Search this operator for the old name
                SoarVertex svOper = ne.V1();
                Enumeration<NamedEdge> enumName = swmm.emanatingEdges(svOper);
                while(enumName.hasMoreElements())
                {
                    ne = enumName.nextElement();
                    if (ne.getName().equals("name"))
                    {
                        SoarVertex svName = ne.V1();
                        if (svName instanceof EnumerationVertex)
                        {
                            EnumerationVertex evName = (EnumerationVertex)svName;
                            if (evName.contains(oldName))
                            {
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
