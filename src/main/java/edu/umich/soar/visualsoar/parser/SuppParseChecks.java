package edu.umich.soar.visualsoar.parser;

import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackEntryOpNode;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * class SuppParseChecks
 * <p>
 * contains static methods for performing supplementary
 * checks on parsed productions.
 *
 * @author Andrew Nuxoll
 * created 15 Sep 2022
 */
public class SuppParseChecks {

    /**
     * getSimpleTestVarName
     * <p>
     * is a helper method for {@link #checkUndefinedVarRHS}.  It
     * extracts the variable name from a SimpleTest object and places
     * it in the given vector (if found).
     */
    private static void getSimpleTestVarName(Vector<String> vars, SimpleTest sTest) {
        //disjunctions can't have a variable
        if (sTest.isDisjunctionTest()) return;

        SingleTest singleTest = sTest.getRelationalTest().getSingleTest();
        if (singleTest.isConstant()) return;
        vars.add(singleTest.getVariable().getString());
    }//getSimpleTestVarName

    /**
     * getAttrTestVarNames
     * <p>
     * is a helper method for {@link #checkUndefinedVarRHS}.  It
     * extracts the variable names from an AttributeTest object and
     * places them in the given vector (if found).
     */
    private static void getAttrTestVarNames(Vector<String> vars, Test test) {
        if (test.isConjunctiveTest()) {
            ConjunctiveTest cjTest = test.getConjunctiveTest();
            Iterator<SimpleTest> stIter = cjTest.getSimpleTests();
            while (stIter.hasNext()) {
                SimpleTest sTest = stIter.next();
                getSimpleTestVarName(vars, sTest);
            }
        } else {
            SimpleTest sTest = test.getSimpleTest();
            getSimpleTestVarName(vars, sTest);
        }
    }//getAttrTestVarNames

    /**
     * getPCondVarNames
     * <p>
     * is a helper method for {@link #checkUndefinedVarRHS}.  It
     * extracts the variable names from PositiveCondition object and
     * places them in the given vector (if found).
     * <p>
     * Warning:  recursive method
     */
    private static void getPCondVarNames(Vector<String> vars, PositiveCondition pCond) {
        //A PCond can be a conjunction of conditions...
        if (pCond.isConjunction()) {
            //Recurse into the conjunction
            Iterator<Condition> conjIter = pCond.getConjunction();
            while (conjIter.hasNext()) {
                Condition cond = conjIter.next();
                getPCondVarNames(vars, cond.getPositiveCondition());
            }
            return;
        }

        //Identifier
        vars.add(pCond.getConditionForOneIdentifier().getVariable().getString());

        //attribute-value tests
        Iterator<AttributeValueTest> avIter = pCond.getConditionForOneIdentifier().getAttributeValueTests();
        while (avIter.hasNext()) {
            AttributeValueTest avt = avIter.next();

            //Attribute tests
            Iterator<AttributeTest> attrIter = avt.getAttributeTests();
            while (attrIter.hasNext()) {
                AttributeTest attr = attrIter.next();
                getAttrTestVarNames(vars, attr.getTest());
            }//attr tests

            //Value Tests
            Iterator<ValueTest> valIter = avt.getValueTests();
            while (valIter.hasNext()) {
                ValueTest val = valIter.next();
                getValTestVarNames(vars, val.getTest());
            }//Value

        }//attr-val tests


    }//getPCondVarNames

    /**
     * getAttrTestVarNames
     * <p>
     * is a helper method for {@link #checkUndefinedVarRHS}.  It
     * extracts the variable names from an AttributeTest object and
     * places them in the given vector (if found).
     */
    private static void getValTestVarNames(Vector<String> vars, Test test) {
        if (test.isConjunctiveTest()) {
            ConjunctiveTest cjTest = test.getConjunctiveTest();
            Iterator<SimpleTest> stIter = cjTest.getSimpleTests();
            while (stIter.hasNext()) {
                SimpleTest sTest = stIter.next();
                getSimpleTestVarName(vars, sTest);
            }
        } else {
            SimpleTest sTest = test.getSimpleTest();
            getSimpleTestVarName(vars, sTest);
        }
    }//getValTestVarNames

