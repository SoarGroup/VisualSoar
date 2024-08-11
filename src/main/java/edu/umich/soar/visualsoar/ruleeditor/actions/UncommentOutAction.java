package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.ruleeditor.EditingUtils;

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
	private final Toolkit toolkit;
	private final JTextComponent editorPane;

	public UncommentOutAction(JTextComponent editorPane, Toolkit toolkit) {
		super("Uncomment Out");
		this.toolkit = toolkit;
		this.editorPane = editorPane;
	}

//	TODO: doesn't uncomment current line if beginning is not selected
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
				selEnd--;
			}
			int nlp = uncommentText.indexOf("\n#");
			while (nlp != -1) {
				uncommentText = uncommentText.substring(0, nlp + 1) + uncommentText.substring(nlp + 2);
				nlp = uncommentText.indexOf("\n#", nlp + 1);

//decrease the selection range to accommodate missing char
				selEnd--;
			}

			EditingUtils.replaceRange(editorPane.getDocument(), uncommentText, editorPane.getSelectionStart(),
				editorPane.getSelectionEnd());

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
