package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.graph.*;
import edu.umich.soar.visualsoar.misc.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;
import edu.umich.soar.visualsoar.util.ReaderUtils;

import java.io.*;
import java.util.Scanner;
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
     * reads one SoarVertex object from a file.  Unlike {@link #readVertex}
     * this method does not throw exceptions on a parse error but, instead,
     * logs the error
     *
     * Note:  will recurse for foreign nodes
     *
     * @param line        a line from a .dm file that describes a vertex
     * @param expectedId  this vertex is expected to have the given id.  You can pass -1 if not known.
     * @param errors      any parse error found will be placed in this vertex
     *
     * @return a SoarVertex object or null on failure
     */
    private static SoarVertex readVertexSafe(String line, int expectedId, Vector<FeedbackListEntry> errors)  {
        //Any vertex definition must have at least two words
        if (line == null) return null;
        if (line.trim().length() == 0) return null;
        String[] words = line.split("[ \\t]");  //split on spaces and tabs
        if (words.length < 2) {
            errors.add(new FeedbackListEntry("Error:  truncated datamap entry: " + line));
            return null;
        }

        //Verify a valid type
        boolean valid = false;
        String vertexType = words[0];
        for(String validType : SoarVertex.VERTEX_TYPES) {
            if (validType.equals(vertexType)) {
                valid = true;
                break;
            }
        }
        if (! valid) {
            errors.add(new FeedbackListEntry("Error:  datamap entry has invalid type: " + line));
            return null;
        }

        //Verify a valid id
        int id = -1;
        try {
            id = Integer.parseInt(words[1]);
        }
        catch(NumberFormatException nfe) {
            /* nothing to do here */
        }
        if (id < 0) {
            errors.add(new FeedbackListEntry("Error:  datamap entry has invalid id: " + line));
            return null;
        }
        if ( (expectedId >= 0) && (id != expectedId) ) {
            errors.add(new FeedbackListEntry("Warning:  datamap entry has unexpected id.  Expected " + expectedId + " but found " + id));
        }

        //Special Case:  Foreign Node
        if (vertexType.equals("FOREIGN")) {
            if (words.length < 5) {
                errors.add(new FeedbackListEntry("Error:  truncated FOREIGN datamap entry: " + line));
                return null;
            }

            String foreignDM = words[2];
            StringBuilder subline = new StringBuilder();
            for(int i = 3; i < words.length; ++i) {
                subline.append(words[i]);
                subline.append(" ");
            }
            SoarVertex foreignSV = readVertexSafe(subline.toString(), -1, errors);  //recurse to read foreign vertex
            if (foreignSV == null) {
                return null;
            }
            else {
                return new ForeignVertex(id, foreignDM, foreignSV);
            }
        }

        //SOAR_ID
        SoarVertex vertexToAdd;
        if (vertexType.equals("SOAR_ID")) {
            if (words.length != 2) {
                errors.add(new FeedbackListEntry("Error:  Extraneous data found on SOAR_ID datamap entry: " + line));
            }
            vertexToAdd = new SoarIdentifierVertex(id);
        }

        //ENUMERATION
        else if (vertexType.equals("ENUMERATION")) {
            if (words.length < 4) {
                errors.add(new FeedbackListEntry("Error:  Truncated ENUMERATION datamap entry: " + line));
            }

            //Read in specified number of values for the enumeration
            int enumerationSize = -1;
            try {
                enumerationSize = Integer.parseInt(words[2]);
            }
            catch(NumberFormatException nfe) {
                /* nothing to do here */
            }
            if (enumerationSize <= 0) {
                errors.add(new FeedbackListEntry("Error:  datamap ENUMERATION entry has invalid number of values: " + line));
                return null;
            }
            Vector<String> vals = new Vector<>();
            for(int i = 3; i < words.length; ++i) {
                if (words[i].charAt(0) != '|') {
                    vals.add(words[i]);
                }
                else {
                    //The pipe delimeter can be uesd to include spaces in ENUMERATION values.
                    //This is handled here.
                    StringBuilder compound = new StringBuilder();
                    for(int j = i; j < words.length; ++j) {
                        compound.append(words[i]);
                        if (words[i].endsWith("|")) break;
                        compound.append(" "); //if you use more than one space in your compound enum value this will be a problem
                    }
                    vals.add(compound.toString());
                }
            }//for
            if (vals.size() != enumerationSize) {
                errors.add(new FeedbackListEntry("Warning:  datamap ENUMERATION entry specifies " + enumerationSize + " values but only " + vals.size() + " values were found in line: " + line));
            }
            vertexToAdd = new EnumerationVertex(id, vals);
        }

        //INTEGER_RANGE
        else if (vertexType.equals("INTEGER_RANGE")) {
            int min = Integer.MIN_VALUE;
            int max = Integer.MAX_VALUE;
            if (words.length < 4) {
                errors.add(new FeedbackListEntry("Error:  Truncated INTEGER_RANGE datamap entry: " + line));
            }
            else if (words.length > 4) {
                errors.add(new FeedbackListEntry("Warning:  Extraneous data on INTEGER_RANGE datamap entry: " + line));
            }
            else {
                try {
                    min = Integer.parseInt(words[2]);
                }
                catch(NumberFormatException nfe) {
                    errors.add(new FeedbackListEntry("Error:  Invalid minimum on INTEGER_RANGE datamap entry: " + line));
                }

                try {
                    max = Integer.parseInt(words[3]);
                }
                catch(NumberFormatException nfe) {
                    errors.add(new FeedbackListEntry("Error:  Invalid maximum on INTEGER_RANGE datamap entry: " + line));
                }
            }

            vertexToAdd = new IntegerRangeVertex(id, min, max);
        }

        //INTEGER
        else if (vertexType.equals("INTEGER")) {
            vertexToAdd = new IntegerRangeVertex(id, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        //FLOAT_RANGE
        else if (vertexType.equals("FLOAT_RANGE")) {
            float min = Float.NEGATIVE_INFINITY;
            float max = Float.POSITIVE_INFINITY;
            if (words.length < 4) {
                errors.add(new FeedbackListEntry("Error:  Truncated FLOAT_RANGE datamap entry: " + line));
            }
            else if (words.length > 4) {
                errors.add(new FeedbackListEntry("Warning:  Extraneous data on FLOAT_RANGE datamap entry: " + line));
            }
            else {
                try {
                    min = Float.parseFloat(words[2]);
                }
                catch(NumberFormatException nfe) {
                    errors.add(new FeedbackListEntry("Error:  Invalid minimum on FLOAT_RANGE datamap entry: " + line));
                }

                try {
                    max = Float.parseFloat(words[3]);
                }
                catch(NumberFormatException nfe) {
                    errors.add(new FeedbackListEntry("Error:  Invalid maximum on FLOAT_RANGE datamap entry: " + line));
                }
            }

            vertexToAdd = new FloatRangeVertex(id, min, max);
        }

        //FLOAT
        else if (vertexType.equals("FLOAT")) {
            vertexToAdd = new FloatRangeVertex(id, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        }

        //STRING
        else {
            vertexToAdd = new StringVertex(id);
        }
        return vertexToAdd;
    }//readVertexSafe




    /**
     * This is a new version of the {@link #read} method that handles
     * mis-formatted .dm files without throwing exceptions on parse errors
     *
     * @param swmm the working memory model that the description should be read into
     * @param fr   the Reader that the description should be read from
     * @param cr   the Reader that datamap comments should be read from (if exists)
     */
    public static boolean readSafe(SoarWorkingMemoryModel swmm, Reader fr, Reader cr)  {
        //Any errors found will be stored here and reported at the end
        Vector<FeedbackListEntry> errors = new Vector<>();
        int MAX_SANE = 999999;  //no sane datamap would have more vertices than this

        // Get the number of vertices from the file
        Scanner scanFR = new Scanner(fr);
        String lineOne = scanFR.nextLine().trim();
        int numVertices = -1;
        try {
            numVertices = Integer.parseInt(lineOne);
        }
        catch(NumberFormatException nfe) {
            /* nothing to do here */
        }
        if (numVertices < 1) {
            errors.add(new FeedbackListEntry("Warning: First line of datamap file does not contain a valid number of vertices: " + lineOne));

            //Note:  We could abort here but let's continue with a large number and see if it works out
            numVertices = MAX_SANE;
        }

        // Get the root node
        String lineTwo = scanFR.nextLine().trim();
        String[] words = lineTwo.split("[ \\t]");  //split on spaces and tabs
        if (! words[0].equals("SOAR_ID")) {
            errors.add(new FeedbackListEntry("Error: Root type must be Soar id.  Expected \"SOAR_ID 0\" but found \"" + lineTwo + "\""));
            MainFrame.getMainFrame().setFeedbackListData(errors);
            return false;
        }
        int rootNodeId = -1;
        try {
            rootNodeId = Integer.parseInt(words[1]);
        }
        catch(NumberFormatException | ArrayIndexOutOfBoundsException e) {
            /* no action needed here */
        }
        if (rootNodeId < 0) {
            errors.add(new FeedbackListEntry("Datamap root must have a valid Soar id.  Expected \"SOAR_ID 0\" but found \"" + lineTwo + "\""));
            MainFrame.getMainFrame().setFeedbackListData(errors);
            return false;
        }
        SoarIdentifierVertex topState = new SoarIdentifierVertex(rootNodeId);
        swmm.setTopstate(topState);


        // Get the rest of the vertices
        int numEdges = -1;  //this may be needed below if we reach the value early
        for (int i = 1; i < numVertices; ++i) {
            //check for end of file
            if (!scanFR.hasNextLine()) {
                errors.add(new FeedbackListEntry("Error:  The .dm file appears to be truncated in the vertex list.  Aborting."));
                MainFrame.getMainFrame().setFeedbackListData(errors);
                return false;
            }

            String line = scanFR.nextLine().trim();
            if (line.length() == 0) {
                errors.add(new FeedbackListEntry("Warning:  blank line found in .dm file's vertex list"));
                i--; //to keep the count accurate (hopefully)
                continue;  //skip blank lines
            }

            //check here for a line that's a number only.  That means we've hit the
            //number of edges early (probably)
            if (line.length() < 6) { //no Soar project will have more than 99999 edges, right?
                try {
                    numEdges = Integer.parseInt(line);
                } catch (NumberFormatException nfe) {
                    errors.add(new FeedbackListEntry("Warning:  Ignoring invalid vertex list entry in .dm file: " + line));
                    continue;
                }

                if (numEdges > 0) {
                    if (numVertices < MAX_SANE) {  //This indicates we weren't given a max (see init of numVertices above)
                        errors.add(new FeedbackListEntry("Warning:  reached end of vertex list section early in .dm file.  Was expecting " + numVertices + " vertices but found only " + (i - 1) + " vertices."));
                    }
                    break;
                }
            }//if

            //Okay, we're finally ready to parse the vertex definition
            SoarVertex vertexToAdd = readVertexSafe(line, i, errors);
            if (vertexToAdd != null) {
                swmm.addVertex(vertexToAdd);

                //If the id is out of sync, fix it here
                i = vertexToAdd.getValue();
            }

            //Note:  no need for an 'else' here as readVertexSafe() should have added a report to the errors list
        }//for


        // Get the number edges
        if (numEdges < 0) {
            String line = scanFR.nextLine().trim();
            try {
                Integer.parseInt(line);
            }
            catch(NumberFormatException nfe) {
                errors.add(new FeedbackListEntry("Warning:  expecting a number of edges in .dm file but found this instead: " + line));
                //Note:  this method doesn't need to know how many edges there are anyway so just continue
            }
        }

        //If the comment file exists, read in all its comments
        Vector<String> commentVec = new Vector<>();
        if (cr != null) {
            Scanner scanCR = new Scanner(cr);
            while (scanCR.hasNextLine()) {
                commentVec.add(scanCR.nextLine());
            }
        }

        //Read in the edges
        int currEdgeNum = -1;  //keep track so we know which comment line to use
        while(scanFR.hasNextLine()) {
            currEdgeNum++;
            String line = scanFR.nextLine().trim();
            if (line.length() == 0) {
                errors.add(new FeedbackListEntry("Warning:  blank line found in .dm file's vertex list"));
                continue;  //skip blank lines
            }

            words = line.split("[ \\t]");  //split on spaces and tabs
            if (words.length != 3) {
                errors.add(new FeedbackListEntry("Warning:  Ignoring truncated datamap edge entry: " + line));
                continue;
            }

            //get the parent vertex
            int parentVertexId;
            try {
                parentVertexId = Integer.parseInt(words[0]);
            }
            catch(NumberFormatException nfe) {
                errors.add(new FeedbackListEntry("Warning:  Ignoring datamap edge entry with unparsable parent vertex id: " + line));
                continue;
            }
            SoarVertex parentVertex;
            try {
                parentVertex = swmm.getVertexForId(parentVertexId);
            }
            catch(ArrayIndexOutOfBoundsException aioobe) {
                errors.add(new FeedbackListEntry("Warning:  Ignoring datamap edge entry with invalid parent vertex id: " + line));
                continue;
            }

            //get the edge name
            String edgeName = words[1];
            if (! OperatorWindow.operatorNameIsValid(edgeName)) {
                errors.add(new FeedbackListEntry("Warning:  Ignoring datamap edge entry with invalid edge name: " + line));
                continue;
            }

            //get the child vertex
            int childVertexId;
            try {
                childVertexId = Integer.parseInt(words[2]);
            }
            catch(NumberFormatException nfe) {
                errors.add(new FeedbackListEntry("Warning:  Ignoring datamap edge entry with unparsable child vertex id: " + line));
                continue;
            }
            SoarVertex childVertex;
            try {
                childVertex = swmm.getVertexForId(childVertexId);
            }
            catch(ArrayIndexOutOfBoundsException aioobe) {
                errors.add(new FeedbackListEntry("Warning:  Ignoring datamap edge entry with invalid child vertex id: " + line));
                continue;
            }

            //If there are no comments then we can create a triple now
            if (commentVec.size() <= currEdgeNum) {
                swmm.addTriple(parentVertex, edgeName, childVertex);
                continue;
            }

            //Retrieve the 'generated' status (0 or 1)
            line = commentVec.get(currEdgeNum);
            int generated;
            if (line.charAt(0) == '0') {
                generated = 0;
            }
            else if (line.charAt(0) == '1') {
                generated = 1;
            }
            else {
                errors.add(new FeedbackListEntry("Warning:  Ignoring datamap comment.dm entry has invalid 'generated' status: " + line));
                generated = 0;  //assume a default
            }

            //Retrieve any comment
            String commentText = line.substring(1).trim();
            swmm.addTriple(parentVertex, edgeName, childVertex, generated, commentText);
        }//while

        //if any issues were found, report them to the user
        if (errors.size() > 0) {
            MainFrame.getMainFrame().setFeedbackListData(errors);
        }
        return true;

    }//readSafe

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
        boolean success = SoarWorkingMemoryReader.readSafe(swmm, rDM, rComment);
        if (!success) {
            MainFrame.getMainFrame().setStatusBarError("Unable to parse " + dataMapFile.getName());
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
     * Given a project's .vsa file, this method returns its .dm file or null if it doesn't exist
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
