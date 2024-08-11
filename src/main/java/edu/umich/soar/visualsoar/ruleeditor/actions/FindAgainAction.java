package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class FindAgainAction extends AbstractAction {
	private static final long serialVersionUID = 20221225L;

	private final RuleEditor ruleEditor;

	public FindAgainAction(RuleEditor ruleEditor) {
		super("Find Again");
		this.ruleEditor = ruleEditor;
		setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		ruleEditor.find();
	}
}
