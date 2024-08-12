package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.ruleeditor.EditingUtils;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * This class un-comments (takes out the # in the first position for every
 * line) from the currently selected text of the text area.
 */
public class UncommentOutAction extends AbstractAction {
	private static final long serialVersionUID = 20221225L;
	private final JTextComponent editorPane;
	private final RuleEditor.CustomUndoManager undoManager;
	private final Toolkit toolkit;

	public UncommentOutAction(JTextComponent editorPane, RuleEditor.CustomUndoManager undoManager, Toolkit toolkit) {
		super("Uncomment Out");
		this.undoManager = undoManager;
		this.toolkit = toolkit;
		this.editorPane = editorPane;
	}

//	TODO: doesn't uncomment current line if beginning is not selected (works correctly when called from
//	CommentOutAction, but not when called from menu)
	public void actionPerformed(ActionEvent e) {
		String selectedText = editorPane.getSelectedText();
		if (selectedText != null) {
			//Save the current selection to restore later
			int selStart = editorPane.getSelectionStart();
			int selEnd = editorPane.getSelectionEnd();


			String uncommentText = selectedText;
			if (uncommentText.charAt(0) == '#') {
				uncommentText = uncommentText.substring(1);
				//decrease the selection range to accommodate missing char
//					selStart--; // TODO: do this if selection not at beginning of line (need to fix above issue first)
				selEnd--;
			}
			int nlp = uncommentText.indexOf("\n#");
			while (nlp != -1) {
				uncommentText = uncommentText.substring(0, nlp + 1) + uncommentText.substring(nlp + 2);
				nlp = uncommentText.indexOf("\n#", nlp + 1);

//decrease the selection range to accommodate missing char
				selEnd--;
			}
			try(RuleEditor.CustomUndoManager.CompoundModeManager ignored = undoManager.compoundMode()) {
				EditingUtils.replaceRange(editorPane.getDocument(), uncommentText, editorPane.getSelectionStart(),
					editorPane.getSelectionEnd());
			} catch(Exception exception) {
				// should never happen, as CompoundManager doesn't throw any exceptions
				exception.printStackTrace();
			}

			//restore the selection
			if (selEnd > selStart) {
				editorPane.setSelectionStart(selStart);
				editorPane.setSelectionEnd(selEnd);
			}
		} else {
			toolkit.beep();
		}
	}
}