    /**
     * getLHSVarNames
     * <p>
     * builds a list of variables that match in the LHS
     */
    private static Vector<String> getLHSVarNames(SoarProduction prod) {

        Vector<String> lhsVars = new Vector<>();
        Iterator<Condition> condIter = prod.getConditionSide().getConditions();
        while (condIter.hasNext()) {
            PositiveCondition pCond = condIter.next().getPositiveCondition();
            getPCondVarNames(lhsVars, pCond);
        }//while

        return lhsVars;
    }//getLHSVarNames

    /**
     * checkUndefinedVarRHS
     * <p>
     * This method reviews a parsed productions for the case where
     * a variable is being used as an identifier on the RHS but that
     * variable has neither been matched on LHS nor created on the RHS.
     * <p>
     * The Soar Parser apparently doesn't check for this, so I'm doing
     * it manually here.
     *
     * @param opNode the file containing this production
     * @param prod   a SoarProduction object to inspect
     * @return null if no error found or a FeedbackListEntry otherwise
     */
    public static FeedbackListEntry checkUndefinedVarRHS(OperatorNode opNode, SoarProduction prod) throws ParseException {
        Vector<String> createdVars = getLHSVarNames(prod);
        //Used vars are saved as Pair so their line number can be referenced
        Vector<Pair> usedVars = new Vector<>();

        //Get variables from the on RHS.  Some of these will be variables
        //that are created and others will be used.
        // Example:  "(<s> ^foo <v>)" uses "<s>" and creates "<v>"
        Iterator<Action> actIter = prod.getActionSide().getActions();
        while (actIter.hasNext()) {
            Action act = actIter.next();
            if (!act.isVarAttrValMake()) continue; //ignore function call

            //The identifier slot is always a used variable
            usedVars.add(act.getVarAttrValMake().getVariable());

            Iterator<AttributeValueMake> avIter = act.getVarAttrValMake().getAttributeValueMakes();
            while (avIter.hasNext()) {
                AttributeValueMake avMake = avIter.next();

                //RHS value tested
                Iterator<RHSValue> rhsValIter = avMake.getRHSValues();
                while (rhsValIter.hasNext()) {
                    RHSValue rhsVal = rhsValIter.next();
                    if (rhsVal.isVariable()) {
                        usedVars.add(rhsVal.getVariable());
                    }
                }

                //RHS value created
                Iterator<ValueMake> rhsMakeIter = avMake.getValueMakes();
                while (rhsMakeIter.hasNext()) {
                    ValueMake val = rhsMakeIter.next();
                    if (val.getRHSValue().isVariable()) {
                        createdVars.add(val.getRHSValue().getVariable().getString());
                    }
                }
            }//loop over Action VarAttrVals

        }//loop over Actions

        //Now verify that every variable used has been created
        for (Pair p : usedVars) {
            if (createdVars.contains(p.getString())) continue;
            String errString = "Variable " + p.getString() + " is used on the RHS but is never tested or created.";
            return new FeedbackEntryOpNode(opNode, p.getLine(), errString);
        }

        return null;

    }//checkUndefinedVarRHS

