package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Panel that displays the contact info for the about dialog
 *
 * @author Jon Bauman
 * @see AboutDialog
 */
class AboutEmailPanel extends JPanel {
    private static final long serialVersionUID = 20221225L;


    JLabel emailLabel =
            new JLabel("If you have any questions or comments,");
    JLabel emailLabel2 =
            new JLabel("contact us at soar-help@googlegroups.com");

    public AboutEmailPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        add(emailLabel);
        add(emailLabel2);
    }
}
