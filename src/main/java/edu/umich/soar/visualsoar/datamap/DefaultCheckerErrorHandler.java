package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackEntryOpNode;
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
        FeedbackEntryOpNode entry = new FeedbackEntryOpNode(opNode, productionName, startLine, "variable " + variable + " could not be matched in production");
        entry.setCanFix(false);
        d_errors.add(entry);
    }
}
