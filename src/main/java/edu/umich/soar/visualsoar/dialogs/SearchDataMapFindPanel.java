package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.event.KeyEvent;

/**
 * Panel that contains the input field for the find string and the
 * option panel for the find dialogs
 *
 * @author Brian Harleton
 * @see SearchDataMapDialog
 */
class SearchDataMapFindPanel extends JPanel {
    private static final long serialVersionUID = 20221225L;


    JTextField findField = new JTextField(20);
    FindInProjectOptionsPanel optionsPanel;

    public SearchDataMapFindPanel() {
        optionsPanel = new FindInProjectOptionsPanel();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(findField);
        add(optionsPanel);

        setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder("Find"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        // So that enter can affirmatively dismiss the dialog
        findField.getKeymap().removeKeyStrokeBinding(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
    }


    /**
     * gets all the data input into the panel by the user
     *
     * @return an array of objects representing the data
     */
    public Object[] getData() {
        Object[] findData = new Object[2];

        findData[0] = findField.getText();
        findData[1] = optionsPanel.getMatchCase();

        return findData;
    }

    public void requestFocus() {
        findField.requestFocus();
    }
}
