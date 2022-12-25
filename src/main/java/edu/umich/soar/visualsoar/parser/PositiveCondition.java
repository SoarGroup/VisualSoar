package edu.umich.soar.visualsoar.parser;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class PositiveCondition {
    // Data Members
    private ConditionForOneIdentifier d_condition;
    private List<Condition> d_conjunction;
    private final boolean d_isConjunction;

    // Constructors
    public PositiveCondition() {
        d_isConjunction = true;
        d_conjunction = new LinkedList<>();
    }

    public PositiveCondition(ConditionForOneIdentifier cfoi) {
        d_condition = cfoi;
        d_isConjunction = false;
    }

    // Accessors
    public boolean isConjunction() {
        return d_isConjunction;
    }

    public void add(Condition c) {
        if (!d_isConjunction) {
            throw new IllegalArgumentException("Not Conjunction");
        } else {
            d_conjunction.add(c);
        }
    }

    public Iterator<Condition> getConjunction() {
        if (!d_isConjunction) {
            throw new IllegalArgumentException("Not Conjunction");
        } else {
            return d_conjunction.iterator();
        }
    }

    public ConditionForOneIdentifier getConditionForOneIdentifier() {
        if (d_isConjunction) {
            throw new IllegalArgumentException("Not Condition For One Identifier");
        } else {
            return d_condition;
        }
    }
}
