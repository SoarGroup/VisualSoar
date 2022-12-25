package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that contains the buttons for the find and replace dialog
 *
 * @author Jon Bauman
 * @see FindReplaceDialog
 */
class FindReplaceButtonPanel extends JPanel {
    private static final long serialVersionUID = 20221225L;


    JCheckBox keepDialog = new JCheckBox("Keep Dialog", true);

    JButton cancelButton = new JButton("Cancel");
    JButton replaceAllButton = new JButton("Replace All");
    JButton replaceButton = new JButton("Replace");
    JButton findButton = new JButton("Find");

    public FindReplaceButtonPanel() {
        cancelButton.setMnemonic('c');
        replaceButton.setMnemonic('r');
        replaceAllButton.setMnemonic('a');
        findButton.setMnemonic('f');
        keepDialog.setMnemonic('k');

        setLayout(new FlowLayout());
        add(keepDialog);
        add(findButton);
        add(replaceButton);
        add(replaceAllButton);
        add(cancelButton);
    }
}
