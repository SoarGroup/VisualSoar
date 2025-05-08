package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.graph.*;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import edu.umich.soar.visualsoar.parser.*;
import edu.umich.soar.visualsoar.util.EnumerationIteratorWrapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreePath;
import java.util.*;


/**
 * This class provides some static methods to do the matching against the
 * datamap
 *
 * @author Brad Jones
 */

public class DataMapMatcher {
    // There is no need to instantiate this class
    private DataMapMatcher() {
    }

  public static final class Match {

    private final SoarVertex vertex;
    private final String name;

    Match(SoarVertex vertex, String name) {
      this.vertex = vertex;
      this.name = name;
    }

    static Match fromVertex(@NotNull SoarVertex vertex) {
      return new Match(vertex, null);
    }

    static Match fromString(@NotNull String name) {
      return new Match(null, name);
    }

    public boolean hasVertex() {
      return vertex != null;
    }

    public SoarVertex getVertex() {
      return vertex;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      if (vertex != null) {
        return vertex.toString();
      }
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Match match = (Match) o;
      return Objects.equals(vertex, match.vertex) && Objects.equals(name, match.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(vertex, name);
    }
  }

  // Static Member Functions

  /**
   * Checks to see if a production has a matching data structure in the datamap
   *
   * @param dataMap Soar Working Memory
   * @param startVertex the state in working memory that is being examined
   * @param triplesExtractor all the triples that were in a production
   * @param meh the structure that holds the errors when they are found
   */
  public static Map<String, Set<Match>> matches(
      SoarWorkingMemoryModel dataMap,
      SoarIdentifierVertex startVertex,
      TriplesExtractor triplesExtractor,
      MatcherErrorHandler meh) {
        Map<String, Set<Match>> varMap = new HashMap<>();
        Iterator<Pair> pairIter = triplesExtractor.variables();
        while (pairIter.hasNext()) {
      varMap.put(pairIter.next().getString(), new HashSet<>());
        }

        // Take care of the first Variable
        // Make sure there are the right number of state variables
        int result = triplesExtractor.getStateVariableCount();
        if (result == 0) {
            meh.noStateVariable();
            return null;
        } else if (triplesExtractor.getStateVariableCount() > 1) {
            meh.tooManyStateVariables();
            return null;
        }

        Pair stateVar = triplesExtractor.stateVariable();
        Set<Match> stateSet = varMap.get(stateVar.getString());
        stateSet.add(Match.fromVertex(startVertex));

        EnumerationIteratorWrapper tripleEnum = new EnumerationIteratorWrapper(triplesExtractor.triples());
        while (tripleEnum.hasMoreElements()) {
            Triple currentTriple = (Triple) tripleEnum.nextElement();
            if ((currentTriple.getAttribute().getString().equals("operator"))
                    && (TripleUtils.isFloat(currentTriple.getValue().getString())
                    || TripleUtils.isInteger(currentTriple.getValue().getString()))) {
                continue;
            }
            if (!addConstraint(dataMap, currentTriple, varMap)) {
                meh.badConstraint(currentTriple);
            }
        }
        return varMap;
    }//matches


