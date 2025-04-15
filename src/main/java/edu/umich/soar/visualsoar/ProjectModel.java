package edu.umich.soar.visualsoar;

import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryReader;
import edu.umich.soar.visualsoar.files.projectjson.Datamap;
import edu.umich.soar.visualsoar.files.projectjson.Json;
import edu.umich.soar.visualsoar.files.projectjson.LayoutNode;
import edu.umich.soar.visualsoar.files.projectjson.Project;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.graph.SoarVertex;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackManager;
import edu.umich.soar.visualsoar.operatorwindow.*;
import edu.umich.soar.visualsoar.parser.SoarProduction;
import edu.umich.soar.visualsoar.util.ReaderUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryReader.loadFromJson;

/**
 * A VisualSoar project has a datamap and an operator hierarchy (project layout). This class
 * encapsulates the project data, handling reading and writing of the project file as well as
 * project changes and validations.
 *
 * Note: Some of the logic still resides in {@link OperatorWindow}, and should be moved here.
 */
public class ProjectModel {
  int nextId = 1;

  // TODO: make final
  public SoarWorkingMemoryModel swmm;
  public DefaultTreeModel operatorHierarchy;

  public ProjectModel(DefaultTreeModel operatorHierarchy, SoarWorkingMemoryModel swmm) {
    this.operatorHierarchy = operatorHierarchy;
    this.swmm = swmm;
  }

  /**
   * Initializes a brand-new project with a default operator hierarchy and datamap.
   *
   * @param projectPath path to the project .vsa.json file
   */
  public static ProjectModel newProject(String projectName, Path projectPath) {
    SoarWorkingMemoryModel swmm = new SoarWorkingMemoryModel(true, projectName, projectPath);
    ProjectModel pm =
        new ProjectModel(new DefaultTreeModel(new DefaultMutableTreeNode("Dummy")), swmm);
    pm.operatorHierarchy = pm.createDefaultProjectLayout(projectName, projectPath);
    return pm;
  }

  public static ProjectModel openExistingProject(Path projectFile)
      throws IOException {
    ProjectModel pm =
        new ProjectModel(
            new DefaultTreeModel(new DefaultMutableTreeNode("Dummy")),
            new SoarWorkingMemoryModel(false, null, null));
    pm.openHierarchy(projectFile.toFile());
    return pm;
  }

  /**
   * Saves the current project to disk
   *
   * @param inProjFile name of the file to be saved - .vsa file
   * @see TreeSerializer#toJson(DefaultTreeModel)
   * @see SoarWorkingMemoryModel#toJson()
   */
  public void writeProject(File inProjFile) throws IOException {
    Datamap dmJson = swmm.toJson();
    LayoutNode layoutNodeJson = TreeSerializer.toJson(operatorHierarchy);
    Project project = new Project(dmJson, layoutNodeJson);
    Json.writeJsonToFile(Paths.get(inProjFile.getAbsolutePath()), project);
  }

  /**
   * Attempts to reduce Working Memory by finding all vertices that are unreachable from a state and
   * adds them to a list of holes so that they can be recycled for later use
   *
   * @see SoarWorkingMemoryModel#reduce(java.util.List)
   */
  public void reduceWorkingMemory() {
    List<SoarVertex> vertList = new LinkedList<>();
    Enumeration<TreeNode> e =
        ((DefaultMutableTreeNode) operatorHierarchy.getRoot()).breadthFirstEnumeration();
    while (e.hasMoreElements()) {
      OperatorNode on = (OperatorNode) e.nextElement();
      SoarVertex v = on.getStateIdVertex(swmm);
      if (v != null) {
        vertList.add(v);
      }
    }
    swmm.reduce(vertList);
  }

  /** Returns a breadth first enumeration of the nodes of the operator hierarchy */
  public Enumeration<TreeNode> breadthFirstEnumeration() {
    return ((DefaultMutableTreeNode) (operatorHierarchy.getRoot())).breadthFirstEnumeration();
  }

  /**
   * Given the associated Operator Node, a vector of parsed soar productions, and a list to put the
   * errors in, this function will check the productions' consistency against the datamap.
   *
   * @see SoarProduction
   * @see SoarWorkingMemoryModel#checkProduction
   */
  public void checkProductions(
      OperatorNode parent,
      OperatorNode child,
      Vector<SoarProduction> productions,
      List<FeedbackListEntry> errors) {

    // Find the state that these productions should be checked against
    SoarIdentifierVertex siv = parent.getStateIdVertex(swmm);
    if (siv == null) {
      siv = swmm.getTopstate();
    }
    Enumeration<SoarProduction> prodEnum = productions.elements();

    while (prodEnum.hasMoreElements()) {
      SoarProduction sp = prodEnum.nextElement();
      errors.addAll(swmm.checkProduction(child, siv, sp));
    }
  }

