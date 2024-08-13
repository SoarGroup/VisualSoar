package edu.umich.soar.visualsoar.ruleeditor.actions;

import static org.junit.jupiter.api.Assertions.*;

import edu.umich.soar.visualsoar.ruleeditor.CompoundUndoManager;
import edu.umich.soar.visualsoar.ruleeditor.EditorPane;
import edu.umich.soar.visualsoar.util.BooleanProperty;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.StringReader;
import javax.swing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class UncommentOutActionTest {
  @Mock
  CompoundUndoManager undoManager;
  @Mock ActionEvent actionEvent;

  @Mock Toolkit toolkit;

  @BeforeEach
  public void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void uncommentsFirstLine() {
    JEditorPane editorPane = new JEditorPane();
    UncommentOutAction uncommentOutAction =
        new UncommentOutAction(editorPane, undoManager, toolkit);
    editorPane.setText("#hello\nhello\nhello");
    editorPane.setSelectionStart(0);
    editorPane.setSelectionEnd(0);
    uncommentOutAction.actionPerformed(actionEvent);
    assertEquals(
        "hello\nhello\nhello", editorPane.getText(), "First line should have been uncommented");
    assertEquals(0, editorPane.getSelectionStart(), "Selection start should be unchanged");
    assertEquals(0, editorPane.getSelectionEnd(), "Selection end should be unchanged");
  }

  @Test
  public void uncommentsSubsequentLines() {
    JEditorPane editorPane = new JEditorPane();
    UncommentOutAction uncommentOutAction =
        new UncommentOutAction(editorPane, undoManager, toolkit);
    editorPane.setText("#hello\n#hello\n#hello");
    // middle of first through middle of last line
    editorPane.setSelectionStart(3);
    editorPane.setSelectionEnd(17);
    uncommentOutAction.actionPerformed(actionEvent);
    assertEquals(
        "hello\nhello\nhello", editorPane.getText(), "All lines should have been uncommented");
    assertEquals(2, editorPane.getSelectionStart(), "Selection start should be decremented");
    assertEquals(
        14,
        editorPane.getSelectionEnd(),
        "Selection end should be decremented once per removed comment char");
  }

  @Test
  public void doesntChangeSelectionStartIfWasBeforeFirstCommentChar() {
    JEditorPane editorPane = new JEditorPane();
    UncommentOutAction uncommentOutAction =
        new UncommentOutAction(editorPane, undoManager, toolkit);
    editorPane.setText("#hello\n#hello\n#hello");
    // beginning of second through middle of last line
    editorPane.setSelectionStart(7);
    editorPane.setSelectionEnd(17);
    uncommentOutAction.actionPerformed(actionEvent);
    assertEquals(
        "#hello\nhello\nhello",
        editorPane.getText(),
        "Second and third lines should have been uncommented");
    assertEquals(7, editorPane.getSelectionStart(), "Selection start should be decremented");
    assertEquals(
        15,
        editorPane.getSelectionEnd(),
        "Selection end should be decremented once per removed comment char");
  }

  @Test
  public void uncommentsIfAlreadyCommented() {
    JEditorPane editorPane = new JEditorPane();
    UncommentOutAction uncommentOutAction =
        new UncommentOutAction(editorPane, undoManager, toolkit);
    editorPane.setText("#hello\nhello\n#hello");
    editorPane.selectAll();
    uncommentOutAction.actionPerformed(actionEvent);
    assertEquals(
        "hello\nhello\nhello",
        editorPane.getText(),
        "First and last lines should have been uncommented");
    assertEquals(0, editorPane.getSelectionStart(), "Selection start should be unchanged");
    assertEquals(
        editorPane.getText().length(),
        editorPane.getSelectionEnd(),
        "Selection end should be at end of text");
  }

  @Test
  public void doesntUncommentLastLineIfNoCharsSelected() {
    JEditorPane editorPane = new JEditorPane();
    UncommentOutAction uncommentOutAction =
        new UncommentOutAction(editorPane, undoManager, toolkit);
    editorPane.setText("#hello\n#hello\n#hello");
    editorPane.setSelectionStart(0);
    editorPane.setSelectionEnd(14);
    uncommentOutAction.actionPerformed(actionEvent);
    assertEquals(
        "hello\nhello\n#hello",
        editorPane.getText(),
        "Only first line should have been uncommented");
    assertEquals(
        "hello\nhello\n",
        editorPane.getSelectedText(),
        "selection should be updated to correspond to original selection");
  }

  @Test
  public void doesntAlterEmptyLines() {
    JEditorPane editorPane = new JEditorPane();
    UncommentOutAction uncommentOutAction =
        new UncommentOutAction(editorPane, undoManager, toolkit);
    editorPane.setText("#hello\n\n#hello\n");
    // beginning through middle of third line
    editorPane.setSelectionStart(0);
    editorPane.setSelectionEnd(10);
    uncommentOutAction.actionPerformed(actionEvent);
    assertEquals(
        "hello\n\nhello\n",
        editorPane.getText(),
        "Only first and last lines should have been uncommented");
    assertEquals(
        "hello\n\nh",
        editorPane.getSelectedText(),
        "selection should be updated to correspond to original selection");
  }

  @Test
  public void selectionEmptyAndCursorOnEmptyLine() {
    JEditorPane editorPane = new JEditorPane();
    UncommentOutAction uncommentOutAction =
        new UncommentOutAction(editorPane, undoManager, toolkit);
    String originalText = "hello\nhello\n\nhello";
    editorPane.setText(originalText);
    // place cursor on empty line with no selection
    editorPane.setSelectionStart(12);
    editorPane.setSelectionEnd(12);
    uncommentOutAction.actionPerformed(actionEvent);
    assertEquals(originalText, editorPane.getText(), "No changes should have been made");
  }

  @Test
  public void entireActionIsOneEdit() throws IOException {
    EditorPane editorPane = new EditorPane();
    CompoundUndoManager localUndoManager =
        new CompoundUndoManager(editorPane, new BooleanProperty(false));
    UncommentOutAction uncommentOutAction =
        new UncommentOutAction(editorPane, localUndoManager, toolkit);
    String originalText = "#hello\n#hi\nbonjour\n\n#what's up\n";
    // the only way to get EditorPane to load a SoarDocument object is by calling read()
    editorPane.read(new StringReader(originalText));

    editorPane.getSoarDocument().addUndoableEditListener(localUndoManager);

    // middle of "hello" through middle of "what's up"
    editorPane.setSelectionStart(4);
    editorPane.setSelectionEnd(23);
    uncommentOutAction.actionPerformed(actionEvent);

    // I don't know a more direct way to test that there's only one edit in the manager,
    // so we'll just undo() to check that the text is back how it was originally
    assertFalse(
        editorPane.getText().contains("#"),
        "Comment chars should have been removed everywhere. Text was: " + editorPane.getText());
    localUndoManager.undo();
    assertEquals(
        originalText, editorPane.getText(), "One undo() should put the text back as it was");
  }
}
