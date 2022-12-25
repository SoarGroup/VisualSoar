package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that contians the buttons for the enumeration dialogs
 *
 * @author Jon Bauman
 * @see EnumerationDialog
 * @see EditEnumerationDialog
 */
class EnumButtonPanel extends JPanel {
    private static final long serialVersionUID = 20221225L;


    JButton cancelButton = new JButton("Cancel");
    JButton okButton = new JButton("OK");
    JButton addButton = new JButton("Add");
    JButton removeButton = new JButton("Remove");

    public EnumButtonPanel() {
        okButton.setMnemonic('o');
        cancelButton.setMnemonic('c');
        addButton.setMnemonic('a');
        removeButton.setMnemonic('r');

        setLayout(new FlowLayout());
        add(okButton);
        add(addButton);
        add(removeButton);
        add(cancelButton);
    }
}
