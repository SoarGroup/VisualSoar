package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.graph.*;
import edu.umich.soar.visualsoar.util.ReaderUtils;

import java.io.*;
import java.util.Vector;

/**
 * This class contains utilities for reading datamap data from files
 *
 * @author Brad Jones
 * @author Andrew Nuxoll
 */
public class SoarWorkingMemoryReader {

    /**
     * reads one SoarVertex object from a file
     *
     * Note:  will recurse for foreign nodes
     */
    private static SoarVertex readVertex(Reader fr) throws IOException {
        String type = ReaderUtils.getWord(fr);
        SoarVertex vertexToAdd = null;
        int id = ReaderUtils.getInteger(fr);

        //Special Case;  Foreign Node
        if (type.equals("FOREIGN")) {
            String foreignDM = ReaderUtils.getWord(fr);
            SoarVertex foreignSV = readVertex(fr);  //recurse to read foreign vertex
            return new ForeignVertex(id, foreignDM, foreignSV);
        }

        if (type.equals("SOAR_ID")) {
            vertexToAdd = new SoarIdentifierVertex(id);
        } else if (type.equals("ENUMERATION")) {
            int enumerationSize = ReaderUtils.getInteger(fr);
            Vector<String> v = new Vector<>();
            for (int j = 0; j < enumerationSize; ++j)
                v.add(ReaderUtils.getWord(fr, '|'));
            vertexToAdd = new EnumerationVertex(id, v);
        } else if (type.equals("INTEGER_RANGE")) {
            vertexToAdd = new IntegerRangeVertex(id, ReaderUtils.getInteger(fr), ReaderUtils.getInteger(fr));
        } else if (type.equals("INTEGER")) {
            vertexToAdd = new IntegerRangeVertex(id, Integer.MIN_VALUE, Integer.MAX_VALUE);
        } else if (type.equals("FLOAT_RANGE")) {
            vertexToAdd = new FloatRangeVertex(id, ReaderUtils.getFloat(fr), ReaderUtils.getFloat(fr));
        } else if (type.equals("FLOAT")) {
            vertexToAdd = new FloatRangeVertex(id, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        } else if (type.equals("STRING")) {
            vertexToAdd = new StringVertex(id);
        } else {
            System.err.println("Unknown type: please update SoarWorking Memory Reader constructor :" + type);
        }
        return vertexToAdd;
    }


    /**
     * This function reads a description of soars working memory
     * from a file and re-creates the datamap
     *
     * @param swmm the working memory model that the description should be read into
     * @param fr   the Reader that the description should be read from
     * @param cr   the Reader that datamap comments should be read from (if exists)
     * @throws IOException if something goes wrong
     */
    public static void read(SoarWorkingMemoryModel swmm, Reader fr, Reader cr) throws IOException {
        try {
            // Get the number of vertices from the file
            int numberOfVertices = ReaderUtils.getInteger(fr);


            // Get the root node
            String rootType = ReaderUtils.getWord(fr);
            SoarIdentifierVertex topState = null;

            if (rootType.equals("SOAR_ID")) {
                topState = new SoarIdentifierVertex(ReaderUtils.getInteger(fr));
            } else {
                System.err.println("Root type must be Soar id");
            }

            swmm.setTopstate(topState);


            // Get the rest of the vertices
            for (int i = 1; i < numberOfVertices; ++i) {
                SoarVertex vertexToAdd = readVertex(fr);
                swmm.addVertex(vertexToAdd);
            }


            // Get the number edges
            int numberOfEdges = ReaderUtils.getInteger(fr);


            // Check to see if a Comment file existed
            if (cr != null) {
                // Read in the edges and connect them and also read in the comment file
                for (int j = 0; j < numberOfEdges; ++j) {
                    swmm.addTriple(swmm.getVertexForId(ReaderUtils.getInteger(fr)), ReaderUtils.getWord(fr), swmm.getVertexForId(ReaderUtils.getInteger(fr)), ReaderUtils.getInteger(cr), ReaderUtils.getLine(cr));
                }

            } else {
                // Read in the edges and connect them
                for (int j = 0; j < numberOfEdges; ++j) {
                    swmm.addTriple(swmm.getVertexForId(ReaderUtils.getInteger(fr)), ReaderUtils.getWord(fr), swmm.getVertexForId(ReaderUtils.getInteger(fr)));
                }
            }
        } catch (IOException | NumberFormatException ioe) {
            ioe.printStackTrace();
            throw ioe;
        }
    }

    /**
     * reads the data in a given datamap (.dm) file into a given SWMM object
     * (which is expected to be empty)
     *
     * @return the foreign datamap file name (or null on failure)
     */
    public static String readDataIntoSWMM(File dataMapFile, SoarWorkingMemoryModel swmm) {
        //Calculate the name of the comment file
        File commentFile = new File(dataMapFile.getParent() + File.separator + "comment.dm");

        //Open the dm files for reading
        Reader rDM;
        try {
            rDM = new FileReader(dataMapFile);
        } catch (FileNotFoundException fnfe) {
            MainFrame.getMainFrame().setStatusBarError("Error opening " + dataMapFile.getName() + ": " + fnfe.getMessage());
            return null;
        }
        Reader rComment = null;
        try {
            if (commentFile.exists()) {
                rComment = new FileReader(commentFile);
            }
        } catch (FileNotFoundException fnfe) {
            MainFrame.getMainFrame().setStatusBarError("Error opening " + commentFile.getName() + ": " + fnfe.getMessage());
            return null;
        }

        //Read the datamap into memory
        try {
            SoarWorkingMemoryReader.read(swmm, rDM, rComment);
        } catch (IOException ioe) {
            MainFrame.getMainFrame().setStatusBarError("Unable to parse " + dataMapFile.getName() + ": " + ioe.getMessage());
            return null;
        }

        //Close the readers
        try {
            rDM.close();
        } catch (IOException e) {
            //nothing to do?
        }
        try {
            if (rComment != null) rComment.close();
        } catch (IOException e) {
            //nothing to do?
        }

        return dataMapFile.getAbsolutePath();
    }//readDataIntoSWMM

    /**
     * getDMFile
     *
     * Given a project's .vsa file, this method returns its .dm file or null if doesn't exist
     */
    public static File getDMFile(File vsaFile) {
        //What is the file name (no path, no ext) of this project
        String projName = vsaFile.getName();
        projName = projName.substring(0, projName.length() - 4);  //chop off .vsa

        //construct what the .dm filename should be
        String dmPath = vsaFile.getAbsolutePath();
        dmPath = dmPath.substring(0, dmPath.length() - 4); //chop off .vsa
        dmPath += File.separatorChar + projName + ".dm";

        //verify it's there
        File dmFile = new File(dmPath);
        if (!dmFile.exists()) {
            MainFrame.getMainFrame().setStatusBarMsg("Could not find datamap file for this project.  Expected:  " + dmFile.getName());
            return null;
        }

        return dmFile;
    }//getDMFile



    /**
     * reads the data in a given project (.vsa) file into this.swmm
     *
     * @return the foreign datamap file name (or null on failure)
     */
    public static String readDataIntoSWMMfromVSA(File vsaFile, SoarWorkingMemoryModel swmm) {
        //Calculate the name of the .dm file for this project
        File dataMapFile = getDMFile(vsaFile);
        if (dataMapFile == null) {
            System.err.println("Error: null vsa file given to readDataIntoSWMM on foreign datamap import.");
            return null;
        }

        return readDataIntoSWMM(dataMapFile, swmm);

    }//readDataIntoSWMMfromVSA

}//class SoarWOrkingMemoryReader
