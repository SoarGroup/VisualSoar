package edu.umich.soar.visualsoar.dialogs.searchdm;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that contains the buttons for the search data map action
 *
 * @author Brian Harleton
 * @see SearchDataMapDialog
 */
class SearchDataMapButtonPanel extends JPanel {
  private static final long serialVersionUID = 20221225L;

  final JButton cancelButton = new JButton("Cancel");
  final JButton findNextButton = new JButton("Find Next");

  public SearchDataMapButtonPanel() {
    cancelButton.setMnemonic('c');
    findNextButton.setMnemonic('f');

    setLayout(new FlowLayout());
    add(findNextButton);
    add(cancelButton);
  }
}
