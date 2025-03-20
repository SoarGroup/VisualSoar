package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.ProjectModel;
import edu.umich.soar.visualsoar.files.projectjson.DMVertex;
import edu.umich.soar.visualsoar.files.projectjson.Datamap;
import edu.umich.soar.visualsoar.files.projectjson.Project;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.graph.*;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.util.ReaderUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * This class contains utilities for reading datamap data from files
 *
 * @author Brad Jones
 * @author Andrew Nuxoll
 */
public class SoarWorkingMemoryReader {

    /**
     * This should not be used by clients; it is package-private for testing purposes.
     * <p/>
     * reads one SoarVertex object from a file
     *
     * Note:  will recurse for foreign nodes
     */
    static SoarVertex readVertex(Reader fr) throws IOException {
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
   * This should not be used by clients; it is package-private for testing purposes.
   *
   * <p>reads one SoarVertex object from a file. Unlike {@link #readVertex} this method does not
   * throw exceptions on a parse error but, instead, logs the error.
   *
   * <p>Note: will recurse for foreign nodes
   *
   * @param line a line from a .dm file that describes a vertex
   * @param expectedId this vertex is expected to have the given id. You can pass -1 if not known.
   * @param errors any parse error found will be placed in this vertex
   * @return a SoarVertex object or null on failure
   */
  static SoarVertex readVertexSafe(
      @NotNull String line, int expectedId, @NotNull Vector<FeedbackListEntry> errors) {
    SoarVertex vertexToAdd = null;
    Reader lineReader = new StringReader(line);

    try {
      String vertexType = ReaderUtils.getWord(lineReader);
      if (!SoarVertex.VERTEX_TYPES.contains(vertexType)) {
        errors.add(new FeedbackListEntry("Error:  datamap entry has invalid type: " + line));
        return null;
      }

      int id = -1;
      try {
        id = ReaderUtils.getInteger(lineReader);
      } catch (NumberFormatException ignored) {
        /* handled below */
      }
      if (id < 0) {
        errors.add(new FeedbackListEntry("Error:  datamap entry has invalid id: " + line));
        return null;
      } else if ((expectedId >= 0) && (id != expectedId)) {
        errors.add(
            new FeedbackListEntry(
                "Warning:  datamap entry has unexpected id.  Expected "
                    + expectedId
                    + " but found "
                    + id));
      }

      // Special Case;  Foreign Node
      switch (vertexType) {
        case "FOREIGN":
          {
            String foreignDM = ReaderUtils.getWord(lineReader);
            String remainingLine = ReaderUtils.getLine(lineReader);
            SoarVertex foreignSV =
                readVertexSafe(remainingLine, -1, errors); // recurse to read foreign vertex
            if (foreignSV == null) {
              return null;
            }
            vertexToAdd = new ForeignVertex(id, foreignDM, foreignSV);
            break;
          }
        case "SOAR_ID":
          {
            vertexToAdd = new SoarIdentifierVertex(id);
            break;
          }
        case "ENUMERATION":
          {
            int enumerationSize = -1;
            try {
              enumerationSize = ReaderUtils.getInteger(lineReader);
            } catch (NumberFormatException nfe) {
              /* handled below */
            }
            if (enumerationSize <= 0) {
              errors.add(
                  new FeedbackListEntry(
                      "Error:  datamap ENUMERATION entry has invalid number of values: " + line));
              return null;
            }

            Vector<String> vals = new Vector<>();
            for (int j = 0; j < enumerationSize; ++j) {
              vals.add(ReaderUtils.getWord(lineReader, '|'));
            }
            if (vals.size() != enumerationSize) {
              errors.add(
                  new FeedbackListEntry(
                      "Warning:  datamap ENUMERATION entry specifies "
                          + enumerationSize
                          + " values but "
                          + vals.size()
                          + " values were found in line: "
                          + line));
            }

            vertexToAdd = new EnumerationVertex(id, vals);
            break;
          }
        case "INTEGER_RANGE":
          {
            int low = Integer.MIN_VALUE - 1;
            try {
              low = ReaderUtils.getInteger(lineReader);
            } catch (NumberFormatException ignored) {
              errors.add(
                  new FeedbackListEntry(
                      "Error:  Invalid minimum on INTEGER_RANGE datamap entry: " + line));
            }
            int high = -1;
            try {
              high = ReaderUtils.getInteger(lineReader);
            } catch (NumberFormatException ignored) {
              errors.add(
                  new FeedbackListEntry(
                      "Error:  Invalid maximum on INTEGER_RANGE datamap entry: " + line));
            }
            vertexToAdd = new IntegerRangeVertex(id, low, high);
            break;
          }
        case "INTEGER":
          {
            vertexToAdd = new IntegerRangeVertex(id, Integer.MIN_VALUE, Integer.MAX_VALUE);
            break;
          }
        case "FLOAT_RANGE":
          {
            float low = Float.NEGATIVE_INFINITY;
            try {
              low = ReaderUtils.getFloat(lineReader);
            } catch (NumberFormatException ignored) {
              errors.add(
                  new FeedbackListEntry(
                      "Error:  Invalid minimum on FLOAT_RANGE datamap entry: " + line));
            }
            float high = Float.POSITIVE_INFINITY;
            try {
              high = ReaderUtils.getFloat(lineReader);
            } catch (NumberFormatException ignored) {
              errors.add(
                  new FeedbackListEntry(
                      "Error:  Invalid maximum on FLOAT_RANGE datamap entry: " + line));
            }
            vertexToAdd = new FloatRangeVertex(id, low, high);
            break;
          }
        case "FLOAT":
          {
            vertexToAdd =
                new FloatRangeVertex(id, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
            break;
          }
        case "STRING":
          {
            vertexToAdd = new StringVertex(id);
            break;
          }
        default:
          {
            System.err.println(
                "Unknown type in datamap entry. Please update readVertexSafe to handle " + vertexType + ".");
            break;
          }
      }
      if (!ReaderUtils.getWord(lineReader).isEmpty()) {
        errors.add(
            new FeedbackListEntry("Error:  Extraneous data found in datamap entry. Fix and reload project or risk losing data. Line: " + line));
      }
    } catch (IOException e) {
      // This is purely programmer error if it ever happens, as we've created the StringReader
      // ourselves
      errors.add(
          new FeedbackListEntry(
              "Error:  Unknown logic error led to an IOException while reading entry: " + line));
    }
    return vertexToAdd;
  } // readVertexSafe

  /**
   * This is a new version of the {@link #read} method that handles mis-formatted .dm files without
   * throwing exceptions on parse errors
   *
   * @param swmm the working memory model that the description should be read into
   * @param fr the Reader that the description should be read from
   * @param cr the Reader that datamap comments should be read from (if exists)
   */
  public static boolean readSafe(SoarWorkingMemoryModel swmm, Reader fr, Reader cr) {
    // Any errors found will be stored here and reported at the end
    Vector<FeedbackListEntry> errors = new Vector<>();

    try {
      int MAX_SANE = 999999; // no sane datamap would have more vertices than this

      // Get the number of vertices from the file
      Scanner scanFR = new Scanner(fr);
      String lineOne = scanFR.nextLine().trim();
      int numVertices = -1;
      try {
        numVertices = Integer.parseInt(lineOne);
      } catch (NumberFormatException nfe) {
        /* nothing to do here */
      }
      if (numVertices < 1) {
        errors.add(
            new FeedbackListEntry(
                "Warning: First line of datamap file does not contain a valid number of vertices: "
                    + lineOne));

        // Note:  We could abort here but let's continue with a large number and see if it works out
        numVertices = MAX_SANE;
      }

      // Get the root node
      String lineTwo = scanFR.nextLine().trim();
      String[] words = lineTwo.split("[ \\t]"); // split on spaces and tabs
      if (!words[0].equals("SOAR_ID")) {
        errors.add(
            new FeedbackListEntry(
                "Error: Root type must be Soar id.  Expected \"SOAR_ID 0\" but found \""
                    + lineTwo
                    + "\""));
        MainFrame.getMainFrame().getFeedbackManager().showFeedback(errors);
        return false;
      }
      int rootNodeId = -1;
      try {
        rootNodeId = Integer.parseInt(words[1]);
      } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
        /* no action needed here */
      }
      if (rootNodeId < 0) {
        errors.add(
            new FeedbackListEntry(
                "Datamap root must have a valid Soar id.  Expected \"SOAR_ID 0\" but found \""
                    + lineTwo
                    + "\""));
        MainFrame.getMainFrame().getFeedbackManager().showFeedback(errors);
        return false;
      }
      SoarIdentifierVertex topState = new SoarIdentifierVertex(rootNodeId);
      swmm.setTopstate(topState);

      // Get the rest of the vertices
      int numEdges = -1; // this may be needed below if we reach the value early
      for (int i = 1; i < numVertices; ++i) {
        // check for end of file
        if (!scanFR.hasNextLine()) {
          errors.add(
              new FeedbackListEntry(
                  "Error:  The .dm file appears to be truncated in the vertex list.  Aborting."));
          MainFrame.getMainFrame().getFeedbackManager().showFeedback(errors);
          return false;
        }

        String line = scanFR.nextLine().trim();
        if (line.length() == 0) {
          errors.add(new FeedbackListEntry("Warning:  blank line found in .dm file's vertex list"));
          i--; // to keep the count accurate (hopefully)
          continue; // skip blank lines
        }

        // check here for a line that's a number only.  That means we've hit the
        // number of edges early (probably)
        if (line.length() < 6) { // no Soar project will have more than 99999 edges, right?
          try {
            numEdges = Integer.parseInt(line);
          } catch (NumberFormatException nfe) {
            errors.add(
                new FeedbackListEntry(
                    "Warning:  Ignoring invalid vertex list entry in .dm file: " + line));
            continue;
          }

          if (numEdges > 0) {
            if (numVertices
                < MAX_SANE) { // This indicates we weren't given a max (see init of numVertices
                              // above)
              errors.add(
                  new FeedbackListEntry(
                      "Warning:  reached end of vertex list section early in .dm file.  Was expecting "
                          + numVertices
                          + " vertices but found only "
                          + (i - 1)
                          + " vertices."));
            }
            break;
          }
        } // if

        // Okay, we're finally ready to parse the vertex definition
        SoarVertex vertexToAdd = readVertexSafe(line, i, errors);
        if (vertexToAdd != null) {
          swmm.addVertex(vertexToAdd);

          // If the id is out of sync, fix it here
          i = vertexToAdd.getValue();
        }

        // Note:  no need for an 'else' here as readVertexSafe() should have added a report to the
        // errors list
      } // for

      // Get the number edges
      if (numEdges < 0) {
        String line = scanFR.nextLine().trim();
        try {
          Integer.parseInt(line);
        } catch (NumberFormatException nfe) {
          errors.add(
              new FeedbackListEntry(
                  "Warning:  expecting a number of edges in .dm file but found this instead: "
                      + line));
          // Note:  this method doesn't need to know how many edges there are anyway so just
          // continue
        }
      }

      // If the comment file exists, read in all its comments
      Vector<String> commentVec = new Vector<>();
      if (cr != null) {
        Scanner scanCR = new Scanner(cr);
        int lineNum = 0;
        while (scanCR.hasNextLine()) {
          lineNum++;
          String line = scanCR.nextLine();
          if (line.isEmpty()) {
            errors.add(
                new FeedbackListEntry(
                    "Warning:  ignoring blank line at comment.dm line " + lineNum));
            continue;
          }
          commentVec.add(line);
        }
      }

      // Read in the edges
      int currEdgeNum = -1; // keep track so we know which comment line to use
      while (scanFR.hasNextLine()) {
        currEdgeNum++;
        String line = scanFR.nextLine().trim();
        if (line.length() == 0) {
          errors.add(new FeedbackListEntry("Warning:  blank line found in .dm file's vertex list"));
          continue; // skip blank lines
        }

        words = line.split("[ \\t]"); // split on spaces and tabs
        if (words.length != 3) {
          errors.add(
              new FeedbackListEntry("Warning:  Ignoring truncated datamap edge entry: " + line));
          continue;
        }

        // get the parent vertex
        int parentVertexId;
        try {
          parentVertexId = Integer.parseInt(words[0]);
        } catch (NumberFormatException nfe) {
          errors.add(
              new FeedbackListEntry(
                  "Warning:  Ignoring datamap edge entry with unparsable parent vertex id: "
                      + line));
          continue;
        }
        SoarVertex parentVertex;
        try {
          parentVertex = swmm.getVertexForId(parentVertexId);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
          errors.add(
              new FeedbackListEntry(
                  "Warning:  Ignoring datamap edge entry with invalid parent vertex id: " + line));
          continue;
        }

        // get the edge name
        String edgeName = words[1];
        if (!ProjectModel.operatorNameIsValid(edgeName)) {
          errors.add(
              new FeedbackListEntry(
                  "Warning:  Ignoring datamap edge entry with invalid edge name: " + line));
          continue;
        }

        // get the child vertex
        int childVertexId;
        try {
          childVertexId = Integer.parseInt(words[2]);
        } catch (NumberFormatException nfe) {
          errors.add(
              new FeedbackListEntry(
                  "Warning:  Ignoring datamap edge entry with unparsable child vertex id: "
                      + line));
          continue;
        }
        SoarVertex childVertex;
        try {
          childVertex = swmm.getVertexForId(childVertexId);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
          errors.add(
              new FeedbackListEntry(
                  "Warning:  Ignoring datamap edge entry with invalid child vertex id: " + line));
          continue;
        }

        // If there are no comments then we can create a triple now
        if (commentVec.size() <= currEdgeNum) {
          swmm.addTriple(parentVertex, edgeName, childVertex);
          continue;
        }

        // Retrieve the 'generated' status (0 or 1)
        line = commentVec.get(currEdgeNum);
        int generated;
        if (line.charAt(0) == '0') {
          generated = 0;
        } else if (line.charAt(0) == '1') {
          generated = 1;
        } else {
          errors.add(
              new FeedbackListEntry(
                  "Warning:  Ignoring datamap comment.dm entry has invalid 'generated' status: "
                      + line));
          generated = 0; // assume a default
        }

        // Retrieve any comment
        String commentText = line.substring(1).trim();
        swmm.addTriple(parentVertex, edgeName, childVertex, generated, commentText);
      } // while
    } catch (Throwable e) {
      e.printStackTrace();
      errors.add(
          new FeedbackListEntry(
              "Could not load datamap due to Exception: " + e.getMessage(), true));
    }

    // if any issues were found, report them to the user
    if (errors.size() > 0) {
      MainFrame.getMainFrame().getFeedbackManager().showFeedback(errors);
    }
    // return true if no errors were found
    return errors.stream().filter(FeedbackListEntry::isError).findAny().isEmpty();
  } // readSafe

    /**
     * loads the data in a given datamap (.dm, .vsa.json) file
     *
     * @return the foreign datamap file name (or null on failure)
     */
    public static SoarWorkingMemoryModel loadSWMM(File dataMapFile) {
        if(dataMapFile.getName().endsWith(".json")) {
          try {
            Project projectJson = Project.loadJsonFile(dataMapFile.toPath());
            return loadFromJson(projectJson.datamap, dataMapFile.toPath());
          }catch(IOException e) {
            MainFrame.getMainFrame().getFeedbackManager().setStatusBarError("Error opening " + dataMapFile.getName() + ": " + e.getMessage());
          }
        }

        SoarWorkingMemoryModel swmm = new SoarWorkingMemoryModel(false, null, dataMapFile.toPath());
        //Calculate the name of the comment file
        File commentFile = new File(dataMapFile.getParent() + File.separator + "comment.dm");

        //Open the dm files for reading
        Reader rDM;
        try {
            rDM = new FileReader(dataMapFile);
        } catch (FileNotFoundException fnfe) {
            MainFrame.getMainFrame().getFeedbackManager().setStatusBarError("Error opening " + dataMapFile.getName() + ": " + fnfe.getMessage());
            return null;
        }
        Reader rComment = null;
        try {
            if (commentFile.exists()) {
                rComment = new FileReader(commentFile);
            }
        } catch (FileNotFoundException fnfe) {
            MainFrame.getMainFrame().getFeedbackManager().setStatusBarError("Error opening " + commentFile.getName() + ": " + fnfe.getMessage());
            return null;
        }

        //Read the datamap into memory
        boolean success = SoarWorkingMemoryReader.readSafe(swmm, rDM, rComment);
        if (!success) {
            MainFrame.getMainFrame().getFeedbackManager().setStatusBarError("Unable to parse " + dataMapFile.getName());
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

        return swmm;
    }//readDataIntoSWMM

    /**
     * getDMFile
     *
     * Given a project's .vsa file, this method returns its .dm file or null if it doesn't exist
     */
    public static File getDMFile(File vsaFile) {
      // JSON project files contain the DM within the same file
      if (vsaFile.getName().endsWith(".json")) {
        return vsaFile;
      }

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
            MainFrame.getMainFrame().getFeedbackManager().setStatusBarMsg("Could not find datamap file for this project.  Expected:  " + dmFile.getName());
            return null;
        }

        return dmFile;
    }//getDMFile

  /**
   * reads the DM data in a given project (.vsa, .vsa.json) file
   *
   * @return the foreign datamap file name (or null on failure)
   */
  public static SoarWorkingMemoryModel readDataIntoSWMMfromVSA(File vsaFile) {
    Objects.requireNonNull(vsaFile, "Error: readDataIntoSWMMfromVSA: vsaFile must not be null");
    // Calculate the name of the .dm file for this project
    File dataMapFile = getDMFile(vsaFile);
    if (dataMapFile == null) {
      System.err.println(
          "Error: readDataIntoSWMMfromVSA could not figure out DM file from vsaFile " + vsaFile);
      return null;
    }

    return loadSWMM(dataMapFile);
  }

  public static SoarWorkingMemoryModel loadFromJson(Datamap datamap, Path dmPath) {
    SoarWorkingMemoryModel swmm = new SoarWorkingMemoryModel(false, null, dmPath);

    // First pass: translate JSON string IDs to internal integer IDs and find/add root
    Map<String, Integer> jsonIdsToInternalIds = new HashMap<>();
    DMVertex jsonRootVertex = null;
    int idCounter = 1;
    for (DMVertex v : datamap.vertices) {
      if (jsonIdsToInternalIds.containsKey(v.id)) {
        throw new IllegalArgumentException("Multiple datamap vertices use the ID '" + v.id + "'");
      }
      // root is required to be 0 for internals to work!
      if (datamap.rootId.equals(v.id)) {
        jsonIdsToInternalIds.put(v.id, 0);
        jsonRootVertex = v;
      } else {
        jsonIdsToInternalIds.put(v.id, idCounter);
        idCounter++;
      }
    }

    // Root must be added first to avoid array index out of bounds errors in underlying vector of vertices
    if (jsonRootVertex == null) {
      throw new IllegalArgumentException("Datamap root ID is '" + datamap.rootId + "', but no such vertex was found");
    } else {
      SoarVertex converted = vertexFromJson(jsonRootVertex, jsonIdsToInternalIds, false);
      if (!(converted instanceof SoarIdentifierVertex)) {
        throw new IllegalArgumentException(
          "Root datamap vertex (" + jsonRootVertex.id + ") must be of type "
            + DMVertex.VertexType.SOAR_ID
            + ", but found "
            + jsonRootVertex.type);
      }
      swmm.setTopstate((SoarIdentifierVertex) converted);
    }

    // Second pass: convert and add non-root vertices
    for (DMVertex jsonVertex : datamap.vertices) {
      SoarVertex converted = vertexFromJson(jsonVertex, jsonIdsToInternalIds, false);
      swmm.addVertex(converted);
      if (datamap.rootId.equals(jsonVertex.id)) {
        // already added above
        continue;
      }
    }

    // Third pass: convert edges
    for (DMVertex jsonVertex : datamap.vertices) {
      if (jsonVertex instanceof DMVertex.SoarIdVertex) {
        int tailId = jsonIdsToInternalIds.get(jsonVertex.id);
        SoarVertex tailVertex = swmm.getVertexForId(tailId);
        readEdgesFromJson(swmm, tailVertex, (DMVertex.SoarIdVertex) jsonVertex, jsonIdsToInternalIds);
      } else if(jsonVertex instanceof DMVertex.ForeignVertex){
        DMVertex.ForeignVertex foreignJsonVertex = (DMVertex.ForeignVertex) jsonVertex;
        if (foreignJsonVertex.importedVertex instanceof DMVertex.SoarIdVertex) {
          // parent of imported vertex is the tail
          int tailId = jsonIdsToInternalIds.get(jsonVertex.id);
          SoarVertex tailVertex = swmm.getVertexForId(tailId);
          readEdgesFromJson(swmm, tailVertex, (DMVertex.SoarIdVertex) foreignJsonVertex.importedVertex, jsonIdsToInternalIds);
        }

      }
    }
    return swmm;
  }

  private static void readEdgesFromJson(
      SoarWorkingMemoryModel swmm,
      SoarVertex tailVertex,
      DMVertex.SoarIdVertex jsonVertex,
      Map<String, Integer> jsonIdsToInternalIds) {
    for (DMVertex.OutEdge edge : jsonVertex.outEdges) {
      int headId = jsonIdsToInternalIds.get(edge.toId);
      SoarVertex headVertex = swmm.getVertexForId(headId);
      if (headVertex == null) {
        throw new IllegalArgumentException(
            "toId value \""
                + edge.toId
                + "\" in edge from vertex \""
                + jsonVertex.id
                + "\" does not specify any known vertex.");
      }
      swmm.addTriple(
          tailVertex,
          edge.getName(),
          headVertex,
          edge.getGenerated() ? 1 : 0,
          edge.comment != null ? edge.comment : "");
    }
  }

  private static SoarVertex vertexFromJson(
      DMVertex jsonVertex, Map<String, Integer> jsonIdsToInternalIds, boolean isImported) {
    int intId;
    if (isImported) {
      // imported vertices are not directly added to SoarWorkingMemoryModel; use a negative value to
      // trigger obvious errors in case we ever do that
      intId = -1337;
    } else {
      intId = jsonIdsToInternalIds.get(jsonVertex.id);
    }
    switch (jsonVertex.type) {
      case SOAR_ID:
        return new SoarIdentifierVertex(intId, jsonVertex.id);
      case ENUMERATION:
        Vector<String> choices = new Vector<>(((DMVertex.EnumerationVertex) jsonVertex).choices);
        return new EnumerationVertex(intId, jsonVertex.id, choices);
      case FLOAT:
        DMVertex.FloatRangeVertex jsonFloatRangeVertex = (DMVertex.FloatRangeVertex) jsonVertex;
        return new FloatRangeVertex(
            intId, jsonVertex.id, jsonFloatRangeVertex.min, jsonFloatRangeVertex.max);
      case FOREIGN:
        DMVertex.ForeignVertex jsonForeignVertex = (DMVertex.ForeignVertex) jsonVertex;
        SoarVertex foreignVertex =
            vertexFromJson(jsonForeignVertex.importedVertex, jsonIdsToInternalIds, true);
        return new ForeignVertex(
            intId, jsonVertex.id, jsonForeignVertex.foreignDMPath, foreignVertex);
      case INTEGER:
        DMVertex.IntegerRangeVertex jsonIntegerRangeVertex =
            (DMVertex.IntegerRangeVertex) jsonVertex;
        return new IntegerRangeVertex(
            intId, jsonVertex.id, jsonIntegerRangeVertex.min, jsonIntegerRangeVertex.max);
      case STRING:
        return new StringVertex(intId, jsonVertex.id);
      default:
        throw new IllegalArgumentException("Unknown node type " + jsonVertex.type);
    }
  }
}//class SoarWOrkingMemoryReader
