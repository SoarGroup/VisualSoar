package edu.umich.soar.visualsoar.mainframe.feedback;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
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
    private boolean canFix = true;          //is this a code error VS can auto-fix for the user

    ///////////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////////

    // TODO: builder would be better than all of these constructors

    /**
     * FeedbackEntryOpNode associated with a specific line in a rules file
     */
    public FeedbackEntryOpNode(OperatorNode in_node,
                               int in_ln,
                               String msg) {
        super(msg);
        node = in_node;
        lineNumber = in_ln;
        setCanGoto(true);
    }

    /**
     * FeedbackEntryOpNode associated with a specific production/line in a rules file
     */
    public FeedbackEntryOpNode(OperatorNode in_node,
                               String in_prod,
                               int in_ln,
                               String msg, boolean isError) {
      this(in_node, in_ln, msg);
      prodName = in_prod;
      this.isError = isError;
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

    public String getProdName() { return prodName; }

    public boolean canFix() { return this.canFix; }

    public void setCanFix(boolean val) { this.canFix = val; }

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

  public String toJsonLine() {
    // Unfortunately we don't have the character position or end line number.
    // TODO: derive that from the lineNumber and assocString somehow
    String position = "{\"line\": " + lineNumber + ", \"character\": 0}";
    String escapedFileName = String.valueOf(JsonStringEncoder.getInstance().quoteAsString(getFileName()));
    String location =
        "{\"uri\": \"file://"
            + escapedFileName
            + "\", \"range\": {\"start\": "
            + position
            + ", \"end\": "
            + position
            + "}}";
    String escapedMessage = String.valueOf(JsonStringEncoder.getInstance().quoteAsString(getMessage()));
    return "{\"message\": \"Operator node diagnostic\", \"severity\": "
        + (isError() ? "1" : "3")
        + ", \"relatedInformation\": [{\"message\": \""
        + escapedMessage
        + "\", \"location\": "
        + location
        + "}]"
        + ", \"source\": \"VisualSoar\"}";
  }
}//class FeedbackEntryOpNode
