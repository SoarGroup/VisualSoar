package edu.umich.soar.visualsoar.graph;

import edu.umich.soar.visualsoar.dialogs.EditEnumerationDialog;

import java.util.Iterator;
import java.util.Vector;

/**
 * This class encapsulates the notion of an enumeration
 * in working memory
 *
 * @author Brad Jones
 * @version 0.9a 6/5/00
 */

public class EnumerationVertex extends SoarVertex {
    ///////////////////////////////////////////////
// Data Members
///////////////////////////////////////////////
    private Vector<String> theStrings;

    ///////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////
    public EnumerationVertex(int id, Vector<String> strings) {
        super(id);
        theStrings = strings;
    }

    public EnumerationVertex(int id, String singleton) {
        super(id);
        theStrings = new Vector<>();
        theStrings.add(singleton);
    }

    ///////////////////////////////////////////////
// Accessors
///////////////////////////////////////////////
    public SoarVertex copy(int newId) {
        return new EnumerationVertex(newId, theStrings);
    }

    public void add(String s) {
        theStrings.add(s);
    }

    public void remove(String s) {
        Iterator<String> iter = theStrings.iterator();
        while (iter.hasNext()) {
            String t = iter.next();
            if (t.equals(s)) {
                iter.remove();
            }
        }
    }

    public boolean contains(String s) {
        return theStrings.contains(s);
    }

    public boolean allowsEmanatingEdges() {
        return false;
    }

    public Iterator<String> getEnumeration() {
        return theStrings.iterator();
    }

    public boolean isEditable() {
        return true;
    }

    public boolean isValid(String s) {
        for (String cs : theStrings) {
			if (cs.compareTo(s) == 0) {
				return true;
			}
        }
        return false;
    }

    public String toString() {
        StringBuilder s = new StringBuilder(": [ ");
        for (String theString : theStrings) {
            s.append(theString).append(" ");
        }
        s.append("]");
        return s.toString();
    }

    ///////////////////////////////////////////////
// Modifiers
///////////////////////////////////////////////
    public boolean edit(java.awt.Frame owner) {
        EditEnumerationDialog theDialog = new EditEnumerationDialog(owner, theStrings);
        theDialog.setVisible(true);
        if (theDialog.wasApproved()) {
            theStrings = theDialog.getTreeSet();
            return true;
        }
        return false;
    }

    public void write(java.io.Writer w) throws java.io.IOException {
        w.write("ENUMERATION " + number + " " + theStrings.size());
        for (String theString : theStrings) {
            w.write(" " + theString);
        }
        w.write('\n');
    }
}
