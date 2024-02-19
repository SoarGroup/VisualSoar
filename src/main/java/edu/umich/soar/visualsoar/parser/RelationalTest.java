package edu.umich.soar.visualsoar.parser;

public class RelationalTest {
    // Enumeration
    public static final int NEQ = 0;
    public static final int EQUIV = 1;
    public static final int LT = 2;
    public static final int LTE = 3;
    public static final int GTE = 4;
    public static final int GT = 5;
    public static final int EQ = 6;

    //used for long-term identifier predicates
    public static final int AT = 7;
    public static final int ATPLUS = 8;
    public static final int ATMINUS = 9;
    public static final int BANGAT = 10;

    // Data Members
    private final int d_relation;
    private final SingleTest d_singleTest;

    // Constructors
    public RelationalTest(int relation, SingleTest singleTest) {
        d_relation = relation;
        d_singleTest = singleTest;
    }

    // Accessors
    public int getRelation() {
        return d_relation;
    }

    public SingleTest getSingleTest() {
        return d_singleTest;
    }

}
