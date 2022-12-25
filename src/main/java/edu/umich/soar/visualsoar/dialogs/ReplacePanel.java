package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.event.KeyEvent;

/**
 * Panel that contains the input field for the replace string for the
 * replace dialog
 *
 * @author Jon Bauman
 * @see ReplaceInProjectDialog
 */
class ReplacePanel extends JPanel {

    JTextField replaceField = new JTextField(20);

    public ReplacePanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(replaceField);

        setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder("Replace With"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        // So that enter can affirmatively dismiss the dialog
        replaceField.getKeymap().removeKeyStrokeBinding(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
    }

    /**
     * @return the user inputted String
     */
    public String getText() {
        return replaceField.getText();
    }
}