    /*
     * This function checks to see if the triplesExtractor triples match the
     * datamap correctly.  If not, the datamap is then corrected to match the
     * triple.
     */
    public static void complete(SoarWorkingMemoryModel dataMap,
                                SoarIdentifierVertex startVertex,
                                TriplesExtractor triplesExtractor,
                                MatcherErrorHandler meh,
                                OperatorNode current) {
        Map<String, Set<Match>> varMap = new HashMap<>();
        Iterator<Pair> iter = triplesExtractor.variables();
        while (iter.hasNext()) {
      varMap.put((iter.next()).getString(), new HashSet<>());
        }


        Pair stateVar = triplesExtractor.stateVariable();
        Set<Match> stateSet = varMap.get(stateVar.getString());
        stateSet.add(Match.fromVertex(startVertex));


        EnumerationIteratorWrapper iterEnumWrap = new EnumerationIteratorWrapper(triplesExtractor.triples());
        while (iterEnumWrap.hasMoreElements()) {
            Triple currentTriple = (Triple) iterEnumWrap.nextElement();
            if ((currentTriple.getAttribute().getString().equals("operator")) && (TripleUtils.isFloat(currentTriple.getValue().getString()) || TripleUtils.isInteger(currentTriple.getValue().getString()))) {
                continue;
            }

            // Error in DataMap, generate new structure to fix this error
            if (!addConstraint(dataMap, currentTriple, varMap)) {

                // Ignore case if attribute is a 'variable' (<' '>)
                if (TripleUtils.isVariable(currentTriple.getAttribute().getString())) {
                    return;
                }

                Set<Match> varSet = varMap.get(currentTriple.getVariable().getString());


                // for every possible start
                EnumerationIteratorWrapper ed = new EnumerationIteratorWrapper(varSet.iterator());
                while (ed.hasMoreElements()) {
                    boolean notFound = true;
                    NamedEdge attributeEdge = null;
                    SoarVertex currentSV = (SoarVertex) ed.nextElement();

                    // If parent Vertex is not a SoarIdentifierVertex, need to
                    // create one
                    if (!(currentSV.allowsEmanatingEdges())) {
                        SoarVertex matchingVertex =
                                dataMap.getMatchingParent(currentSV);
                        if (matchingVertex != null) {
                            currentSV = matchingVertex;
                        } else {
                            List<SoarVertex> matchingVertices = dataMap.getParents(currentSV);
                            Iterator<SoarVertex> z = matchingVertices.iterator();
                            String parentName = "";
                            SoarVertex parentVertex = null;

                            while (z.hasNext()) {
                                SoarVertex oz = z.next();
                                if (oz.allowsEmanatingEdges()) {
                                    parentVertex = oz;
                                    // Get the name of the vertex to create
                                    Enumeration<NamedEdge> parentEdges = dataMap.emanatingEdges(parentVertex);
                                    while (parentEdges.hasMoreElements()) {
                                        NamedEdge parentEdge = parentEdges.nextElement();
                                        if (parentEdge.V1().equals(currentSV)) {
                                            parentName = parentEdge.getName();
                                        }
                                    }
                                }
                            }//while finding all the possible parent vertices

                            if (parentName.length() > 0) {
                                SoarVertex v1 = dataMap.createNewSoarId();
                                dataMap.addTriple(parentVertex,
                                        parentName,
                                        v1,
                                        true,
                                        current,
                                        currentTriple.getLine());
                                meh.generatedIdentifier(currentTriple,
                                        parentName);
                                currentSV = v1;
                            }
                        }     // end of else create a new SoarIdentifierVertex
                    }   // end of if parent vertex wasn't a SoarIdentifierVertex


                    /*  If attribute is 'name'
                     *    1.  See if name is in reference to the name of a operator.
                     *    2.  See if that name and value already exists.
                     *    3.  See if there is an empty operator folder that can be used.
                     *    4.  If operator, name doesn't already exist and no empty folders,
                     *         create a new operator identifier folder for the name attribute
                     *         to be put.
                     */
                    if (currentTriple.getAttribute().getString().equals("name") && currentSV != null && (dataMap.getTopstate().getValue() != currentSV.getValue())) {
                        // create new identifier 'operator' at higher level

                        // Get the parent vertices
                        List<SoarVertex> parentVertices = dataMap.getParents(currentSV);
                        Iterator<SoarVertex> x = parentVertices.iterator();
                        SoarVertex parentVertex = null;
                        while (x.hasNext()) {
                            SoarVertex ox = x.next();
                            if (ox.allowsEmanatingEdges()) {
                                parentVertex = ox;
                            }
                        }

                        // make sure found the parent
                        if (parentVertex == null) {
                            continue;
                        }

                        //Keeps track of if the name attribute is attached to an
                        //operator identifier
                        boolean operatorType = false;

                        //Check to see if name and value already exist somewhere
                        boolean alreadyHere = false;

                        //Keep track of if there are any operator folders with
                        //no name identifier attached
                        boolean emptyFolder = false;

                        // Check to see if this name attribute is part of an
                        // operator
                        Enumeration<NamedEdge> fatherEdges = dataMap.emanatingEdges(parentVertex);
                        while (fatherEdges.hasMoreElements() && !operatorType) {
                            NamedEdge edge = fatherEdges.nextElement();

                            if ((edge.V1().getValue() == currentSV.getValue()) && edge.getName().equals("operator")) {
                                operatorType = true;
                            }
                        }     // end of looking to see if the name attribute's parent is an operator identifier


                        // If it is an operator type,  might need to create new operator identifier
                        if (operatorType) {
                            // Check to see if an identifier 'operator' with same name exists already
                            Enumeration<NamedEdge> parentEdges = dataMap.emanatingEdges(parentVertex);
                            while (parentEdges.hasMoreElements() && !alreadyHere) {

                                NamedEdge edge = parentEdges.nextElement();
                                if (edge.getName().equals("operator")) {
                                    Enumeration<NamedEdge> operatorEdges = dataMap.emanatingEdges(edge.V1());
                                    emptyFolder = true;    // keeps track if there is already operator identifier with no name attached

                                    while (operatorEdges.hasMoreElements() && !alreadyHere) {
                                        NamedEdge operatorEdge = operatorEdges.nextElement();
                                        if (operatorEdge.getName().equals("name")) {
                                            emptyFolder = false;
                                            if (operatorEdge.V1() instanceof EnumerationVertex) {
                                                EnumerationVertex nameVertex = (EnumerationVertex) operatorEdge.V1();
                                                Iterator<String> vertEnum = nameVertex.getEnumeration();
                                                while (vertEnum.hasNext() && !alreadyHere) {
                                                    String ns = vertEnum.next();
                                                    if (ns.equals(currentTriple.getValue().getString())) {
                                                        alreadyHere = true;
                                                        currentSV = null;
                                                    }
                                                }
                                            }     // end of if EnumerationVertex found
                                        }     // end of if found edge called 'name'
                                    }
                                    // If empty operator folder found, use it
                                    if (emptyFolder) {
                                        currentSV = edge.V1();
                                    }
                                }   // end of checking operator identifier children for identical name
                            }   // while checking parent edges for soarIdentifierVertexs 'operator'




                            /* create new identifier 'operator' at higher level if not already one
                             * with same name or an operator identifier without a name
                             */
                            if (!alreadyHere && !emptyFolder) {
                                // Create a new SoarIdentifierVertex with name 'operator'
                                SoarVertex v1 = dataMap.createNewSoarId();
                                dataMap.addTriple(parentVertex, "operator", v1, true, current, currentTriple.getLine());
                                meh.generatedIdentifier(currentTriple, "operator");
                                // Set new 'operator' soar identifier as the parent vertex
                                currentSV = v1;
                            }
                        }   // end of if attribute name is part of an Operator
                    }     // end of if Attribute == 'name'

                    if (currentSV == null) {   // Couldn't find the parent, give up
                        continue;
                    }


                    // Case UNKNOWN VERTEX   [use when 'value' is a variable or unknown at this time]
                    // Possibly create as SoarIdentifier, then change later if needed
                    if (TripleUtils.isVariable(currentTriple.getValue().getString())) {
                        // First check to see if somehow this was already created
                        boolean alreadyThere = false;
                        Enumeration<NamedEdge> dEdges = dataMap.emanatingEdges(currentSV);
                        while (dEdges.hasMoreElements()) {
                            NamedEdge dEdge = dEdges.nextElement();
                            if (dEdge.getName().equals(currentTriple.getAttribute().getString())
                                    && (dEdge.V1().allowsEmanatingEdges())) {
                                alreadyThere = true;
                            }
                        }
                        if (!alreadyThere) {
                            SoarVertex v1 = dataMap.createNewSoarId();
                            dataMap.addTriple(currentSV, currentTriple.getAttribute().getString(), v1, true, current, currentTriple.getLine());
                            meh.generatedIdentifier(currentTriple, currentTriple.getAttribute().getString());
                        }
                    }

                    // INTEGER VERTEX
                    else if (TripleUtils.isInteger(currentTriple.getValue().getString())) {
                        SoarVertex v1 = dataMap.createNewInteger();
                        dataMap.addTriple(currentSV, currentTriple.getAttribute().getString(), v1, true, current, currentTriple.getLine());
                        meh.generatedInteger(currentTriple, currentTriple.getAttribute().getString());
                    }
                    // FLOAT VERTEX
                    else if (TripleUtils.isFloat(currentTriple.getValue().getString())) {
                        SoarVertex v1 = dataMap.createNewFloat();
                        dataMap.addTriple(currentSV, currentTriple.getAttribute().getString(), v1, true, current, currentTriple.getLine());
                        meh.generatedFloat(currentTriple, currentTriple.getAttribute().getString());
                    }
                    // Case ENUMERATED VERTEX
                    else {
                        // Get all the edges from the start
                        // ATTRIBUTE Search for the matching attribute
                        Enumeration<NamedEdge> edges = dataMap.emanatingEdges(currentSV);
                        while (edges.hasMoreElements() && notFound) {
                            NamedEdge currentEdge = edges.nextElement();
                            if (currentEdge.getName().equals(currentTriple.getAttribute().getString())) {
                                notFound = false;
                                attributeEdge = currentEdge;  // remember this edge
                            }
                        }    // end of while going through edges of currentSV
                        // If attribute is not found, add it
                        if (notFound) {
                            Vector<String> v1Vector = new Vector<>();
                            v1Vector.add(currentTriple.getValue().getString());
                            SoarVertex v1 = dataMap.createNewEnumeration(v1Vector);
                            dataMap.addTriple(currentSV, currentTriple.getAttribute().getString(), v1, true, current, currentTriple.getLine());
                            meh.generatedEnumeration(currentTriple, currentTriple.getAttribute().getString());
                        } // end of if not found

                        // VALUE Attribute was already there.  Check for value on attribute, if not there, add it
                        else {
                            // if attributeEdge is already a SoarIdentifierVertex, remove it and make it an EnumerationVertex
                            if (attributeEdge.V1().allowsEmanatingEdges()) {
                                dataMap.removeTriple(attributeEdge.V0(), currentTriple.getAttribute().getString(), attributeEdge.V1());
                                Vector<String> v1Vector = new Vector<>();
                                v1Vector.add(currentTriple.getValue().getString());
                                SoarVertex v1 = dataMap.createNewEnumeration(v1Vector);
                                dataMap.addTriple(currentSV, currentTriple.getAttribute().getString(), v1, true, current, currentTriple.getLine());
                                meh.generatedAddToEnumeration(currentTriple, currentTriple.getAttribute().getString(), currentTriple.getValue().getString());
                            }
                            // AttributeEdge is already an EnumerationVertex, now see if value is there
                            else if (attributeEdge.V1() instanceof EnumerationVertex) {
                                boolean valueFound = false;
                                SoarVertex attributeVertex = attributeEdge.V1();
                                Enumeration<NamedEdge> attributeEdges = dataMap.emanatingEdges(attributeVertex);
                                while (attributeEdges.hasMoreElements()) {
                                    NamedEdge currentEdge = attributeEdges.nextElement();
                                    if (currentEdge.getName().equals(currentTriple.getValue().getString())) {
                                        valueFound = true;
                                    }
                                } // end of looking through the enumerated edges of the attribute

                                if (!valueFound) {
                                    EnumerationVertex enumV = (EnumerationVertex) attributeEdge.V1();
                                    enumV.add(currentTriple.getValue().getString());
                                    dataMap.removeTriple(attributeEdge.V0(), attributeEdge.getName(), attributeEdge.V1());
                                    dataMap.addTriple(attributeEdge.V0(), attributeEdge.getName(), enumV, true, current, currentTriple.getLine());
                                    meh.generatedAddToEnumeration(currentTriple, currentTriple.getAttribute().getString(), currentTriple.getValue().getString());
                                }    // end of if value not found on enumeration, then add it
                            } // end of adding value to attribute
                            // AttributeEdge is neither an EnumerationVertex nor a SoarIdentifier, don't even try to handle this
                            else {
                                return;
                            }
                        }   // end of attribute already there, make sure value is there

                    }  // end of create enumerated vertex
                }
            }    // end of if addConstraint() fails
        }   // while going through the elements

    } // end of complete()


