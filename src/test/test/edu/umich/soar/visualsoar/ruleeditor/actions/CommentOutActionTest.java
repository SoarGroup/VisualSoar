package edu.umich.soar.visualsoar.ruleeditor.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

import edu.umich.soar.visualsoar.ruleeditor.EditorPane;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditorUndoManager;
import edu.umich.soar.visualsoar.util.BooleanProperty;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.StringReader;
import javax.swing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class CommentOutActionTest {
  @Mock RuleEditorUndoManager undoManager;
  @Mock UncommentOutAction uncommentOutAction;
  @Mock ActionEvent actionEvent;

  @BeforeEach
  public void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void uncommentsIfAlreadyCommented() {
    JEditorPane editorPane = new JEditorPane();
    CommentOutAction commentOutAction =
        new CommentOutAction(editorPane, undoManager, uncommentOutAction);
    editorPane.setText("#hello\n#hello\n#hello");
    editorPane.selectAll();
    commentOutAction.actionPerformed(actionEvent);
    Mockito.verify(uncommentOutAction, times(1)).actionPerformed(actionEvent);
  }

  @Test
  public void commentsIfAnyLinesNotAlreadyCommented() {
    JEditorPane editorPane = new JEditorPane();
    CommentOutAction commentOutAction =
        new CommentOutAction(editorPane, undoManager, uncommentOutAction);
    editorPane.setText("#hello\nhello\n#hello");
    editorPane.selectAll();
    commentOutAction.actionPerformed(actionEvent);
    assertEquals(
        "##hello\n#hello\n##hello", editorPane.getText(), "All lines should have been commented");
    // notice that the first # char is not selected! It should not be.
    assertEquals(
        "#hello\n#hello\n##hello",
        editorPane.getSelectedText(),
        "All original text should still be selected");
  }

  @Test
  public void selectionEmptyAndCursorOnEmptyLine() {
    JEditorPane editorPane = new JEditorPane();
    CommentOutAction commentOutAction =
        new CommentOutAction(editorPane, undoManager, uncommentOutAction);
    editorPane.setText("hello\nhello\n\nhello");
    // place cursor on empty line with no selection
    editorPane.setSelectionStart(12);
    editorPane.setSelectionEnd(12);
    commentOutAction.actionPerformed(actionEvent);
    assertEquals("hello\nhello\n\nhello", editorPane.getText(), "No changes should have been made");
  }

  @Test
  public void commentsFirstLineWithCursorAtBeginning() {
    JEditorPane editorPane = new JEditorPane();
    CommentOutAction commentOutAction =
        new CommentOutAction(editorPane, undoManager, uncommentOutAction);
    editorPane.setText("hello\nhello\nhello");
    editorPane.setSelectionStart(0);
    editorPane.setSelectionEnd(0);
    commentOutAction.actionPerformed(actionEvent);
    assertEquals(
        "#hello\nhello\nhello",
        editorPane.getText(),
        "Cursor at beginning of first line; should comment first line");
    // check that cursor location is correct
    assertEquals(
        1,
        editorPane.getSelectionStart(),
        "Cursor location should be moved due to comment character");
  }

  @Test
  public void commentsFirstLineWithSomeTextSelected() {
    JEditorPane editorPane = new JEditorPane();
    CommentOutAction commentOutAction =
        new CommentOutAction(editorPane, undoManager, uncommentOutAction);
    editorPane.setText("hello\nhello\nhello");
    editorPane.setSelectionStart(0);
    editorPane.setSelectionEnd(3);
    commentOutAction.actionPerformed(actionEvent);
    assertEquals(
        "#hello\nhello\nhello",
        editorPane.getText(),
        "Some text selected; should comment first line");
    assertEquals(
        "hel",
        editorPane.getSelectedText(),
        "selection should be updated to correspond to original selection");
  }

  @Test
  public void commentsSubsequentLines() {
    JEditorPane editorPane = new JEditorPane();
    CommentOutAction commentOutAction =
        new CommentOutAction(editorPane, undoManager, uncommentOutAction);
    editorPane.setText("hello\nhi\n\nこんにちは\nbonjour\n\nwhat's up\nhello");
    // middle of "hi" through middle of "what's up
    editorPane.setSelectionStart(7);
    editorPane.setSelectionEnd(30);
    commentOutAction.actionPerformed(actionEvent);
    assertEquals(
        "hello\n#hi\n#\n#こんにちは\n#bonjour\n#\n#what's up\nhello",
        editorPane.getText(),
        "Should comment all selected lines");
    assertEquals(
        editorPane.getSelectedText(),
        "i\n#\n#こんにちは\n#bonjour\n#\n#what'",
        "selection should be updated to correspond to original selection");
  }

  @Test
  public void doesntCommentLastLineIfNoCharsSelected() {
    JEditorPane editorPane = new JEditorPane();
    CommentOutAction commentOutAction =
        new CommentOutAction(editorPane, undoManager, uncommentOutAction);
    editorPane.setText("hello\nhi\nbonjour\n\nwhat's up\n");
    // middle of "hi" through beggining of "what's up" line
    editorPane.setSelectionStart(7);
    editorPane.setSelectionEnd(18);
    commentOutAction.actionPerformed(actionEvent);
    assertEquals(
        "hello\n#hi\n#bonjour\n#\nwhat's up\n",
        editorPane.getText(),
        "Should comment all lines with characters selected");
    assertEquals(
        editorPane.getSelectedText(),
        "i\n#bonjour\n#\n",
        "selection was updated to correspond to original selection");
  }

  @Test
  public void entireActionIsOneEdit() throws IOException {
    EditorPane editorPane = new EditorPane();
    RuleEditorUndoManager localUndoManager =
        new RuleEditorUndoManager(editorPane, new BooleanProperty(false));
    CommentOutAction commentOutAction =
        new CommentOutAction(editorPane, localUndoManager, uncommentOutAction);
    String originalText = "hello\nhi\nbonjour\n\nwhat's up\n";
    // the only way to get EditorPane to load a SoarDocument object is by calling read()
    editorPane.read(new StringReader(originalText));

    editorPane.getSoarDocument().addUndoableEditListener(localUndoManager);

    // middle of "hi" through middle of "what's up"
    editorPane.setSelectionStart(7);
    editorPane.setSelectionEnd(20);
    commentOutAction.actionPerformed(actionEvent);

    // I don't know a more direct way to test that there's only one edit in the manager,
    // so we'll just undo() to check that the text is back how it was originally
    assertTrue(
        editorPane.getText().contains("#"), "Comment chars should have been inserted somewhere");
    localUndoManager.undo();
    assertEquals(
        originalText, editorPane.getText(), "One undo() should put the text back as it was");
  }
}
