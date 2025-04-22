package edu.umich.soar.visualsoar.dialogs.prefs;

import edu.umich.soar.visualsoar.misc.Prefs;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;

/**
 * Panel for changing preferences related to project saving
 */
class SaveActionsPanel extends JPanel {
  private static final long serialVersionUID = 20221225L;

  private final JCheckBox saveOnDmCheckPass =
      new JCheckBox("Save project when datamap check passes");
  private final JCheckBox checkDmOnSave = new JCheckBox("Check project against datamap when saved");

  public SaveActionsPanel() {
    setLayout(new GridLayout(2, 1, 0, 5));

    saveOnDmCheckPass.setSelected(Prefs.saveOnDmCheckPass.getBoolean());
    checkDmOnSave.setSelected(Prefs.checkDmOnSave.getBoolean());

    add(saveOnDmCheckPass);
    add(checkDmOnSave);

    setBorder(new CompoundBorder(
      BorderFactory.createTitledBorder("Save Actions"),
      BorderFactory.createEmptyBorder(10, 10, 10, 10)));
  } // constructor

  public boolean getSaveOnDmCheckPass() {
    return saveOnDmCheckPass.isSelected();
  }
  public boolean getCheckDmOnSave() {
    return checkDmOnSave.isSelected();
  }
}

