package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.dialogs.find.FindDialog;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class FindAction extends AbstractAction {
	private static final long serialVersionUID = 20221225L;

	private final RuleEditor ruleEditor;

	public FindAction(RuleEditor ruleEditor) {
		super("Find");
		this.ruleEditor = ruleEditor;
	}

	public void actionPerformed(ActionEvent e) {
		FindDialog findDialog = new FindDialog(MainFrame.getMainFrame(), ruleEditor);
		findDialog.setVisible(true);
	}
}
