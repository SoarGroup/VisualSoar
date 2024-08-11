package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Gets the currently selected rule editor and tells it to save itself
 */
public class SaveAction extends AbstractAction {
	private static final long serialVersionUID = 20221225L;

	private final RuleEditor ruleEditor;

	public SaveAction(RuleEditor ruleEditor) {
		super("Save File");
		this.ruleEditor = ruleEditor;
	}

	public void actionPerformed(ActionEvent event) {
		try {
			ruleEditor.write();
		} catch (java.io.IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
