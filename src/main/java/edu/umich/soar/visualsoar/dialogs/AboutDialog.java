package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Dialog that displays contact info and our logo
 *
 * @author Jon Bauman
 */
public class AboutDialog extends JDialog {
    private static final long serialVersionUID = 20221225L;

    AboutVersionPanel versionPanel = new AboutVersionPanel();
    /**
     * OLD - Panel which contains the AI logo image
     * Now contains information about the authors of Visual Soar
     */
    AboutImagePanel imagePanel = new AboutImagePanel();

    /**
     * Panel which contians the email info
     */
    AboutEmailPanel emailPanel = new AboutEmailPanel();

    /**
     * Panel which contains all the buttons
     */
    AboutButtonPanel buttonPanel = new AboutButtonPanel();


    /**
     * @param owner Frame which owns the dialog
     */
    public AboutDialog(final Frame owner) {
        super(owner, "About Visual Soar", false);

        setResizable(false);
        Container contentPane = getContentPane();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        contentPane.setLayout(gridbag);

        // specifies component as last one on the row
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;

        contentPane.add(versionPanel, c);
        contentPane.add(imagePanel, c);
        contentPane.add(emailPanel, c);
        contentPane.add(buttonPanel, c);
        pack();
        getRootPane().setDefaultButton(buttonPanel.greatButton);

        DialogUtils.setUpDialogFocus(this, owner, null);

        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent we) {
                setLocationRelativeTo(owner);
            }
        });

        buttonPanel.greatButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }
}
