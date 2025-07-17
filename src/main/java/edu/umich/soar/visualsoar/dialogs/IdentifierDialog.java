package edu.umich.soar.visualsoar.dialogs;

import edu.umich.soar.visualsoar.datamap.DataMapUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Dialog which takes input for the creation of a new
 * SoarIdentifierVertex in the data map
 *
 * @author Jon Bauman
 * @see edu.umich.soar.visualsoar.graph.SoarIdentifierVertex
 * @see edu.umich.soar.visualsoar.datamap.DataMapTree
 */
public class IdentifierDialog extends JDialog {
    private static final long serialVersionUID = 20221225L;


    boolean approved = false;

    String nameText = null;

    /**
     * panel which contains the name input field
     */
    NamePanel namePanel = new NamePanel("Attribute Name");

    DefaultButtonPanel buttonPanel = new DefaultButtonPanel();

    /**
     * @param owner Frame which owns the dialog
     */
    public IdentifierDialog(final Frame owner) {
        super(owner, "Enter Attribute Name", true);

        setResizable(false);
        Container contentPane = getContentPane();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        contentPane.setLayout(gridbag);

        // specifies component as last one on the row
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;

        contentPane.add(namePanel, c);
        contentPane.add(buttonPanel, c);
        pack();
        getRootPane().setDefaultButton(buttonPanel.okButton);

        DialogUtils.setUpDialogFocus(this, owner, namePanel);

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

        buttonPanel.okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                nameText = namePanel.getText().trim();

                if (nameText.length() == 0) {
                    JOptionPane.showMessageDialog(IdentifierDialog.this,
                            "Attribute names cannot have length zero",
                            "Invalid Name", JOptionPane.ERROR_MESSAGE);
                } else if (!DataMapUtils.attributeNameIsValid(nameText)) {
                    JOptionPane.showMessageDialog(IdentifierDialog.this,
                            "Attribute names may only contain letters, numbers, hyphens and underscores",
                            "Invalid Name: '" + nameText + "'", JOptionPane.ERROR_MESSAGE);
                } else {
                    approved = true;
                    dispose();
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                namePanel.requestFocus();
            }
        });

    }

    public boolean wasApproved() {
        return approved;
    }

    public String getText() {
        return nameText;
    }

    /**
     * @param s String to set the name entry field to
     *          before making the dialog visible
     */
    public void makeVisible(String s) {
        namePanel.setText(s);
        setVisible(true);
    }

}