    private static boolean addConstraint(SoarWorkingMemoryModel dataMap,
                                         Triple triple,
                                         Map<String, Set<Match>> matches) {
        Set<Match> varSet = matches.get(triple.getVariable().getString());
        boolean matched = false;
        // for every possible start
        for (Match match : varSet){
          if (!match.hasVertex()) {
            continue;
          }
            SoarVertex currentSV = match.vertex;

            // Get all the edges from the start
            Enumeration<NamedEdge> edges = dataMap.emanatingEdges(currentSV);
            while (edges.hasMoreElements()) {
                NamedEdge currentEdge = edges.nextElement();
                if (currentEdge.satisfies(triple)) {
                    // Used for the Datamap Searches for untested/uncreated elements
                    if (triple.isCondition()) {
                        currentEdge.tested();
                    } else {
                        currentEdge.created();
                    }

                    if (!matched) {
                        matched = true;
                    }

                    if (TripleUtils.isVariable(triple.getAttribute().getString())) {
                      Set<Match> attrSet = matches.get(triple.getAttribute().getString());

                        attrSet.add(Match.fromString(currentEdge.getName()));
                    }
                    if (TripleUtils.isVariable(triple.getValue().getString())) {
                        Set<Match> valSet = matches.get(triple.getValue().getString());
                        valSet.add(Match.fromVertex(currentEdge.V1()));
                    }
                }
            }
        }
        return matched;
    }


