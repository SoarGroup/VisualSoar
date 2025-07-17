package edu.umich.soar.visualsoar.dialogs;

import edu.umich.soar.visualsoar.datamap.DataMapUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

/**
 * Dialog which takes input for the creation of an EnumerationVertex
 * in the data map.
 *
 * @author Jon Bauman
 * @see edu.umich.soar.visualsoar.graph.EnumerationVertex
 * @see edu.umich.soar.visualsoar.datamap.DataMapTree
 */
public class EnumerationDialog extends JDialog {
    private static final long serialVersionUID = 20221225L;

    public static int NAME = 0;
    public static int ENUMERATION = 1;

    boolean approved = false;
    Vector<String> theStrings = null;
    String nameText = null;
    Action enterAction = new EnterAction();

    /**
     * which entry field will recieve focus. Valid values are
     * EnumerationDialog.NAME and EnumerationDialog.ENUMERATION
     */
    int focusTarget = NAME;

    /**
     * panel which contains the name imput field
     */
    NamePanel namePanel = new NamePanel("Attribute Name");

    /**
     * panel which facilitates entry of enumerations
     */
    EnumPanel enumPanel = new EnumPanel();

    EnumButtonPanel buttonPanel = new EnumButtonPanel();

    /**
     * @param owner Frame which owns the dialog
     */
    public EnumerationDialog(final Frame owner) {
        super(owner, "Enter Enumeration", true);

        setResizable(false);
        Container contentPane = getContentPane();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        contentPane.setLayout(gridbag);

        // specifies component as last one on the row
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;

        contentPane.add(namePanel, c);
        contentPane.add(enumPanel, c);
        contentPane.add(buttonPanel, c);
        pack();

        DialogUtils.setUpDialogFocus(this, owner, namePanel.nameField);

        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

        namePanel.nameField.getKeymap().addActionForKeyStroke(enter, enterAction);


        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent we) {
                setLocationRelativeTo(owner);
                owner.repaint();
            }
        });

        buttonPanel.cancelButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        approved = false;
                        dispose();
                    }
                });

        buttonPanel.addButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (enumPanel.addString()) {
                            enumPanel.clearText();
                        }
                        focusTarget = ENUMERATION;
                    }
                });

        buttonPanel.removeButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        enumPanel.removeString();
                    }
                });

        buttonPanel.okButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        nameText = namePanel.getText().trim();
                        theStrings = enumPanel.getVector();

                        if (nameText.length() == 0) {
                            JOptionPane.showMessageDialog(EnumerationDialog.this,
                                    "Attribute names cannot have length zero",
                                    "Invalid Name", JOptionPane.ERROR_MESSAGE);
                            focusTarget = NAME;
                        } else if (!DataMapUtils.attributeNameIsValid(nameText)) {
                            JOptionPane.showMessageDialog(EnumerationDialog.this,
                                    "Attribute names may only contain letters, numbers, hyphens and underscores",
                                    "Invalid Name", JOptionPane.ERROR_MESSAGE);
                            focusTarget = NAME;
                        } else if (theStrings.isEmpty()) {
                            JOptionPane.showMessageDialog(EnumerationDialog.this,
                                    "Enumeration may not have zero elements",
                                    "Invalid Enumeration", JOptionPane.ERROR_MESSAGE);
                            focusTarget = ENUMERATION;
                        } else { // valid entry
                            approved = true;
                            dispose();
                        }
                    }
                });

        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                if (focusTarget == NAME) {
                    namePanel.requestFocus();
                } else { // focusTarget == ENUMERATION
                    enumPanel.requestFocus();
                }
            }
        });

    }

    public Vector<String> getVector() {
        return theStrings;
    }

    public String getText() {
        return nameText;
    }

    public boolean wasApproved() {
        return approved;
    }

    class EnterAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public EnterAction() {
            super("Enter Action");
        }

        public void actionPerformed(ActionEvent e) {
            Object o = e.getSource();
            if (o == namePanel.nameField) {
                enumPanel.newString.requestFocus();
            } else if (o == enumPanel.newString) {
                if (enumPanel.newString.getText().length() == 0) {
                    buttonPanel.okButton.doClick();
                } else {
                    buttonPanel.addButton.doClick();
                }
            }
        }
    }//class EnterAction

}//class EnumerationDialog

