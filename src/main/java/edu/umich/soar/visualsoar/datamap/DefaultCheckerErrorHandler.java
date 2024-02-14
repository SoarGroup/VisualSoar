package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.misc.FeedbackEntryOpNode;
import edu.umich.soar.visualsoar.misc.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;

/**
 * This class is notified of errors by the datamap checker
 *
 * @author Brad Jones
 */

public class DefaultCheckerErrorHandler extends DefaultMatcherErrorHandler implements CheckerErrorHandler {

    ///////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////
    public DefaultCheckerErrorHandler(OperatorNode initOpNode, String initProdName, int initStartLine) {
        super(initOpNode, initProdName, initStartLine);
    }


//////////////////////////////////////////////////
// Modifiers
//////////////////////////////////////////////////
    public void variableNotMatched(String variable) {
        d_errors.add(new FeedbackEntryOpNode(opNode, productionName, startLine, "variable " + variable + " could not be matched in production"));
    }
}
