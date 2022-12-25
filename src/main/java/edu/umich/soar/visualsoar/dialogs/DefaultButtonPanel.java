package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that contains the buttons for the average dialog
 *
 * @author Jon Bauman
 */
class DefaultButtonPanel extends JPanel {
    private static final long serialVersionUID = 20221225L;


    JButton cancelButton = new JButton("Cancel");
    JButton okButton = new JButton("OK");

    public DefaultButtonPanel() {
        okButton.setMnemonic('o');
        cancelButton.setMnemonic('c');

        setLayout(new FlowLayout());
        add(okButton);
        add(cancelButton);
    }
}
