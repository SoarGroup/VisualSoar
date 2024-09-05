package edu.umich.soar.visualsoar.mainframe.feedback;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.graph.*;
import edu.umich.soar.visualsoar.operatorwindow.OperatorRootNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;

/**
 * class FeedbackEntryForeignDatamap
 * <p>
 * is a subclass of {@link FeedbackListEntry} that identifies a
 * {@link edu.umich.soar.visualsoar.graph.ForeignVertex} entry in the
 * local datamap that has some issue with.  In most cases, it no longer
 * matches the corresponding vertex in the external (foreign) datamap.
 * It's react() method allows the user to resolve the issue in some way.
 */
public class FeedbackEntryForeignDatamap extends FeedbackEntryDatamap {

    //Valid behaviors for this type of feedback correspond to a finite set of issues below
    public static final int ADDED_FOREIGN_VERTEX = 1;
    public static final int ERR_UNKNOWN_ISSUE = -1;
    public static final int ERR_DM_FILE_UNREADABLE = -2;
    public static final int ERR_FOREIGN_VERTEX_MISSING = -3;
    public static final int ERR_FOREIGN_VERTEX_TYPE_MISMATCH = -4;

    private int entryType;                      //one of the constants defined above

///////////////////////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////////////////////

    /**
     *
     * @param localEdge    the NamedEdge in localSWMM that's causing the problem.
     *                     The V1() of this edge must be a ForeignVertex.
     * @param entryType     one of the constants listed above
     * @param msg          the associated error message
     */
    public FeedbackEntryForeignDatamap(NamedEdge localEdge,
                                       int entryType,
                                       String msg) {
        //The nulls setn to this parent ctor will be calculated and set below.
        super(localEdge, null, null, null);

        this.entryType = entryType;
        setMessage(msg);

        //calculate super.siv
        OperatorWindow operatorWindow = MainFrame.getMainFrame().getOperatorWindow();
        this.siv = operatorWindow.getDatamap().getTopstate();

        //calculate super.dataMapName
        OperatorRootNode root = (OperatorRootNode)(operatorWindow.getModel().getRoot());
        this.dataMapName = root.toString();

        //For now, VS ignores context menu actions for missing datamap files
        //TODO:  implement code to find the lost .dm file and update the entire subtree
        if (entryType == ERR_DM_FILE_UNREADABLE) {
            this.setCanGoto(false);
        }

    }//ctor

    @Override
    public String toString() { return this.getMessage(); }

    /**
     * A factory method that examples a local ForeignVertex and the foreign entry it is referring to.
     *
     * This is used be DataMapTree.compareForeignSubTree().
     *
     * @param localNE    the local NamedEdge whose V1() value refers to an entry in a foreign datamap.
     * @param foreignSV  the foreign datamap entry that localNE.V1() is referring to
     * @param rep        a string representation of the local vertex (ala "^foo.bar.baz")
     *
     * @return a FeedbackEntryForeignDatamap object describing the mismatch (or null if no mismatch).
     *         Note that the message for this object will be preceded with four spaces (indented) as
     *         it is expected to be included in a list.
     *
     * CAVEAT: The caller must ensure that:
     *           - localNE.V1() is a ForeignVertex
     *           - localNE.V1().getCopyOfForeignSoarVertex().getNumber() == foreignSV.getNumber()
     */
    public static FeedbackEntryForeignDatamap compareForeignVertex(NamedEdge localNE, SoarVertex foreignSV, String rep) {
        //Check:  type mismatch
        String fcTypeName = foreignSV.typeName();
        ForeignVertex localSV = (ForeignVertex)localNE.V1();
        SoarVertex efcSV = localSV.getCopyOfForeignSoarVertex();
        String efcSVTypeName = efcSV.typeName();
        if (! fcTypeName.equals(efcSVTypeName)) {
            String msg =  "    " + rep + " is a " + efcSVTypeName + " but it is a " + fcTypeName + " in " + localSV.getForeignDMName();
            return new FeedbackEntryForeignDatamap(localNE, ERR_FOREIGN_VERTEX_TYPE_MISMATCH, msg);
        }

        //Check:  different integer range
        if (foreignSV instanceof IntegerRangeVertex) {
            String actualRange = ((IntegerRangeVertex)foreignSV).getRangeString();
            String expRange = ((IntegerRangeVertex) efcSV).getRangeString();
            if (! expRange.equals(actualRange)) {
                String msg = "    " + rep + " has range " + expRange + " but the range is " + actualRange + " in " + localSV.getForeignDMName();
                return new FeedbackEntryForeignDatamap(localNE, ERR_FOREIGN_VERTEX_TYPE_MISMATCH, msg);
            }
        }

        //Check:  different float range
        if (foreignSV instanceof FloatRangeVertex) {
            String actualRange = ((FloatRangeVertex)foreignSV).getRangeString();
            String expRange = ((FloatRangeVertex) efcSV).getRangeString();
            if (! expRange.equals(actualRange)) {
                String msg = "    " + rep + " has range " + expRange + " but the range is " + actualRange + " in " + localSV.getForeignDMName();
                return new FeedbackEntryForeignDatamap(localNE, ERR_FOREIGN_VERTEX_TYPE_MISMATCH, msg);
            }
        }

        //Check:  different enumeration contents
        if (foreignSV instanceof EnumerationVertex) {
            String actualContents = foreignSV.toString();
            String expContents = efcSV.toString();
            if (! expContents.equals(actualContents)) {
                //adjust string to make more human-readable by remove content before the '['
                int brack = actualContents.indexOf('[');
                if (brack != -1) actualContents = actualContents.substring(brack);
                brack = expContents.indexOf('[');
                if (brack != -1) expContents = expContents.substring(brack);

                String msg = "    " + rep + " has contents " + expContents + " but the contents are " + actualContents + " in " + localSV.getForeignDMName();
                return new FeedbackEntryForeignDatamap(localNE, ERR_FOREIGN_VERTEX_TYPE_MISMATCH, msg);
            }
        }

        //foreign vertex is a leaf when local vertex isn't a leaf
        if (localSV.allowsEmanatingEdges() && (! foreignSV.allowsEmanatingEdges())) {
            String msg = "    " + rep + " is a vertex with children but it is not so in " + localSV.getForeignDMName();
            return new FeedbackEntryForeignDatamap(localNE, ERR_FOREIGN_VERTEX_TYPE_MISMATCH, msg);
        }

        //local vertex is a leaf when foreign vertex isn't a leaf
        if ((! localSV.allowsEmanatingEdges()) && foreignSV.allowsEmanatingEdges()) {
            String msg = "    " + rep + " is a vertex without children but it is not so in " + localSV.getForeignDMName();
            return new FeedbackEntryForeignDatamap(localNE, ERR_FOREIGN_VERTEX_TYPE_MISMATCH, msg);
        }


        return null;   //no mismatch, yay :)
    }//compareForeignVertex


    //TODO:  Override react() to handle search for missing foreign datmap

}//class FeedbackEntryForeignDatamap
