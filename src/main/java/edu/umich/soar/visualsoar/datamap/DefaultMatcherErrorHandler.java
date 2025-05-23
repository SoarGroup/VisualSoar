package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackEntryOpNode;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;

import java.util.Vector;

/**
 * This class implements the default behavior that corresponds to the
 * {@link MatcherErrorHandler}
 *
 * @author Brad Jones
 */

public class DefaultMatcherErrorHandler implements MatcherErrorHandler {
    //////////////////////////////////////////////////////////
// Data Members
//////////////////////////////////////////////////////////
    protected OperatorNode opNode;    //The node containing the production
    protected String productionName;  //The production
    protected int startLine;          //Line number of the production in the file
    protected Vector<FeedbackListEntry> d_errors = new Vector<>();

    //////////////////////////////////////////////////////////
// Constructors
//////////////////////////////////////////////////////////
    public DefaultMatcherErrorHandler(OperatorNode initOpNode, String initProdName, int initStartLine) {
        this.opNode = initOpNode;
        this.productionName = initProdName;
        this.startLine = initStartLine;
    }

    //////////////////////////////////////////////////////////
// Accessors
//////////////////////////////////////////////////////////
    public Vector<FeedbackListEntry> getErrors() {
        return d_errors;
    }

    /////////////////////////////////////////////////////////
    // Modifiers
    //////////////////////////////////////////////////////////
    public void badConstraint(edu.umich.soar.visualsoar.parser.Triple triple) {
        String errMsg = "could not match constraint " + triple + " in production";
        d_errors.add(new FeedbackEntryOpNode(opNode, productionName, triple.getLine(), errMsg, true));
    }

    public void generatedIdentifier(edu.umich.soar.visualsoar.parser.Triple triple, String element) {
        String errMsg = "Added Identifier '" + element + "' to the datamap to match constraint " + triple;
        d_errors.add(new FeedbackEntryOpNode(opNode, productionName, triple.getLine(), errMsg, true));
    }

    public void generatedInteger(edu.umich.soar.visualsoar.parser.Triple triple, String element) {
        String errMsg = "Added Integer '" + element + "' to the datamap to match constraint " + triple;
        d_errors.add(new FeedbackEntryOpNode(opNode, productionName, triple.getLine(), errMsg, true));
    }

    public void generatedFloat(edu.umich.soar.visualsoar.parser.Triple triple, String element) {
        String errMsg = "Added Float '" + element + "' to the datamap to match constraint " + triple;
        d_errors.add(new FeedbackEntryOpNode(opNode, productionName, triple.getLine(), errMsg, true));
    }

    public void generatedEnumeration(edu.umich.soar.visualsoar.parser.Triple triple, String element) {
        String errMsg = "Added Enumeration '" + element + "' to the datamap to match constraint " + triple;
        d_errors.add(new FeedbackEntryOpNode(opNode, productionName, triple.getLine(), errMsg, true));
    }

    public void generatedAddToEnumeration(edu.umich.soar.visualsoar.parser.Triple triple, String attribute, String value) {
        String errMsg = "Added value '" + value + "' to the enumeration '" + attribute + "' to match constraint " + triple;
        d_errors.add(new FeedbackEntryOpNode(opNode, productionName, triple.getLine(), errMsg, true));
    }

    public void noStateVariable() {
        String errMsg = "no state variable in production";
        d_errors.add(new FeedbackEntryOpNode(opNode, productionName, startLine, errMsg, true));
    }

    public void tooManyStateVariables() {
        String errMsg = "too many state variables in production";
        d_errors.add(new FeedbackEntryOpNode(opNode, productionName, startLine, errMsg, true));
    }

}
