package edu.umich.soar.visualsoar.misc;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;

/**
 * class FeedbackEntryOpNode
 * <p>
 * is a subclass of {@link FeedbackListEntry} that identifies a place in the
 * code for a particular operator node. It reacts by displaying this code
 * file to the user and highlighting the associated line.
 */
public class FeedbackEntryOpNode extends FeedbackListEntry {

    ///////////////////////////////////////////////////////////////////
    // Data Members
    ///////////////////////////////////////////////////////////////////
    private OperatorNode node;              //node associated with this message
    private String prodName = null;         //name of production associated with this message
    private int lineNumber = -1;            //line number associated with this message
    private String assocString = null;      //code string to highlight in file

    ///////////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////////

    /**
     * FeedbackEntryOpNode associated with a specific line in a rules file
     */
    public FeedbackEntryOpNode(OperatorNode in_node,
                               int in_ln,
                               String msg) {
        super(msg);
        node = in_node;
        lineNumber = in_ln;
    }

    /**
     * FeedbackEntryOpNode associated with a specific production/line in a rules file
     */
    public FeedbackEntryOpNode(OperatorNode in_node,
                               String in_prod,
                               int in_ln,
                               String msg) {
        this(in_node, in_ln, msg);
        prodName = in_prod;
    }


    /**
     * FeedbackEntryOpNode associated with a specific substring of a particular line in a rules file
     */
    public FeedbackEntryOpNode(OperatorNode in_node,
                               int in_ln,
                               String msg,
                               String in_assocString) {
        this(in_node, in_ln, msg);
        assocString = in_assocString;
    }

    /**
     * FeedbackEntryOpNode associated with a specific line in a rules file
     * that is an ERROR and, thus, will be displayed with red text
     */
    public FeedbackEntryOpNode(OperatorNode in_node,
                               int in_ln,
                               String msg,
                               boolean isError) {
        super(msg, isError);
        this.node = in_node;
        this.lineNumber = in_ln;
    }

    ///////////////////////////////////////////////////////////////////
    // Accessor Methods
    ///////////////////////////////////////////////////////////////////
    public String getFileName() {
        return node.getFileName();
    }

    public OperatorNode getNode() {
        return node;
    }

    public void setNode(OperatorNode newNode) {
        this.node = newNode;
    }

    public int getLine() {
        return lineNumber;
    }

    @Override
    public String toString() {
        String retVal = getMessage();
        if (node != null) {
            retVal = node.getUniqueName() + "(" + lineNumber + "): " + getMessage();
            if (prodName != null) {
                retVal = prodName + ": " + retVal;
            }
        }
        return retVal;
    }

    ///////////////////////////////////////////////////////////////////
    // Methods
    ///////////////////////////////////////////////////////////////////


    /** open the associated rules file to the correct line */
    @Override
    public void react() {
        if (assocString != null) {
            node.openRulesToString(MainFrame.getMainFrame(),
                    lineNumber,
                    assocString);
        } else if (lineNumber >= 0) {
            node.openRules(MainFrame.getMainFrame(), lineNumber);
        } else {
            node.openRules(MainFrame.getMainFrame());
        }
    }


}//class FeedbackEntryOpNode
