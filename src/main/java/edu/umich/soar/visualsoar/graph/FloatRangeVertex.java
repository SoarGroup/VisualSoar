package edu.umich.soar.visualsoar.graph;

import edu.umich.soar.visualsoar.dialogs.EditNumberDialog;

public class FloatRangeVertex extends SoarVertex {
    private static final long serialVersionUID = 20221225L;

//////////////////////////////////////////
// Data Members
//////////////////////////////////////////
    private double low, high;
    String rep;

    //////////////////////////////////////////
// Constructors
//////////////////////////////////////////
    public FloatRangeVertex(int id, double _low, double _high) {
        super(id);
        if (low > high) {
            throw new IllegalArgumentException("Low cannot be greater than high");
        }
        low = _low;
        high = _high;
        calculateRep();
    }

//////////////////////////////////////////
// Accessors
//////////////////////////////////////////	
    @Override
    public SoarVertex copy(int newId) {
        return new FloatRangeVertex(newId, low, high);
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
            float f = Float.parseFloat(s);
            return f >= low && f <= high;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    @Override
    public String toString() {
        return rep;
    }

    //////////////////////////////////////////
// Manipulators
//////////////////////////////////////////	
    public boolean edit(java.awt.Frame owner) {
        EditNumberDialog theDialog = new EditNumberDialog(owner, "Float");
        theDialog.setLow(new Float(low));
        theDialog.setHigh(new Float(high));
        theDialog.setVisible(true);

        if (theDialog.wasApproved()) {
            low = theDialog.getLow().floatValue();
            high = theDialog.getHigh().floatValue();
            calculateRep();
            return true;
        }
        return false;
    }

    @Override
    public void write(java.io.Writer w) throws java.io.IOException {
        w.write("FLOAT_RANGE " + number + " " + low + " " + high + '\n');
    }

    private void calculateRep() {
        rep = ": float";
        if (low != Float.NEGATIVE_INFINITY || high != Float.POSITIVE_INFINITY) {
            rep += " [ ";
            if (low == Float.NEGATIVE_INFINITY) {
                rep += "... ";
            } else {
                rep += low + " ";
            }
            rep += "- ";
            if (high == Float.POSITIVE_INFINITY) {
                rep += "... ";
            } else {
                rep += high + " ";
            }
            rep += " ]";
        }
    }


}
