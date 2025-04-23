package edu.umich.soar.visualsoar.dialogs.find;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that contains the buttons for the find dialogs
 *
 * @author Jon Bauman
 * @see FindDialog
 * @see FindReplaceDialog
 */
class FindButtonPanel extends JPanel {
  private static final long serialVersionUID = 20221225L;

  final JButton cancelButton = new JButton("Cancel");
  final JButton findButton = new JButton("Find");

  public FindButtonPanel() {
    cancelButton.setMnemonic('c');
    findButton.setMnemonic('f');

    setLayout(new FlowLayout());
    add(findButton);
    add(cancelButton);
  }
}
