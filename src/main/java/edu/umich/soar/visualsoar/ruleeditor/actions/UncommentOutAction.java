package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.ruleeditor.RuleEditorUndoManager;
import edu.umich.soar.visualsoar.ruleeditor.EditingUtils;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * This class un-comments (takes out the # in the first position for every
 * line) from the currently selected text of the text area.
 *
 * CommentOutAction and UncommentOutAction are inverses, and can repeatedly undo one another.
 */
public class UncommentOutAction extends AbstractAction {
	private static final long serialVersionUID = 20221225L;
	private final JTextComponent editorPane;
	private final RuleEditorUndoManager undoManager;
	private final Toolkit toolkit;

	public UncommentOutAction(JTextComponent editorPane, RuleEditorUndoManager undoManager, Toolkit toolkit) {
		super("Uncomment Out");
		this.undoManager = undoManager;
		this.toolkit = toolkit;
		this.editorPane = editorPane;
	}

	public void actionPerformed(ActionEvent e) {
		// Save the current selection to restore later
		int selStart = editorPane.getSelectionStart();
		int selEnd = editorPane.getSelectionEnd();
		try {
			// if the selection starts in the middle of the line, move the start back to the start of the line
			// so that a comment character can be removed there. We don't expand to the end of the last line because
			// it's not necessary, and would cause a line to be uncommented even if no characters in it are selected.
			EditingUtils.expandSelectionToEntireLines(editorPane, true, false);
		} catch (BadLocationException ex) {
			ex.printStackTrace();
			return; //shouldn't happen...
		}
		String selectedText = editorPane.getSelectedText();
		if (selectedText != null) {
			String uncommentText = selectedText;
			if (uncommentText.charAt(0) == '#') {
				uncommentText = uncommentText.substring(1);
				//decrease the selection range to accommodate missing char
				selEnd--;
				// don't decrement selection start if it was before where we removed the # char
				if (editorPane.getSelectionStart() != selStart) {
					selStart--;
				}
			}
			int nlp = uncommentText.indexOf("\n#");
			while (nlp != -1) {
				uncommentText = uncommentText.substring(0, nlp + 1) + uncommentText.substring(nlp + 2);
				nlp = uncommentText.indexOf("\n#", nlp + 1);

//decrease the selection range to accommodate missing char
				selEnd--;
			}
			try(RuleEditorUndoManager.CompoundModeManager ignored = undoManager.compoundMode()) {
				EditingUtils.replaceRange(editorPane.getDocument(), uncommentText, editorPane.getSelectionStart(),
					editorPane.getSelectionEnd());
			} catch(Exception exception) {
				// should never happen, as CompoundManager doesn't throw any exceptions
				exception.printStackTrace();
			}

			//restore the selection
			editorPane.setSelectionStart(selStart);
			editorPane.setSelectionEnd(selEnd);
		} else {
			toolkit.beep();
		}
	}
}
