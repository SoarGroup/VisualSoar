package edu.umich.soar.visualsoar.ruleeditor.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;

import edu.umich.soar.visualsoar.ruleeditor.RuleEditorUndoManager;
import java.awt.event.ActionEvent;
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
        editorPane.getText(), "##hello\n#hello\n##hello", "All lines should have been commented");
    // notice that the first # char is not selected! It should not be.
    assertEquals(
        editorPane.getSelectedText(),
        "#hello\n#hello\n##hello",
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
    assertEquals(editorPane.getText(), "hello\nhello\n\nhello", "No changes should have been made");
  }

  @Test
  public void commentsFirstLine() {
    //		check different points of selection
  }

  @Test
  public void commentsSubsequentLines() {}

  @Test
  public void doesntCommentLastLineIfNoCharsSelected() {}

  @Test
  public void restoresSelectionAfterCommenting() {}

  @Test
  public void entireActionIsOneEdit() {}
}
