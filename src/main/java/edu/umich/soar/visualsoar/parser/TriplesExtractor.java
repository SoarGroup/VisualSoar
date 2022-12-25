package edu.umich.soar.visualsoar.parser;

import java.util.*;

/**
 * This class extracts triples from a given Soar production
 * it ignores the condition/action side information it also ignores
 * any relation besides equals, it is not sensitive to negations
 *
 * @author Brad Jones
 */

public class TriplesExtractor {
    //////////////////////////////////////////////////////////////////////////////
// Data Members
/////////////////////////////////////////////////////////////////////////////
    private int d_currentUnnamedVar = 0;
    private final Vector<Triple> d_triples = new Vector<>();
    private final SoarProduction d_soarProduction;
    private final Set<Pair> d_variables = new TreeSet<>();
    private final Set<Pair> d_stateVariables = new TreeSet<>();
    private TripleFactory d_tripleFactory;

    /////////////////////////////////////////////////////////////////////////////
// Constructors
/////////////////////////////////////////////////////////////////////////////
    public TriplesExtractor(SoarProduction soarProduction) {
        d_soarProduction = soarProduction;
        d_tripleFactory = new DefaultTripleFactory();
        extractTriples();
        extractVariables();
        extractStateVariables();
    }

    //Create an object that only contains 1 triple (used to fix one datamap error at a time)
    public TriplesExtractor(SoarProduction soarProduction, Triple one) {
        d_soarProduction = soarProduction;
        d_triples.add(one);
        extractVariables();
        extractStateVariables();
    }

    /////////////////////////////////////////////////////////////////////////////
// Accessors
/////////////////////////////////////////////////////////////////////////////
    public Iterator<Triple> triples() {
        return d_triples.iterator();
    }

    public Iterator<Pair> variables() {
        return d_variables.iterator();
    }

    public int getStateVariableCount() {
        return d_stateVariables.size();
    }

    public Pair stateVariable() {
        Iterator<Pair> i = d_stateVariables.iterator();
        if (i.hasNext()) {
            return i.next();
        } else {
            return null;
        }
    }

    // Implementation Functions
    private void extractTriples() {
        // Extract Triples from the condition side
        Iterator<Condition> condIter = d_soarProduction.getConditionSide().getConditions();
        while (condIter.hasNext()) {
            d_triples.addAll(extractTriples(condIter.next().getPositiveCondition()));
        }

        // Extract Triples from the action side
        Iterator<Action> actIter = d_soarProduction.getActionSide().getActions();
        while (actIter.hasNext()) {
            Action a = actIter.next();
            if (a.isVarAttrValMake()) {
                d_triples.addAll(extractTriples(a.getVarAttrValMake()));
            }
        }
    }//extractTriples

    private List<Triple> extractTriples(PositiveCondition pc) {
        // If this positive condition is a conjunctions then extract
        // all the positive conditions out of it and recursively
        // interpret those
        if (pc.isConjunction()) {
            List<Triple> triples = new LinkedList<>();
            Iterator<Condition> i = pc.getConjunction();
            while (i.hasNext()) {
                triples.addAll(extractTriples((i.next()).getPositiveCondition()));
            }
            return triples;
        } else {
            // Just extract the condition for one identifier
            return extractTriples(pc.getConditionForOneIdentifier());
        }
    }//extractTriples

