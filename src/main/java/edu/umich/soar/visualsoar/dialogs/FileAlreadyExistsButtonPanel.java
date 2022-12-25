package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that contains the buttons for the FileAlreadyExistsDialog
 *
 * @author Brian Harleton
 * @see FileAlreadyExistsDialog
 */
class FileAlreadyExistsButtonPanel extends JPanel {
    private static final long serialVersionUID = 20221225L;


    JButton cancelButton = new JButton("Cancel");
    JButton useButton = new JButton("Use");
    JButton replaceButton = new JButton("Replace");

    public FileAlreadyExistsButtonPanel() {
        useButton.setMnemonic('u');
        cancelButton.setMnemonic('c');
        replaceButton.setMnemonic('r');

        setLayout(new FlowLayout());
        add(useButton);
        add(replaceButton);
        add(cancelButton);
    }
}