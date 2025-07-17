package edu.umich.soar.visualsoar.dialogs;

import edu.umich.soar.visualsoar.ProjectModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Dialog which takes input for saving an agent as a new agent
 *
 * @author Brian Harleton
 */

public class SaveProjectAsDialog extends JDialog {
    private static final long serialVersionUID = 20221225L;

    private String newAgentName = null;
    private String newAgentPath = null;
    private boolean approved = false;

    /**
     * Name of Agent Input field
     */
    AgentNamePanel namePanel = new AgentNamePanel();

    /**
     * Path of where the new agent is to be located
     */
    AgentPathPanel pathPanel = new AgentPathPanel();

    SaveAsButtonPanel buttonPanel = new SaveAsButtonPanel();


    public SaveProjectAsDialog(final Frame owner) {
        super(owner, "Save Project As . . .", true);

        setResizable(false);
        Container contentPane = getContentPane();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        contentPane.setLayout(gridbag);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;

        contentPane.add(namePanel, c);
        contentPane.add(pathPanel, c);
        contentPane.add(buttonPanel, c);
        pack();
        getRootPane().setDefaultButton(buttonPanel.newButton);

        DialogUtils.closeOnEscapeKeyWithFocus(this, owner, namePanel);

        // Remove the windowActivated listener and let DialogUtils handle focus
        // This provides more consistent behavior across platforms

        buttonPanel.cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
                approved = false;
            }
        });


        buttonPanel.newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String nameText = namePanel.getName();
                if (nameText.length() == 0) {
                    JOptionPane.showMessageDialog(SaveProjectAsDialog.this,
                            "Project names cannot have length zero",
                            "Invalid Name", JOptionPane.ERROR_MESSAGE);
                } else if (!ProjectModel.isProjectNameValid(nameText)) {
                    JOptionPane.showMessageDialog(SaveProjectAsDialog.this,
                            "Project names may only contain letter, numbers, hyphens and underscores",
                            "Invalid Name", JOptionPane.ERROR_MESSAGE);
                } else {
                    newAgentName = namePanel.getName();
                    newAgentPath = pathPanel.getPath();
                    approved = true;
                    dispose();
                }
            }
        });
    }   // end of SaveProjectAsDialog constructor

    public String getNewAgentName() {
        return newAgentName;
    }

    public String getNewAgentPath() {
        return newAgentPath;
    }

    public boolean wasApproved() {
        return approved;
    }
}   // end of SaveProjectAsDialog class1
