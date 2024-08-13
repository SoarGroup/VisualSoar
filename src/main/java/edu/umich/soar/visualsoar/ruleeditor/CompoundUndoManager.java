// Based on code from Rob Camick:
// https://github.com/tips4java/tips4java/blob/main/source/CompoundUndoManager.java
// Original license:
// MIT License
//
// 	Copyright (c) 2023 Rob Camick
//
// 	Permission is hereby granted, free of charge, to any person obtaining a copy
// 	of this software and associated documentation files (the "Software"), to deal
// 	in the Software without restriction, including without limitation the rights
// 	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// 	copies of the Software, and to permit persons to whom the Software is
// 	furnished to do so, subject to the following conditions:
//
// 	The above copyright notice and this permission notice shall be included in all
// 	copies or substantial portions of the Software.
//
// 	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// 	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// 	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// 	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// 	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// 	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// 	SOFTWARE.
package edu.umich.soar.visualsoar.ruleeditor;

import edu.umich.soar.visualsoar.util.BooleanProperty;
import java.awt.event.*;
import java.util.Objects;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

/**
 * This class will merge individual edits into a single larger edit based on some heuristics:
 * incremental edits (e.g. typing) are merged together except where an element of {@link
 * #BREAK_CHARS} is involved, and saving the document or changing between insertion/deletion will
 * start new edits.
 */
