package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

/**
 * class NumberTextField
 * <p>
 * is a JTextField that only allows whole numbers
 * <p>
 * Initial code was borrowed from the UpperCaseDocument example on this page:
 * https://docs.oracle.com/javase/7/docs/api/javax/swing/JTextField.html
 */
public class NumberTextField extends JTextField {
    private static final long serialVersionUID = 20221225L;


    public NumberTextField() {
        super("", 3);  //set's field width
    }

    protected Document createDefaultModel() {
        return new NumberDocument();
    }

    static class NumberDocument extends PlainDocument {
        private static final long serialVersionUID = 20221225L;


        public void insertString(int offset, String str, AttributeSet a)
                throws BadLocationException {

            if (str == null) return;  //can this even happen?

            //Construct the new String value the user intends
            StringBuilder sb = new StringBuilder();
            sb.append(this.getText(0, this.getLength()));
            sb.insert(offset, str);

            //Is this a number?
            int numVal;
            try {
                numVal = Integer.parseInt(sb.toString());
            } catch (Exception e) {
                return;  //ignore the user's input
            }

            super.insertString(offset, str, a);
        }
    }
}//class NumberTextField
