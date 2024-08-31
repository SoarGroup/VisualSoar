package edu.umich.soar.visualsoar.dialogs;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.misc.Prefs;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Panel that contains the input field for the path of the agent
 * in the new agent dialog
 *
 * @author Jon Bauman
 * @see NewAgentDialog
 */
class AgentPathPanel extends JPanel {
    private static final long serialVersionUID = 20221225L;


    String workingDirName;
    JTextField pathField = new JTextField(workingDirName, 20);
    JButton browse = new JButton("Browse...");

    public AgentPathPanel() {
        browse.setMnemonic('b');

        setLayout(new FlowLayout());
        add(pathField);
        add(browse);

        setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder("Agent Path"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        //Calculate the starting directory for the Browse button
        workingDirName = Prefs.openFolder.get();
        File workingDir = new File(workingDirName);
        if (! workingDir.exists()) {
            workingDirName = ".";
        }
        if (!workingDir.isDirectory()) {
            workingDirName = ".";
        }

        // So that enter can affirmatively dismiss the dialog
        // pathField.getKeymap().removeKeyStrokeBinding(
        // KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));

        browse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DirectorySelectionDialog dsd = new DirectorySelectionDialog(MainFrame.getMainFrame());
                dsd.setPath(new File(workingDirName));
                dsd.setVisible(true);
                if (dsd.wasApproved()) {
                    File file = dsd.getSelectedDirectory();
                    pathField.setText(file.getPath());

                    //Save this working folder for next time
                    if (file.isDirectory()) {
                        workingDirName = file.getPath();
                    }
                }
            }
        });
    }

    /**
     * @return the inputted path for the new agent
     */
    public String getPath() {
        return pathField.getText();
    }
}
