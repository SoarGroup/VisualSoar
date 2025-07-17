package edu.umich.soar.visualsoar.dialogs.find;

import edu.umich.soar.visualsoar.dialogs.DialogUtils;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Dialog which takes input for, and initiates a find or replace operation
 *
 * @author Jon Bauman
 * @see RuleEditor#find
 * @see RuleEditor#replace
 */
public class FindReplaceDialog extends JDialog {
  private static final long serialVersionUID = 20221225L;

  /** panel which contains the find input field and option buttons */
  private final FindPanel findPanel;

  /** the rule editor this find was excecuted from, null if this is a project-wide search */
  private final RuleEditor d_ruleEditor;

  /** panel which contians all the replace input field */
  private final ReplacePanel replacePanel = new ReplacePanel();

  private final FindReplaceButtonPanel buttonPanel = new FindReplaceButtonPanel();

  /**
   * @param owner Frame which owns the dialog
   * @param ruleEditor a reference to the RuleEditor
   */
  public FindReplaceDialog(final Frame owner, RuleEditor ruleEditor) {
    super(owner, "Find & Replace", false);

    d_ruleEditor = ruleEditor;
    setResizable(false);
    Container contentPane = getContentPane();
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    contentPane.setLayout(gridbag);

    // specifies component as last one on the row
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.fill = GridBagConstraints.HORIZONTAL;

    findPanel = new FindPanel(ruleEditor.getSelectedText());
    contentPane.add(findPanel, c);
    contentPane.add(replacePanel, c);
    contentPane.add(buttonPanel, c);
    pack();
    getRootPane().setDefaultButton(buttonPanel.findButton);

    DialogUtils.setUpDialogFocus(this, owner, null);
    // Set the options components as unfocusable so user can
    // quickly tab between the find & replace fields
    findPanel.optionsPanel.upButton.setFocusable(false);
    findPanel.optionsPanel.downButton.setFocusable(false);
    findPanel.optionsPanel.matchCase.setFocusable(false);
    findPanel.optionsPanel.wrap.setFocusable(false);

    addWindowListener(
        new WindowAdapter() {
          public void windowOpened(WindowEvent we) {
            setLocationRelativeTo(owner);
            findPanel.requestFocus();
          }
        });

    buttonPanel.cancelButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            dispose();
          }
        });

    buttonPanel.replaceAllButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Object[] theData = findPanel.getData();
            String toReplace = replacePanel.getText();
            String toFind = (String) theData[0];
            Boolean forward = (Boolean) theData[1];
            Boolean caseSensitive = (Boolean) theData[2];
            Boolean wrap = (Boolean) theData[3];

            d_ruleEditor.setFindReplaceData(toFind, toReplace, forward, caseSensitive, wrap);
            d_ruleEditor.replaceAll();
          }
        });

    buttonPanel.replaceButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Object[] theData = findPanel.getData();
            String toReplace = replacePanel.getText();
            String toFind = (String) theData[0];
            Boolean forward = (Boolean) theData[1];
            Boolean caseSensitive = (Boolean) theData[2];
            Boolean wrap = (Boolean) theData[3];

            d_ruleEditor.setFindReplaceData(toFind, toReplace, forward, caseSensitive, wrap);
            d_ruleEditor.replace();
          }
        });

    buttonPanel.findButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Object[] theData = findPanel.getData();
            String toReplace = replacePanel.getText();
            String toFind = (String) theData[0];
            Boolean forward = (Boolean) theData[1];
            Boolean caseSensitive = (Boolean) theData[2];
            Boolean wrap = (Boolean) theData[3];

            d_ruleEditor.setFindReplaceData(toFind, toReplace, forward, caseSensitive, wrap);
            d_ruleEditor.find();
          }
        });
  }
}
