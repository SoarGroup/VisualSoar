package edu.umich.soar.visualsoar.dialogs;

import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Dialog which takes input for, and initiates a find operation
 *
 * @author Jon Bauman
 * @see RuleEditor#find
 */
public class FindDialog extends JDialog {
  private static final long serialVersionUID = 20221225L;

  /** panel which contains the find input field and option buttons */
  FindPanel findPanel;

  /** the rule editor this find was excecuted from, null if this is a project-wide search */
  RuleEditor d_ruleEditor;

  FindButtonPanel buttonPanel;

  /**
   * @param owner Frame which owns the dialog
   * @param ruleEditor the rule editor in which to search
   */
  public FindDialog(final Frame owner, RuleEditor ruleEditor) {
    super(owner, "Find", false);
    findPanel = new FindPanel(ruleEditor.getSelectedText());
    buttonPanel = new FindButtonPanel();
    d_ruleEditor = ruleEditor;
    setResizable(false);
    Container contentPane = getContentPane();
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    contentPane.setLayout(gridbag);

    // specifies component as last one on the row
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.fill = GridBagConstraints.HORIZONTAL;

    contentPane.add(findPanel, c);
    contentPane.add(buttonPanel, c);
    pack();
    getRootPane().setDefaultButton(buttonPanel.findButton);

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

    buttonPanel.findButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Object[] theData = findPanel.getData();
            String toFind = (String) theData[0];
            Boolean forward = (Boolean) theData[1];
            Boolean caseSensitive = (Boolean) theData[2];
            Boolean wrap = (Boolean) theData[3];

            d_ruleEditor.setFindReplaceData(toFind, forward, caseSensitive, wrap);
            d_ruleEditor.find();
            if (!buttonPanel.keepDialog.isSelected()) {
              dispose();
            }
          }
        });
  }
}
