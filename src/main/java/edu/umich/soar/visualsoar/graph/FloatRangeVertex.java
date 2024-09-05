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
    public FloatRangeVertex(int id, double low, double high) {
        super(id);
        if (low > high) {
            throw new IllegalArgumentException("Low cannot be greater than high");
        }
        this.low = low;
        this.high = high;
        rep = ": float" + getRangeString();
    }

//////////////////////////////////////////
// Accessors
//////////////////////////////////////////
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
    public SoarVertex copy(int newId) {
        return new FloatRangeVertex(newId, low, high);
    }

    @Override
    public String typeName() { return "float"; }

    @Override
    public String toString() {
        return rep;
    }

    //////////////////////////////////////////
// Manipulators
//////////////////////////////////////////
    public boolean edit(java.awt.Frame owner) {
        EditNumberDialog theDialog = new EditNumberDialog(owner, "Float");
        theDialog.setLow((float) low);
        theDialog.setHigh((float) high);
        theDialog.setVisible(true);

        if (theDialog.wasApproved()) {
            low = theDialog.getLow().floatValue();
            high = theDialog.getHigh().floatValue();
            rep = ": float" + getRangeString();
            return true;
        }
        return false;
    }

    @Override
    public void write(java.io.Writer w) throws java.io.IOException {
        w.write("FLOAT_RANGE " + number + " " + low + " " + high + '\n');
    }

    public double getLow() {
      return low;
    }

    public double getHigh() {
      return high;
    }

   /** creates a string representing this integer's range.
    *  If there is no limit then an empty string is returned */
    public String getRangeString() {
        String result = "";
        if (low != Float.NEGATIVE_INFINITY || high != Float.POSITIVE_INFINITY) {
            result += " [ ";
            if (low == Float.NEGATIVE_INFINITY) {
                result += "... ";
            } else {
                result += low + " ";
            }
            result += "- ";
            if (high == Float.POSITIVE_INFINITY) {
                result += "... ";
            } else {
                result += high + " ";
            }
            result += " ]";
        }
        return result;
    }


}
