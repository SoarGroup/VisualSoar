package edu.umich.soar.visualsoar.dialogs.find;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.event.KeyEvent;

/**
 * Panel that contains the input field for the find string and the
 * option panel for the find dialogs
 *
 * @author Andrew Nuxoll
 * @see FindInProjectDialog
 */
public class FindInProjectPanel extends JPanel {
    private static final long serialVersionUID = 20221225L;

    private final JTextField findField = new JTextField(20);
    //FIXME:  this shouldn't need to be public
    public final FindInProjectOptionsPanel optionsPanel;

    public FindInProjectPanel(String initialText) {
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
        findField.setText(initialText);
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
