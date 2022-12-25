package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that contains the buttons for the search data map action
 *
 * @author Brian Harleton
 * @see SearchDataMapDialog
 */
class SearchDataMapButtonPanel extends JPanel {

    JCheckBox keepDialog = new JCheckBox("Keep Dialog", true);

    JButton cancelButton = new JButton("Cancel");
    JButton findNextButton = new JButton("Find Next");

    public SearchDataMapButtonPanel() {
        cancelButton.setMnemonic('c');
        findNextButton.setMnemonic('f');
        keepDialog.setMnemonic('k');

        setLayout(new FlowLayout());
        add(keepDialog);
        add(findNextButton);
        add(cancelButton);
    }
}