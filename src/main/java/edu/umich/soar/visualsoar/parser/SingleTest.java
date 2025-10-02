package edu.umich.soar.visualsoar.parser;

public class SingleTest {
    // Constants for dollar test types
    public static final int NO_DOLLAR = 0;
    public static final int SINGLE_DOLLAR = 1;
    public static final int DOUBLE_DOLLAR = 2;
    
    // Data Members
    private Constant d_constant;
    private Pair d_variable;
    private final boolean d_isConstant;
    private final int d_dollarType;

    // Constructors
    public SingleTest(Constant constant) {
        d_constant = constant;
        d_isConstant = true;
        d_dollarType = NO_DOLLAR; // Constants cannot have dollar modifiers
    }

    public SingleTest(Pair variable) {
        d_variable = variable;
        d_isConstant = false;
        d_dollarType = NO_DOLLAR;
    }

    public SingleTest(Pair variable, int dollarType) {
        d_variable = variable;
        d_isConstant = false;
        d_dollarType = dollarType;
    }

    // Accessors
    public final boolean isConstant() {
        return d_isConstant;
    }

    public final Constant getConstant() {
        if (!d_isConstant) {
            throw new IllegalArgumentException("Not Constant");
        } else {
            return d_constant;
        }
    }

    public final Pair getVariable() {
        if (d_isConstant) {
            throw new IllegalArgumentException("Not Variable");
        } else {
            return d_variable;
        }
    }

    // New dollar modifier accessors
    public final int getDollarType() {
        return d_dollarType;
    }
    
    public final boolean hasDollarModifier() {
        return d_dollarType != NO_DOLLAR;
    }
    
    public final boolean isSingleDollar() {
        return d_dollarType == SINGLE_DOLLAR;
    }
    
    public final boolean isDoubleDollar() {
        return d_dollarType == DOUBLE_DOLLAR;
    }
    
    public final String getDollarTypeString() {
        switch(d_dollarType) {
            case SINGLE_DOLLAR: return "$";
            case DOUBLE_DOLLAR: return "$$";
            default: return "";
        }
    }
}