    /**
     * This function scans a list of triples for all the triples whose
     * variable (id) is a state variable.  All such triples are
     * returned in a second vector.
     *
     * @param te the triples from the production to match
     * @see #pmpHelper
     */
    private static Vector<Triple> findStateTriples(TriplesExtractor te) {
        Iterator<Triple> iterTriples = te.triples();
        Vector<Triple> vecStateTriples = new Vector<>();

        //Find the ones that have state
        while (iterTriples.hasNext()) {
            Triple trip = iterTriples.next();

            if (trip.hasState()) {
                vecStateTriples.add(trip);
            }
        }//while

        //Find the ones that that use a state variable as their id
        iterTriples = te.triples();
        while (iterTriples.hasNext()) {
            Triple trip = iterTriples.next();
            if (vecStateTriples.contains(trip)) continue;

            Enumeration<Triple> enumStateTriples = vecStateTriples.elements();
            while (enumStateTriples.hasMoreElements()) {
                Triple trip2 = enumStateTriples.nextElement();
                String s1 = trip.getVariable().getString();
                String s2 = trip2.getVariable().getString();

                if (s1.equals(s2)) {
                    vecStateTriples.add(trip);
                    break;
                }
            }//while
        }//while

        return vecStateTriples;

    }//findStateTriples

