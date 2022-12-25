package edu.umich.soar.visualsoar.parser;

public class Constant {
    // Data Members
    private final int d_beginLine;
    private final int d_constType;
    private int d_intConst;
    private float d_floatConst;
    private String d_symConst;

    // Enumeration
    public static final int INTEGER_CONST = 0;
    public static final int SYMBOLIC_CONST = 1;
    public static final int FLOATING_CONST = 2;

    // Constructors
    public Constant(String symConst, int beginLine) {
        d_beginLine = beginLine;
        d_symConst = symConst;
        d_constType = SYMBOLIC_CONST;
    }

    public Constant(int intConst, int beginLine) {
        d_beginLine = beginLine;
        d_intConst = intConst;
        d_constType = INTEGER_CONST;
    }

    public Constant(float floatConst, int beginLine) {
        d_beginLine = beginLine;
        d_floatConst = floatConst;
        d_constType = FLOATING_CONST;
    }


    public Pair toPair() {
        return new Pair(toString(), d_beginLine);
    }

    public String toString() {
        switch (d_constType) {
            case INTEGER_CONST:
                return "" + d_intConst;
            case FLOATING_CONST:
                return "" + d_floatConst;
            case SYMBOLIC_CONST:
                return d_symConst;
            default:
                return "Unknown Type";
        }
    }
}
