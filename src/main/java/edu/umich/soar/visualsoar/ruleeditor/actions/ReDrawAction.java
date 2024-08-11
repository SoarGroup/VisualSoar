package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.ruleeditor.EditorPane;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ReDrawAction extends AbstractAction {
	private static final long serialVersionUID = 20221225L;
	private final EditorPane editorPane;

	public ReDrawAction(EditorPane editorPane) {
		super("Redraw");
		this.editorPane = editorPane;
	}

	public void actionPerformed(ActionEvent e) {
		editorPane.colorSyntax();
	}
}
