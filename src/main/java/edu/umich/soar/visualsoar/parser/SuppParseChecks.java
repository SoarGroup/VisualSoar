package edu.umich.soar.visualsoar.parser;

import edu.umich.soar.visualsoar.misc.FeedbackListObject;
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
    private static void getSimpleTestVarName(Vector<String> vars, SimpleTest stest) {
        //disjunctions can't have a variable
        if (stest.isDisjunctionTest()) return;

        SingleTest singleTest = stest.getRelationalTest().getSingleTest();
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
                SimpleTest stest = stIter.next();
                getSimpleTestVarName(vars, stest);
            }
        } else {
            SimpleTest stest = test.getSimpleTest();
            getSimpleTestVarName(vars, stest);
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
    private static void getPCondVarNames(Vector<String> vars, PositiveCondition pcond) {
        //A PCond can be a conjunction of conditions...
        if (pcond.isConjunction()) {
            //Recurse into the conjunction
            Iterator<Condition> conjIter = pcond.getConjunction();
            while (conjIter.hasNext()) {
                Condition cond = conjIter.next();
                getPCondVarNames(vars, cond.getPositiveCondition());
            }
            return;
        }

        //Identifier
        vars.add(pcond.getConditionForOneIdentifier().getVariable().getString());

        //attribute-value tests
        Iterator<AttributeValueTest> avIter = pcond.getConditionForOneIdentifier().getAttributeValueTests();
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
                SimpleTest stest = stIter.next();
                getSimpleTestVarName(vars, stest);
            }
        } else {
            SimpleTest stest = test.getSimpleTest();
            getSimpleTestVarName(vars, stest);
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
            PositiveCondition pcond = condIter.next().getPositiveCondition();
            getPCondVarNames(lhsVars, pcond);
        }//while

        return lhsVars;
    }//getLHSVarNames

    /**
     * checkUndefinedVarRHS
     * <p>
     * This method reviews a parsed productions for the case where
     * a variable is being used as an idenifier on the RHS but that
     * variable has neither been matched on LHS nor created on the RHS.
     * <p>
     * The Soar Parser apparently doesn't check for this so I'm doing
     * it manually here.
     *
     * @param opNode the file containing this production
     * @param prod   a SoarProduction object to inspect
     * @return null if no error found or a FeedbackListObject otherwise
     */
    public static FeedbackListObject checkUndefinedVarRHS(OperatorNode opNode, SoarProduction prod) throws ParseException {
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
            return new FeedbackListObject(opNode, p.getLine(), errString);
        }

        return null;

    }//checkUndefinedVarRHS

    /**
     * findMissingBracePositions
     * <p>
     * given the text of some Soar code, this method indentifies
     * if there is an unmatched brace at the end of any of its productions.
     * This is a syntax error that can be fixed automatically for the user.
     *
     * @param text the code to analyze
     * @return the locations where braces should be placed
     */
    public static Vector<Integer> findMissingBracePositions(String text) {
        //find the start position of each production
        Pattern prodPattern = Pattern.compile("[ \n\t\r][sg]p[ \t\n\r]*\\{");
        Matcher prodMatch = prodPattern.matcher(text);
        Vector<Integer> prodStarts = new Vector<>();
        while (prodMatch.find()) {
            prodStarts.add(prodMatch.start());
        }
        prodStarts.add(text.length() - 1); //add end of file

        //This vertor stores all the places to insert a courtesy close brace
        // (typically there will be none)
        Vector<Integer> bracePositions = new Vector<>();
        for (int start = 0; start < prodStarts.size() - 1; ++start) {
            int end = prodStarts.get(start + 1);

            //Count the open and close braces to detect a mismatch
            int depth = 0;
            for (int i = start; i <= end; ++i) {
                switch (text.charAt(i)) {
                    case '{':
                        depth++;
                        break;
                    case '}':
                        depth--;
                        break;
                    case '#':  //ignore comments
                        while (text.charAt(i) != '\n') {
                            i++;
                        }
                        break;
                }
            }//counting braces

            //if there was a one-level mismatch see if the last brace appears
            // to be missing
            if (depth == 1) {
                int lastCloseParen = text.lastIndexOf(')', end);
                int lastCloseBrace = text.lastIndexOf('}', end);

                //if there was no close paren at all then something's up,
                //time to bail out
                if (lastCloseParen != -1) {

                    //Either the last brace is completely absent (-1) or
                    //it precedes the last paren.  Either way, we've found
                    //a place to insert a brace
                    if (lastCloseParen > lastCloseBrace) {
                        //calculate where to put a courtesy brace
                        int index = end - 1;
                        while (Character.isWhitespace(text.charAt(index))) {
                            index--;
                        }
                        index++;
                        bracePositions.add(index + 1);
                    }
                }
            }//mismatch found
        }//for each production

        return bracePositions;
    }//findMissingBracePositions

    /**
     * inserts closing braces at given positions.  This is meant to be used
     * with the return value of {@link #findMissingBracePositions}
     *
     * @param text           the to to insert them
     * @param bracePositions where to insert them
     * @return the revised text
     */
    public static String insertBraces(String text, Vector<Integer> bracePositions) {
        if (bracePositions.size() > 0) {
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
        //Read in the file content
        Path fPath = Paths.get(filename);
        String fileContent;
        try {
            byte[] bytes = Files.readAllBytes(fPath);
            fileContent = new String(bytes);
        } catch (IOException e) {
            //quiet fail.  This inot important enough to do anything about it
            //and likely to be caught by other parts of VisualSoar.
            return;
        }

        //insert braces as needed
        Vector<Integer> bracePositions = findMissingBracePositions(fileContent);
        if (bracePositions.size() > 0) {
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


}//class SuppParseChecks
