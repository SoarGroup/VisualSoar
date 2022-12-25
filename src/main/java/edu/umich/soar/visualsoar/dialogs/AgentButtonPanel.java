package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that contains the buttons for the new agent dialog
 *
 * @author Jon Bauman
 * @see NewAgentDialog
 */
class AgentButtonPanel extends JPanel {

    JButton cancelButton = new JButton("Cancel");
    JButton newButton = new JButton("New");

    public AgentButtonPanel() {
        newButton.setMnemonic('n');
        cancelButton.setMnemonic('c');

        setLayout(new FlowLayout());
        add(newButton);
        add(cancelButton);
    }
}
