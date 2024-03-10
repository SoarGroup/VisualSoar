package edu.umich.soar.visualsoar.misc;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;


/**
 * A FeedbackListEntry contains a message to be displayed to the user in the
 * Feedback Pane (bottom of window) along with additional information.
 * A FLE has a method {@link #react()} that can be called when the entry
 * activated by the user to resolve it in some way.
 *
 * Nominally, this base class does nothing when {@link #react()}  is called.
 * The following subclasses exist to enact varying behaviors:
 *
 * Generally there are three
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
 * @author Andrew Nuxoll
 * @version 14 Feb 2024
 */
public class FeedbackListEntry {

///////////////////////////////////////////////////////////////////
// Data Members
///////////////////////////////////////////////////////////////////
    private String message;                 //message text
    private boolean isError = false;        //if 'true' message will be displayed in red text

    //disable to turn off the "go to source" context menu option by default.  A subclass that overrides
    //the react() method should set this to true
    private boolean canGoto = false;

///////////////////////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////////////////////
    public FeedbackListEntry(String message) { this.message = message; }

    public FeedbackListEntry(String message, boolean isError) {
        this(message);
        this.isError = isError;
    }

///////////////////////////////////////////////////////////////////
// Accessor Methods
///////////////////////////////////////////////////////////////////
    public String getMessage() { return message; }
    public void setMessage(String s) { message = s; }
    public boolean isError() { return isError; }
    public boolean canGoto() { return this.canGoto; }
    public void setCanGoto(boolean b) { this.canGoto = b; }
    @Override
    public String toString() {  return message; }

///////////////////////////////////////////////////////////////////
// Other Methods
///////////////////////////////////////////////////////////////////

    /**
     * react
     *
     * is called when the user interacts with this message typically via a double click.
     */
    public void react() { /* do nothing */ }

}//class FeedbackListEntry
