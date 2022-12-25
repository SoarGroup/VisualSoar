package edu.umich.soar.visualsoar.misc;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;


/**
 * A FeedbackListObject contains a message to be displayed to the user
 * in the Feedback Pane (bottom of window).  Generally there are three
 * type of feedback messages:
 *  1.  just some text
 *         Example: "Save finished"
 *  2.  text associated with a place in the code
 *         Example: "tanksoar/wander(22): Parse exception: '}' expected"
 *  3.  text associated with a mismatch between a place in the code and a
 *      a particular datamap entry
 *         Example: "propose*wander(17) does not match constraint (&lt;s&gt; name foo)"
 *
 * @author Brad Jones
 * @version 0.5a 5 Aug 1999
 */
public class FeedbackListObject {
///////////////////////////////////////////////////////////////////
// Data Members
///////////////////////////////////////////////////////////////////
    //NOTE:  Of all the properties below, only message is mandatory.  However,
    //       the more information you provide the more helpful VS can be.
    private OperatorNode node;              //node associated with this message
    private String prodName = null;         //name of production associated with this message
    private int lineNumber = -1;            //line number associated with this message
    private String message;                 //message text
    private boolean isError = false;        //if 'true' message will be displayed in red text
    private String assocString = null;      //code string to highlight in file when user double-clicks

    // Feedback list members that are used for accessing datamap info use
    // these properties as well.
    // TODO: There should be a whole subclass for this type of feedback object
    private boolean dataMapObject = false;
    private NamedEdge edge;
    private SoarIdentifierVertex siv;
    private String dataMapName;

///////////////////////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////////////////////

    public FeedbackListObject(OperatorNode in_node,
                              int in_ln,
                              String msg) {
        node = in_node;
        lineNumber = in_ln;
        message = msg;
    }

    public FeedbackListObject(OperatorNode in_node,
                              String in_prod,
                              int in_ln,
                              String msg) {
        this(in_node, in_ln, msg);
        prodName = in_prod;
    }



    public FeedbackListObject(OperatorNode in_node,
                              int in_ln,
                              String msg,
                              String in_assocString) {
        this(in_node, in_ln, msg);
        assocString = in_assocString;
    }

    public FeedbackListObject(OperatorNode in_node,
                              int in_ln,
                              String msg,
                              boolean isError) {
        this(in_node, in_ln, msg);
        this.isError = isError;
    }

    public FeedbackListObject(OperatorNode in_node,
                              String in_prod,
                              int in_ln,
                              String msg,
                              boolean _msgEnough,
                              boolean isError) {
        this(in_node, in_ln, msg, _msgEnough);
        prodName = in_prod;
        this.isError = isError;
    }

    public FeedbackListObject(NamedEdge in_edge,
                              SoarIdentifierVertex in_siv,
                              String inDataMapName,
                              String msg) {
        edge = in_edge;
        siv = in_siv;
        message = msg;
        dataMapName = inDataMapName;
        dataMapObject = true;

    }

    //For simple messages there may be no associated node
    public FeedbackListObject(String msg) {
        this(null, 0, msg, true);

    }

    ///////////////////////////////////////////////////////////////////
// Methods
///////////////////////////////////////////////////////////////////
    public boolean isError() {
        return isError;
    }

    public boolean hasNode() {
        return node != null;
    }

    /**
     * returns the filename of the file for which the node of this
     * list object is associated
     *
     * @return a string which is the file name of the file of which this object is associated
     */
    public String getFileName() {
        return node.getFileName();
    }

    /**
     * returns the node for which this object is associated
     *
     * @return a reference to the node
     */
    public OperatorNode getNode() {
        return node;
    }

    /**
     * sets the node with a new value
     */
    public void setNode(OperatorNode newNode) {
        this.node = newNode;
    }


    /**
     * returns the line number for which this object is associated
     *
     * @return an int that is positive
     */
    public int getLine() {
        return lineNumber;
    }

    /**
     * returns the message for this object
     *
     * @return a string that is additional information for the user
     */
    public String getMessage() {
        return message;
    }

    /**
     * lets you set a new message
     *
     * @param s a string that is the new message
     */
    public void setMessage(String s) {
        message = s;
    }

    /**
     * determines whether object relates to a datamap or production
     *
     * @return true if object relates to a datamap entry
     */
    public boolean isDataMapObject() {
        return dataMapObject;
    }

    /**
     * Creates a DataMap from the datamap information
     *
     * @return a datamap based on the information in this object, null if not a datamap object type
     */
    public DataMap createDataMap(SoarWorkingMemoryModel swmm) {
        DataMap dm;
        if (isDataMapObject()) {
            if (siv.getValue() != 0) {
                dm = new DataMap(swmm, siv, dataMapName);
            } else {
                dm = new DataMap(swmm, swmm.getTopstate(), dataMapName);
            }
            return dm;
        } else {
            return null;
        }
    }

    public int getDataMapId() {
        if (isDataMapObject()) {
            return siv.getValue();
        } else {
            return -1;
        }
    }

    /**
     * Returns the NamedEdge associated with this object, null if not a datamap object
     */
    public NamedEdge getEdge() {
        if (isDataMapObject()) {
            return edge;
        } else {
            return null;
        }
    }

    /**
     * returns a string to represent this object, it is a combination of
     * the file, line number and the message
     *
     * @return a string that represents this object
     */
    public String toString() {
        if (!isDataMapObject()) {
            if (node == null) {
                return message;
            }
            else {
                String retVal = node.getUniqueName() + "(" + lineNumber + "): " + message;
                if (prodName != null) {
                    retVal = prodName + ": " + retVal;
                }
                return retVal;
            }
        } else {
            return dataMapName + ":  " + edge.toString();
        }
    }

    /**
     * Displays the file associated with the node referenced by this object
     */
    public void DisplayFile() {
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

}//class FeedbackListObject
