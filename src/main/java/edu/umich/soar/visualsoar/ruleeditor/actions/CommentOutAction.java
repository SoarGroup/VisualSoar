package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.ruleeditor.CustomUndoManager;
import edu.umich.soar.visualsoar.ruleeditor.EditingUtils;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;

/**
 * This class comments (puts a # in the first position for every line) for the currently selected text
 * of the text area. If the cursor sits at the beginning of a line, this line is not commented. The lines are already
 * commented, this runs UncommentOutAction instead.
 * <br/>
 * CommentOutAction and UncommentOutAction are inverses, and can repeatedly undo one another.
 */
public class CommentOutAction extends AbstractAction {
	private static final long serialVersionUID = 20221225L;
	private final JTextComponent editorPane;
	private final Action uncommentOutAction;
	private final CustomUndoManager undoManager;

	public CommentOutAction(JTextComponent editorPane, CustomUndoManager undoManager, Action uncommentOutAction) {
		super("Comment Out");
		this.editorPane = editorPane;
		this.undoManager = undoManager;
		this.uncommentOutAction = uncommentOutAction;
	}

	private boolean isCommentedOut(String text) {
		if (text.isEmpty()) return false;
		String[] lines = text.split("[\r\n]+");
		for (String line : lines) {
			if (line.trim().isEmpty()) return false;
			if (line.trim().charAt(0) != '#') return false;
		}
		return true;
	}

	public void actionPerformed(ActionEvent e) {
		//Save the current selection to restore later
		int selStart = editorPane.getSelectionStart();
		int selEnd = editorPane.getSelectionEnd();
		//Get the text to be commented out
		try {
			// if the selection starts in the middle of the line, move the start back to the start of the line
			// so that a comment character can be placed there. We don't expand to the end of the last line because
			// it's not necessary, and would cause a line to be commented even if no characters in it are selected.
			EditingUtils.expandSelectionToEntireLines(editorPane, true, false);
		} catch (BadLocationException ex) {
			ex.printStackTrace();
			return; //shouldn't happen...
		}
		String selectedText = editorPane.getSelectedText();
		if ((selectedText == null) || (selectedText.isEmpty())) {
			// action was called with nothing selected and cursor on empty line
			return;
		}

		//If all the selected text is already commented out then
		//we want to uncomment instead (i.e., a toggle)
		if (isCommentedOut(selectedText)) {
			editorPane.setSelectionStart(selStart);
			editorPane.setSelectionEnd(selEnd);
			uncommentOutAction.actionPerformed(e);
			return;
		}

		// comment out the text
		String commentText = "#" + selectedText;
		// increment selection end to accommodate added char
		selStart++;
		selEnd++;
		int nl = commentText.indexOf('\n');
		while (nl >= 0 && nl + 1 < commentText.length()) {
			commentText = commentText.substring(0, nl + 1) + "#" + commentText.substring(nl + 1);
			nl = commentText.indexOf('\n', nl + 1);

			//increment selection end to accommodate added char
			selEnd++;
		}

		try(CustomUndoManager.CompoundModeManager ignored = undoManager.compoundMode()) {
			EditingUtils.replaceRange(editorPane.getDocument(), commentText, editorPane.getSelectionStart(),
				editorPane.getSelectionEnd());
		} catch(Exception exception) {
			// should never happen, as CompoundManager doesn't throw any exceptions
			exception.printStackTrace();
		}

		//restore the selection
		editorPane.setSelectionStart(selStart);
		editorPane.setSelectionEnd(selEnd);

	}//actionPerformed
}//class CommentOutAction
