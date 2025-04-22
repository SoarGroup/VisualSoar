package edu.umich.soar.visualsoar.dialogs.prefs;

import edu.umich.soar.visualsoar.misc.Prefs;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;

/**
 * Panel containing editing facilities for selecting custom colors for use in syntax highlighting
 *
 * @author Jon Bauman
 */
class AutoTilePanel extends JPanel {
  private static final long serialVersionUID = 20221225L;

  private final JCheckBox enable = new JCheckBox("Enable Auto-tiling");
  private final JRadioButton horizontal = new JRadioButton("Horizontal");

  public AutoTilePanel() {
    JPanel horizVertPanel = new JPanel();

    setLayout(new GridLayout(2, 1, 0, 5));

    ButtonGroup horizVert = new ButtonGroup();
    horizVert.add(horizontal);
    JRadioButton vertical = new JRadioButton("Vertical");
    horizVert.add(vertical);

    horizVertPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    JLabel tileStyle = new JLabel("Default Tile Style:");
    horizVertPanel.add(tileStyle);
    horizVertPanel.add(Box.createHorizontalStrut(5));
    horizVertPanel.add(horizontal);
    horizVertPanel.add(vertical);

    enable.setSelected(Prefs.autoTileEnabled.getBoolean());
    if (Prefs.horizTile.getBoolean()) {
      horizontal.setSelected(true);
    } else {
      vertical.setSelected(true);
    }

    add(enable);
    add(horizVertPanel);

    setBorder(
        new CompoundBorder(
            BorderFactory.createTitledBorder("Tiling"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
  } // constructor

  public boolean getAutoTile() {
    return enable.isSelected();
  }

  public boolean getHorizontalTile() {
    return horizontal.isSelected();
  }
}
