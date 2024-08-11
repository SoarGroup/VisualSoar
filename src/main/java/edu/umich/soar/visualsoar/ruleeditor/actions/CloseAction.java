package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.misc.Prefs;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;

/**
 * Closes the current window
 */
public class CloseAction extends AbstractAction {
	private static final long serialVersionUID = 20221225L;

	private final RuleEditor ruleEditor;

	public CloseAction(RuleEditor ruleEditor) {
		super("Close");
		this.ruleEditor = ruleEditor;
	}

	public void actionPerformed(ActionEvent event) {
		try {
			ruleEditor.setClosed(true);

		} catch (PropertyVetoException pve) {
			// This is not an error
		}
		MainFrame mf = MainFrame.getMainFrame();
		if (Prefs.autoTileEnabled.getBoolean()) {
			mf.getDesktopPane().performTileAction();
		}

		mf.selectNewInternalFrame();
	}
}
