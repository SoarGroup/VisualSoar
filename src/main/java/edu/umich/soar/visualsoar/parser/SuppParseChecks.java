package edu.umich.soar.visualsoar.parser;

import edu.umich.soar.visualsoar.misc.FeedbackListObject;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;

import java.util.Iterator;
import java.util.Vector;

/**
 * class SuppParseChecks
 *
 * contains static methods for performing supplementary
 * checks on parsed productions.
 *
 * @author Andrew Nuxoll
 * created 15 Sep 2022
 */
public class SuppParseChecks {

    /**
     * getSimpleTestVarName
     *
     * is a helper method for {@link #checkUndefinedVarRHS}.  It
     * extracts the variable name from a SimpleTest object and places
     * it in the given vector (if found).
     */
    private static void getSimpleTestVarName(Vector<String> vars, SimpleTest stest) {
        //disjunctions can't have a variable
        if (stest.isDisjunctionTest()) return;

        SingleTest singleTest = stest.getRelationalTest().getSingleTest();
        if (singleTest.isConstant()) return;
        vars.add(singleTest.getVariable().getString());
    }//getSimpleTestVarName

    /**
     * getAttrTestVarNames
     *
     * is a helper method for {@link #checkUndefinedVarRHS}.  It
     * extracts the variable names from an AttributeTest object and
     * places them in the given vector (if found).
     */
    private static void getAttrTestVarNames(Vector<String> vars, Test test) {
        if (test.isConjunctiveTest()) {
            ConjunctiveTest cjTest = test.getConjunctiveTest();
            Iterator stIter = cjTest.getSimpleTests();
            while (stIter.hasNext()) {
                SimpleTest stest = (SimpleTest) stIter.next();
                getSimpleTestVarName(vars, stest);
            }
        } else {
            SimpleTest stest = test.getSimpleTest();
            getSimpleTestVarName(vars, stest);
        }
    }//getAttrTestVarNames

    /**
     * getPCondVarNames
     *
     * is a helper method for {@link #checkUndefinedVarRHS}.  It
     * extracts the variable names from PositiveCondition object and
     * places them in the given vector (if found).
     *
     * Warning:  recursive method
     */
    private static void getPCondVarNames(Vector<String> vars, PositiveCondition pcond) {
        //A PCond can be a conjunction of conditions...
        if (pcond.isConjunction()) {
            //Recurse into the conjunction
            Iterator conjIter = pcond.getConjunction();
            while (conjIter.hasNext()) {
                Condition cond = (Condition)conjIter.next();
                getPCondVarNames(vars, cond.getPositiveCondition());
            }
            return;
        }

        //Identifier
        vars.add(pcond.getConditionForOneIdentifier().getVariable().getString());

        //attribute-value tests
        Iterator avIter = pcond.getConditionForOneIdentifier().getAttributeValueTests();
        while(avIter.hasNext()) {
            AttributeValueTest avt = ((AttributeValueTest)avIter.next());

            //Attribute tests
            Iterator attrIter = avt.getAttributeTests();
            while(attrIter.hasNext()) {
                AttributeTest attr = ((AttributeTest)attrIter.next());
                getAttrTestVarNames(vars, attr.getTest());
            }//attr tests

            //Value Tests
            Iterator valIter = avt.getValueTests();
            while(valIter.hasNext()) {
                ValueTest val = ((ValueTest)valIter.next());
                getValTestVarNames(vars, val.getTest());
            }//Value

        }//attr-val tests


    }//getPCondVarNames

    /**
     * getAttrTestVarNames
     *
     * is a helper method for {@link #checkUndefinedVarRHS}.  It
     * extracts the variable names from an AttributeTest object and
     * places them in the given vector (if found).
     */
    private static void getValTestVarNames(Vector<String> vars, Test test) {
        if (test.isConjunctiveTest()) {
            ConjunctiveTest cjTest = test.getConjunctiveTest();
            Iterator stIter = cjTest.getSimpleTests();
            while (stIter.hasNext()) {
                SimpleTest stest = (SimpleTest) stIter.next();
                getSimpleTestVarName(vars, stest);
            }
        } else {
            SimpleTest stest = test.getSimpleTest();
            getSimpleTestVarName(vars, stest);
        }
    }//getValTestVarNames

    /**
     * getLHSVarNames
     *
     * builds a list of variables that match in the LHS
     *
     * @param prod
     */
    private static Vector<String> getLHSVarNames(SoarProduction prod) {

        Vector<String> lhsVars = new Vector<>();
        Iterator condIter = prod.getConditionSide().getConditions();
        while(condIter.hasNext()) {
            PositiveCondition pcond = ((Condition) condIter.next()).getPositiveCondition();
            getPCondVarNames(lhsVars, pcond);
        }//while

        return lhsVars;
    }//getLHSVarNames

    /**
     * checkUndefinedVarRHS
     *
     * This method reviews a parsed productions for the case where
     * a variable is being used as an idenifier on the RHS but that
     * variable has neither been matched on LHS nor created on the RHS.
     *
     * The Soar Parser apparently doesn't check for this so I'm doing
     * it manually here.
     *
     * @param opNode the file containing this production
     * @param prod  a SoarProduction object to inspect
     *
     * @return null if no error found or a FeedbackListObject otherwise
     */
    public static FeedbackListObject checkUndefinedVarRHS(OperatorNode opNode, SoarProduction prod) throws ParseException {
        Vector<String> createdVars = getLHSVarNames(prod);
        //Used vars are saved as Pair so their line number can be referenced
        Vector<Pair> usedVars = new Vector<>();

        //Get variables from the on RHS.  Some of these will be variables
        //that are created and others will be used.
        // Example:  "(<s> ^foo <v>)" uses "<s>" and creates "<v>"
        Iterator actIter = prod.getActionSide().getActions();
        while(actIter.hasNext()) {
            //Have to use full path to class name because javax.swing.Action also exists...
            edu.umich.soar.visualsoar.parser.Action act = (edu.umich.soar.visualsoar.parser.Action)actIter.next();
            if (! act.isVarAttrValMake()) continue; //ignore function call

            //The identifier slot is always a used variable
            usedVars.add(act.getVarAttrValMake().getVariable());

            Iterator avIter = act.getVarAttrValMake().getAttributeValueMakes();
            while(avIter.hasNext()) {
                AttributeValueMake avMake = (AttributeValueMake)avIter.next();

                //RHS value tested
                Iterator rhsValIter = avMake.getRHSValues();
                while (rhsValIter.hasNext()) {
                    RHSValue rhsVal = (RHSValue) rhsValIter.next();
                    if (rhsVal.isVariable()) {
                        usedVars.add(rhsVal.getVariable());
                    }
                }

                //RHS value created
                Iterator rhsMakeIter = avMake.getValueMakes();
                while(rhsMakeIter.hasNext()) {
                    ValueMake val = (ValueMake) rhsMakeIter.next();
                    if (val.getRHSValue().isVariable()) {
                        createdVars.add(val.getRHSValue().getVariable().getString());
                    }
                }
            }//loop over Action VarAttrVals

        }//loop over Actions

        //Now verify that every variable used has been created
        for(Pair p : usedVars) {
            if (createdVars.contains(p.getString())) continue;
            String errString = "Variable " + p.getString() + " is used on the RHS but is never tested or created.";
            return new FeedbackListObject(opNode, p.getLine(), errString);
        }

        return null;

    }//checkUndefinedVarRHS


}//class SuppParseChecks