  /**
   * Constructs a DefaultTreeModel and associated files exactly the way we decided how to do it.
   * Creates a root node named after the project name at the root of the tree. Children of that are
   * an 'all' folder node, a tcl file node called '_firstload' and an 'elaborations' folder node.
   * Children of the elaborations folder include two file operator nodes called '_all' and
   * 'top-state'. Also created is the datamap file called <project name> + '.dm'
   *
   * @param projectName name of the project
   * @param projectFile name of project's .vsa file
   * @see OperatorRootNode
   * @see FileOperatorNode
   * @see FolderNode
   */
  private DefaultTreeModel createDefaultProjectLayout(String projectName, Path projectFile) {
    File parent = new File(projectFile.getParent() + File.separator + projectName);
    File elabFolder = new File(parent.getPath() + File.separator + "elaborations");
    File allFolder = new File(parent.getPath() + File.separator + "all");
    File initFile =
        new File(parent.getPath() + File.separator + "initialize-" + projectName + ".soar");
    File tclFile = new File(parent.getPath() + File.separator + "_firstload.soar");
    parent.mkdir();

    elabFolder.mkdir();
    allFolder.mkdir();

    try {
      // Root node
      OperatorRootNode root =
          createOperatorRootNode(projectName, parent.getParent(), parent.getName());

      // Elaborations Folder
      FolderNode elaborationsFolderNode = createFolderNode("elaborations", elabFolder.getName());
      File topStateElabsFile = new File(elabFolder, "top-state.soar");
      File allFile = new File(elabFolder, "_all.soar");
      topStateElabsFile.createNewFile();
      allFile.createNewFile();
      writeOutTopStateElabs(topStateElabsFile);
      writeOutAllElabs(allFile);

      // Initialize File
      initFile.createNewFile();
      writeOutInitRules(initFile, projectName);

      // TCL file
      tclFile.createNewFile();

      // Construct the tree
      root.add(createFileOperatorNode("_firstload", tclFile.getName()));
      root.add(createFolderNode("all", allFolder.getName()));
      root.add(elaborationsFolderNode);
      elaborationsFolderNode.add(createFileOperatorNode("_all", allFile.getName()));
      elaborationsFolderNode.add(createFileOperatorNode("top-state", topStateElabsFile.getName()));
      root.add(createSoarOperatorNode("initialize-" + projectName, initFile.getName()));

      return new DefaultTreeModel(root);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
    return null;
  }

  /**
   * Checks name entries for illegal values
   *
   * @param theName the name entered
   * @return true if a valid name false otherwise
   */
  public static boolean operatorNameIsValid(String theName) {

    for (int i = 0; i < theName.length(); i++) {
      char testChar = theName.charAt(i);
      if (!(Character.isLetterOrDigit(testChar) || (testChar == '-') || (testChar == '_'))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Checks name entries for illegal values
   *
   * @param theName the name entered
   * @return true if a valid name false otherwise
   */
  public static boolean isProjectNameValid(String theName) {
    return operatorNameIsValid(theName);
  }

  /** Returns the next id used for keeping track of each operator's datamap */
  public final int getNextId() {
    return nextId++;
  }

  /**
   * Creates the Root Node of the operator hierarchy. From here all sub operators branch. This is
   * the method called when a new project is created
   *
   * @param inName name of the root node, should be the same as the project name
   * @param inFullPathStart full path of the project
   * @param inFolderName created folder name, same as project name
   * @see OperatorRootNode
   */
  public OperatorRootNode createOperatorRootNode(
      String inName, String inFullPathStart, String inFolderName) {
    return OperatorRootNode.rootNodeForNewProject(inName, getNextId(), inFullPathStart, inFolderName);
  }

  /**
   * Opens up an existing operator hierarchy
   *
   * @param in_file the file that describes the operator hierarchy
   * @see #openVersionFour(FileReader, String)
   */
  private void openHierarchy(File in_file) throws IOException, NumberFormatException {
    if (in_file.getName().endsWith(".json")) {
      openProjectJson(in_file.toPath());
    } else {
      FileReader fr = new FileReader(in_file);
      String buffer = ReaderUtils.getWord(fr);
      if (buffer.compareToIgnoreCase("VERSION") == 0) {
        int versionId = ReaderUtils.getInteger(fr);
        if (versionId == 4) {
          openVersionFour(fr, in_file.getParent());
        } else if (versionId == 3) {
          openVersionThree(fr, in_file.getParent());
        } else if (versionId == 2) {
          openVersionTwo(fr, in_file.getParent());
        } else if (versionId == 1) {
          openVersionOne(fr, in_file.getParent());
        } else {
          throw new IOException("Invalid Version Number" + versionId);
        }
      }
    }
  }

  /**
   * Opens a Version One Operator Hierarchy file
   *
   * @see #readVersionOne
   * @see SoarWorkingMemoryReader#readSafe
   */
  private void openVersionOne(FileReader fr, String parentPath)
      throws IOException, NumberFormatException {
    String relPathToDM = ReaderUtils.getWord(fr);

    // If this project was created on one platform and ported to another
    // The wrong file separator might be present
    if (relPathToDM.charAt(0) != File.separatorChar) {
      char c = (File.separatorChar == '/') ? '\\' : '/';
      relPathToDM = relPathToDM.replace(c, File.separatorChar);
    }

    File dataMapFile = new File(parentPath + relPathToDM);
    File commentFile = new File(dataMapFile.getParent() + File.separator + "comment.dm");

    readVersionOne(fr);
    OperatorRootNode root = (OperatorRootNode) operatorHierarchy.getRoot();
    root.setFullPath(parentPath);
    fr.close();

    Reader r = new FileReader(dataMapFile);

    // If a comment file exists, then make sure that it gets read in by the reader.
    boolean success;
    if (commentFile.exists()) {
      Reader rComment = new FileReader(commentFile);
      success = SoarWorkingMemoryReader.readSafe(swmm, r, rComment);
      rComment.close();
    } else {
      success = SoarWorkingMemoryReader.readSafe(swmm, r, null);
    }
    r.close();
    if (!success) {
      MainFrame.getMainFrame()
          .getFeedbackManager()
          .setStatusBarError("Unable to parse " + dataMapFile.getName());
    }
    restoreStateIds();
  }

  /**
   * Opens a Version two Operator Hierarchy file
   *
   * @see #readVersionTwo
   * @see SoarWorkingMemoryReader#readSafe
   */
  private void openVersionTwo(FileReader fr, String parentPath)
      throws IOException, NumberFormatException {
    String relPathToDM = ReaderUtils.getWord(fr);
    // If this project was created on one platform and ported to another
    // The wrong file separator might be present
    if (relPathToDM.charAt(0) != File.separatorChar) {
      char c = (File.separatorChar == '/') ? '\\' : '/';
      relPathToDM = relPathToDM.replace(c, File.separatorChar);
    }

    File dataMapFile = new File(parentPath + relPathToDM);
    File commentFile = new File(dataMapFile.getParent() + File.separator + "comment.dm");

    readVersionTwo(fr);
    OperatorRootNode root = (OperatorRootNode) operatorHierarchy.getRoot();
    root.setFullPath(parentPath);
    fr.close();

    Reader r = new FileReader(dataMapFile);

    // If a comment file exists, then make sure that it gets read in by the reader.
    boolean success;
    if (commentFile.exists()) {
      Reader rComment = new FileReader(commentFile);
      success = SoarWorkingMemoryReader.readSafe(swmm, r, rComment);
      rComment.close();
    } else {
      success = SoarWorkingMemoryReader.readSafe(swmm, r, null);
    }
    r.close();
    if (!success) {
      MainFrame.getMainFrame()
          .getFeedbackManager()
          .setStatusBarError("Unable to parse " + dataMapFile.getName());
    }
    restoreStateIds();
  }

  /**
   * Opens a Version Three Operator Hierarchy file
   *
   * @see #readVersionThree
   * @see SoarWorkingMemoryReader#readSafe
   */
  private void openVersionThree(FileReader fr, String parentPath)
      throws IOException, NumberFormatException {
    String relPathToDM = ReaderUtils.getWord(fr);
    // If this project was created on one platform and ported to another
    // The wrong file separator might be present
    if (relPathToDM.charAt(0) != File.separatorChar) {
      char c = (File.separatorChar == '/') ? '\\' : '/';
      relPathToDM = relPathToDM.replace(c, File.separatorChar);
    }

    File dataMapFile = new File(parentPath + relPathToDM);
    File commentFile = new File(dataMapFile.getParent() + File.separator + "comment.dm");
    readVersionThree(fr);
    OperatorRootNode root = (OperatorRootNode) operatorHierarchy.getRoot();
    root.setFullPath(parentPath);
    fr.close();

    Reader r = new FileReader(dataMapFile);
    // If a comment file exists, then make sure that it gets read in by the reader.
    boolean success;
    if (commentFile.exists()) {
      Reader rComment = new FileReader(commentFile);
      success = SoarWorkingMemoryReader.readSafe(swmm, r, rComment);
      rComment.close();
    } else {
      success = SoarWorkingMemoryReader.readSafe(swmm, r, null);
    }
    r.close();
    if (!success) {
      MainFrame.getMainFrame()
          .getFeedbackManager()
          .setStatusBarError("Unable to parse " + dataMapFile.getName());
    }
    restoreStateIds();
  }

  /**
   * Opens a Version Four Operator Hierarchy file
   *
   * @see #readVersionFourSafe(Reader)
   * @see SoarWorkingMemoryReader#readSafe
   */
  private void openVersionFour(FileReader fr, String parentPath)
      throws IOException, NumberFormatException {
    String relPathToDM = ReaderUtils.getWord(fr);
    // If this project was created on one platform and ported to another
    // The wrong file separator might be present
    if (relPathToDM.charAt(0) != File.separatorChar) {
      char c = (File.separatorChar == '/') ? '\\' : '/';
      relPathToDM = relPathToDM.replace(c, File.separatorChar);
    }

    File dataMapFile = new File(parentPath + relPathToDM);
    File commentFile = new File(dataMapFile.getParent() + File.separator + "comment.dm");
    readVersionFourSafe(fr);
    OperatorRootNode root = (OperatorRootNode) operatorHierarchy.getRoot();
    root.setFullPath(parentPath);
    fr.close();

    Reader r = new FileReader(dataMapFile);
    // If a comment file exists, then make sure that it gets read in by the reader.
    boolean success;
    if (commentFile.exists()) {
      Reader rComment = new FileReader(commentFile);
      success = SoarWorkingMemoryReader.readSafe(swmm, r, rComment);
      rComment.close();
    } else {
      success = SoarWorkingMemoryReader.readSafe(swmm, r, null);
    }
    r.close();
    if (!success) {
      MainFrame.getMainFrame()
          .getFeedbackManager()
          .setStatusBarError("Unable to parse " + dataMapFile.getName());
    }

    restoreStateIds();
  }

  // JSON uses string IDs, VSTreeNode uses integers, so we generate integer IDs and store their
  // association here
  private static class IdProvider {
    private int count = 0;
    private final Map<String, Integer> serializationIdToId = new HashMap<>();

    int getId(String serializationId) {
      return serializationIdToId.computeIfAbsent(serializationId, sid -> count++);
    }
  }

  private void openProjectJson(Path jsonPath) throws IOException {
    Project projectJson = Project.loadJsonFile(jsonPath);
    this.swmm = loadFromJson(projectJson.datamap, jsonPath);

    Map<Integer, OperatorNode> idToNode = new HashMap<>();
    List<OperatorNode> linkNodes = new ArrayList<>();
    VSTreeNode root =
        loadOperatorHierarchy(
            projectJson.layout, null, idToNode, linkNodes, new IdProvider(), this.swmm);
    for (OperatorNode node : linkNodes) {
      LinkNode linkNodeToRestore = (LinkNode) node;
      linkNodeToRestore.restore(idToNode);
    }

    operatorHierarchy = new DefaultTreeModel(root);

    OperatorRootNode orNode = (OperatorRootNode) root;
    orNode.setFullPath(jsonPath.getParent().toString());

    restoreStateIds();
  }

  /**
   * The VSA file contains operators and their DM ID numbers. This helper connects the Soar IDs
   * loaded from the datamap to the high-level operator nodes loaded from the VSA file.
   */
  private void restoreStateIds() {
    Enumeration<TreeNode> nodeEnum =
        ((OperatorRootNode) operatorHierarchy.getRoot()).breadthFirstEnumeration();
    while (nodeEnum.hasMoreElements()) {
      Object o = nodeEnum.nextElement();
      if (o instanceof SoarOperatorNode) {
        SoarOperatorNode son = (SoarOperatorNode) o;
        if (son.isHighLevel()) {
          son.restoreId(swmm);
        }
      }
    }
  }

  private VSTreeNode loadOperatorHierarchy(
      LayoutNode jsonNode,
      OperatorNode parent,
      Map<Integer, OperatorNode> idToNode,
      List<OperatorNode> linkNodes,
      IdProvider idProvider,
      SoarWorkingMemoryModel swmm) {
    OperatorNode node = createNodeFromJson(jsonNode, idProvider, swmm);
    idToNode.put(node.getId(), node);
    if (node instanceof LinkNode) {
      linkNodes.add(node);
    }

    if (parent != null) {
      addChild(parent, node);
    }
    if (!jsonNode.children.isEmpty()) {
      for (LayoutNode child : jsonNode.children) {
        loadOperatorHierarchy(child, node, idToNode, linkNodes, idProvider, swmm);
      }
    }
    return node;
  }

  /*
   * Method inserts an Operator Node into the Operator Hierarchy tree in
   * alphabetical order preferenced in order of [FileOperators], [SoarOperators],
   * and [ImpasseOperators].
   *
   * @param parent operator of operator to be inserted
   * @param child operator to be inserted into tree
   * @see DefaultTreeModel#insertNodeInto(MutableTreeNode, MutableTreeNode, int)
   */
  public void addChild(OperatorNode parent, OperatorNode child) {
    // Put in alphabetical order in order of [Files], [Operators], [Impasses]
    boolean found = false;

    for (int i = 0; i < parent.getChildCount() && !found; ++i) {
      String childName = child.toString();
      String sl = childName.toLowerCase();
      String childString = (parent.getChildAt(i)).toString();

      // Check for duplicate
      if (childName.compareTo(parent.getChildAt(i).toString()) == 0) {
        JOptionPane.showMessageDialog(
            MainFrame.getMainFrame(),
            "Node conflict for " + childName,
            "Node Conflict",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (!(childString.equals("_firstload")
          || childString.equals("all")
          || childString.equals("elaborations"))) {
        // Adding an Impasse Node
        if (child instanceof ImpasseOperatorNode) {
          if ((sl.compareTo(childString.toLowerCase()) <= 0)) {
            found = true;
            operatorHierarchy.insertNodeInto(child, parent, i);
          }
        }
        // Adding a SoarOperatorNode
        else if (child instanceof OperatorOperatorNode) {
          if (parent.getChildAt(i) instanceof OperatorOperatorNode
              && sl.compareTo(childString.toLowerCase()) <= 0) {
            found = true;
            operatorHierarchy.insertNodeInto(child, parent, i);
          }
        }
        // Adding a File
        else {
          if ((parent.getChildAt(i) instanceof OperatorOperatorNode)
              || (sl.compareTo(childString.toLowerCase()) <= 0)) {
            found = true;
            operatorHierarchy.insertNodeInto(child, parent, i);
          }
        }
      }
    } // go through all the children until find the proper spot for the new child
    if (!found) {
      operatorHierarchy.insertNodeInto(child, parent, parent.getChildCount());
    }
  } // end of addChild()

  private OperatorNode createNodeFromJson(
      LayoutNode node, IdProvider idProvider, SoarWorkingMemoryModel swmm) {
    int id = idProvider.getId(node.id);
    switch (node.type) {
      case FILE:
        {
          LayoutNode.File fileNode = (LayoutNode.File) node;
          return new FileNode(fileNode.name, id, node.id, fileNode.file);
        }
      case OPERATOR:
        {
          LayoutNode.Operator oNode = (LayoutNode.Operator) node;
          return new OperatorOperatorNode(oNode.name, id, node.id, oNode.file);
        }
      case FILE_OPERATOR:
        {
          LayoutNode.FileOperator foNode = (LayoutNode.FileOperator) node;
          return new FileOperatorNode(foNode.name, id, node.id, foNode.file);
        }
      case OPERATOR_ROOT:
        {
          LayoutNode.OperatorRoot orNode = (LayoutNode.OperatorRoot) node;
          return new OperatorRootNode(orNode.name, node.id, id, orNode.folder);
        }
      case LINK:
        {
          LayoutNode.Link lNode = (LayoutNode.Link) node;
          return new LinkNode(
              lNode.name, id, node.id, lNode.file, idProvider.getId(lNode.linkedNodeId));
        }
      case FOLDER:
        {
          LayoutNode.Folder folderNode = (LayoutNode.Folder) node;
          return new FolderNode(folderNode.name, id, node.id, folderNode.folder);
        }
      case IMPASSE_OPERATOR:
        {
          LayoutNode.ImpasseOperator ioNode = (LayoutNode.ImpasseOperator) node;
          return new ImpasseOperatorNode(ioNode.name, id, node.id, ioNode.file);
        }
      case HIGH_LEVEL_OPERATOR:
        {
          LayoutNode.HighLevelOperator hloNode = (LayoutNode.HighLevelOperator) node;
          SoarVertex dmVertex = getVertexForDmId(swmm, node.id, hloNode.dmId);
          int dmId = dmVertex.getValue();
          return new OperatorOperatorNode(
              hloNode.name, id, node.id, hloNode.file, hloNode.folder, dmId);
        }
      case HIGH_LEVEL_FILE_OPERATOR:
        {
          LayoutNode.HighLevelFileOperator hlfoNode = (LayoutNode.HighLevelFileOperator) node;
          SoarVertex dmVertex = getVertexForDmId(swmm, node.id, hlfoNode.dmId);
          int dmId = dmVertex.getValue();
          return new FileOperatorNode(
              hlfoNode.name, id, node.id, hlfoNode.file, hlfoNode.folder, dmId);
        }
      case HIGH_LEVEL_IMPASSE_OPERATOR:
        {
          LayoutNode.HighLevelImpasseOperator hlioNode = (LayoutNode.HighLevelImpasseOperator) node;
          SoarVertex dmVertex = getVertexForDmId(swmm, node.id, hlioNode.dmId);
          int dmId = dmVertex.getValue();
          return new ImpasseOperatorNode(
              hlioNode.name, id, node.id, hlioNode.file, hlioNode.folder, dmId);
        }
      default:
        throw new IllegalArgumentException("Unknown layout node type: " + node.type);
    }
  }

  private static SoarVertex getVertexForDmId(
      SoarWorkingMemoryModel swmm, String layoutNodeId, String dmId) {
    SoarVertex sv = swmm.getVertexForSerializationId(dmId);
    if (sv == null) {
      throw new IllegalArgumentException(
          "Operator node '"
              + layoutNodeId
              + "' has dmId='"
              + dmId
              + "', but no such datamap vertex exists");
    }
    return sv;
  }

  /**
   * helper method for integrityCheck() methods to extract the highest numbered operator node id
   * from the lines of the file.
   *
   * @param lines a list of lines that may contain operator ids. There are presumed to be no blank
   *     lines and all lines should already be trimmed.
   * @return the highest node found or -1 if none found
   */
  private int getLastOpIdFromLines(Vector<String> lines) {
    int lastId = -1;
    for (String line : lines) {
      String[] words = line.split("[ \\t]"); // split on spaces and tabs
      int nodeId = -1;
      try {
        nodeId = Integer.parseInt(words[0]);
      } catch (NumberFormatException nfe) {
        // just ignore this line.  This method does not report parse errors
      }

      if (nodeId > lastId) lastId = nodeId;
    } // for

    return lastId;
  } // getLastOpIdFromLines

  /**
   * helper method for integrityCheckV4. It reads through a list of lines that contain operator node
   * definitions and places them in the proper slots in an array. Dummy entries are inserted for
   * missing nodes
   *
   * @param blankless original lines from the file that should contain operator node definitions
   * @param errors Any missing or duplicate nodes are reported here.
   * @return an array with exactly one entry for each node
   */
  private String[] fillOperatorNodeLineArray(
      Vector<String> blankless, Vector<FeedbackListEntry> errors) {
    // Get the id number of the last valid line in the file.  This should also tell us the number of
    // nodes
    int lastId = getLastOpIdFromLines(blankless);
    if (lastId < 0) return null; // no operator node ids found!

    // Since we now have an exact number of nodes, create an array to store each associated line
    // This will allow us to detect duplicate and missing operator nodes
    String[] nodeLines = new String[lastId + 1]; // +1 because id numbers start at zero
    for (int i = 0; i < blankless.size(); ++i) {
      String line = blankless.get(i);

      // Extract the node id from the line
      String[] words = line.split("[ \\t]"); // split on spaces and tabs
      int foundNodeId = -1;
      boolean errFlag = false;
      try {
        foundNodeId = Integer.parseInt(words[0]);
      } catch (NumberFormatException nfe) {
        errFlag = true;
      }

      // Check for mismatch or invalid id number
      if (foundNodeId != i) {
        errFlag = true;
      }

      // report any unexpected node id
      if (errFlag) {
        String err = "Found invalid operator node id: " + foundNodeId;
        err += " from this entry: " + line;
        errors.add(new FeedbackListEntry(err));
      }

      // Check for duplicate id
      if (nodeLines[foundNodeId] != null) {
        String err = "Skipping duplicate entry for operator node with id: " + foundNodeId;
        err += " original entry: " + nodeLines[foundNodeId];
        err += " duplicate entry: " + line;
        errors.add(new FeedbackListEntry(err));
      } else {
        nodeLines[foundNodeId] = line;
      }
    } // for

    // Detect if any node ids are missing
    for (int i = 0; i < nodeLines.length; ++i) {
      if (nodeLines[i] == null) {
        String err = "No entry found for operator node with id: " + i + ".";
        err += " A dummy entry will be substituted.";
        errors.add(new FeedbackListEntry(err));

        nodeLines[i] = i + "\t0\tFOPERATOR dummy" + i + "  dummy" + i + ".soar 0";
      }
    }

    return nodeLines;
  } // fillOperatorNodeLineArray

  /**
   * given a mal-formatted operator node string, this method tries to find a valid identifier string
   * in it
   *
   * @param nodeLine search this string
   */
  private String findIdentifier(String nodeLine) {
    String[] words = nodeLine.split("[ \\t]");
    for (String word : words) {
      // doesn't begin with a letter
      if (!Character.isAlphabetic(word.charAt(0))) continue;

      // check for invalid letters
      boolean invalidChar = false;
      for (int i = 0; i < word.length(); ++i) {
        char c = word.charAt(i);
        if (Character.isLetterOrDigit(c)) continue;
        if (c == '_') continue;
        invalidChar = true;
        break;
      }
      if (invalidChar) continue;

      // check for reserved words
      boolean conflict = false;
      for (String nodeTypeStr : OperatorNode.VSA_NODE_TYPES) {
        if (word.equals(nodeTypeStr)) {
          conflict = true;
          break;
        }
      }
      if (conflict) continue;

      // If we get this far it's a valid identifier string.
      // Heuristically, the first one we find is likely correct so use it
      return word;
    } // for

    // no valid id name found
    return null;
  } // findIdentifier

  /**
   * scans each node line of a given version 4 .vsa file for integrity issues
   *
   * @param lines the lines of the .vsa file. The first two lines of the file (version number and
   *     relative path to .dm file) are presumed to be absent.
   * @param errors any errors found will be placed in this vector
   * @return the valid operator node lines of the file (or null on unrecoverable failure)
   */
  private String[] integrityCheckV4(Vector<String> lines, Vector<FeedbackListEntry> errors) {
    // The last line should be 'END'.
    String lastLine = lines.lastElement().trim();
    if (!lastLine.equals("END")) {
      errors.add(
          new FeedbackListEntry(
              "[line "
                  + lines.size()
                  + "] Project file (.vsa) is truncated.  Some operator nodes may be missing.",
              true));
    }
    lines.remove(lines.size() - 1); // remove "END" so remaining lines are consistent

    // Remove any blank lines
    int skipped = 2; // number of lines skipped so far (counting the version num and .dm file name)
    Vector<String> blankless = new Vector<>();
    for (int i = 0; i < lines.size(); ++i) {
      String line = lines.get(i).trim();
      if (line.length() == 0) {
        errors.add(
            new FeedbackListEntry(
                "Error on line " + (i + skipped + 1) + " of .vsa file: illegal blank line ignored.",
                true));
        skipped++;
        continue;
      }
      blankless.add(line);
    }

    // The lines should be numbered sequentially with no blank lines but things may be jumbled.
    // Sort things out and make sure there is exactly one line for each id
    String[] nodeLines = fillOperatorNodeLineArray(blankless, errors);
    if (nodeLines == null) return null;

    // Verify the ROOT node's format
    String[] words = nodeLines[0].split("[ \\t]");
    if ((words.length != 5)
        || (!words[0].trim().equals("0"))
        || (!words[1].trim().equals("ROOT"))
        || (!words[2].equals(words[3]))) {
      String err = "Root node has improper format.";
      err += "  Expecting '0\\tROOT <name> <name> 1'";
      err += "  Received '" + nodeLines[0] + "' instead.";
      errors.add(new FeedbackListEntry(err, true));

      // Replace this node with something in the correct format
      String projName = findIdentifier(nodeLines[0]);
      if (projName == null) projName = "Unknown_Project";
      nodeLines[0] = "0\tROOT " + projName + " " + projName + " 1";
    }

    return nodeLines;
  } // integrityCheckV4

  /**
   * readVersionFourSafe
   *
   * <p>Safely (in terms of corrupted input file handling) read a version four .vsa file.
   *
   * @param r the Reader of the .vsa project file
   * @see #makeNodeVersionFourSafe(String, int, Vector)
   */
  private void readVersionFourSafe(Reader r) {
    // This hash table has keys of ids and a pointer as a value
    // it is used for parent lookup
    Hashtable<Integer, VSTreeNode> nodeTable = new Hashtable<>();

    // Read in the entire file
    Vector<String> lines = new Vector<>();
    Scanner scan = new Scanner(r);
    while (scan.hasNextLine()) {
      lines.add(scan.nextLine());
    }

    // Check for any file format problems.  This method will also return an
    // array of all lines that describe an operator node
    Vector<FeedbackListEntry> errors = new Vector<>();
    String[] nodeLines = integrityCheckV4(lines, errors);

    // Check each operator node line for errors (skipping ROOT)
    int errCount = 0;
    if (nodeLines != null) {
      for (int i = 1; i < nodeLines.length; ++i) {
        verifyV4OperatorLine(nodeLines[i], nodeLines.length, errors);
      }
    }

    // Report errors
    if (errors.size() > 0) {
      FeedbackManager fb = MainFrame.getMainFrame().getFeedbackManager();
      fb.showFeedback(errors);
      fb.showFeedback(
          new FeedbackListEntry(
              "TAKE NOTE: Saving this project may result in data loss. "
                  + "If you think you can fix the above errors yourself, please fix them and re-open the project.",
              true));

      // TODO:  At this point we could present the user with these choices:
      //       a) abort the project load operation
      //       b) proceed, attempting to detect and skip any issues
      //       c) attempt to rebuild the .vsa file from the project's source files
      // At the moment (July 2024) this is beyond the scope of the project
    }

    if (nodeLines == null) return; // abort, as nothing to load

    // Special Case: Root Node
    String[] words = nodeLines[0].split("[ \\t]");
    VSTreeNode root = createOperatorRootNode(words[2], words[2]);
    nodeTable.put(0, root);

    // Parse all the other nodes
    for (int i = 1; i < nodeLines.length; ++i) {
      OperatorNode node = makeNodeVersionFourSafe(nodeLines[i], nodeLines.length - 1, errors);

      // If no node is given, then the line was invalid.  That should only happen if the user
      // has requested that a corrupted file be read anyway.  So, skip this invalid line.
      if (node == null) continue;

      // add the node to the tree
      words = nodeLines[i].split("[ \\t]"); // split on spaces and tabs
      int parentId = -1;
      try {
        parentId = Integer.parseInt(words[1]);
      } catch (NumberFormatException nfe) {
        /* no action needed here */
      }
      if (parentId > -1) {
        OperatorNode parent = (OperatorNode) nodeTable.get(parentId);
        addChild(parent, node);

        // add the node to the hash table
        nodeTable.put(i, node);
      }
    }

    operatorHierarchy = new DefaultTreeModel(root);
  } // readVersionFourSafe

  /**
   * Reads a Version Three .vsa project file and interprets it to create a Visual Soar project from
   * the file.
   *
   * @param r the Reader of the .vsa project file
   * @see #makeNodeVersionThree(HashMap, java.util.List, Reader)
   */
  private void readVersionThree(Reader r) throws IOException, NumberFormatException {
    // This hash table has keys of ids and a pointer as a value
    // it is used for parent lookup
    Hashtable<Integer, VSTreeNode> ht = new Hashtable<>();
    List<VSTreeNode> linkNodesToRestore = new LinkedList<>();
    HashMap<Integer, VSTreeNode> persistentIdLookup = new HashMap<>();

    // Special Case Root Node
    // tree specific stuff
    int rootId = ReaderUtils.getInteger(r);

    // node stuff
    VSTreeNode root = makeNodeVersionThree(persistentIdLookup, linkNodesToRestore, r);

    // add the new node to the hash table
    ht.put(rootId, root);

    // Read in all the other nodes
    boolean done = false;
    for (; ; ) {
      // again read in the tree specific stuff
      String nodeIdOrEnd = ReaderUtils.getWord(r);
      if (!nodeIdOrEnd.equals("END")) {
        int nodeId = Integer.parseInt(nodeIdOrEnd);
        int parentId = ReaderUtils.getInteger(r);

        // get the parent
        OperatorNode parent = (OperatorNode) ht.get(parentId);

        // read in the node
        OperatorNode node = makeNodeVersionThree(persistentIdLookup, linkNodesToRestore, r);
        addChild(parent, node);

        // add that node to the hash table
        ht.put(nodeId, node);
      } else {
        for (VSTreeNode vsTreeNode : linkNodesToRestore) {
          LinkNode linkNodeToRestore = (LinkNode) vsTreeNode;
          linkNodeToRestore.restore(persistentIdLookup);
        }
        operatorHierarchy = new DefaultTreeModel(root);
        return;
      }
    }
  }

  /**
   * Reads a Version Two .vsa project file and interprets it to create a Visual Soar project from
   * the file.
   *
   * @param r the Reader of the .vsa project file
   * @see #makeNodeVersionTwo(HashMap, java.util.List, Reader)
   */
  private void readVersionTwo(Reader r) throws IOException, NumberFormatException {
    // This hash table has keys of ids and a pointer as a value
    // it is used for parent lookup
    Hashtable<Integer, VSTreeNode> ht = new Hashtable<>();
    List<VSTreeNode> linkNodesToRestore = new LinkedList<>();
    HashMap<Integer, VSTreeNode> persistentIdLookup = new HashMap<>();

    // Special Case Root Node
    // tree specific stuff
    int rootId = ReaderUtils.getInteger(r);

    // node stuff
    VSTreeNode root = makeNodeVersionTwo(persistentIdLookup, linkNodesToRestore, r);

    // add the new node to the hash table
    ht.put(rootId, root);

    // Read in all the other nodes
    while (r.ready()) {
      // again read in the tree specific stuff
      int nodeId = ReaderUtils.getInteger(r);
      int parentId = ReaderUtils.getInteger(r);

      // get the parent
      OperatorNode parent = (OperatorNode) ht.get(parentId);

      // read in the node
      OperatorNode node = makeNodeVersionTwo(persistentIdLookup, linkNodesToRestore, r);

      addChild(parent, node);

      // add that node to the hash table
      ht.put(nodeId, node);
    }
    for (VSTreeNode vsTreeNode : linkNodesToRestore) {
      LinkNode linkNodeToRestore = (LinkNode) vsTreeNode;
      linkNodeToRestore.restore(persistentIdLookup);
    }
    operatorHierarchy = new DefaultTreeModel(root);
  }

  /**
   * verifies that a given line from a .vsa file that describes a node is valid. If it's invalid, it
   * attempts to recreate the line as best it can
   *
   * @param line to verify
   * @param maxOpId the maximum valid operator id (used to verify parent id)
   * @param errors any errors found are reported here
   * @return 'true' if line appears valid; 'false' otherwise
   */
  private boolean verifyV4OperatorLine(String line, int maxOpId, Vector<FeedbackListEntry> errors) {
    // Sanity check: no null input!
    if (line == null) {
      // Note:  this should never happen
      errors.add(new FeedbackListEntry("Error!  Received null operator node line."));
      return false;
    }

    // The line should contain at least 6 words
    String[] words = line.split("[ \\t]"); // split on spaces and tabs
    if (words.length < 6) {
      errors.add(new FeedbackListEntry("Incomplete operator node line found: " + line, true));
      return false;
    }
    // High level operators need 8 words
    boolean isHLOperator = words[2].startsWith("HL");
    if (isHLOperator && (words.length < 8)) {
      errors.add(
          new FeedbackListEntry(
              "Incomplete high-level operator node line found in .vsa file: " + line, true));
      return false;
    }

    // --------------------------------------------------------------------
    // 0. Node id (should already have been checked)
    try {
      Integer.parseInt(words[0]);
    } catch (NumberFormatException nfe) {
      errors.add(
          new FeedbackListEntry("Error!  Operator node line has invalid id number: " + line, true));
      return false;
    }

    // --------------------------------------------------------------------
    // 1. Parent id
    int parentId = -1;
    try {
      parentId = Integer.parseInt(words[1]);
    } catch (NumberFormatException nfe) {
      errors.add(
          new FeedbackListEntry(
              "Error!  Operator node line has invalid parent id number: " + line, true));
      return false;
    }
    if ((parentId < 0) || (parentId > maxOpId)) {
      errors.add(
          new FeedbackListEntry(
              "Error: Operator node line (\""
                  + line
                  + "\") has an invalid parent id number: "
                  + parentId,
              true));
      return false;
    }

    // --------------------------------------------------------------------
    // 2. Node Type
    // node type must be in the list of valid types
    boolean isValid = false;
    for (String validType : OperatorNode.VSA_NODE_TYPES) {
      if (validType.equals(words[2])) {
        isValid = true;
        break;
      }
    }
    if (!isValid) {
      errors.add(
          new FeedbackListEntry("Error!  Operator node line has unknown type: " + line, true));
      return false;
    }

    // Check for extraneous ROOT
    if (words[2].equals("ROOT")) {
      errors.add(
          new FeedbackListEntry(
              "Error!  Non-root operator node line has ROOT type: " + line, true));
      return false;
    }

    // Check for types that aren't used in V4
    if ((words[2].equals("LINK")) || (words[2].equals("FILE"))) {
      errors.add(
          new FeedbackListEntry("Error!: Operator node line has obsolete type: " + line, true));
      return false;
    }

    // --------------------------------------------------------------------
    // 3. Node Name
    if (!operatorNameIsValid(words[3])) {
      errors.add(
          new FeedbackListEntry("Error!: Operator node line has invalid name: " + line, true));
      return false;
    }

    // --------------------------------------------------------------------
    // 4. Node source file name
    // For non-folders filename should end with ".soar"
    if (!words[2].equals("FOLDER")) {
      if (!words[4].endsWith(".soar")) {
        errors.add(
            new FeedbackListEntry(
                "Warning: Operator node line has a source filename that does not have \".soar\" extension: "
                    + line));
      }
    }

    // --------------------------------------------------------------------
    // Note:  non-high-level operators have a words[5] that is the nodeId
    //       repeated.  High level operators have this also at index 7.
    //       This value is not used, so it is not checked in this method

    // --------------------------------------------------------------------
    // 5. Folder name
    // For high-level operators verify that the folder name matches the node name
    if (words[2].startsWith("HL")) {
      if (!words[5].equals(words[3])) {
        errors.add(
            new FeedbackListEntry(
                "Warning: Operator node line has a folder name that does not match the node name: "
                    + line));
      }
    }

    // --------------------------------------------------------------------
    // 6. Datamap Root Id
    // For high-level operators verify that an integer datamap id is present.
    // The value of this id can not be checked with the datamap since the
    // datamap has not yet been loaded.  So, we
    // just verify it is a positive number
    if (words[2].startsWith("HL")) {
      try {
        int datamapId = Integer.parseInt(words[7]);
        if (datamapId < 1) throw new NumberFormatException();
      } catch (NumberFormatException nfe) {
        errors.add(
            new FeedbackListEntry(
                "Error: Operator node line is missing a valid datamap root id: " + line, true));
        return false;
      }
    }

    // All tests passed
    return true;
  } // verifyV4OperatorLine

  /**
   * Creates an operator node from a given specification line taken from a .vsa file. Unlike its
   * predecessor, this "safe" version does not throw exceptions but, instead, checks for errors and
   * tries to recover when they are found.
   *
   * <p>Valid Format is:
   *
   * <p><node_id>(TAB)<parent_node_id>(TAB)<node_type> [node_name] [node_contents_filename]
   * <type_sepcific_contents> where: <node_id> is a unique integer. These ids should appear in
   * sequential, numerical order in the file <parent_mode_id> is the id of the parent node or ROOT
   * if this is the root node. The operator tree has only one root. <node_type> is one of the types
   * in {@link OperatorNode#VSA_NODE_TYPES}
   *
   * @param line a line from a .vsa file that describes an operator node
   * @param maxOpId the maximum valid operator id (used to verify parent id)
   * @param errors any errors found are reported here
   * @return the created OperatorNode
   * @see OperatorNode
   * @see #readVersionFourSafe
   */
  private OperatorNode makeNodeVersionFourSafe(
      String line, int maxOpId, Vector<FeedbackListEntry> errors) {
    // ensure the line is valid
    if (!verifyV4OperatorLine(line, maxOpId, errors)) return null;

    // split into parts
    OperatorNode retVal;
    String[] words = line.split("[ \\t]"); // split on spaces and tabs
    String type = words[2];

    switch (type) {
      case "HLOPERATOR":
        retVal = createSoarOperatorNode(words[3], words[4], words[5], Integer.parseInt(words[6]));
        break;
      case "OPERATOR":
        retVal = createSoarOperatorNode(words[3], words[4]);
        break;
      case "HLFOPERATOR":
        retVal =
            createHighLevelFileOperatorNode(
                words[3], words[4], words[5], Integer.parseInt(words[6]));
        break;
      case "FOPERATOR":
        retVal = createFileOperatorNode(words[3], words[4]);
        break;
      case "HLIOPERATOR":
        retVal =
            createHighLevelImpasseOperatorNode(
                words[3], words[4], words[5], Integer.parseInt(words[6]));
        break;
      case "IOPERATOR":
        retVal = createImpasseOperatorNode(words[3], words[4]);
        break;
      case "FOLDER":
        retVal = createFolderNode(words[3], words[4]);
        break;
      default:
        // This should never happen...
        return null;
    }

    return retVal;
  } // makeNodeVersionFourSafe

  /**
   * Opens a Visual Soar project by creating the appropriate node
   *
   * @param linkedToMap hashmap used to keep track of linked nodes, not used
   * @param linkNodesToRestore list of linked nodes needed to restore, not used
   * @param r .vsa file that is being read to open project
   * @return the created OperatorNode
   * @see OperatorNode
   * @see #readVersionThree(Reader)
   */
  private OperatorNode makeNodeVersionThree(
      HashMap<Integer, VSTreeNode> linkedToMap, List<VSTreeNode> linkNodesToRestore, Reader r)
      throws IOException, NumberFormatException {
    OperatorNode retVal;
    String type = ReaderUtils.getWord(r);
    switch (type) {
      case "HLOPERATOR":
        retVal =
            createSoarOperatorNode(
                ReaderUtils.getWord(r),
                ReaderUtils.getWord(r),
                ReaderUtils.getWord(r),
                ReaderUtils.getInteger(r));
        break;
      case "OPERATOR":
        retVal = createSoarOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        break;
      case "FOLDER":
        retVal = createFolderNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        break;
      case "FILE":
        retVal = createFileNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        break;
      case "ROOT":
        retVal = createOperatorRootNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        break;
      case "LINK":
        retVal =
            createLinkNode(
                ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getInteger(r));
        linkNodesToRestore.add(retVal);
        break;
      default:
        throw new IOException("Parse Error");
    }

    if (retVal != null) {
      linkedToMap.put(ReaderUtils.getInteger(r), retVal);
    }
    return retVal;
  }

  /**
   * Opens a Visual Soar project by creating the appropriate node
   *
   * @param linkedToMap hashmap used to keep track of linked nodes, not used
   * @param linkNodesToRestore list of linked nodes needed to restore, not used
   * @param r .vsa file that is being read to open project
   * @return the created OperatorNode
   * @see OperatorNode
   * @see #readVersionTwo(Reader)
   */
  private OperatorNode makeNodeVersionTwo(
      HashMap<Integer, VSTreeNode> linkedToMap, List<VSTreeNode> linkNodesToRestore, Reader r)
      throws IOException, NumberFormatException {
    OperatorNode retVal;
    String type = ReaderUtils.getWord(r);
    switch (type) {
      case "HLOPERATOR":
        retVal =
            createSoarOperatorNode(
                ReaderUtils.getWord(r),
                ReaderUtils.getWord(r),
                ReaderUtils.getWord(r),
                ReaderUtils.getInteger(r));
        break;
      case "OPERATOR":
        retVal = createSoarOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        break;
      case "FOLDER":
        retVal = createFolderNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        break;
      case "FILE":
        retVal = createFileNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        break;
      case "ROOT":
        retVal = createOperatorRootNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
        break;
      case "LINK":
        retVal =
            createLinkNode(
                ReaderUtils.getWord(r), ReaderUtils.getWord(r), ReaderUtils.getInteger(r));
        linkNodesToRestore.add(retVal);
        break;
      default:
        throw new IOException("Parse Error");
    }

    if (retVal != null) {
      linkedToMap.put(ReaderUtils.getInteger(r), retVal);
    }
    return retVal;
  }

  /**
   * Opens a Visual Soar project by creating the appropriate node
   *
   * @param r .vsa file that is being read to open project
   * @return the created OperatorNode
   * @see OperatorNode
   * @see #readVersionOne(Reader)
   */
  private OperatorNode makeNodeVersionOne(Reader r) throws IOException, NumberFormatException {
    String type = ReaderUtils.getWord(r);
    switch (type) {
      case "HLOPERATOR":
        return createSoarOperatorNode(
            ReaderUtils.getWord(r),
            ReaderUtils.getWord(r),
            ReaderUtils.getWord(r),
            ReaderUtils.getInteger(r));
      case "OPERATOR":
        return createSoarOperatorNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
      case "FOLDER":
        return createFolderNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
      case "FILE":
        return createFileNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
      case "ROOT":
        return createOperatorRootNode(ReaderUtils.getWord(r), ReaderUtils.getWord(r));
      default:
        throw new IOException("Parse Error");
    }
  }

  /**
   * Reads a Version One .vsa project file and interprets it to create a Visual Soar project from
   * the file.
   *
   * @param r the Reader of the .vsa project file
   * @see #makeNodeVersionOne(Reader)
   */
  private void readVersionOne(Reader r) throws IOException, NumberFormatException {
    // This hash table has keys of ids and a pointer as a value
    // it is used for parent lookup
    Hashtable<Integer, VSTreeNode> ht = new Hashtable<>();

    // Special Case Root Node
    // tree specific stuff
    int rootId = ReaderUtils.getInteger(r);

    // node stuff
    VSTreeNode root = makeNodeVersionOne(r);

    // add the new node to the hash table
    ht.put(rootId, root);

    // Read in all the other nodes
    while (r.ready()) {
      // again read in the tree specific stuff
      int nodeId = ReaderUtils.getInteger(r);
      int parentId = ReaderUtils.getInteger(r);

      // get the parent
      OperatorNode parent = (OperatorNode) ht.get(parentId);

      // read in the node
      OperatorNode node = makeNodeVersionOne(r);
      addChild(parent, node);

      // add that node to the hash table
      ht.put(nodeId, node);
    }
    operatorHierarchy = new DefaultTreeModel(root);
  }

  /**
   * Creates a new folder node in the operator window
   *
   * @param inName Name given to new node
   * @param inFolderName same as inName, name of created folder
   * @see FolderNode
   */
  public FolderNode createFolderNode(String inName, String inFolderName) {
    return new FolderNode(inName, getNextId(), inFolderName);
  }

  /**
   * Creates a new File node in the operator window
   *
   * @param inName name of file node
   * @param inFile name of created rule editor file, same as inName
   * @see FileNode
   */
  public FileNode createFileNode(String inName, String inFile) {
    return new FileNode(inName, getNextId(), inFile);
  }

  /**
   * Creates a new Impasse Operator Node in the operator window
   *
   * @param inName name of node
   * @param inFileName name of created rule editor file, same as inName
   * @see ImpasseOperatorNode
   */
  public ImpasseOperatorNode createImpasseOperatorNode(String inName, String inFileName) {
    return new ImpasseOperatorNode(inName, getNextId(), inFileName);
  }

  /**
   * Creates a high level Impasse Operator Node in the operator window
   *
   * @param inName name of node
   * @param inFileName name of created rule editor file, same as inName
   * @param inFolderName name of created folder, same as inName
   * @param inDataMapIdNumber integer corresponding to node's datamap
   * @see ImpasseOperatorNode
   */
  public ImpasseOperatorNode createHighLevelImpasseOperatorNode(
      String inName, String inFileName, String inFolderName, int inDataMapIdNumber) {
    return new ImpasseOperatorNode(
        inName, getNextId(), inFileName, inFolderName, inDataMapIdNumber);
  }

  /**
   * Creates a new File Operator Node in the operator window
   *
   * @param inName name of node
   * @param inFileName name of created rule editor file, same as inName
   * @see FileOperatorNode
   */
  public FileOperatorNode createFileOperatorNode(String inName, String inFileName) {
    return new FileOperatorNode(inName, getNextId(), inFileName);
  }

  /**
   * Creates a high-level File Operator Node in the operator window
   *
   * @param inName name of node
   * @param inFileName name of created rule editor file, same as inName
   * @param inFolderName name of created folder, same as inName
   * @param inDataMapId SoarIdentifierVertex corresponding to node's datamap
   * @see FileOperatorNode
   * @see SoarIdentifierVertex
   */
  public FileOperatorNode createHighLevelFileOperatorNode(
      String inName, String inFileName, String inFolderName, SoarIdentifierVertex inDataMapId) {
    return new FileOperatorNode(inName, getNextId(), inFileName, inFolderName, inDataMapId);
  }

  /**
   * Creates a high-level File Operator Node in the operator window
   *
   * @param inName name of node
   * @param inFileName name of created rule editor file, same as inName
   * @param inFolderName name of created folder, same as inName
   * @param inDataMapIdNumber integer corresponding to node's datamap
   * @see FileOperatorNode
   */
  public FileOperatorNode createHighLevelFileOperatorNode(
      String inName, String inFileName, String inFolderName, int inDataMapIdNumber) {
    return new FileOperatorNode(inName, getNextId(), inFileName, inFolderName, inDataMapIdNumber);
  }

  /**
   * Creates a new Soar Operator Node in the operator window
   *
   * @param inName name of the node
   * @param inFileName name of created rule editor file, same as inName
   * @see OperatorOperatorNode
   */
  public OperatorOperatorNode createSoarOperatorNode(String inName, String inFileName) {
    return new OperatorOperatorNode(inName, getNextId(), inFileName);
  }

  /**
   * Creates a high level Soar Operator Node in the operator window
   *
   * @param inName name of the node
   * @param inFileName name of created rule editor file, same as inName
   * @param inFolderName name of created folder, same as inName
   * @param inDataMapIdNumber integer corresponding to node's datamap
   * @see OperatorOperatorNode
   */
  public OperatorOperatorNode createSoarOperatorNode(
      String inName, String inFileName, String inFolderName, int inDataMapIdNumber) {
    return new OperatorOperatorNode(
        inName, getNextId(), inFileName, inFolderName, inDataMapIdNumber);
  }

  /**
   * Creates the Root Node of the operator hierarchy. From here all sub operators branch. This is
   * the root node method called when opening an existing project.
   *
   * @param inName name of the node, same as the name of the project
   * @param inFolderName name of the root operator's folder, same as inName
   * @see OperatorRootNode
   */
  public OperatorRootNode createOperatorRootNode(String inName, String inFolderName) {
    return new OperatorRootNode(inName, getNextId(), inFolderName);
  }

  /**
   * LinkNodes not used in this version of Visual Soar
   *
   * @see LinkNode
   */
  public LinkNode createLinkNode(String inName, String inFileName, int inHighLevelId) {
    return new LinkNode(inName, getNextId(), inFileName, inHighLevelId);
  }

  /**
   * Writes out the default productions in the "top-state.soar" file
   *
   * @param fileToWriteTo the "top-state.soar" file
   */
  private void writeOutTopStateElabs(File fileToWriteTo) throws IOException {
    Writer w = new FileWriter(fileToWriteTo);
    w.write("sp {elaborate*top-state*top-state\n");
    w.write("   (state <s> ^superstate nil)\n");
    w.write("-->\n");
    w.write("   (<s> ^top-state <s>)\n");
    w.write("}\n");
    w.write("\n");
    w.close();
  }

  /**
   * Writes out the default productions in the _all.soar file
   *
   * @param fileToWriteTo the _all.soar file
   */
  private void writeOutAllElabs(File fileToWriteTo) throws IOException {
    Writer w = new FileWriter(fileToWriteTo);
    w.write("sp {elaborate*state*name\n");
    w.write("   (state <s> ^superstate.operator.name <name>)\n");
    w.write("-->\n");
    w.write("   (<s> ^name <name>)\n");
    w.write("}\n\n");
    w.write("sp {elaborate*state*top-state\n");
    w.write("   (state <s> ^superstate.top-state <ts>)\n");
    w.write("-->\n");
    w.write("   (<s> ^top-state <ts>)\n");
    w.write("}\n\n");
    // w.write();
    w.close();
  }

  /**
   * Writes out the default productions in the _all.soar file
   *
   * @param fileToWriteTo the _all.soar file
   */
  private void writeOutInitRules(File fileToWriteTo, String topStateName) throws IOException {
    Writer w = new FileWriter(fileToWriteTo);
    w.write("sp {propose*initialize-" + topStateName + "\n");
    w.write("   (state <s> ^superstate nil\n");
    w.write("             -^name)\n");
    w.write("-->\n");
    w.write("   (<s> ^operator <o> +)\n");
    w.write("   (<o> ^name initialize-" + topStateName + ")\n");
    w.write("}\n");
    w.write("\n");
    w.write("sp {apply*initialize-" + topStateName + "\n");
    w.write("   (state <s> ^operator <op>)\n");
    w.write("   (<op> ^name initialize-" + topStateName + ")\n");
    w.write("-->\n");
    w.write("   (<s> ^name " + topStateName + ")\n");
    w.write("}\n\n");

    w.close();
  }
}
