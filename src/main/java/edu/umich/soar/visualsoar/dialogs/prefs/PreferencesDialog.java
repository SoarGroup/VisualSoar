package edu.umich.soar.visualsoar.dialogs.prefs;

import edu.umich.soar.visualsoar.dialogs.DefaultButtonPanel;
import edu.umich.soar.visualsoar.dialogs.DialogUtils;
import edu.umich.soar.visualsoar.dialogs.NumberTextField;
import edu.umich.soar.visualsoar.misc.Prefs;
import edu.umich.soar.visualsoar.misc.SyntaxColor;
import edu.umich.soar.visualsoar.ruleeditor.SoarDocument;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Dialog that allows preferences to be edited
 *
 * @author Jon Bauman
 * @author Brian Harleton
 */
public class PreferencesDialog extends JDialog {
  private static final long serialVersionUID = 20221225L;

  /** Panel which contains the syntax color editing buttons */
  private final SyntaxColorsPanel colorPanel;

  /** Panel which contains the auto-tiling pref buttons */
  private final AutoTilePanel tilePanel = new AutoTilePanel();

  private final SaveActionsPanel saveActionsPanel = new SaveActionsPanel();

  /** The syntax colors */
  private final SyntaxColor[] colorTable = Prefs.getSyntaxColors();

  private final JCheckBox autoIndentingCheckBox =
      new JCheckBox("Auto-Indenting", Prefs.autoIndentingEnabled.getBoolean());
  private final JCheckBox autoSoarCompleteCheckBox =
      new JCheckBox("Auto-Soar Complete", Prefs.autoSoarCompleteEnabled.getBoolean());

  /** used to let the user change the font size of the editor font */
  private final NumberTextField editorFontField = new NumberTextField();

  private boolean approved = false;

  /**
   * @param owner Frame which owns the dialog
   */
  public PreferencesDialog(final Frame owner) {
    super(owner, "Preferences", true);

    Container contentPane = getContentPane();
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    JPanel ruleEditorPanel = new JPanel();
    JPanel generalPanel = new JPanel();
    JPanel checkBoxPanel = new JPanel();
    JPanel editorFontPanel = new JPanel();

    colorPanel = new SyntaxColorsPanel(Prefs.getSyntaxColors());

    setResizable(false);

    contentPane.setLayout(gridbag);

    // specifies component as last one on the row
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.fill = GridBagConstraints.HORIZONTAL;

    // Auto-Formatting Section
    checkBoxPanel.setLayout(new BorderLayout());
    checkBoxPanel.setBorder(
        new CompoundBorder(
            BorderFactory.createTitledBorder("Auto Formatting"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
    checkBoxPanel.add(autoIndentingCheckBox, BorderLayout.NORTH);
    checkBoxPanel.add(autoSoarCompleteCheckBox, BorderLayout.SOUTH);

    // Font Size Section (added by :AMN: on 25 Sep 2022)
    editorFontPanel.setBorder(
        new CompoundBorder(
            BorderFactory.createTitledBorder("Editor Font"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
    editorFontPanel.add(Box.createHorizontalBox());
    editorFontPanel.add(new JLabel("Font Size: "));
    editorFontField.setText(String.valueOf(SoarDocument.getFontSize()));
    editorFontPanel.add(editorFontField);

    ruleEditorPanel.setLayout(new BoxLayout(ruleEditorPanel, BoxLayout.Y_AXIS));
    ruleEditorPanel.add(colorPanel);
    ruleEditorPanel.add(editorFontPanel);
    ruleEditorPanel.add(checkBoxPanel);

    generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));
    generalPanel.add(tilePanel, BorderLayout.SOUTH);
    generalPanel.add(saveActionsPanel);

    JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
    tabPane.addTab("Rule Editor", ruleEditorPanel);
    tabPane.addTab("General", generalPanel);

    tabPane.setSelectedIndex(0);

    contentPane.add(tabPane, c);

    // This panel contains all the buttons
    DefaultButtonPanel buttonPanel = new DefaultButtonPanel();
    contentPane.add(buttonPanel, c);

    pack();
    getRootPane().setDefaultButton(buttonPanel.okButton);

    DialogUtils.closeOnEscapeKey(this, owner);
    addWindowListener(
        new WindowAdapter() {
          public void windowOpened(WindowEvent we) {
            setLocationRelativeTo(owner);
          }
        });

    buttonPanel.cancelButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            dispose();
          }
        });

    buttonPanel.okButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            approved = true;

            Prefs.highlightingEnabled.setBoolean(colorPanel.getEnableHighlighting());
            Prefs.autoTileEnabled.setBoolean(tilePanel.getAutoTile());
            Prefs.horizTile.setBoolean(tilePanel.getHorizontalTile());
            Prefs.autoIndentingEnabled.setBoolean(autoIndentingCheckBox.isSelected());
            Prefs.autoIndentingEnabled.setBoolean(autoSoarCompleteCheckBox.isSelected());
            Prefs.saveOnDmCheckPass.setBoolean(saveActionsPanel.getSaveOnDmCheckPass());
            Prefs.checkDmOnSave.setBoolean(saveActionsPanel.getCheckDmOnSave());

            commitChanges();
            Prefs.setSyntaxColors(colorTable);
            dispose();

            // Set a new font size
            int fontSize = getFontSize();
            if (fontSize != SoarDocument.getFontSize()) {
              SoarDocument.setFontSize(fontSize);
              Prefs.editorFontSize.set(editorFontField.getText());
            }
          }
        });
  } // end of constructor

  /**
   * retrieves the font size from the dialog. A separate method is needed to handle exceptions and
   * enforce min/max rules.
   */
  private int getFontSize() {
    int fontSize = SoarDocument.getFontSize();
    try {
      fontSize = Integer.parseInt(editorFontField.getText());
    } catch (NumberFormatException nfe) {
      /* no action needed.  default will be used */
    }
    fontSize = Math.max(SoarDocument.MIN_FONT_SIZE, fontSize);
    fontSize = Math.min(SoarDocument.MAX_FONT_SIZE, fontSize);

    return fontSize;
  } // getFontSize

  public void commitChanges() {
    TreeMap<Integer, Color> colorsToChange = colorPanel.getChanges();

    for (int theKey : colorsToChange.keySet()) {
      Color theColor = colorsToChange.get(theKey);

      colorTable[theKey] = new SyntaxColor(theColor, colorTable[theKey]);
    }
  }

  public boolean wasApproved() {
    return approved;
  }
}
