package edu.umich.soar.visualsoar.dialogs.find;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that contains the buttons for the find and replace dialog
 *
 * @author Jon Bauman
 * @see FindReplaceDialog
 */
public class FindReplaceButtonPanel extends JPanel {
  private static final long serialVersionUID = 20221225L;

  public final JButton cancelButton = new JButton("Cancel");
  public final JButton replaceAllButton = new JButton("Replace All");
  public final JButton replaceButton = new JButton("Replace");
  public final JButton findButton = new JButton("Find");

  public FindReplaceButtonPanel() {
    cancelButton.setMnemonic('c');
    replaceButton.setMnemonic('r');
    replaceAllButton.setMnemonic('a');
    findButton.setMnemonic('f');

    setLayout(new FlowLayout());
    add(findButton);
    add(replaceButton);
    add(replaceAllButton);
    add(cancelButton);
  }
}
