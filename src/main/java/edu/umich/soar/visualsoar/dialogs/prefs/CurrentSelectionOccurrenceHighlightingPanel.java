package edu.umich.soar.visualsoar.dialogs.prefs;

import edu.umich.soar.visualsoar.components.HighlightedTextButton;
import edu.umich.soar.visualsoar.misc.Prefs;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.CompoundBorder;

/** Preferences for highlighting occurrences of the current selection */
class CurrentSelectionOccurrenceHighlightingPanel extends JPanel {
  private static final long serialVersionUID = 20221225L;

  private final JCheckBox enable =
      new JCheckBox("Enable highlighting occurrences of current selection");

  private Color selectedColor;

  /** ctor */
  public CurrentSelectionOccurrenceHighlightingPanel() {
    JPanel enablePanel = new JPanel();
    JPanel buttonPanel = new JPanel();

    enable.setSelected(Prefs.highlightingEnabled.getBoolean());
    enablePanel.add(enable);

    selectedColor = new Color(Prefs.currentSelectionOccurrenceHighlightColor.getInt());
    HighlightedTextButton changeColorButton =
        new HighlightedTextButton("Highlight Color", selectedColor);
    changeColorButton.setTextBackground(selectedColor);

    changeColorButton.addActionListener(
        e -> {
          Color c =
              JColorChooser.showDialog(
                  CurrentSelectionOccurrenceHighlightingPanel.this, "Select Color", null);
          if (c != null) {
            changeColorButton.setTextBackground(c);
            selectedColor = c;
          }
        });

    buttonPanel.add(changeColorButton);

    // organize vertically
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(enablePanel);
    add(buttonPanel);

    setBorder(
        new CompoundBorder(
            BorderFactory.createTitledBorder("Current Selection Occurrence Highlighting"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
  }

  public boolean enabled() {
    return enable.isSelected();
  }

  public Color getSelectedColor() {
    return selectedColor;
  }
}
