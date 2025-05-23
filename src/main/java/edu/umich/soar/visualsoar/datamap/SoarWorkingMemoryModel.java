package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.files.projectjson.Datamap;
import edu.umich.soar.visualsoar.files.projectjson.DMVertex;
import edu.umich.soar.visualsoar.graph.*;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackEntryOpNode;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;
import edu.umich.soar.visualsoar.parser.SoarProduction;
import edu.umich.soar.visualsoar.parser.Triple;
import edu.umich.soar.visualsoar.parser.TriplesExtractor;
import edu.umich.soar.visualsoar.util.EnumerationIteratorWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.*;

/**
 * This is a model of Soar's Working Memory represented by a directed graph structure
 *
 * @author Brad Jones
 */
public class SoarWorkingMemoryModel {
  //////////////////////////////////////////////////////////
  // Data Members
  //////////////////////////////////////////////////////////
  // A Directed Graph that is supposed to represent WM
  private final DirectedGraph rep = new DirectedGraphAsAdjacencyLists();

  private Map<String, SoarVertex> serializationId2Vertex = new HashMap<>();

  private final Vector<WorkingMemoryListener> listeners = new Vector<>();
  private final TreeMap<String, SoarVertex> properties = new TreeMap<>();
  private final Path dmPath;

  /////////////////////////////////////////////////////////
  // Constructors
  /////////////////////////////////////////////////////////

  /**
   * Creates a default model of working memory
   *
   * @param isNew only create if model is new
   * @param name name used for the top-state, or {@code null} if isNew is false
   * @param dmPath path to the project dm/json file
   * @see #addTriple(SoarVertex, String, SoarVertex)
   * @see #addProperty(String, SoarVertex)
   */
  public SoarWorkingMemoryModel(boolean isNew, String name, Path dmPath) {
    if (isNew) {
      addProperty("TOPSTATE", createNewSoarId());
      addProperty("IO", createNewSoarId());
      addProperty("INPUTLINK", createNewSoarId());
      addProperty("OUTPUTLINK", createNewSoarId());
      addProperty("INITIALIZE-" + name, createNewSoarId());
      addTriple(getProperty("TOPSTATE"), "type", createNewEnumeration("state"));
      addTriple(getProperty("TOPSTATE"), "superstate", createNewEnumeration("nil"));
      addTriple(getProperty("TOPSTATE"), "top-state", getProperty("TOPSTATE"));
      addTriple(getProperty("TOPSTATE"), "name", createNewEnumeration(name));
      addTriple(getProperty("TOPSTATE"), "io", getProperty("IO"));
      addTriple(getProperty("TOPSTATE"), "operator", getProperty("INITIALIZE-" + name));
      addTriple(getProperty("IO"), "input-link", getProperty("INPUTLINK"));
      addTriple(getProperty("IO"), "output-link", getProperty("OUTPUTLINK"));
      addTriple(
          getProperty("INITIALIZE-" + name), "name", createNewEnumeration("initialize-" + name));

      // reward, epmem and smem (added Dec 2022)
      addNewStandardWMEs(getProperty("TOPSTATE"));
    }
    this.dmPath = dmPath;
  }

  /////////////////////////////////////////////////////////
  // Methods
  /////////////////////////////////////////////////////////
  public void addProperty(String name, SoarVertex sv) {
    properties.put(name, sv);
  }

  public SoarVertex getProperty(String name) {
    return properties.get(name);
  }

  /** This sets the topstate, this should only be called by trusted members */
  public void setTopstate(SoarIdentifierVertex siv) {
    addVertex(siv);
    addProperty("TOPSTATE", siv);
  }

  /** For a particular id, returns the corresponding soar vertex */
  public SoarVertex getVertexForId(int i) {
    return rep.selectVertex(i);
  }

  /** For a particular serialization ID, returns the corresponding soar vertex */
  public SoarVertex getVertexForSerializationId(String serializationId) {
    return serializationId2Vertex.get(serializationId);
  }

  /** Adds a listener to working memory, to receive working memory events */
  public void addWorkingMemoryListener(WorkingMemoryListener l) {
    listeners.add(l);
  }

