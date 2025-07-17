package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

/**
 * Dialog which takes input for the editing of an EnumerationVertex
 * in the data map.
 *
 * @author Jon Bauman
 * @see edu.umich.soar.visualsoar.graph.EnumerationVertex
 * @see edu.umich.soar.visualsoar.datamap.DataMapTree
 */
public class EditEnumerationDialog extends JDialog {
    private static final long serialVersionUID = 20221225L;


    /**
     * panel which facilitates entry of enumerations
     */
    EnumPanel enumPanel = new EnumPanel();
    EnumButtonPanel buttonPanel = new EnumButtonPanel();
    boolean approved = false;
    Vector<String> theStrings = null;
    Action enterAction = new EnterAction();


    /**
     * @param owner  Frame which owns the dialog
     * @param theSet a TreeSet of data to edit
     */
    public EditEnumerationDialog(final Frame owner, Vector<String> theSet) {
        super(owner, "Enter Enumeration", true);
        enumPanel.setVector(new Vector<>(theSet));

        setResizable(false);
        Container contentPane = getContentPane();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        contentPane.setLayout(gridbag);

        // specifies component as last one on the row
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;

        contentPane.add(enumPanel, c);
        contentPane.add(buttonPanel, c);
        pack();
        getRootPane().setDefaultButton(buttonPanel.addButton);

        DialogUtils.setUpDialogFocus(this, owner, enumPanel.newString);

        //Special handling for the Enter key
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        enumPanel.newString.getKeymap().addActionForKeyStroke(enter, enterAction);

        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent we) {
                setLocationRelativeTo(owner);
                owner.repaint();
            }
        });

        buttonPanel.cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                approved = false;
                dispose();
            }
        });

        buttonPanel.addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (enumPanel.addString()) {
                    enumPanel.clearText();
                }
            }
        });

        buttonPanel.removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enumPanel.removeString();
            }
        });

        buttonPanel.okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                theStrings = new Vector<>(enumPanel.getVector());

                if (theStrings.isEmpty()) {
                    JOptionPane.showMessageDialog(EditEnumerationDialog.this,
                            "Enumeration may not have zero elements",
                            "Invalid Enumeration", JOptionPane.ERROR_MESSAGE);
                } else { // valid entry
                    approved = true;
                    dispose();
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                enumPanel.requestFocus();
            }
        });

    }

    public Vector<String> getTreeSet() {
        return theStrings;
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
            if (o == enumPanel.newString) {
                if (enumPanel.newString.getText().length() == 0) {
                    buttonPanel.okButton.doClick();
                } else {
                    buttonPanel.addButton.doClick();
                }
            }
        }
    }//class EnterAction

}//class EditEnumerationDialog
