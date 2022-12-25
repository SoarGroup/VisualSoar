package edu.umich.soar.visualsoar.parser;

/**
 * class Triple
 * <p>
 * represents one entry in the datamap
 */
public class Triple {
    // The var, attr, and val are represented as an {@link edu.umich.soar.visualsoar.parser.Pair}
    //  *NOT* the javafx.util.Pair<K,V>
    Pair d_variable;
    Pair d_attribute;
    Pair d_value;
    boolean d_hasState = false;
    boolean d_condition = true;   // is this a condition (LHS) or action (RHS)

    // Constructors
    public Triple(Pair variable, Pair attribute, Pair value) {
        d_variable = variable;
        d_attribute = attribute;
        d_value = value;
    }

    public Triple(Pair variable, Pair attribute, Pair value, boolean hasState) {
        this(variable, attribute, value);
        d_hasState = hasState;
    }

    public Triple(Pair variable, Pair attribute, Pair value, boolean hasState, boolean in_condition) {
        this(variable, attribute, value, hasState);
        d_condition = in_condition;
    }

    // Accessors
    public boolean hasState() {
        return d_hasState;
    }

    public Pair getVariable() {
        return d_variable;
    }

    public Pair getAttribute() {
        return d_attribute;
    }

    public Pair getValue() {
        return d_value;
    }

    public int getLine() {
        if (d_value.getLine() != -1) {
            return d_value.getLine();
        } else if (d_attribute.getLine() != -1) {
            return d_attribute.getLine();
        } else {
            return d_variable.getLine();
        }
    }

    public boolean isCondition() {
        return d_condition;
    }

    public boolean isAction() {
        return !d_condition;
    }

    public String toString() {
        return "(" + d_variable.getString() + "," + d_attribute.getString() + "," + d_value.getString() + ")";
    }
}
