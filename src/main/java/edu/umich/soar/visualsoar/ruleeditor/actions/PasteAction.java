package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.ruleeditor.EditorPane;

import javax.swing.text.DefaultEditorKit;
import java.awt.event.ActionEvent;

public class PasteAction extends DefaultEditorKit.PasteAction {
	private static final long serialVersionUID = 20221225L;
	private final EditorPane editorPane;

	public PasteAction(EditorPane editorPane) {
		super();
		this.editorPane = editorPane;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		editorPane.colorSyntax();
	}
}
