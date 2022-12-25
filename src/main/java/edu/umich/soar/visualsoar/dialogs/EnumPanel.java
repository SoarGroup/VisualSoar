package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.event.KeyEvent;
import java.util.Vector;

/**
 * Panel that facilitates the entry of a list of strings,
 * or enumeration, for the enumeration dialogs.
 *
 * @author Jon Bauman
 * @see EnumerationDialog
 * @see EditEnumerationDialog
 */
class EnumPanel extends JPanel {
    private static final long serialVersionUID = 20221225L;


    JTextField newString = new JTextField(20);
    Vector<String> theStrings = new Vector<>();
    JList<String> theList = new JList<>(theStrings);
    JScrollPane sp = new JScrollPane(theList);

    public EnumPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(newString);
        add(Box.createVerticalStrut(15));
        add(sp);

        setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder("Enumeration"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        // So that enter can affirmatively dismiss the dialog
        newString.getKeymap().removeKeyStrokeBinding(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
    }

    /**
     * removes all the strings
     */
    public void clear() {
        theStrings.removeAllElements();
        theList.setListData(theStrings);
        clearText();
    }

    /**
     * clears the string entry field
     */
    public void clearText() {
        newString.setText("");
    }

    /**
     * @return vector of strings entered
     */
    public Vector<String> getVector() {
        return new Vector<>(theStrings);
    }

    /**
     * @param v vector to set the list data to
     */
    public void setVector(Vector<String> v) {
        theStrings = v;
        theList.setListData(theStrings);
        clearText();
    }

    boolean addString() {
        String s = newString.getText().trim();
        if (s.length() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Zero length strings are not allowed in enumerations",
                    "Invalid Enumeration Data", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (s.indexOf(' ') != -1) {
            // voigtjr: bug 986: Spaces are now allowed in enumeration values provided the string starts and ends with a pipe.
            if (!(s.startsWith("|") && s.endsWith("|"))) {
                JOptionPane.showMessageDialog(this,
                        "Spaces are not allowed in enumeration values",
                        "Invalid Enumeration Data", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        theStrings.add(s);
        theList.setListData(theStrings);
        return true;
    }

    void removeString() {
        Object val = theList.getSelectedValue();
        theStrings.remove(val);
        theList.setListData(theStrings);
    }

    public void requestFocus() {
        newString.selectAll();
        newString.requestFocus();
    }

}
