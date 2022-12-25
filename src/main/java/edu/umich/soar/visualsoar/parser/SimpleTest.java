package edu.umich.soar.visualsoar.parser;

public final class SimpleTest {
    // Data Members
    private DisjunctionTest d_disjunctionTest;
    private RelationalTest d_relationalTest;
    private final boolean d_isDisjunctionTest;

    // Constructor
    public SimpleTest(DisjunctionTest disjunctionTest) {
        d_disjunctionTest = disjunctionTest;
        d_isDisjunctionTest = true;
    }

    public SimpleTest(RelationalTest relationalTest) {
        d_relationalTest = relationalTest;
        d_isDisjunctionTest = false;
    }

    // Accessors
    public boolean isDisjunctionTest() {
        return d_isDisjunctionTest;
    }

    public DisjunctionTest getDisjunctionTest() {
        if (!d_isDisjunctionTest) {
            throw new IllegalArgumentException("Not Disjunction");
        } else {
            return d_disjunctionTest;
        }
    }

    public RelationalTest getRelationalTest() {
        if (d_isDisjunctionTest) {
            throw new IllegalArgumentException("Not Relation");
        } else {
            return d_relationalTest;
        }
    }
}
