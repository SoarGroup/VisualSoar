package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.ruleeditor.EditorPane;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class UncommentOutActionTest {
	@Mock
	EditorPane editorPane;
	//	TODO: Use mocks to write tests in terms of replaceRange and selection start/end
	// test replaceRange separately

	@Test
	public void uncommentsFirstLine() {
//		JTextComponent text = new JTextComponent();
//		check different selection points
	}

	@Test
	public void uncommentsSubsequentLines() {
		// check different selection points
	}

	@Test
	public void restoresSelectionAfterUncomment() {

	}

	@Test
	public void entireActionIsOneEdit() {

	}

	@Test
	public void doesntChangeTextThatIsntCommented() {

	}
}
