package edu.umich.soar.visualsoar.parser;

public final class Pair implements Comparable<Pair> {
    /////////////////////////////////////
// Data Members
/////////////////////////////////////
    private final String d_string;
    private final int d_line;

    public Pair(String string, int line) {
        d_string = string;
        d_line = line;
    }

    /////////////////////////////////////
// Accessors
/////////////////////////////////////
    public String getString() {
        return d_string;
    }

    public int getLine() {
        return d_line;
    }

    public boolean equals(Object o) {
        if (o instanceof Pair) {
            Pair p = (Pair) o;
            return d_string.equals(p.getString());
        }
        return false;
    }

    public int compareTo(Pair p) {
        return d_string.compareTo(p.getString());
    }

}
