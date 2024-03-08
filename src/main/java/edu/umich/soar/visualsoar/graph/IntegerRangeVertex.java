package edu.umich.soar.visualsoar.graph;

import edu.umich.soar.visualsoar.dialogs.EditNumberDialog;

import java.awt.*;

public class IntegerRangeVertex extends SoarVertex {
    private static final long serialVersionUID = 20221225L;

    private int low, high;
    private String rep;

    public IntegerRangeVertex(int id, int _low, int _high) {
        super(id);
        if (high < low) {
            throw new IllegalArgumentException("the low cannot be greater than the high");
        }
        low = _low;
        high = _high;
        rep = ": integer" + getRangeString();
    }

    @Override
    public boolean allowsEmanatingEdges() {
        return false;
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public boolean isValid(String s) {
        try {
            int i = Integer.parseInt(s);
            return i >= low && i <= high;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    @Override
    public SoarVertex copy(int newId) {
        return new IntegerRangeVertex(newId, low, high);
    }

    @Override
    public String typeName() { return "integer"; }

    @Override
    public String toString() {
        return rep;
    }

    public void write(java.io.Writer w) throws java.io.IOException {
        w.write("INTEGER_RANGE " + number + ' ' + low + ' ' + high + '\n');
    }

    public boolean edit(Frame owner) {
        EditNumberDialog theDialog = new EditNumberDialog(owner, "Integer");
        theDialog.setLow(low);
        theDialog.setHigh(high);
        theDialog.setVisible(true);

        if (theDialog.wasApproved()) {
            low = theDialog.getLow().intValue();
            high = theDialog.getHigh().intValue();
            rep = ": integer" + getRangeString();
            return true;
        }
        return false;
    }

    /** creates a string representing this integer's range.
     * If there is no limit then an empty string is returned */
    public String getRangeString() {
        String result = "";
        if (low != Integer.MIN_VALUE || high != Integer.MAX_VALUE) {
            result += " [ ";
            if (low == Integer.MIN_VALUE) {
                result += "... ";
            } else {
                result += low + " ";
            }
            result += "- ";
            if (high == Integer.MAX_VALUE) {
                result += "... ";
            } else {
                result += high + " ";
            }
            result += " ]";
        }
        return result;
    }//getRangeString

}
