package edu.umich.soar.visualsoar.ruleeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import edu.umich.soar.visualsoar.misc.Prefs;
import java.awt.Color;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EditorPaneTest {
  private EditorPane editorPane;

  @BeforeEach
  void setUp() {
    editorPane = new EditorPane();
    editorPane.setText("test test test");
  }

  public class TemporaryPrefsBooleanOverride implements AutoCloseable {
    private final Prefs prefs;
    private final boolean originalValue;

    public TemporaryPrefsBooleanOverride(Prefs prefs, boolean temporaryValue) {
      this.prefs = prefs;
      this.originalValue = prefs.getBoolean();
      prefs.setBoolean(temporaryValue);
    }

    @Override
    public void close() {
      prefs.setBoolean(originalValue); // Restore the original value
    }
  }

  @Test
  void testHighlightNoOccurrencesWhenSelectionEmpty() {
    try (TemporaryPrefsBooleanOverride ignore =
        new TemporaryPrefsBooleanOverride(
            Prefs.enableCurrentSelectionOccurrenceHighlighting, true)) {
      // move cursor but select nothing
      editorPane.select(3, 3);
    }

    Highlighter highlighter = editorPane.getHighlighter();
    Highlighter.Highlight[] highlights = highlighter.getHighlights();

    assertEquals(0, highlights.length);
  }

  @Test
  void testHighlightOccurrences() {
    try (TemporaryPrefsBooleanOverride ignore =
           new TemporaryPrefsBooleanOverride(
             Prefs.enableCurrentSelectionOccurrenceHighlighting, true)) {
      // Select "test"
      editorPane.select(0, 4);
    }

    Highlighter highlighter = editorPane.getHighlighter();
    Highlighter.Highlight[] highlights = highlighter.getHighlights();

    // We should highlight the two occurrences that are not the current selection
    assertEquals(2, highlights.length);

    assertEquals(5, highlights[0].getStartOffset());
    assertEquals(9, highlights[0].getEndOffset());

    assertEquals(10, highlights[1].getStartOffset());
    assertEquals(14, highlights[1].getEndOffset());

    // Verify that all occurrences are highlighted with correct highlighter
    for (Highlighter.Highlight highlight : highlights) {
      assertInstanceOf(DefaultHighlighter.DefaultHighlightPainter.class, highlight.getPainter());
      DefaultHighlighter.DefaultHighlightPainter dfhp =
        (DefaultHighlighter.DefaultHighlightPainter) highlight.getPainter();
      assertEquals(
        new Color(Prefs.currentSelectionOccurrenceHighlightColor.getInt()), dfhp.getColor());
    }
  }

  @Test
  void testHighlightOccurrencesDisabledInPrefs() {
    try (TemporaryPrefsBooleanOverride ignore =
           new TemporaryPrefsBooleanOverride(
             Prefs.enableCurrentSelectionOccurrenceHighlighting, false)) {
      // Select "test"
      editorPane.select(0, 4);
    }

    Highlighter highlighter = editorPane.getHighlighter();
    Highlighter.Highlight[] highlights = highlighter.getHighlights();

    // We should highlight the two occurrences that are not the current selection
    assertEquals(0, highlights.length);
  }

  @Test
  void testClearOccurrenceHighlights() {
    try (TemporaryPrefsBooleanOverride ignore =
        new TemporaryPrefsBooleanOverride(
            Prefs.enableCurrentSelectionOccurrenceHighlighting, true)) {
      // Select "test"
      editorPane.select(0, 4);
      // move cursor to select nothing
      editorPane.select(3, 3);
    }

    Highlighter highlighter = editorPane.getHighlighter();
    Highlighter.Highlight[] highlights = highlighter.getHighlights();

    // Verify that all custom highlights are cleared
    assertEquals(0, highlights.length);
  }
}
