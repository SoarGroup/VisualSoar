package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.ruleeditor.EditorPane;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class CommentOutActionTest {
	@Mock
	EditorPane editorPane;
	@Mock UncommentOutAction uncommentOutAction;

	@Test
	public void uncommentsIfAlreadyCommented() {

	}
	@Test
	public void commentsIfAnyLinesNotAlreadyCommented() {

	}

	@Test
	public void selectionEmptyAndCursorOnEmptyLine() {
//		should do nothing
	}

	@Test
	public void commentsFirstLine() {
//		check different points of selection
	}

	@Test
	public void commentsSubsequentLines() {

	}

	@Test
	public void doesntCommentLastLineIfNoCharsSelected() {

	}

	@Test
	public void restoresSelectionAfterCommenting() {

	}
}
