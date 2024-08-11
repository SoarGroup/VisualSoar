package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;
import sml.Agent;

import javax.swing.*;
import java.awt.event.ActionEvent;

// 3P
// Handles the "Runtime|Matches Production" menu item
public class SendMatchesToSoarAction extends AbstractAction {
	private static final long serialVersionUID = 20221225L;

	private final RuleEditor ruleEditor;

	public SendMatchesToSoarAction(RuleEditor ruleEditor) {
		super("Matches Production");
		this.ruleEditor = ruleEditor;
	}

	public void actionPerformed(ActionEvent e) {
		// Get the agent
		Agent agent = MainFrame.getMainFrame().getActiveAgent();
		if (agent == null) {
			JOptionPane.showMessageDialog(ruleEditor, "Not connected to an agent.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Call matches in Soar
		String sProductionName = ruleEditor.GetProductionNameUnderCaret();
		if (sProductionName != null) {
			String result = agent.ExecuteCommandLine("matches " + sProductionName, true);
			MainFrame.getMainFrame().reportResult(result);
		}
	}
}//SendMatchesToSoarAction