public class CompoundUndoManager extends UndoManager
    implements UndoableEditListener, DocumentListener {

  /**
   * For the purpose of creating compound edits, we consider these characters to be significant
   * breaking points. They are ends of words/lines/phrases and Soar coding element.
   */
  private static final char[] BREAK_CHARS = {' ', '.', '\n', '\t', '{', '}', '(', ')', '^', '*'};

  private final UndoManager undoManager;
  private final BooleanProperty lastActionWasSave;
  private CompoundEdit compoundEdit;
  private final JTextComponent textComponent;
  private final UndoAction undoAction;
  private final RedoAction redoAction;

  //  These fields are used to help determine whether the edit is an
  //  incremental edit. The offset and length should increase by 1 for
  //  each character added or decrease by 1 for each character removed.

  private int previousOffset;
  private int previousLength;
  private Character previousLastCharInDocument;
  private boolean previousWasInsert;

  private boolean inAtomicEdit;

  /**
   * @see #atomicMode()
   */
  public class AtomicModeManager implements AutoCloseable {
    private AtomicModeManager() {
      endCompoundEdit();
      inAtomicEdit = true;
    }

    @Override
    public void close() throws Exception {
      if (!inAtomicEdit) {
        System.err.println(
            "WARNING: AtomicModeManager closed after undo manager "
                + "had already exited atomic mode");
      }
      endCompoundEdit();
    }
  }

  /**
   * When this class is in atomic mode, all edits are combined into one single edit. Using undo() or
   * redo() immediately closes atomic mode.
   *
   * @return A manager for atommic mode that is {@link AutoCloseable auto-closeable}, meaning that
   *     the client can restrict atomic mode to a specific code block using a try-with-resources
   *     statement. Atomic mode will end early if undo() or redo() are called.
   */
  public AtomicModeManager atomicMode() {
    return new AtomicModeManager();
  }

  public CompoundUndoManager(JTextComponent textComponent, BooleanProperty lastActionWasSave) {
    this.textComponent = textComponent;
    undoManager = this;
    undoAction = new UndoAction();
    redoAction = new RedoAction();
    textComponent.getDocument().addUndoableEditListener(this);
    this.lastActionWasSave = lastActionWasSave;
  }

  /**
   * Add a DocumentLister before the undo is done so we can position the Caret correctly as each
   * edit is undone.
   */
  public void undo() {
    textComponent.getDocument().addDocumentListener(this);
    super.undo();
    textComponent.getDocument().removeDocumentListener(this);

    // If the user clears the undo queue, treat the buffer as if
    // it has just been saved
    if (!this.canUndo()) {
      lastActionWasSave.set(true);
    }
  }

  /**
   * Add a DocumentLister before the redo is done so we can position the Caret correctly as each
   * edit is redone.
   */
  public void redo() {
    textComponent.getDocument().addDocumentListener(this);
    super.redo();
    textComponent.getDocument().removeDocumentListener(this);
  }

  /**
   * Whenever an UndoableEdit happens the edit will either be absorbed by the current compound edit
   * or a new compound edit will be started
   */
  @Override
  public void undoableEditHappened(UndoableEditEvent e) {
    UndoableEdit ue = e.getEdit();
    // System.out.println(ue.getPresentationName());

    boolean shouldAddToExistingEdit = shouldAddToExistingEdit(ue);

    updatePreviousEditInfo();
    if (shouldAddToExistingEdit) {
      compoundEdit.addEdit(e.getEdit());
      return;
    }

    //  Not incremental edit; end previous edit and start a new one

    if (!inAtomicEdit) {
      endCompoundEdit();
    }
    startCompoundEdit(e.getEdit());
  }

  private boolean shouldAddToExistingEdit(UndoableEdit ue) {
    if (compoundEdit == null) {
      // impossible to add to one because none exists
      return false;
    }
    // honor atomic guarantee if requested by client
    if (inAtomicEdit) {
      return true;
    }

    // after save, we start a new edit
    if (lastActionWasSave.get()) {
      return false;
    }

    // start new edit if user switches between insertion/deletion
    if (editingSwitchedBetweenInsertAndDelete()) {
      return false;
    }

    return (isStyleChange(ue) || isIncrementalEditOrBackspace());
  }

  private void updatePreviousEditInfo() {
    // if doc length doesn't change, then it doesn't count as insertion/deletion
    if (docLengthChanged()) {
      previousWasInsert = wasInsert();
    }
    previousOffset = textComponent.getCaretPosition();
    previousLength = textComponent.getDocument().getLength();
    if (textComponent.getCaretPosition() != 0) {
      previousLastCharInDocument = textComponent.getText().charAt(previousOffset - 1);
    } else {
      previousLastCharInDocument = null;
    }
    lastActionWasSave.set(false);
  }

  private boolean editingSwitchedBetweenInsertAndDelete() {
    // System.out.println(docLengthChanged() && wasInsert() != previousWasInsert);
    return docLengthChanged() && wasInsert() != previousWasInsert;
  }

  private boolean docLengthChanged() {
    return textComponent.getDocument().getLength() != previousLength;
  }

  private boolean wasInsert() {
    return textComponent.getDocument().getLength() > previousLength;
  }

  private boolean isStyleChange(UndoableEdit ue) {
    return Objects.equals(ue.getPresentationName(), "style change");
  }

  /**
   * @return true if the user has typed or deleted a single char which is not a {@link
   *     #isBreakChar(Character) breaking character}
   */
  private boolean isIncrementalEditOrBackspace() {
    int caretPosition = textComponent.getCaretPosition();
    int docLength = textComponent.getDocument().getLength();
    int offsetChange = caretPosition - previousOffset;
    int lengthChange = docLength - previousLength;

    // The Change in Caret position and Document length should both be either 1 or -1
    if (offsetChange == lengthChange && Math.abs(offsetChange) == 1) {
      // ...and we can't have inserted or a break char...
      if (offsetChange == 1) {
        char insertedChar = textComponent.getText().charAt(caretPosition - 1);
        return !isBreakChar(insertedChar);
      }
      // ...or deleted a break char
      else {
        return !isBreakChar(previousLastCharInDocument);
      }
    }
    return false;
  }

  /**
   * @see #BREAK_CHARS
   */
  private boolean isBreakChar(Character c) {
    for (char breakChar : BREAK_CHARS) {
      if (Objects.equals(breakChar, c)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Each CompoundEdit will store a group of related incremental edits (ie. each character typed or
   * backspaced is an incremental edit)
   */
  private void startCompoundEdit(UndoableEdit anEdit) {
    //  Track Caret and Document information of this compound edit

    previousOffset = textComponent.getCaretPosition();
    previousLength = textComponent.getDocument().getLength();

    //  The compound edit is used to store incremental edits

    compoundEdit = new MyCompoundEdit();
    if (anEdit != null) {
      compoundEdit.addEdit(anEdit);
    }

    //  The compound edit is added to the UndoManager. All incremental
    //  edits stored in the compound edit will be undone/redone at once

    addEdit(compoundEdit);

    undoAction.updateUndoState();
    redoAction.updateRedoState();
  }

  private void endCompoundEdit() {
    if (compoundEdit != null) {
      compoundEdit.end();
      compoundEdit = null;
      inAtomicEdit = false;
    }
  }

  /**
   * The Action to Undo changes to the Document. The state of the Action is managed by the
   * CompoundUndoManager
   */
  public Action getUndoAction() {
    return undoAction;
  }

  /**
   * The Action to Redo changes to the Document. The state of the Action is managed by the
   * CompoundUndoManager
   */
  public Action getRedoAction() {
    return redoAction;
  }

  //
  //  Implement DocumentListener
  //
  /** Updates to the Document as a result of Undo/Redo will cause the Caret to be repositioned */
  @Override
  public void insertUpdate(final DocumentEvent e) {
    SwingUtilities.invokeLater(
        new Runnable() {
          public void run() {
            int offset = e.getOffset() + e.getLength();
            offset = Math.min(offset, textComponent.getDocument().getLength());
            textComponent.setCaretPosition(offset);
          }
        });
  }

  @Override
  public void removeUpdate(DocumentEvent e) {
    textComponent.setCaretPosition(e.getOffset());
  }

  @Override
  public void changedUpdate(DocumentEvent e) {}

  @Override
  public String toString() {
    return "Compound" + super.toString();
  }

  class MyCompoundEdit extends CompoundEdit {
    public boolean isInProgress() {
      //  in order for the canUndo() and canRedo() methods to work
      //  assume that the compound edit is never in progress

      return false;
    }

    public void undo() throws CannotUndoException {
      //  End the edit so future edits don't get absorbed by this edit
      endCompoundEdit();
      super.undo();
    }
  }

  /** Perform the Undo and update the state of the undo/redo Actions */
  class UndoAction extends AbstractAction {
    public UndoAction() {
      putValue(Action.NAME, "Undo");
      putValue(Action.SHORT_DESCRIPTION, getValue(Action.NAME));
      putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control Z"));
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
      try {
        undoManager.undo();
        textComponent.requestFocusInWindow();
      } catch (CannotUndoException ignored) {
      }

      updateUndoState();
      redoAction.updateRedoState();
    }

    private void updateUndoState() {
      setEnabled(undoManager.canUndo());
    }
  }

  /** Perform the Redo and update the state of the undo/redo Actions */
  class RedoAction extends AbstractAction {
    public RedoAction() {
      putValue(Action.NAME, "Redo");
      putValue(Action.SHORT_DESCRIPTION, getValue(Action.NAME));
      putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("Control Y"));
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
      try {
        undoManager.redo();
        textComponent.requestFocusInWindow();
      } catch (CannotRedoException ignored) {
      }

      updateRedoState();
      undoAction.updateUndoState();
    }

    protected void updateRedoState() {
      setEnabled(undoManager.canRedo());
    }
  }
}
