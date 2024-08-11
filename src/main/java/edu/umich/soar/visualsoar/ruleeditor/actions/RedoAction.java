package edu.umich.soar.visualsoar.ruleeditor.actions;

import javax.swing.*;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.ActionEvent;

public class RedoAction extends AbstractAction {
	private static final long serialVersionUID = 20221225L;
	private final UndoableEdit undoManager;
	private final Toolkit toolkit;

	public RedoAction(UndoableEdit undoManager, Toolkit toolkit) {
		super("Redo");
		this.undoManager = undoManager;
		this.toolkit = toolkit;
	}

	public void actionPerformed(ActionEvent e) {
		if (undoManager.canRedo()) {
			undoManager.redo();
		} else {
			toolkit.beep();
		}
	}
}
