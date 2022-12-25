package edu.umich.soar.visualsoar.parser;


public class DefaultTripleFactory implements TripleFactory {
    public Triple createTriple(Pair variable, Pair attribute, Pair value, boolean hasState, boolean isChecking, boolean isCondition) {
        return new Triple(variable, attribute, value, hasState, isCondition);
    }
}
