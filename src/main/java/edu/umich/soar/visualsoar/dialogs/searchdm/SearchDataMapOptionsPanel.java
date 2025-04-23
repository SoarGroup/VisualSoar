package edu.umich.soar.visualsoar.dialogs.searchdm;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;

/**
 * Panel that contains the buttons for different find options for the SearchDataMapDialog. Allows
 * user to decide to ignore certain types of wmes within the datamap
 *
 * @author Brian Harleton
 * @see SearchDataMapDialog
 */
class SearchDataMapOptionsPanel extends JPanel {
  private static final long serialVersionUID = 20221225L;

  private final JCheckBox identifierCase = new JCheckBox("Identifiers", true);
  private final JCheckBox enumerationCase = new JCheckBox("Enumerations", true);
  private final JCheckBox stringCase = new JCheckBox("Strings", true);
  private final JCheckBox integerCase = new JCheckBox("Integers", true);
  private final JCheckBox floatCase = new JCheckBox("Floats", true);

  public SearchDataMapOptionsPanel() {

    setLayout(new FlowLayout(FlowLayout.LEFT));
    add(identifierCase);
    add(enumerationCase);
    add(stringCase);
    add(integerCase);
    add(floatCase);

    setBorder(
        new CompoundBorder(
            BorderFactory.createTitledBorder("Search Includes"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
  }

  /**
   * gets all the data input into the panel by the user
   *
   * @return an array of objects representing the data
   */
  public Boolean[] getData() {
    Boolean[] optionsData = new Boolean[5];

    optionsData[0] = identifierCase.isSelected();
    optionsData[1] = enumerationCase.isSelected();
    optionsData[2] = stringCase.isSelected();
    optionsData[3] = integerCase.isSelected();
    optionsData[4] = floatCase.isSelected();

    return optionsData;
  }
}
