package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.graph.SoarVertex;
import edu.umich.soar.visualsoar.misc.FeedbackListObject;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

public class SoarOperatorNode extends FileNode
{

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
    public SoarOperatorNode(String inName,int inId,String inFileName) 
    {
       super(inName,inId,inFileName);
    }

    public boolean isHighLevel()
    {
        return isHighLevel;
    }

    /**
     * This adjusts the context menu so that only the valid commands
     * are displayed
     * @param c the owner of the context menu, should be the OperatorWindow
     * @param x the horizontal position on the screen where the context menu
     * should be displayed
     * @param y the vertical position on the screen where the context menu
     * should be displayed
     */
    public void showContextMenu(Component c, int x, int y) 
    {
        if(isHighLevel) 
        {
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
        else 
        {
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

    public void exportDesc(Writer w) throws IOException 
    {

        if(isHighLevel)
        w.write("HLOPERATOR " + name + " " + dataMapIdNumber);
        else
        w.write("OPERATOR " + name);
    }

    /*
     * This represents the set of actions that should be preformed when an
     * operator becomes a high-level operator.  This function is overridden
     * in FileOperatorNode since it does not represent a Soar state.
     */
    public void firstTimeAdd(OperatorWindow operatorWindow,
                             SoarWorkingMemoryModel swmm) throws IOException 
    {

        // Create the Folder
        File folder = new File(((OperatorNode)getParent()).getFullPathName() + File.separator + name);

        if (creationConflict(folder, true))
        return;

        if (!folder.mkdir())
        throw new IOException();


        // Create the elaborations file
        File elaborationsFile = new File(folder.getPath() + File.separator + "elaborations.soar");
        elaborationsFile.createNewFile();

        // Create the elaborations node
        OperatorNode elaborationNode = operatorWindow.createFileOperatorNode("elaborations",elaborationsFile.getName());

        // Create the datamap id
        dataMapId = swmm.createNewStateId(((OperatorNode)getParent()).getStateIdVertex(),name);
        dataMapIdNumber = dataMapId.getValue();

        // Make this node high-level
        isHighLevel = true;

        //Add the elaborations node and folder
        operatorWindow.addChild(this,elaborationNode); 
        folderName = folder.getName();
    }
    
    public void importFunc(Reader r,
                           OperatorWindow operatorWindow,
                           SoarWorkingMemoryModel swmm) throws IOException, NumberFormatException 
    {

        if(!isHighLevel)
        firstTimeAdd(operatorWindow,swmm);
        VSEImporter.read(r,this,operatorWindow,swmm,VSEImporter.HLOPERATOR | VSEImporter.OPERATOR);
    }
    
    public DataFlavor isDropOk(int action,DataFlavor[] dataFlavors)
    {

        if(    (action == java.awt.dnd.DnDConstants.ACTION_LINK)
            || (action == java.awt.dnd.DnDConstants.ACTION_MOVE) )
        {
            List<DataFlavor> flavorList = Arrays.asList(dataFlavors);
            if(flavorList.contains(TransferableOperatorNodeLink.flavors[0])) 
            {
                return TransferableOperatorNodeLink.flavors[0]; 
            }
        }
        return null;
    }
    
    public void exportType(Writer w) throws IOException 
    {

        if(isHighLevel) 
        {
            w.write("IMPORT_TYPE " + VSEImporter.HLOPERATOR + "\n");
        }
        else 
        {
            w.write("IMPORT_TYPE " + VSEImporter.OPERATOR + "\n");
        }
    }
    
    /**
     * Use this getter function to get the path to the folder
     * @return the path to the folder
     */
    public String getFolderName() 
    {

        return getFullPathName();
    }
    
    /**
     * Use this getter function to get the path to the rule file
     * @return the path to the rule file
     */
    public String getFileName() 
    {

        OperatorNode parent = (OperatorNode)getParent();
        return parent.getFullPathName() + File.separator + fileAssociation;
    }

    protected String getFullPathName() 
    {

        if(isHighLevel)
        return ((OperatorNode)getParent()).getFullPathName() + File.separator + folderName;
        else
        return ((OperatorNode)getParent()).getFullPathName();
    }

    public SoarIdentifierVertex getStateIdVertex() 
    {

        return dataMapId;
    }

    /**
   * @return whether an openDataMap() call on this node will work
   */
	public boolean noDataMap()
    {
		return !isHighLevel;
	}

    public void restoreId(SoarWorkingMemoryModel swmm)
    {

        dataMapId = (SoarIdentifierVertex)swmm.getVertexForId(dataMapIdNumber);
    }
    
    /**
     * The user wants to rename this node
     *
     * @param newName the new name that the user wants this node to be called
     */
        
    public void rename(OperatorWindow operatorWindow,
                       String newName) throws IOException 
    {
        //Check for filename conflict
        File oldFile = new File(getFileName());
        File newFile = new File(oldFile.getParent() + File.separator + newName + ".soar");
        if (creationConflict(newFile))
        {
            throw new IOException();
        }

        //Rename the file
        if (!oldFile.renameTo(newFile))
        {
            //Caller must catch this and report fail to user
            throw new IOException();
        }

        //For high-level operators, the folder must also be renamed
        if(isHighLevel) 
        {
            // Check for folder name conflict
            File oldFolder = new File(getFolderName());
            File newFolder = new File(oldFolder.getParent() + File.separator + newName);
            if ((creationConflict(newFolder)) || (creationConflict(newFile)))
            {
                throw new IOException();
            }

            // Rename Folder
            if (!oldFolder.renameTo(newFolder))
            {
                //If folder rename failed change file back to old name
                //(This should never fail since we just renamed it...)
                newFile.renameTo(oldFile);
                throw new IOException();
            }

            folderName = newFolder.getName();
        }//if HL operator

        //Update state to reflect the successful rename
        this.name = newName;
        fileAssociation = newFile.getName();
        if (ruleEditor != null) {
            ruleEditor.fileRenamed(newFile.getPath());
        }
        DefaultTreeModel model = (DefaultTreeModel)operatorWindow.getModel();
        model.nodeChanged(this);

    }//rename

    /**
     * This is the function that gets called when you want to add a suboperator to this node
     *
     * @param newOperatorName the name of the new operator to add
     */
    public OperatorNode addSuboperator(OperatorWindow operatorWindow,
                                       SoarWorkingMemoryModel swmm,
                                       String newOperatorName) throws IOException 
    {
        if(!isHighLevel) firstTimeAdd(operatorWindow, swmm);
        
        File rules = new File(getFullPathName() + File.separator + newOperatorName + ".soar");
        if (creationConflict(rules))
        {
            return this;
        }
        
        if (! rules.createNewFile()) 
        {
            throw new IOException();
        }

        OperatorNode on = operatorWindow.createSoarOperatorNode(newOperatorName,
                                                   rules.getName());
        operatorWindow.addChild(this,on);
        
        SoarVertex opName = swmm.createNewEnumeration(newOperatorName);
        SoarVertex operId = swmm.createNewSoarId();
        swmm.addTriple(dataMapId, "operator", operId);
        swmm.addTriple(operId,"name",opName);

        notifyLinksOfUpdate(operatorWindow);
        sourceChildren();
        return this;
    }//addSuboperator

    /**
     * This is the function that gets called when you want to add a sub Impasse Operator to this node
     *
     * @param newOperatorName the name of the new operator to add
     */
    public OperatorNode addImpasseOperator(OperatorWindow operatorWindow,
                                           SoarWorkingMemoryModel swmm,
                                           String newOperatorName) throws IOException 
    {
        if(!isHighLevel) firstTimeAdd(operatorWindow, swmm);

        File rules = new File(getFullPathName() + File.separator + newOperatorName + ".soar");
        if (creationConflict(rules))
        {
            return this;
        }

        if (! rules.createNewFile()) 
        {
            throw new IOException();
        }

        SoarOperatorNode ion = operatorWindow.createImpasseOperatorNode(newOperatorName,
                                                       rules.getName());
        operatorWindow.addChild(this,ion);
        sourceChildren();

        
        //Automatically create an elaborations file.  Impasses do not have files
        //associated with them directly so, we have to add an elaborations file
        //to the impasse (making it a high level operator) immediately.  I'm not
        //sure if this is the best place for this code design-wise.  But it does
        //work so, I'm leaving it here for the time being.  -:AMN: 20 Oct 03
        try
        {
            ion.firstTimeAdd(operatorWindow, swmm);
        }
        catch(IOException ioe)
        {
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
    public void addFileOperator(OperatorWindow operatorWindow, SoarWorkingMemoryModel swmm, String newFileName) throws IOException
    {
        if(!isHighLevel) firstTimeAdd(operatorWindow,swmm);

        File file = new File(getFullPathName() + File.separator + newFileName + ".soar");
        if(checkCreateReplace(file)) return;

        FileOperatorNode fon = operatorWindow.createFileOperatorNode(newFileName, file.getName());
        operatorWindow.addChild(this,fon);
        sourceChildren();
    }//addFileOperator


    /**
     * Add a linked sub-operator (typically as a result of a drag-and-drop)
     */
    public void addLink(OperatorWindow operatorWindow,LinkNode linkNode) 
    {
        try
        {
            if(!isHighLevel) firstTimeAdd(operatorWindow,operatorWindow.getDatamap());

            operatorWindow.addChild(this,linkNode);
            File rules = new File(linkNode.getFileName());
            rules.createNewFile();
            sourceChildren();
        }
        catch(IOException ioe) { /* quietly fail... */ }
    }//addLink
    
    /**
     * Removes the selected operator from the tree if it is allowed
     *
     */
    public void delete(OperatorWindow operatorWindow) 
    {
        if(isHighLevel)
        {
            if(!linkNodes.isEmpty()) 
            {
                int selOption =
                    JOptionPane.showConfirmDialog(MainFrame.getMainFrame(),
                                                  "This Operator has links, which will be deleted do you want to continue?",
                                                  "Link Deletion Confirmation",
                                                  JOptionPane.YES_NO_OPTION);
                if(selOption == JOptionPane.NO_OPTION) return;
                while(!linkNodes.isEmpty()) {
                    LinkNode linkNodeToDelete = linkNodes.get(0);
                    linkNodeToDelete.delete(operatorWindow);
                }
            }
            renameToDeleted(new File(getFileName()));
            renameToDeleted(new File(getFolderName()));     
            OperatorNode parent = (OperatorNode)getParent();
            operatorWindow.removeNode(this);
            parent.notifyDeletionOfChild(operatorWindow,this);
            
        }
        else //not a HL operator
        {
            renameToDeleted(new File(getFileName()));
            OperatorNode parent = (OperatorNode)getParent();
            operatorWindow.removeNode(this);
            parent.notifyDeletionOfChild(operatorWindow,this);
        }
    }   //delete
    
    
    
    /**
     * A child has been deleted from this node, so check if this node has
     * become a low-level operator now
     *
     */
    public void notifyDeletionOfChild(OperatorWindow operatorWindow,
                                      OperatorNode child) 
    {

        if (getChildCount() == 0) 
        {
            renameToDeleted(new File(getFolderName()));
            isHighLevel = false;
            folderName = null;
            dataMapId = null;
            
            if(!linkNodes.isEmpty()) 
            {
                JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                                              "This node is no longer high-level, links will be deleted.",
                                              "Link deletions",
                                              JOptionPane.INFORMATION_MESSAGE);
                while(!linkNodes.isEmpty()) {
                    LinkNode linkNodeToDelete = linkNodes.get(0);
                    linkNodeToDelete.delete(operatorWindow);
                }
            }
        }
        notifyLinksOfUpdate(operatorWindow);
        try 
        {
            sourceChildren();
        }
        catch(IOException ioe) { /* TODO: something other than quiet fail */ }
    }

    /**
     * This opens/shows a dataMap with this nodes associated Data Map File
     */
    public void openDataMap(SoarWorkingMemoryModel swmm, MainFrame pw)
    {

        DataMap dataMap;


        // Check to see if Operator uses top-state datamap.
        if(dataMapIdNumber != 0) 
        {

            // If File Operator, get the datamap of the first OperatorOperatorNode above it
            if(this instanceof FileOperatorNode) 
            {
                OperatorNode parent = (OperatorNode)this.getParent();
                SoarIdentifierVertex dataMapParent = parent.getStateIdVertex();
                while(    (( parent.getStateIdVertex()).getValue() != 0)
                       && (!(parent instanceof OperatorOperatorNode)) ) 
                {
                    parent = (OperatorNode)parent.getParent();
                    dataMapParent = parent.getStateIdVertex();
                }   
                dataMap = new DataMap(swmm,dataMapParent, parent.toString());
                pw.addDataMap(dataMap);
                pw.getDesktopPane().dmAddDataMap(dataMapParent.getValue(), dataMap);
            }
            else 
            {
                dataMap = new DataMap(swmm,dataMapId,toString());
                pw.addDataMap(dataMap);
                pw.getDesktopPane().dmAddDataMap(dataMapId.getValue(), dataMap);
            }
        }
        else 
        {
            dataMap = new DataMap(swmm, swmm.getTopstate(),toString());
            pw.addDataMap(dataMap);
            pw.getDesktopPane().dmAddDataMap(swmm.getTopstate().getValue(),
                                             dataMap);
        }
      
        dataMap.setVisible(true);
    }

    public SoarIdentifierVertex getState() 
    {

        return dataMapId;
    }

    /**
     * exportDataMap
     *
     * writes current datamap content to a given Writer object
     */
    public void exportDataMap(Writer w) throws IOException 
    {
        w.write("DATAMAP\n");
        MainFrame.getMainFrame().getOperatorWindow().getDatamap().write(w);
    }
    
    public void registerLink(LinkNode inLinkNode)
    {
        linkNodes.add(inLinkNode);
    }
    
    public void removeLink(LinkNode inLinkNode) 
    {

        linkNodes.remove(inLinkNode);
    }
    
    void notifyLinksOfUpdate(OperatorWindow operatorWindow) 
    {
        DefaultTreeModel model = (DefaultTreeModel)operatorWindow.getModel();
        for(LinkNode nodeToUpdate : this.linkNodes) {
            model.nodeStructureChanged(nodeToUpdate);
        }
    }
    
    
    public void copyStructures(File folderToWriteTo) throws IOException 
    {
        super.copyStructures(folderToWriteTo);
        
        if(isHighLevel) 
        {
            File copyOfFolder = new File(folderToWriteTo.getPath() + File.separator + folderName);
            copyOfFolder.mkdir();
            for(int i = 0; i < getChildCount(); ++i) 
            {
                ((OperatorNode)getChildAt(i)).copyStructures(copyOfFolder);
            }       
        }
    }

    /**
     * move
     *
     * moves this operator to a new position in the operator tree
     */
    public boolean move(OperatorWindow operatorWindow, OperatorNode newParent)
    {
        //move the node to its new parent
        if(  (newParent instanceof SoarOperatorNode)
                && !((SoarOperatorNode)newParent).isHighLevel() )
        {
            try 
            {
                ((SoarOperatorNode)newParent).firstTimeAdd(operatorWindow,
                                                           operatorWindow.getDatamap());
            }
            catch(IOException ioe) 
            {
                System.out.println("Move failed, because firstTimeAdd on parent failed");
                return true;
            }
        }

        //check for name conflict (why don't we do this first??)
        if(newParent.creationConflict(new File(newParent.getFolderName() + File.separator + fileAssociation)))
        {
            return true;
        }

        //If moving a high-level operator its folder also needs to be renamed
        if(isHighLevel) 
        {
            // Rename Folder
            File oldFolder = new File(getFolderName());
            File newFolder = new File(newParent.getFolderName() + File.separator + folderName);
            oldFolder.renameTo(newFolder);
            
            // Remove old superstate links
            SoarWorkingMemoryModel swmm = operatorWindow.getDatamap();
            Enumeration<NamedEdge> emanEnum = swmm.emanatingEdges(dataMapId);
            while(emanEnum.hasMoreElements())
            {
                NamedEdge ne = emanEnum.nextElement();
                if(ne.getName().equals("superstate")) {
                    swmm.removeTriple((SoarVertex)ne.V0(),
                            ne.getName(),
                            (SoarVertex)ne.V1());
                }
            }

            // Add new ^superstate link
            SoarVertex soarVertex = newParent.getStateIdVertex();
            if(soarVertex == null) soarVertex = swmm.getTopstate();
            swmm.addTriple(dataMapId,"superstate",soarVertex);
        }//if HL operator
        
        // Rename File
        File oldFile = new File(getFileName());
        File newFile = new File(newParent.getFolderName() + File.separator + fileAssociation);
        oldFile.renameTo(newFile);
        
        DefaultTreeModel model = (DefaultTreeModel)operatorWindow.getModel();
        OperatorNode oldParent = (OperatorNode)getParent();
        model.removeNodeFromParent(this);
        oldParent.notifyDeletionOfChild(operatorWindow,this);
        
        operatorWindow.addChild(newParent,this);
        // Adjust rule editor if one is open
        if (ruleEditor != null)
        ruleEditor.fileRenamed(newFile.getPath());

        return true;
    }   
    
    public void source(Writer w) throws IOException 
    {
        super.source(w);
        if(isHighLevel) 
        {
            String LINE = System.getProperty("line.separator");
            w.write("pushd "  + folderName + LINE + 
                    "source " + folderName + "_source.soar" + LINE + 
                    "popd" + LINE);
        }
    }
    
    public void sourceChildren() throws IOException 
    {

        if(isHighLevel) 
        {
            Writer w = new FileWriter(getFullPathName() + File.separator + folderName + "_source.soar");
            int childCount = getChildCount();
            for(int i = 0; i < childCount; ++i) 
            {
                OperatorNode child = (OperatorNode)getChildAt(i);
                child.source(w);
            }
            w.close();
        }
    }
    
    public void sourceRecursive() throws IOException 
    {

        if(isHighLevel) 
        {
            sourceChildren();
            int childCount = getChildCount();
            for(int i = 0; i < childCount; ++i) 
            {
                OperatorNode child = (OperatorNode)getChildAt(i);
                child.sourceRecursive();
            }
        }
    }

    public void searchTestDataMap(SoarWorkingMemoryModel swmm,
                                  Vector<FeedbackListObject> errors) {}
    public void searchCreateDataMap(SoarWorkingMemoryModel swmm,
                                    Vector<FeedbackListObject> errors) {}
    public void searchTestNoCreateDataMap(SoarWorkingMemoryModel swmm,
                                          Vector<FeedbackListObject> errors) {}
    public void searchCreateNoTestDataMap(SoarWorkingMemoryModel swmm,
                                          Vector<FeedbackListObject> errors) {}
    public void searchNoTestNoCreateDataMap(SoarWorkingMemoryModel swmm,
                                            Vector<FeedbackListObject> errors) {}
}
