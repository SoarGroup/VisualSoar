package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that contains the buttons for the Save As Dialog
 *
 * @author Brian Harleton
 * @see SaveProjectAsDialog
 */
class SaveAsButtonPanel extends JPanel {

    JButton cancelButton = new JButton("Cancel");
    JButton newButton = new JButton("Save As");

    public SaveAsButtonPanel() {
        newButton.setMnemonic('n');
        cancelButton.setMnemonic('c');

        setLayout(new FlowLayout());
        add(newButton);
        add(cancelButton);
    }
}