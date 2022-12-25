package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that contains the buttons for different find options for
 * the find dialogs. This includes direction, case matching, and
 * search wrapping.
 *
 * @author Andrew Nuxoll
 * @see FindInProjectDialog
 */
class FindInProjectOptionsPanel extends JPanel {
    private static final long serialVersionUID = 20221225L;

    JCheckBox matchCase = new JCheckBox("Match Case", false);

    public FindInProjectOptionsPanel() {
        matchCase.setMnemonic('m');
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(matchCase);
    }

    /**
     * @return true if a case specific search is specified
     */
    public Boolean getMatchCase() {
        return matchCase.isSelected();
    }

}//class FindInProjectOptionsPanel