  /**
   * Adds a triple to working memory
   *
   * @return the new NamedEdge that was created
   */
  public NamedEdge addTriple(SoarVertex v0, String attribute, SoarVertex v1) {
    if (!v0.allowsEmanatingEdges()) {
      throw new IllegalArgumentException("The First SoarVertex does not allow emanating edges");
    }
    NamedEdge ne = new NamedEdge(v0, v1, attribute);
    rep.addEdge(ne);
    notifyListenersOfAdd(ne);

    return ne;
  }

  /**
   * Adds a triple to working memory. Also sets a comment to the edge that the triple creates. This
   * comment is read in by the SoarWorkingMemoryReader when the .dm file is loaded
   */
  public void addTriple(
      SoarVertex v0, String attribute, SoarVertex v1, int generated, String comment) {
    if (!v0.allowsEmanatingEdges()) {
      throw new IllegalArgumentException("The First SoarVertex does not allow emanating edges");
    }
    NamedEdge ne = new NamedEdge(v0, v1, attribute);
    if (comment.length() > 1) {
      ne.setComment(comment);
    }
    if (generated == 1) {
      ne.setAsGenerated();
    }
    rep.addEdge(ne);
    notifyListenersOfAdd(ne);
  }

  /**
   * Adds a triple to working memory Also denotes that this triple was created by the automatic
   * DataMap generator. This will cause the dataMap entry of this triple to show up as colored text
   * until the entry is validated by the user as acceptable.
   */
  public void addTriple(
      SoarVertex v0,
      String attribute,
      SoarVertex v1,
      boolean generated,
      OperatorNode current,
      int lineNumber) {
    if (!v0.allowsEmanatingEdges()) {
      throw new IllegalArgumentException("The First SoarVertex does not allow emanating edges");
    }
    NamedEdge ne = new NamedEdge(v0, v1, attribute);
    if (generated) {
      ne.setAsGenerated();
      ne.setNode(current);
      ne.setLineNumber(lineNumber);
    }
    rep.addEdge(ne);
    notifyListenersOfAdd(ne);
  }

  public SoarIdentifierVertex getTopstate() {
    return (SoarIdentifierVertex) getProperty("TOPSTATE");
  }

  /**
   * Returns an enumeration of all the edges that are emanating/leaving from a particular vertex.
   *
   * @see NamedEdge
   */
  public Enumeration<NamedEdge> emanatingEdges(SoarVertex v) {
    return rep.emanatingEdges(v);
  }

  public Enumeration<NamedEdge> getEdges() {
    return rep.edges();
  }

  /**
   * Returns the number of vertices contained in working memory
   *
   * @see SoarVertex
   */
  public int numberOfVertices() {
    return rep.numberOfVertices();
  }

  /**
   * This is only needed by the reader, in the future this will only be package accessible regular
   * users should go through the factory create methods
   */
  public void addVertex(SoarVertex v) {
    rep.addVertex(v);
    serializationId2Vertex.put(v.getSerializationId(), v);
  }

  public void reduce(List<SoarVertex> startVertices) {
    rep.reduce(startVertices);
    // re-do this mapping, since we may have lost some vertices
    serializationId2Vertex = new HashMap<>(getNumberOfVertices());
    rep.vertices()
        .asIterator()
        .forEachRemaining(sv -> serializationId2Vertex.put(sv.getSerializationId(), sv));
  }

  /** Removes the requested triple from Working Memory */
  public void removeTriple(SoarVertex v0, String attribute, SoarVertex v1) {
    NamedEdge ne = new NamedEdge(v0, v1, attribute);
    rep.removeEdge(ne);
    notifyListenersOfRemove(ne);
  }

  /** Returns an exact copy of a SoarVertex with a new id */
  public SoarVertex createVertexCopy(SoarVertex orig) {
    SoarVertex cpy = orig.copy(getNextVertexId());

    addVertex(cpy);
    return cpy;
  }

  /**
   * Create a new EnumerationVertex with a vector of strings that represent possible values for that
   * enumeration
   *
   * @param vec the vector of strings that represent values
   * @see EnumerationVertex
   */
  public EnumerationVertex createNewEnumeration(Vector<String> vec) {
    int id = getNextVertexId();
    EnumerationVertex e = new EnumerationVertex(id, vec);
    addVertex(e);
    return e;
  }

  /**
   * Create a new EnumerationVertex with a string that represents the only acceptable value for that
   * enumeration
   *
   * @param s string representing the value for that enumeration attribute
   * @see EnumerationVertex
   */
  public EnumerationVertex createNewEnumeration(String s) {
    int id = getNextVertexId();
    EnumerationVertex e = new EnumerationVertex(id, s);
    addVertex(e);
    return e;
  }