    /*
     * This *recursive* helper function is used to actually find a path
     * match for pathMatchesProduction()
     *
     * @param vecEdges vector of all the edges to be matched in
     *                  the order that they are to be matched.
     * @param nEdgePos starting position in vecEdges (the original caller
     *             should pass in zero)
     * @param te the triples from the production to match
     * @param vecUsedTriples the triples that have already been
     *                       matched (and cannot be reused)
     * @param vecStateTriples the triples that are headed by a state
     *                        variable.  If this parameter is null
     *                        them pmphelper generates it automatically.
     * @param id the id of any triple that matches the current named
     *           edge must equals() this one.  If this is null then
     *           the id must be a state variable.
     * @param vecMatches return value for pathMatchesProduction (see below)
     *
     * @see pathMatchesProduction
     */
    private static void pmpHelper(Vector<NamedEdge> vecEdges,
                                  int nEdgePos,
                                  TriplesExtractor te,
                                  Vector<Triple> vecUsedTriples,
                                  Vector<Triple> vecStateTriples,
                                  Pair id,
                                  Vector<Triple> vecMatches) {
        //Trivial case  %%%Is this needed?
        if (vecEdges.size() <= nEdgePos) {
            return;
        }


        //Find all the triples headed by a state variable
        if (vecStateTriples == null) {
            vecStateTriples = findStateTriples(te);
        }//if

        Iterator<Triple> iterTriples = te.triples();
        NamedEdge ne = vecEdges.get(nEdgePos);
        while (iterTriples.hasNext()) {
            Triple trip = iterTriples.next();
            if ((ne.satisfies(trip))
                    && (!vecUsedTriples.contains(trip))) {
                if (((id == null) && (vecStateTriples.contains(trip)))
                        || ((id != null) && (id.equals(trip.getVariable())))) {
                    if (vecEdges.size() == nEdgePos + 1) {
                        if (!vecMatches.contains(trip)) {
                            vecMatches.add(trip);
                        }
                    } else {
                        vecUsedTriples.add(trip);
                        pmpHelper(vecEdges,
                                nEdgePos + 1,
                                te,
                                vecUsedTriples,
                                vecStateTriples,
                                trip.getValue(),
                                vecMatches);
                        vecUsedTriples.remove(trip);
                    }//else
                }//if
            }//if
        }//while

    }//pmpHelper


