package edu.umich.soar.visualsoar.ruleeditor;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

public class EditingUtils {

	/**
	 * Stolen from JTextArea...
	 *
	 * @param line the line number to translate >= 0
	 * @return the offset >= 0
	 * @throws BadLocationException thrown if the line is
	 *                              less than zero or greater or equal to the number of
	 *                              lines contained in the document (as reported by
	 *                              getLineCount).
	 * @see JTextArea
	 */
	public static int getLineStartOffset(Document doc, int line) throws BadLocationException {
		Element map = doc.getDefaultRootElement();
		if (line < 0) {
			throw new BadLocationException("Negative line", -1);
		} else if (line >= map.getElementCount()) {
			throw new BadLocationException("No such line", doc.getLength() + 1);
		} else {
			Element lineElem = map.getElement(line);
			return lineElem.getStartOffset();
		}
	}

	/**
	 * Stolen from JTextArea...
	 *
	 * @param offset the offset >= 0
	 * @return the line number >= 0
	 * @throws BadLocationException thrown if the offset is
	 *                              less than zero or greater than the document length.
	 * @see JTextArea
	 */
	public static int getLineOfOffset(Document doc, int offset) throws BadLocationException {
		if (offset < 0) {
			throw new BadLocationException("Can't translate offset to line", -1);
		} else if (offset > doc.getLength()) {
			throw new BadLocationException("Can't translate offset to line", doc.getLength() + 1);
		} else {
			Element map = doc.getDefaultRootElement();
			return map.getElementIndex(offset);
		}
	}

	/**
	 * sets the selection to the current line
	 *
	 * @author Andrew Nuxoll
	 * @version 22 Sep 2022
	 */
	public static void selectCurrLine(JTextComponent textComponent) throws BadLocationException {
		Document doc = textComponent.getDocument();
		int lineNum = 1 + getLineOfOffset(doc, textComponent.getCaretPosition());
		Element map = doc.getDefaultRootElement();
		int startOffset = getLineStartOffset(doc, lineNum - 1);
		int endOffset = doc.getLength() - 1;

		if (lineNum + 1 < map.getElementCount()) {
			endOffset = getLineStartOffset(doc, lineNum) - 1;
		}

		textComponent.setSelectionStart(startOffset);
		textComponent.setSelectionEnd(endOffset);
	}

	/**
	 * Expands the current selection to include only entire lines
	 * If nothing is currently selected, then the current line is
	 * selected.
	 * @param setStart true if start of selection should be expanded backwards to include the entire line
	 * @param setEnd true if end of selection should be expanded backwards to include the entire line
	 */
	public static void expandSelectionToEntireLines(JTextComponent textComponent, boolean setStart, boolean setEnd)
			throws BadLocationException {
		String selectedText = textComponent.getSelectedText();
		if ((selectedText == null) || (selectedText.length() == 0)) {
			selectCurrLine(textComponent);
			return;
		}

		Document doc = textComponent.getDocument();
		Element map = doc.getDefaultRootElement();
		//set new selection start position
		if (setStart) {
			int lineNum = 1 + getLineOfOffset(doc, textComponent.getSelectionStart());
			int startOffset = getLineStartOffset(doc, lineNum - 1);
			textComponent.setSelectionStart(startOffset);
		}

		//set new selection end position
		if(setEnd) {
			int lineNum = 1 + getLineOfOffset(doc, textComponent.getSelectionEnd());
			int endOffset = doc.getLength() - 1;
			if (lineNum + 1 < map.getElementCount()) {
				endOffset = getLineStartOffset(doc, lineNum) - 1;
			}
			textComponent.setSelectionEnd(endOffset);
		}

	}


	/**
	 * Stolen from JTextArea...
	 *
	 * @param str the text to insert
	 * @param pos the position at which to insert >= 0
	 * @throws IllegalArgumentException if pos is an
	 *                                  invalid position in the model
	 * @see JTextArea
	 * @see java.awt.TextComponent#setText
	 */
	public static void insert(Document doc, String str, int pos) {
		if (doc != null) {
			try {
				doc.insertString(pos, str, null);
			} catch (BadLocationException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		}
	}

	/**
	 * Stolen from JTextArea...
	 *
	 * @param str   the text to use as the replacement
	 * @param start the start position >= 0
	 * @param end   the end position >= start
	 * @throws IllegalArgumentException if part of the range is an
	 *                                  invalid position in the model
	 * @see JTextArea
	 * @see #insert
	 * @see #replaceRange
	 */
	public static void replaceRange(Document doc, String str, int start, int end) {
		if (end < start) {
			throw new IllegalArgumentException("end before start");
		}

		if (doc != null) {
			try {
				doc.remove(start, end - start);
				doc.insertString(start, str, null);
			} catch (BadLocationException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		}
	}

}
