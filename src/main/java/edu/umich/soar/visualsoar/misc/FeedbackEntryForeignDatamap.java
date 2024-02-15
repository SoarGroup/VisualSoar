package edu.umich.soar.visualsoar.misc;

import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.graph.ForeignVertex;
import edu.umich.soar.visualsoar.graph.NamedEdge;

/**
 * class FeedbackEntryForeignDatamap
 * <p>
 * is a subclass of {@link FeedbackListEntry} that identifies an entry in a
 * {@link edu.umich.soar.visualsoar.graph.ForeignVertex} entry in the
 * local datamap that no longer matches the corresponding vertex in
 * the external (foreign) datamap. It's react() method allows the user
 * to resolve the issue in some way.
 */
public class FeedbackEntryForeignDatamap extends FeedbackListEntry {

    //Valid behaviors for this type of feedback correspond to a finite set of issues below
    public static final int ERR_UNKNOWN_ISSUE = -1;
    public static final int ERR_DM_FILE_UNREADABLE = -2;
    public static final int ERR_FOREIGN_VERTEX_MISSING = -3;
    public static final int ERR_FOREIGN_VERTEX_TYPE_MISMATCH = -4;
    public static final int ERR_FOREIGN_VERTEX_COMMENT_MISMATCH = -5;


    private SoarWorkingMemoryModel localSWMM;   //the local datamap
    private NamedEdge localEdge;                //the named edge in localSWMM that is the source of the problem
    private ForeignVertex localFV;              //the value of localEdge.V1() <- must be a ForeignVertex object
    private SoarWorkingMemoryModel foreignSWMM; //the external datamap
    private NamedEdge foreignEdge;              //the named edge in foreignSWMM that corresponds to localEdge
    private int errType;                        //one of the constants defined above

///////////////////////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////////////////////

    /**
     *
     * @param localSWMM    the local datmap that contains the ForeignVertex
     * @param foreignSWMM  the external datamap that the ForeignVertex is referring to.
     *                     This can be null to indicate "not found."
     * @param localEdge    the NamedEdge object in localSWMM that's causing the problem
     * @param errType      what the issue is (see the constants above)
     */
    public FeedbackEntryForeignDatamap(SoarWorkingMemoryModel localSWMM,
                                       SoarWorkingMemoryModel foreignSWMM,
                                       NamedEdge localEdge,
                                       NamedEdge foreignEdge,
                                       int errType) {
        super(null);  //the message generated based upon switch statement below
        this.localSWMM = localSWMM;
        this.foreignSWMM = foreignSWMM;
        this.localEdge = localEdge;
        this.foreignEdge = foreignEdge;
        this.errType = errType;
        this.localFV = (ForeignVertex)localEdge.V1();

        switch (errType) {
            case ERR_DM_FILE_UNREADABLE:
                setMessage("Foreign datamap file missing.  " + localFV.getForeignDMName() + " does not exist or can not be read.");
                break;
            case ERR_FOREIGN_VERTEX_MISSING:
                setMessage("Corresponding entry for " + localEdge.toString() + " not found in " + localFV.getForeignDMName());
                break;
            case ERR_FOREIGN_VERTEX_TYPE_MISMATCH:
                //TODO
//                SoarVertex foreignEdge.V1()
//                setMessage("Corresponding entry for " + localEdge.toString() + " is of type " + not found in " + localFV.getForeignDMName());
                break;
            case ERR_FOREIGN_VERTEX_COMMENT_MISMATCH:
                //TODO
                break;
            default:
                //TODO
                setMessage("Unknown issue " + "TODO");
                break;
        }//switch
    }//ctor

//    /** helper for the ctor that inits non-given instance variables and
//     * calculates what the error is based upon the data given */
//    public int calcError() {
//
//        //Makes sure the given NamedEdge is legit.  (It should always be...)
//        if ( (localEdge == null) || (! (localEdge.V1() instanceof ForeignVertex)) ) {
//            return ERR_UNKNOWN_ISSUE;
//        }
//        try {
//            this.localVertex = (ForeignVertex) localEdge.V1();
//        } catch(NullPointerException npe) {
//            this.errType = ERR_UNKNOWN_ISSUE;
//            return;
//        }
//
//
//
//        //Calculate the value for foreignVertex
//        if (foreignSWMM != null) {
//
//            int foreignId = this.localVertex.getLinkedSoarVertex().getValue();
//            SoarVertex foreignFV = foreignSWMM.getVertexForId(foreignId);
//        }
//
//
//        if (localVertex == null) {  //should never happen
//            setMessage("Unexpected error occurred while validating foreign datamap entry.")
//        }
//        else if (foreignSWMM == null) {
//            setMessage("Foreign datamap file missing.  " + localVertex.getForeignDMName() + " does not exist.");
//        }
//        else {
//
//            if (foreignFV == null) {
//                setMessage("Corresponding entry for " + localVertex.toString() + " not found in " + localVertex.getForeignDMName());
//            }
//            else {
//                //check for mismatch
//                foreignSWMM.getMatchingParent()
//                if ()
//            }
//
//        }
//
//        //Set the message string
//        getMessage();
//    }//calcError


///////////////////////////////////////////////////////////////////
// Other Methods
///////////////////////////////////////////////////////////////////

    /**
     * react
     *
     * is called when the user interacts with this message typically via a double click.
     */
    //TODO!!
    public void react() {  }
}
