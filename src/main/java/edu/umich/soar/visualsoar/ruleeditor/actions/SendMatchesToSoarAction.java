package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;
import edu.umich.soar.visualsoar.util.SoarUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

// 3P
// Handles the "Runtime|Matches Production" menu item
public class SendMatchesToSoarAction extends AbstractAction {
	private static final long serialVersionUID = 20221225L;

	private final RuleEditor ruleEditor;
  private final Toolkit toolkit;

  public SendMatchesToSoarAction(RuleEditor ruleEditor, Toolkit toolkit) {
		super("Matches Production");
		this.ruleEditor = ruleEditor;
    this.toolkit = toolkit;
  }

	public void actionPerformed(ActionEvent e) {
		// Call matches in Soar
		String sProductionName = ruleEditor.GetProductionNameUnderCaret();
    if (sProductionName == null) {
      MainFrame.getMainFrame()
          .setStatusBarError(
              "I don't know which production you wish to find matches for; "
                  + "please click inside of it before attempting the command again.");
      toolkit.beep();
      return;
    }
    SoarUtils.executeCommandLine("matches " + sProductionName, ruleEditor, true);
	}
}
