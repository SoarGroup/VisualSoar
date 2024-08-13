package edu.umich.soar.visualsoar.ruleeditor;

import static org.junit.jupiter.api.Assertions.*;

import edu.umich.soar.visualsoar.util.BooleanProperty;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

class CompoundUndoManagerTest {
  @BeforeEach
  public void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void sequentialInsertionsAreJoined() throws BadLocationException {
    JEditorPane editorPane = new JEditorPane();
    Document doc = editorPane.getDocument();
    CompoundUndoManager undoManager =
        new CompoundUndoManager(editorPane, new BooleanProperty(true));
    String inputString = "123456789";
    for (char c : inputString.toCharArray()) {
      insertOneChar(editorPane, c);
    }
    assertEquals(
        inputString, editorPane.getText(), "Should have inserted each character in the document");
    undoManager.undo();
    assertEquals("", editorPane.getText(), "Undo should remove all sequential insertions");
    undoManager.redo();
    assertEquals(inputString, editorPane.getText(), "Redo should restore entire input string");
  }

  @Test
  public void sequentialDeletionsAreJoined() throws BadLocationException {
    JEditorPane editorPane = new JEditorPane();
    Document doc = editorPane.getDocument();
    CompoundUndoManager undoManager =
        new CompoundUndoManager(editorPane, new BooleanProperty(true));
    String inputString = "123456789";
    editorPane.setText(inputString);
    editorPane.setCaretPosition(inputString.length() - 1);
    for (int i = 0; i < inputString.length(); i++) {
      deleteOneChar(editorPane);
    }
    assertEquals("", editorPane.getText(), "Sequential deletes should have removed all characters");
    undoManager.undo();
    assertEquals(inputString, editorPane.getText(), "Undo should restore entire input string");
    undoManager.redo();
    assertEquals("", editorPane.getText(), "Redo should delete entire input string again");
  }

  @Test
  public void allEditsInAtomicModeAreJoined() throws Exception {
    JEditorPane editorPane = new JEditorPane();
    Document doc = editorPane.getDocument();
    CompoundUndoManager undoManager =
        new CompoundUndoManager(editorPane, new BooleanProperty(true));

    String inputString = "hello there";
    doc.insertString(0, inputString, new SimpleAttributeSet());
    try (CompoundUndoManager.AtomicModeManager ignored = undoManager.atomicMode()) {
      doc.insertString(3, "xxxxxxxxxx", new SimpleAttributeSet());
      doc.remove(0, 12);
    }
    assertEquals("xlo there", editorPane.getText(), "Confirming added text contents");
    undoManager.undo();
    assertEquals(
        inputString, editorPane.getText(), "undo should apply to both edits in the atomic context");
    undoManager.redo();
    assertEquals("xlo there", editorPane.getText(), "redo should apply both edits again");
  }

  @Test
  public void sequentialInsertionsAndDeletionsAreSeparated() throws BadLocationException {
    JEditorPane editorPane = new JEditorPane();
    CompoundUndoManager undoManager =
        new CompoundUndoManager(editorPane, new BooleanProperty(true));

    editorPane.setText("hello world!");
    editorPane.setCaretPosition(5);
    insertOneChar(editorPane, 'a');
    insertOneChar(editorPane, 'b');
    insertOneChar(editorPane, 'c');
    editorPane.setCaretPosition(editorPane.getCaretPosition() - 1);
    deleteOneChar(editorPane);
    deleteOneChar(editorPane);
    insertOneChar(editorPane, '1');
    insertOneChar(editorPane, '2');
    editorPane.setCaretPosition(editorPane.getCaretPosition() - 1);
    deleteOneChar(editorPane);
    deleteOneChar(editorPane);

    assertEquals("helloa world!", editorPane.getText(), "Confirming fully-entered text");
    undoManager.undo();
    assertEquals("hello12a world!", editorPane.getText(), "First undo");
    undoManager.undo();
    assertEquals("helloa world!", editorPane.getText(), "Second undo");
    undoManager.undo();
    assertEquals("helloabc world!", editorPane.getText(), "Third undo");
    undoManager.undo();
    assertEquals("hello world!", editorPane.getText(), "Fourth undo");

    undoManager.redo();
    assertEquals("helloabc world!", editorPane.getText(), "Redo fourth undo");
    undoManager.redo();
    assertEquals("helloa world!", editorPane.getText(), "Redo third undo");
    undoManager.redo();
    assertEquals("hello12a world!", editorPane.getText(), "Redo second undo");
    undoManager.redo();
    assertEquals("helloa world!", editorPane.getText(), "Redo first undo");
    assertFalse(undoManager.canRedo(), "Should be out of possible redos");
  }

  @Test
  public void startNewEditAfterDocumentSave() throws BadLocationException {
	  JEditorPane editorPane = new JEditorPane();
	  BooleanProperty lastActionWasSave = new BooleanProperty(false);
	  CompoundUndoManager undoManager =
		  new CompoundUndoManager(editorPane, lastActionWasSave);

	  editorPane.setText("hello world!");
	  editorPane.setCaretPosition(5);
	  insertOneChar(editorPane, 'a');
	  insertOneChar(editorPane, 'b');
	  insertOneChar(editorPane, 'c');
	  lastActionWasSave.set(true);
	  insertOneChar(editorPane, '1');
	  assertFalse(lastActionWasSave.get(), "lastActionWasSave should be set to false on any undoable actions");
	  insertOneChar(editorPane, '2');
	  insertOneChar(editorPane, '3');
	  lastActionWasSave.set(true);
	  insertOneChar(editorPane, 'x');
	  insertOneChar(editorPane, 'y');
	  insertOneChar(editorPane, 'z');
	  lastActionWasSave.set(true);

	  assertEquals("helloabc123xyz world!", editorPane.getText(), "Confirming fully-entered text");
	  undoManager.undo();
	  assertEquals("helloabc123 world!", editorPane.getText(), "First undo");
	  undoManager.undo();
	  assertEquals("helloabc world!", editorPane.getText(), "Second undo");
	  undoManager.undo();
	  assertEquals("hello world!", editorPane.getText(), "Third undo");

	  undoManager.redo();
	  assertEquals("helloabc world!", editorPane.getText(), "Redo third undo");
	  undoManager.redo();
	  assertEquals("helloabc123 world!", editorPane.getText(), "Redo second undo");
	  undoManager.redo();
	  assertEquals("helloabc123xyz world!", editorPane.getText(), "Redo first undo");
  }

  @Test
  public void styleChangesAreJoined() {
	  // TODO: don't know how to add style change events
  }

  //////////////// Util methods ///////////////////

  private static void insertOneChar(JEditorPane editorPane, char c) throws BadLocationException {
    editorPane
        .getDocument()
        .insertString(editorPane.getCaretPosition(), String.valueOf(c), new SimpleAttributeSet());
    editorPane.setCaretPosition(editorPane.getCaretPosition() + 1);
  }

  private static void deleteOneChar(JEditorPane editorPane) throws BadLocationException {
    editorPane.getDocument().remove(editorPane.getCaretPosition(), 1);
    editorPane.setCaretPosition(Math.max(editorPane.getCaretPosition() - 1, 0));
  }
}
