package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.ruleeditor.EditorPane;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.event.ActionEvent;

public class SelectAllAction extends AbstractAction {

	private final EditorPane editorPane;

	public SelectAllAction(EditorPane editorPane) {
		super(DefaultEditorKit.selectAllAction);
		this.editorPane = editorPane;
	}

	public void actionPerformed(ActionEvent e) {
		editorPane.selectAll();
	}
}
