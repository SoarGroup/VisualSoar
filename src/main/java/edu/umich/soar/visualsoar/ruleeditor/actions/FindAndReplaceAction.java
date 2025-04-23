package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.dialogs.find.FindReplaceDialog;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class FindAndReplaceAction extends AbstractAction {
	private static final long serialVersionUID = 20221225L;

	private final RuleEditor ruleEditor;

	public FindAndReplaceAction(RuleEditor ruleEditor) {
		super("Find And Replace");
		this.ruleEditor = ruleEditor;
	}

	public void actionPerformed(ActionEvent e) {
		FindReplaceDialog findReplaceDialog = new FindReplaceDialog(MainFrame.getMainFrame(), ruleEditor);
		findReplaceDialog.setVisible(true);
	}
}
