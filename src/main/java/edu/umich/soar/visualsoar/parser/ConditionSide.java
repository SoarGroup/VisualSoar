package edu.umich.soar.visualsoar.parser;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class ConditionSide {
    private final List<Condition> d_conditions = new LinkedList<>();

    // Constructor
    public ConditionSide() {
    }

    // Accessors
    public void add(Condition condition) {
        d_conditions.add(condition);
    }

    public Iterator<Condition> getConditions() {
        return d_conditions.iterator();
    }
}