    /**
     * findMissingBracePositions
     * <p>
     * given the text of some Soar code, this method identifies
     * if there is an unmatched brace at the end of any of its productions.
     * This is a syntax error that can be fixed automatically for the user.
     *
     * @param text the code to analyze
     * @return the locations where braces should be placed
     */
    public static Vector<Integer> findMissingBracePositions(String text) {
        //This vector stores all the places to insert a courtesy close brace
        // (typically there will be none)
        Vector<Integer> bracePositions = new Vector<>();

        //Bail out now if there isn't sufficient text to bother with
        if (text.length() < 10) return bracePositions;

        //find the start position of each production
        Pattern prodPattern = Pattern.compile("[ \t]*[sg]p[ \t]*\\{");
        Matcher prodMatch = prodPattern.matcher(text);
        Vector<Integer> prodStarts = new Vector<>();
        while (prodMatch.find()) {
            prodStarts.add(prodMatch.start());
        }
        prodStarts.add(text.length()); //add end of file to support loop below

        //Iterate over each subsection of the text that contains a production
        for(int pos = 0; pos < prodStarts.size() - 1; ++pos) {
            int start = prodStarts.get(pos);
            int end = prodStarts.get(pos + 1) - 1;

            //Count the open and close braces to detect a mismatch
            //also keep track of some landmarks in the text b lock
            int depth = 0;
            int lastCloseParen = -1;
            int lastCloseBrace = -1;
            int lastNonWhite = -1;  //last non-whitespace that wasn't in a comment
            for (int i = start; i <= end; ++i) {
                switch (text.charAt(i)) {
                    case '{':
                        depth++;
                        break;
                    case '}':
                        lastCloseBrace = i;
                        depth--;
                        break;
                    case ')':
                        lastCloseParen = i;
                        break;
                    case '#':  //ignore comments
                        while (text.charAt(i) != '\n') {
                            i++;
                        }
                        break;
                }
                //last non-whitespace char that's not in a comment
                if (! Character.isWhitespace(text.charAt(i))) {
                    lastNonWhite = i;
                }
            }//counting braces

            //if there was a one-level mismatch see if the last brace appears
            // to be missing
            if (depth == 1) {
                //Don't make edits in these suspicious situations:
                //1.  no close parens found
                //2.  there's code after the last paren
                //3.  the last close brace is after the last paren
                if ( (lastCloseParen != -1)
                        && (lastNonWhite <= lastCloseParen)
                        && (lastCloseBrace < lastCloseParen)) {

                    //Insert a brace in one of these positions
                    //1. whitespace immediately after a new line (preferred)
                    //2. the first whitespace char after the last paren (acceptable)
                    //3. the very last char in the block (least preferred)
                    int index = lastCloseParen + 1;
                    int firstWhite = -1;  //location of first white space
                    int prefSpot = -1;  //location of preferred position found
                    while(index < end) {
                        char c = text.charAt(index);
                        if (Character.isWhitespace(c)) {
                            //any whitespace is good
                            if (firstWhite == -1) firstWhite = index;
                            //a whitespace after a newline is preferred
                            if (c == '\n') {
                                prefSpot = index;
                                break;
                            }
                        }
                        index++;
                    }
                    if (prefSpot > -1) {
                        bracePositions.add(prefSpot);
                    }
                    else if (firstWhite > -1) {
                        bracePositions.add(firstWhite);
                    }
                    else if (index >= end - 1) {  //very last spot
                        bracePositions.add(index);
                    }
                }//likely a missing close-brace
            }//mismatch found
        }//for each production

        return bracePositions;
    }//findMissingBracePositions

    /**
     * inserts closing braces at given positions.  This is meant to be used
     * with the return value of {@link #findMissingBracePositions}
     *
     * @param text           the text to insert them into
     * @param bracePositions where to insert them
     * @return the revised text
     */
    public static String insertBraces(String text, Vector<Integer> bracePositions) {
        if (!bracePositions.isEmpty()) {
            int offset = 0;
            for (int i : bracePositions) {
                String before = text.substring(0, i + offset);
                String after = text.substring(i + offset);
                text = before + "\n}" + after;
                offset++;
            }
        }//if

        return text;
    }//insertBraces

    /** helper method to load the contents of a given file into a String */
    private static String getFileContent(String filename) {
        Path fPath = Paths.get(filename);
        String fileContent;
        try {
            byte[] bytes = Files.readAllBytes(fPath);
            fileContent = new String(bytes);
        } catch (IOException e) {
            //quiet fail.  This is not important enough to do anything about it
            //and likely to be caught by other parts of VisualSoar.
            return null;  //failure
        }
        return fileContent;
    }