  /**
   * Create Integer Vertex with default (unbounded) range
   *
   * @see IntegerRangeVertex
   */
  public IntegerRangeVertex createNewInteger() {
    int id = getNextVertexId();
    IntegerRangeVertex i = new IntegerRangeVertex(id, Integer.MIN_VALUE, Integer.MAX_VALUE);
    addVertex(i);
    return i;
  }

  /**
   * Create Integer Vertex with specified range
   *
   * @param low the minimum limit of the integer attribute
   * @param high the maximum limit of the integer attribute
   * @see IntegerRangeVertex
   */
  public IntegerRangeVertex createNewIntegerRange(int low, int high) {
    int id = getNextVertexId();
    IntegerRangeVertex i = new IntegerRangeVertex(id, low, high);
    addVertex(i);
    return i;
  }

  /**
   * Create a StringVertex attribute
   *
   * @see StringVertex
   */
  public StringVertex createNewString() {
    int id = getNextVertexId();
    StringVertex s = new StringVertex(id);
    addVertex(s);
    return s;
  }

  /**
   * Create Float Vertex with default (unbounded) range
   *
   * @see FloatRangeVertex
   */
  public FloatRangeVertex createNewFloat() {
    int id = getNextVertexId();
    FloatRangeVertex f = new FloatRangeVertex(id, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
    addVertex(f);
    return f;
  }

  /**
   * Create Float Vertex with specified range
   *
   * @param low the minimum limit of the float attribute
   * @param high the maximum limit of the float attribute
   * @see FloatRangeVertex
   */
  public FloatRangeVertex createNewFloatRange(float low, float high) {
    int id = getNextVertexId();
    FloatRangeVertex f = new FloatRangeVertex(id, low, high);
    addVertex(f);
    return f;
  }

  /**
   * Create a basic soar Identifier Vertex
   *
   * @see SoarIdentifierVertex
   */
  public SoarIdentifierVertex createNewSoarId() {
    int id = getNextVertexId();
    SoarIdentifierVertex s = new SoarIdentifierVertex(id);
    addVertex(s);
    return s;
  }

  /**
   * Called to create the Vertex and appropriate identifiers for a high-level operator
   *
   * @param superstate the parent operator of the operator
   * @param name the name of the high-level operator
   * @return SoarIdentifierVertex that is the dataMapId for that operator
   * @see SoarIdentifierVertex
   */
  public SoarIdentifierVertex createNewStateId(SoarIdentifierVertex superstate, String name) {
    SoarIdentifierVertex s = createNewSoarId();
    SoarVertex type_state = createNewEnumeration("state");
    SoarVertex nameVertex = createNewEnumeration(name);
    addTriple(s, "type", type_state);

    if (superstate != null) {
      addTriple(s, "superstate", superstate);
    }
    addTriple(s, "name", nameVertex);
    addTriple(s, "top-state", getTopstate());

    addSubOperatorWMEs(s);

    // reward, epmem and smem (added Dec 2022)
    addNewStandardWMEs(s);

    return s;
  }

  /**
   * addSubOperatorWMEs
   *
   * <p>adds the WMEs that always appear in a sub-state. There are other WMEs that don't always
   * appear which have been omitted. Perhaps JEL will ask me to add them in a subsequent revision?
   */
  private void addSubOperatorWMEs(SoarIdentifierVertex s) {
    // ^quiescence t
    SoarVertex qui = createNewEnumeration("t");
    addTriple(s, "quiescence", qui);

    // ^impasse
    Vector<String> impVec = new Vector<>();
    impVec.add("tie");
    impVec.add("conflict");
    impVec.add("constraint-failure");
    impVec.add("no-change");
    SoarVertex impasse = createNewEnumeration(impVec);
    addTriple(s, "impasse", impasse);

    // ^choices
    Vector<String> choiceVec = new Vector<>();
    choiceVec.add("multiple");
    choiceVec.add("constraint-failure");
    choiceVec.add("none");
    SoarVertex choices = createNewEnumeration(choiceVec);
    addTriple(s, "choices", choices);
  }

  /**
   * addNewStandardWMEs
   *
   * <p>adds the new WMEs that are now standard on every state in Soar since Visual Soar was first
   * created. I put this in a helper method decided not to move the existing standard WMEs in order
   * to minimize my impact on the source code. Perhaps I'm being too careful.
   *
   * @author :AMN: Dec 2022
   */
  private void addNewStandardWMEs(SoarVertex s) {
    // ^reward-link
    SoarVertex rewardLink = createNewSoarId();
    addTriple(s, "reward-link", rewardLink);
    SoarVertex reward = createNewSoarId();
    addTriple(rewardLink, "reward", reward);
    SoarVertex rewardVal = createNewFloat();
    addTriple(reward, "value", rewardVal);

    // ^epmem et.al.
    SoarVertex epmem = createNewSoarId();
    addTriple(s, "epmem", epmem);
    SoarVertex epCmd = createNewSoarId();
    addTriple(epmem, "command", epCmd);
    SoarVertex epPresentId = createNewInteger();
    addTriple(epmem, "present-id", epPresentId);
    SoarVertex epResult = createNewSoarId();
    addTriple(epmem, "result", epResult);

    // ^smem et.al.
    SoarVertex smem = createNewSoarId();
    addTriple(s, "smem", smem);
    SoarVertex smCmd = createNewSoarId();
    addTriple(smem, "command", smCmd);
    SoarVertex smResult = createNewSoarId();
    addTriple(smem, "result", smResult);
  }

  /**
   * Each Vertex in Working Memory has a unique ID, function returns the next available Vertex ID
   */
  protected int getNextVertexId() {
    return rep.numberOfVertices();
  }

  /**
   * Notifies the listeners to refresh the screen
   *
   * @param ne the edge that was added
   */
  protected void notifyListenersOfAdd(NamedEdge ne) {
    for (WorkingMemoryListener wml : listeners) {
      wml.WMEAdded(new WorkingMemoryEvent(ne));
    }
  }

  /**
   * Notifies the listeners to refresh the screen
   *
   * @param ne the edge that has been removed
   */
  protected void notifyListenersOfRemove(NamedEdge ne) {
    for (WorkingMemoryListener wml : this.listeners) {
      wml.WMERemoved(new WorkingMemoryEvent(ne));
    }
  }

  /**
   * Writes out information for the datamap file (.dm file) .dm datamap file is in the format of:
   * number of vertices <br>
   * write out all vertices: &nbsp;&nbsp; |vertex type| &nbsp;&nbsp; |vertex id int| &nbsp;&nbsp;
   * |number of enumerations| &nbsp;&nbsp; |enumeration strings .....| <br>
   * number of edges <br>
   * write out all edges: &nbsp;&nbsp; | left vertex | &nbsp;&nbsp; |name of edge| &nbsp;&nbsp;
   * |right vertex|
   *
   * @param graphWriter the file to be written too - .dm file
   */
  public void write(Writer graphWriter) throws IOException {
    // Write out the number of Vertices
    graphWriter.write("" + rep.numberOfVertices() + '\n');

    // Write out all the vertices
    Enumeration<SoarVertex> v = rep.vertices();
    while (v.hasMoreElements()) {
      SoarVertex vertex = v.nextElement();
      if (vertex != null) {
        vertex.write(graphWriter);
      }
    }

    // Write out the number of edges
    graphWriter.write("" + rep.numberOfEdges() + '\n');
    // Write out all the edges
    Enumeration<NamedEdge> e = rep.edges();
    while (e.hasMoreElements()) {
      NamedEdge edge = e.nextElement();
      edge.write(graphWriter);
    }
  }

  public Datamap toJson() {
    // first convert edges, which are stored with their tail in the JSON model
    Map<String, List<DMVertex.OutEdge>> edgeIndex = buildJsonEdgeIndex();

    List<DMVertex> jsonVertices = new ArrayList<>();
    rep.vertices()
        .asIterator()
        .forEachRemaining(
            (vertex) -> {
              if (vertex == null) {
                // I hope this will never happen, but just to be safe!
                System.err.println("Found a null vertex while saving project JSON");
                return;
              }
              jsonVertices.add(toJsonVertex(vertex, edgeIndex));
            });

    return new Datamap(getTopstate().getSerializationId(), jsonVertices);
  }

  @NotNull
  private Map<String, List<DMVertex.OutEdge>> buildJsonEdgeIndex() {
    Map<String, List<DMVertex.OutEdge>> edgeIndex = new HashMap<>();
    Enumeration<NamedEdge> e = rep.edges();
    while (e.hasMoreElements()) {
      NamedEdge namedEdge = e.nextElement();
      List<DMVertex.OutEdge> edgeList =
          edgeIndex.computeIfAbsent(namedEdge.V0().getSerializationId(), (k) -> new ArrayList<>());
      edgeList.add(
          new DMVertex.OutEdge(
              namedEdge.getName(),
              namedEdge.V1().getSerializationId(),
              namedEdge.getComment(),
              namedEdge.isGenerated()));
    }
    return edgeIndex;
  }


  private DMVertex toJsonVertex(SoarVertex vertex, Map<String, List<DMVertex.OutEdge>> edgeIndex) {
    return toJsonVertex(vertex, vertex.getSerializationId(), edgeIndex);
  }

  /**
   * @param vertex to translate to JSON class instance
   * @param edgeTailId in the case of foreign vertices, we use the parent ID to determine edges
   * @param edgeIndex map from IDs to edges, used to retrieve edges for applicable vertices
   */
  private DMVertex toJsonVertex(SoarVertex vertex, String edgeTailId, Map<String, List<DMVertex.OutEdge>> edgeIndex) {
    String id = vertex.getSerializationId();
    if (vertex instanceof EnumerationVertex) {
      List<String> enumChoices = new ArrayList<>();
      ((EnumerationVertex) vertex).getEnumeration().forEachRemaining(enumChoices::add);
      return new DMVertex.EnumerationVertex(id, enumChoices);
    } else if (vertex instanceof FloatRangeVertex) {
      double min = ((FloatRangeVertex) vertex).getLow();
      double max = ((FloatRangeVertex) vertex).getHigh();
      return new DMVertex.FloatRangeVertex(id, min, max);
    } else if (vertex instanceof ForeignVertex) {
      DMVertex importedVertex =
          toJsonVertex(((ForeignVertex) vertex).getForeignSoarVertex(), id, edgeIndex);
      String foreignDmName = ((ForeignVertex) vertex).getForeignDMName();
      return new DMVertex.ForeignVertex(
          id, foreignDmName, importedVertex);
    } else if (vertex instanceof IntegerRangeVertex) {
      int min = ((IntegerRangeVertex) vertex).getLow();
      int max = ((IntegerRangeVertex) vertex).getHigh();
      return new DMVertex.IntegerRangeVertex(id, min, max);
    } else if (vertex instanceof SoarIdentifierVertex) {
      List<DMVertex.OutEdge> edgeList =
          edgeIndex.getOrDefault(edgeTailId, Collections.emptyList());
      return new DMVertex.SoarIdVertex(id, edgeList);
    } else if (vertex instanceof StringVertex) {
      return new DMVertex(id, DMVertex.VertexType.STRING);
    } else {
      throw new IllegalArgumentException(
          "Found unknown vertex type "
              + vertex.getClass().getName()
              + " while saving project JSON");
    }
  }

  /**
   * Writes all the edge comments to the writer to create the comment file and whether a datamap
   * entry is generated or not. Each line represents an entry of the datamap. Each line contains a 1
   * or a 0 that signifies if generates or not and is followed by a comment if there is one.
   */
  public void writeComments(Writer commentWriter) throws IOException {
    // Write out all the edge comments
    Enumeration<NamedEdge> e = rep.edges();
    while (e.hasMoreElements()) {
      NamedEdge edge = e.nextElement();
      if (edge.isGenerated()) {
        commentWriter.write("1 " + edge.getComment() + '\n');
      } else {
        commentWriter.write("0 " + edge.getComment() + '\n');
      }
    }
  }

  /**
   * Function finds the set of variables within a productions that matches a given string
   *
   * @param sv the SoarIdentifierVertex currently checking
   * @param sp the SoarProduction to check for matching variables
   * @param variable the string that function tries to match
   * @return a List of matches, empty list if nothing found
   */
  public List<DataMapMatcher.Match> matches(SoarIdentifierVertex sv, SoarProduction sp, String variable) {
    TriplesExtractor triplesExtractor = new TriplesExtractor(sp);
    Map<String, Set<DataMapMatcher.Match>> matchesMap =
        DataMapMatcher.matches(this, sv, triplesExtractor, new DoNothingMatcherErrorHandler());
    List<DataMapMatcher.Match> matches = new LinkedList<>();
    if (matchesMap != null) {
      Set<DataMapMatcher.Match> matchesSet = matchesMap.get(variable);
      if (matchesSet != null) {
        matches.addAll(matchesSet);
      }
    }
    return matches;
  }

  /**
   * Used to determine if a soar production matches Working Memory
   *
   * @param sv the SoarIdentifierVertex in WorkingMemory currently checking
   * @param sp the Soar Production to check
   * @return a list of errors
   * @see DefaultCheckerErrorHandler
   * @see DataMapChecker#check
   */
  public List<FeedbackListEntry> checkProduction(
      OperatorNode current, SoarIdentifierVertex sv, SoarProduction sp) {
    TriplesExtractor triplesExtractor = new TriplesExtractor(sp);
    DefaultCheckerErrorHandler dceh =
        new DefaultCheckerErrorHandler(current, sp.getName(), sp.getStartLine());
    DataMapChecker.check(this, sv, triplesExtractor, dceh);
    return dceh.getErrors();
  }

  /////////////////////////////////
  ////////////////////////////////////////////

  /**
   * Used by the {@link OperatorWindow#generateDataMap} actions to look for holes in Working Memory
   * and fix those holes.
   *
   * @param sv the SoarIdentifierVertex in WorkingMemory currently checking
   * @param sp the Soar Production to check
   * @param current the node being examined
   * @return a list of errors
   * @see DefaultCheckerErrorHandler
   * @see DataMapChecker#complete
   */
  public Vector<FeedbackListEntry> checkGenerateProduction(
      SoarIdentifierVertex sv, SoarProduction sp, OperatorNode current) {
    TriplesExtractor triplesExtractor = new TriplesExtractor(sp);
    DefaultCheckerErrorHandler dceh =
        new DefaultCheckerErrorHandler(current, sp.getName(), sp.getStartLine());
    DataMapChecker.complete(this, sv, triplesExtractor, dceh, current);
    return dceh.getErrors();
  }

  /**
   * checkGenerateSingleEntry
   *
   * <p>is similar to {@link #checkGenerateProduction} except that it repairs only a single new
   * datamap entry to address a particular error.
   *
   * @author Andrew Nuxoll (27 Nov 2022)
   */
  public Vector<FeedbackListEntry> checkGenerateSingleEntry(
      SoarIdentifierVertex sv,
      SoarProduction sp,
      OperatorNode current,
      FeedbackListEntry errToFix) {
    // Find the triple associated with this error
    TriplesExtractor triplesExtractor = new TriplesExtractor(sp);
    DefaultCheckerErrorHandler dceh =
        new DefaultCheckerErrorHandler(current, sp.getName(), sp.getStartLine());
    EnumerationIteratorWrapper triplesEnum =
        new EnumerationIteratorWrapper(triplesExtractor.triples());
    while (triplesEnum.hasMoreElements()) {
      Triple currentTriple = (Triple) triplesEnum.nextElement();
      String tripStr = currentTriple.toString();
      if (errToFix.getMessage().contains(tripStr)) {
        // found it!  Now repair it.
        DataMapChecker.complete(this, sv, triplesExtractor, dceh, current);
        break;
      }
    }

    // Since this error has been fixed, it can't be re-fixed
    Vector<FeedbackListEntry> errs = dceh.getErrors();
    for (FeedbackListEntry entry : errs) {
      if (entry instanceof FeedbackEntryOpNode) {
        ((FeedbackEntryOpNode) entry).setCanFix(false);
      }
    }

    return errs;
  } // checkGenerateSingleEntry

  /**
   * Return all parents of a vertex
   *
   * @param sv the source vertex
   * @return a list of all matching SoarVertex 's
   */
  public List<SoarVertex> getParents(SoarVertex sv) {
    return rep.getParentVertices(this, sv);
  }

  /**
   * @return first matching SoarVertex parent
   * @see DirectedGraph#getMatchingParent(SoarWorkingMemoryModel, SoarVertex)
   */
  public SoarVertex getMatchingParent(SoarVertex sv) {
    return rep.getMatchingParent(this, sv);
  }

  /** Returns the number of vertices in Working Memory */
  public int getNumberOfVertices() {
    return rep.numberOfVertices();
  }

  public Path getDmPath() {
    return dmPath;
  }
}