    /**
     * This function is long and complicated so, I'll explain it the best
     * that I can
     */
    private List<Triple> extractTriples(ConditionForOneIdentifier cfoi) {
        List<Triple> triples = new LinkedList<>();
        // Get all the attribute Value tests
        Iterator<AttributeValueTest> attrValTestIter = cfoi.getAttributeValueTests();
        boolean hasState = cfoi.hasState();

        // For all the attribute value tests
        while (attrValTestIter.hasNext()) {
            Pair variable = cfoi.getVariable();
            List<Pair> attributes = null;
            AttributeValueTest avt = attrValTestIter.next();

            // Get the attribute chain
            Iterator<AttributeTest> attrTestIter = avt.getAttributeTests();
            while (attrTestIter.hasNext()) {
                AttributeTest at = attrTestIter.next();

                // First time switch
                if (attributes == null) {
                    attributes = extract(at.getTest());
                } else {

                    // Ok, they are doing the '.' thing so create a variable
                    // value and march on down the line
                    List<Pair> newAttributes = extract(at.getTest());
                    Pair newVariable = getNextUnnamedVar();
                    for (Pair attr : attributes) {
                        triples.add(d_tripleFactory.createTriple(variable, attr, newVariable, hasState, true, true));
                    }
                    attributes = newAttributes;
                    variable = newVariable;
                    hasState = false;
                }
            }

            // In case they didn't have any attributes, put a variable one
            // in its place, (my understanding is that this is exactly what
            // soar does)
            if (attributes == null) {
                attributes = new LinkedList<>();
                attributes.add(getNextUnnamedVar());
            }

            // Ok get all the values that we are checking
            List<Pair> values = null;
            Iterator<ValueTest> valTestIter = avt.getValueTests();
            while (valTestIter.hasNext()) {
                ValueTest vt = valTestIter.next();
                if (values == null) {
                    values = extract(vt.getTest());
                } else {
                    values.addAll(extract(vt.getTest()));
                }
            }

            // In case they didn't check for any values, put a variable in
            // there, my understanding is that soar does the exact same thing
            if (values == null) {
                values = new LinkedList<>();
                values.add(getNextUnnamedVar());
            }

            // Put the attributes and variables together with the
            // variables into triples
            for (Pair attr : attributes) {
                for (Pair val : values) {
                    triples.add(d_tripleFactory.createTriple(variable, attr, val, hasState, true, true));
                }
            }
        }
        return triples;
    }

    private List<Pair> extract(Test t) {
        if (t.isConjunctiveTest()) {
            List<Pair> strings = new LinkedList<>();
            Iterator<SimpleTest> i = t.getConjunctiveTest().getSimpleTests();
            while (i.hasNext()) {
                strings.addAll(extract(i.next()));
            }
            return strings;
        } else {
            return extract(t.getSimpleTest());
        }
    }

    private List<Pair> extract(SimpleTest simpleTest) {
        if (simpleTest.isDisjunctionTest()) {
            List<Pair> strings = new LinkedList<>();
            Iterator<Constant> i = simpleTest.getDisjunctionTest().getConstants();
            while (i.hasNext()) {
                Constant c = i.next();
                strings.add(c.toPair());
            }
            return strings;
        } else {
            SingleTest st = simpleTest.getRelationalTest().getSingleTest();
            List<Pair> strings = new LinkedList<>();
            if (st.isConstant()) {
                strings.add(st.getConstant().toPair());
            } else {
                strings.add(st.getVariable());
            }
            return strings;
        }
    }

    private List<Triple> extractTriples(VarAttrValMake vavm) {
        List<Triple> triples = new LinkedList<>();
        Iterator<AttributeValueMake> i = vavm.getAttributeValueMakes();
        while (i.hasNext()) {
            Pair variable = vavm.getVariable();
            Pair attributeMakes = null;
            AttributeValueMake avm = i.next();
            Iterator<RHSValue> rhsValueIterator = avm.getRHSValues();
            while (rhsValueIterator.hasNext()) {
                if (attributeMakes == null) {
                    attributeMakes = extract(rhsValueIterator.next());
                } else {
                    Pair newAttributeMakes = extract(rhsValueIterator.next());
                    Pair newVariable = getNextUnnamedVar();
                    triples.add(d_tripleFactory.createTriple(variable, attributeMakes, newVariable, false, false, false));
                    attributeMakes = newAttributeMakes;
                    variable = newVariable;
                }
            }
            Iterator<ValueMake> valueMakeIterator = avm.getValueMakes();
            while (rhsValueIterator.hasNext()) {
                ValueMake vm = valueMakeIterator.next();
                Pair value = extract(vm.getRHSValue());
                triples.add(d_tripleFactory.createTriple(variable, attributeMakes, value, false, false, false));
            }
        }
        return triples;
    }

    private Pair extract(RHSValue rhsValue) {
        if (rhsValue.isFunctionCall()) {
            return getNextUnnamedVar();
        }
        if (rhsValue.isVariable()) {
            return rhsValue.getVariable();
        }
        return rhsValue.getConstant().toPair();
    }

    private Pair getNextUnnamedVar() {
        return new Pair("< " + d_currentUnnamedVar++ + ">", -1);
    }

    private void extractVariables() {
        for (Triple t : d_triples) {
            d_variables.add(t.getVariable());
            if (TripleUtils.isVariable(t.getAttribute().getString())) {
                d_variables.add(t.getAttribute());
            }
            if (TripleUtils.isVariable(t.getValue().getString())) {
                d_variables.add(t.getValue());
            }
        }
    }

    private void extractStateVariables() {
        for (Triple t : d_triples) {
            if (t.hasState()) {
                d_stateVariables.add(t.getVariable());
            }
        }
    }
}