    /**
     * fixUnmatchedBraces
     * <p>
     * If you forget to put a close brace at the end of your production
     * VS will thoughtfully insert it for you.
     * <p>
     * Note:  There is a sister method {@link RuleEditor#fixUnmatchedBraces()}
     * which does the same thing for files that are currently open.  They
     * share {@link #findMissingBracePositions(String)} and
     * {@link #insertBraces} as helper methods.
     *
     * @param filename of the file to check
     */
    public static void fixUnmatchedBraces(String filename) {
        String fileContent = getFileContent(filename);
        if (fileContent == null) return; //fail silently

        //insert braces as needed
        Vector<Integer> bracePositions = findMissingBracePositions(fileContent);
        if (!bracePositions.isEmpty()) {
            fileContent = insertBraces(fileContent, bracePositions);

            //Write back the file
            try {
                PrintWriter pw = new PrintWriter(filename);
                pw.print(fileContent);
                pw.close();
            } catch (IOException e) {
                //quiet fail.  This is not important enough to do anything about it.
                //and likely to be caught by other parts of VisualSoar.
            }

        }//if file was changed

    }//fixUnmatchedBraces


    /**
     * getSimpleTestConstants
     * <p>
     * is a helper method for {@link #warnSuspiciousConstants}.
     * It extracts the constant names from a SimpleTest object and places
     * them in the given vector (if found).
     */
    private static void getSimpleTestConstants(Vector<Pair> cons, SimpleTest sTest) {
        //Disjunction test
        if (sTest.isDisjunctionTest()) {
            DisjunctionTest djTest = sTest.getDisjunctionTest();
            Iterator<Constant> constants = djTest.getConstants();
            while(constants.hasNext()) {
                cons.add(constants.next().toPair());
            }
        }

        //Relation test
        else {
            SingleTest singleTest = sTest.getRelationalTest().getSingleTest();
            if (singleTest.isConstant()) {
                cons.add(singleTest.getConstant().toPair());
            }
        }
    }//getSimpleTestConstants



    /**
     * getAttrTestConstants
     * <p>
     * is a helper method for {@link #warnSuspiciousConstants}.
     * It extracts the variable names from an AttributeTest object and
     * places them in the given vector (if found).
     */
    private static void getAttrTestConstants(Vector<Pair> cons, Test test) {
        if (test.isConjunctiveTest()) {
            ConjunctiveTest cjTest = test.getConjunctiveTest();
            Iterator<SimpleTest> stIter = cjTest.getSimpleTests();
            while (stIter.hasNext()) {
                SimpleTest sTest = stIter.next();
                getSimpleTestConstants(cons, sTest);
            }
        } else {
            SimpleTest sTest = test.getSimpleTest();
            getSimpleTestConstants(cons, sTest);
        }
    }//getAttrTestConstants

    /**
     * getValTestConstants
     * <p>
     * is a helper method for {@link #warnSuspiciousConstants}.
     * It extracts the variable names from an AttributeTest object and
     * places them in the given vector (if found).
     */
    private static void getValTestConstants(Vector<Pair> cons, Test test) {
        if (test.isConjunctiveTest()) {
            ConjunctiveTest cjTest = test.getConjunctiveTest();
            Iterator<SimpleTest> stIter = cjTest.getSimpleTests();
            while (stIter.hasNext()) {
                SimpleTest sTest = stIter.next();
                getSimpleTestConstants(cons, sTest);
            }
        } else {
            SimpleTest sTest = test.getSimpleTest();
            getSimpleTestConstants(cons, sTest);
        }
    }//getValTestConstants



