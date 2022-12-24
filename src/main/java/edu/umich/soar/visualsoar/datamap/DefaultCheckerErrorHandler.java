package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.misc.FeedbackListObject;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;

/**
 * This class is notified of errors by the datamap checker
 * @author Brad Jones
 */

public class DefaultCheckerErrorHandler extends DefaultMatcherErrorHandler implements CheckerErrorHandler {
	private final String d_errorBegin;

///////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////
	public DefaultCheckerErrorHandler(OperatorNode initOpNode, String initProdName, int initStartLine) {
		super(initOpNode, initProdName, initStartLine);
		d_errorBegin = "" + this.productionName + "(" + initStartLine + "): ";
	}


//////////////////////////////////////////////////
// Modifiers
//////////////////////////////////////////////////
	public void variableNotMatched(String variable) {
//		d_errors.add(d_errorBegin + "variable " + variable + " could not be matched in production");
		d_errors.add(new FeedbackListObject(null, startLine, "variable " + variable + " could not be matched in production"));
	}
}
