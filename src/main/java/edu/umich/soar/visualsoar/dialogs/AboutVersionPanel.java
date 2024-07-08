package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Panel that displays the Version number of Visual Soar for the 'About' dialog
 *
 * @author Brian Harleton
 * @see AboutDialog
 */
class AboutVersionPanel extends JPanel {
    private static final long serialVersionUID = 20221225L;


    JLabel versionLabel =
            new JLabel("Visual Soar");
    JLabel versionLabel2 =
            new JLabel("    Version 4.6.21 (08 Jul 2024)");

    public AboutVersionPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        add(versionLabel);
        add(versionLabel2);
    }
}
