package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that contains the buttons for the average dialog
 *
 * @author Jon Bauman
 */
public class DefaultButtonPanel extends JPanel {
    private static final long serialVersionUID = 20221225L;


    public final JButton cancelButton = new JButton("Cancel");
    public final JButton okButton = new JButton("OK");

    public DefaultButtonPanel() {
        okButton.setMnemonic('o');
        cancelButton.setMnemonic('c');

        setLayout(new FlowLayout());
        add(okButton);
        add(cancelButton);
    }
}
