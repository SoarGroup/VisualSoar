package edu.umich.soar.visualsoar.dialogs;

import edu.umich.soar.visualsoar.ProjectModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/**
 * Dialog for entry of a generic name
 *
 * @author Jon Bauman
 */
public class NameDialog extends JDialog {
    private static final long serialVersionUID = 20221225L;


    boolean approved = false;

    String nameText = null;

    /**
     * panel which contains the name input field
     */
    NamePanel namePanel = new NamePanel("Name");

    DefaultButtonPanel buttonPanel = new DefaultButtonPanel();


    /**
     * @param owner Frame which owns the dialog
     */
    public NameDialog(final Frame owner) {
        super(owner, "Enter Name", true);

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

        DialogUtils.setUpDialogFocus(this, owner, namePanel.nameField);

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
                nameText = namePanel.getText();

                if (nameText.isEmpty()) {
                    JOptionPane.showMessageDialog(NameDialog.this,
                            "Names cannot have length zero",
                            "Invalid Name", JOptionPane.ERROR_MESSAGE);
                } else if (nameText.indexOf(' ') != -1) {
                    JOptionPane.showMessageDialog(NameDialog.this,
                            "Names may not contain spaces",
                            "Invalid Name", JOptionPane.ERROR_MESSAGE);
                } else if (!ProjectModel.operatorNameIsValid(nameText)) {
                    JOptionPane.showMessageDialog(NameDialog.this,
                            "Names may only contain letters, numbers, hyphens, and underscores",
                            "Invalid Name", JOptionPane.ERROR_MESSAGE);
                } else if (nameText.equals("elaborations")) {
                    JOptionPane.showMessageDialog(NameDialog.this,
                            "The file name 'elaborations' is reserved by Visual Soar",
                            "Invalid Name", JOptionPane.ERROR_MESSAGE);
                } else if (nameText.equals("all")) {
                    JOptionPane.showMessageDialog(NameDialog.this,
                            "The file name 'all' is reserved by Visual Soar",
                            "Invalid Name", JOptionPane.ERROR_MESSAGE);
                } else {
                    approved = true;
                    dispose();
                }
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
