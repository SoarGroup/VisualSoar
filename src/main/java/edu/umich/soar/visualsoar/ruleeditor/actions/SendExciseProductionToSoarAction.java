package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;
import edu.umich.soar.visualsoar.util.SoarUtils;
import sml.Agent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

// 3P
// Handles the "Runtime|Excise Production" menu item
public class SendExciseProductionToSoarAction extends AbstractAction {
	private static final long serialVersionUID = 20221225L;

	private final RuleEditor ruleEditor;
  private final Toolkit toolkit;

  public SendExciseProductionToSoarAction(RuleEditor ruleEditor, Toolkit toolkit) {
		super("Excise Production");
		this.ruleEditor = ruleEditor;
    this.toolkit = toolkit;
  }

	public void actionPerformed(ActionEvent e) {
		// Call excise in Soar
		String sProductionName = ruleEditor.GetProductionNameUnderCaret();
    if (sProductionName == null) {
      MainFrame.getMainFrame()
        .setStatusBarError(
          "I don't know which production you wish to excise; "
            + "please click inside of it before attempting the command again.");
      toolkit.beep();
      return;
    }
    SoarUtils.executeCommandLine("excise " + sProductionName, ruleEditor, true);
	}
}