    /**
     * getPCondConstants
     * <p>
     * is a helper method for {@link #warnSuspiciousConstants}.  It
     * extracts all the constants from a PositiveCondition object and
     * places them in the given vector (if found).
     * <p>
     * Warning:  recursive method
     */
    private static void getPCondConstants(Vector<Pair> conds, PositiveCondition pCond) {
        //A PCond can be a conjunction of conditions...
        if (pCond.isConjunction()) {
            //Recurse into the conjunction
            Iterator<Condition> conjIter = pCond.getConjunction();
            while (conjIter.hasNext()) {
                Condition cond = conjIter.next();
                getPCondConstants(conds, cond.getPositiveCondition());
            }
            return;
        }

        //search all attribute-value tests
        Iterator<AttributeValueTest> avIter = pCond.getConditionForOneIdentifier().getAttributeValueTests();
        while (avIter.hasNext()) {
            AttributeValueTest avt = avIter.next();

            //Attribute tests
            Iterator<AttributeTest> attrIter = avt.getAttributeTests();
            while (attrIter.hasNext()) {
                AttributeTest attr = attrIter.next();
                getAttrTestConstants(conds, attr.getTest());
            }//attr tests

            //Value Tests
            Iterator<ValueTest> valIter = avt.getValueTests();
            while (valIter.hasNext()) {
                ValueTest val = valIter.next();
                getValTestConstants(conds, val.getTest());
            }//Value

        }//attr-val tests
    }//getPCondConstants

    /**
     * getActionConstants
     * <p>
     * is a helper method for {@link #warnSuspiciousConstants}.
     * It extracts all the constants from an Action object and
     * places them in the given vector (if found).
     */
    private static void getActionConstants(Action act, Vector<Pair> constants) {
        if (!act.isVarAttrValMake()) return;

        Iterator<AttributeValueMake> avIter = act.getVarAttrValMake().getAttributeValueMakes();
        while (avIter.hasNext()) {
            AttributeValueMake avMake = avIter.next();

            //RHS value tested
            Iterator<RHSValue> rhsValIter = avMake.getRHSValues();
            while (rhsValIter.hasNext()) {
                RHSValue rhsVal = rhsValIter.next();
                if (rhsVal.isConstant()) {
                    constants.add(rhsVal.getConstant().toPair());
                }
            }

            //RHS value created
            Iterator<ValueMake> rhsMakeIter = avMake.getValueMakes();
            while (rhsMakeIter.hasNext()) {
                ValueMake val = rhsMakeIter.next();
                if (val.getRHSValue().isConstant()) {
                    constants.add(val.getRHSValue().getConstant().toPair());
                }
            }
        }//loop over Action VarAttrVals
    }//getActionConstants



    /**
     * warnSuspiciousConstants
     * <p>
     * When an unclosed angle bracket is used as a constant in Soar it's
     * legal syntax, but it's likely not intentional.  For example:
     * <code>
     *         (<op> ^direction <dir)
     *         (<op> ^direction dir>)
     * </code>
     * <p>
     * In this example, it's likely that 'dir' is a meant to be a variable,
     * not a constant.
     * <p>
     *
     * @param opNode the operator that contains these productions
     * @param prods parsed SoarProduction objects to inspect
     *
     * @return FeedbackListEntry object for the first suspicious constant found
     */
    public static FeedbackListEntry warnSuspiciousConstants(OperatorNode opNode, Vector<SoarProduction> prods) {
        Vector<Pair> constants = new Vector<>();
        for(SoarProduction prod : prods) {

            //Retrieve constants from LHS
            Iterator<Condition> lhsIter = prod.getConditionSide().getConditions();
            while(lhsIter.hasNext()) {
                //Get all the constant names
                PositiveCondition pCond = lhsIter.next().getPositiveCondition();
                getPCondConstants(constants, pCond);
            }

            //Retrieve constants from RHS
            Iterator<Action> actIter = prod.getActionSide().getActions();
            while (actIter.hasNext()) {
                Action act = actIter.next();
                getActionConstants(act, constants);
            }


            //Check for suspicious items in the list
            for(Pair pair : constants) {
                boolean angleBegin = pair.getString().startsWith("<");
                boolean angleEnd = pair.getString().endsWith(">");
                if (angleBegin != angleEnd) {
                    String msg = "Warning: constant '" + pair.getString() + "' contains an unmatched angle bracket.  Did you intend to use a variable here?";
                    return new FeedbackEntryOpNode(opNode, pair.getLine(), msg);
                }
            }

        }

        return null;  //no issues found
    }//warnSuspiciousConstants


}//class SuppParseChecks