    /*
     *  Determines whether a given datamap TreePath matches a given
     *  SoarProduction.  This function returns a vector of all the
     *  Triples that match the last node in the path for each complete
     *  match that is found.  For example, if the path is:
     *          "<s> ^foo.bar.baz.qux <q>"
     *  then this function will return all the Triples in SoarProduction
     *  that match "<0> ^qux <q>" *and* are part of unique set of
     *  Triples that matches the entire path.  Note that this means
     *  that this function can be fooled in unusual circumstances.  If a
     *  production looks like the following then it will generate a false match
     *  because all the WMEs are present, and they are in the right order:
     *      sp {tricky
     *         (state <s> ^foo.bar <s>
     *                    ^baz.qux <qux>)
     *      -->
     *         etc...
     *
     *  If the production does not satisfy the path then the vector returned
     *  will be empty.
     *
     * @param thePath the path to match
     * @param SoarProduction the production to match
     *
     * @see #pmpHelper
     */
    public static Vector<Triple> pathMatchesProduction(TreePath thePath,
                                                       SoarProduction sp) {

        Vector<NamedEdge> vecEdges = new Vector<>();
        for (int i = 0; i < thePath.getPathCount(); i++) {
            FakeTreeNode ftn = (FakeTreeNode) thePath.getPathComponent(i);
            NamedEdge ne = ftn.getEdge();
            if (ne != null) {
                vecEdges.add(ne);
            }
        }//for

        TriplesExtractor te = new TriplesExtractor(sp);
        Vector<Triple> vecMatches = new Vector<>();

    pmpHelper(vecEdges, 0, te, new Vector<>(), null, null, vecMatches);

        return vecMatches;

    }//pathMatchesProduction


}//class DataMapMatcher

