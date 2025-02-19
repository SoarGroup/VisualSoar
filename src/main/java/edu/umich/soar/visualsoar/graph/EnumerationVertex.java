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
    private static final long serialVersionUID = 20221225L;

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

  public EnumerationVertex(int id, String serializationId, Vector<String> strings) {
    super(id, serializationId);
    theStrings = strings;
  }

    public EnumerationVertex(int id, String singleton) {
        super(id);
        theStrings = new Vector<>();
        theStrings.add(singleton);
    }

    ///////////////////////////////////////////////
    // Abstract Method Implementations
    ///////////////////////////////////////////////
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
        for (String cs : theStrings) {
            if (cs.compareTo(s) == 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public SoarVertex copy(int newId) {
        return new EnumerationVertex(newId, theStrings);
    }

    @Override
    public String typeName() { return "enumeration"; }

    @Override
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
    public void add(String s) {
        theStrings.add(s);
    }

    public void remove(String s) {
      theStrings.removeIf(t -> t.equals(s));
    }

    public boolean contains(String s) {
        return theStrings.contains(s);
    }

    public Iterator<String> getEnumeration() {
        return theStrings.iterator();
    }

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
