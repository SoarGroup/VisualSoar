package edu.umich.soar.visualsoar.ruleeditor;

import edu.umich.soar.visualsoar.misc.Prefs;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.TextUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * This is the EditorPane visual soar uses. It adds some functionality to make some actions nicer.
 */
public class EditorPane extends javax.swing.JEditorPane {
  private static final long serialVersionUID = 20221225L;

  private final JPopupMenu contextMenu = new JPopupMenu();

  // used for keeping track of when the cursor selection changes
  private int previousSelectionStart = 0;
  private int previousSelectionEnd = 0;
  // used for clearing previous highlights
  private Highlighter.HighlightPainter lastUsedOccurrenceHighlighter;

  private static Highlighter.HighlightPainter occurrenceHighlightPainter =
      new DefaultHighlighter.DefaultHighlightPainter(
          new Color(Prefs.currentSelectionOccurrenceHighlightColor.getInt()));

  /** Set the color used to highlight occurrences of the currently selected text */
  public static void setCurrentSelectionOccurrenceHighlightColor(Color c) {
    occurrenceHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(c);
  }

  protected void processKeyEvent(KeyEvent e) {
    super.processKeyEvent(e);
  }

  public EditorPane() {

    if (Prefs.highlightingEnabled.getBoolean()) {
      StyledEditorKit sek = new StyledEditorKit();
      setEditorKit(sek);
    }

    Keymap map = JTextComponent.addKeymap("Justify Keymap", getKeymap());

    KeyStroke tab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
    KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    KeyStroke circumflex = KeyStroke.getKeyStroke('^');
    KeyStroke leftParen = KeyStroke.getKeyStroke('(');
    KeyStroke rightBracket = KeyStroke.getKeyStroke('}');
    KeyStroke rightArrow = KeyStroke.getKeyStroke('>');

    Action justifyAction = new AutoJustifyAction();

    map.addActionForKeyStroke(tab, justifyAction);
    map.addActionForKeyStroke(circumflex, justifyAction);
    map.addActionForKeyStroke(leftParen, justifyAction);
    map.addActionForKeyStroke(enter, justifyAction);
    map.addActionForKeyStroke(rightBracket, justifyAction);
    map.addActionForKeyStroke(rightArrow, justifyAction);

    setKeymap(map);

    // Setup the context menu
    MouseListener popupListener = new PopupListener();
    addMouseListener(popupListener);

    // Whenever the selected text changes, highlight occurrences of that text
    addCaretListener(e -> updateCurrentSelectionOccurrenceHighlights());
    addMouseMotionListener(
        new MouseMotionAdapter() {
          @Override
          public void mouseDragged(MouseEvent e) {
            updateCurrentSelectionOccurrenceHighlights();
          }
        });
    lastUsedOccurrenceHighlighter = occurrenceHighlightPainter;
  }

  /**
   * Call whenever the cursor selection potentially changes. If the selection start/end changes and
   * text is selected, highlight all occurrences of the same string in the document.
   */
  private void updateCurrentSelectionOccurrenceHighlights() {
    int selectionStart = getSelectionStart();
    int selectionEnd = getSelectionEnd();
    if (selectionStart == previousSelectionStart && selectionEnd == previousSelectionEnd) {
      // nothing changed, so no need to update highlights
      return;
    }
    previousSelectionStart = selectionStart;
    previousSelectionEnd = selectionEnd;
    if (Prefs.enableCurrentSelectionOccurrenceHighlighting.getBoolean()) {
      String selectedText = getSelectedText();
      highlightOccurrences(selectedText);
    }
  }

  /** Highlight all occurrences of {@code textToHighlight} in the document */
  private void highlightOccurrences(@Nullable String textToHighlight) {
    // Remove previous custom highlights
    clearSelectionOccurrenceHighlights();
    lastUsedOccurrenceHighlighter = occurrenceHighlightPainter;

    if (textToHighlight == null || textToHighlight.isEmpty()) {
      // Nothing to highlight
      return;
    }

    int selectionStart = getSelectionStart();

    try {
      Document doc = getDocument();
      String content = doc.getText(0, doc.getLength());
      int index = content.indexOf(textToHighlight);

      // Highlight all occurrences
      Highlighter highlighter = getHighlighter();
      while (index >= 0) {
        int end = index + textToHighlight.length();
        // don't highlight the current selection
        if (index != selectionStart) {
          highlighter.addHighlight(index, end, occurrenceHighlightPainter);
        }
        index = content.indexOf(textToHighlight, end);
      }
    } catch (BadLocationException e) {
      e.printStackTrace();
      clearSelectionOccurrenceHighlights();
    }
  }

  /** Clear highlights added by {@link #updateCurrentSelectionOccurrenceHighlights} */
  private void clearSelectionOccurrenceHighlights() {
    Highlighter highlighter = getHighlighter();
    Highlighter.Highlight[] highlights = highlighter.getHighlights();

    // and remove all highlights that did not originate with it
    for (Highlighter.Highlight highlight : highlights) {
      if (highlight.getPainter() == lastUsedOccurrenceHighlighter) {
        highlighter.removeHighlight(highlight);
      }
    }
  }

  public JPopupMenu getContextMenu() {
    return contextMenu;
  }

  /**
   * Only safe to do after calling {@link #read(Reader in)}, where we set the document to a {@link
   * SoarDocument} instance.
   *
   * @return the underlying {@link SoarDocument}
   */
  public SoarDocument getSoarDocument() {
    return (SoarDocument) super.getDocument();
  }

  /**
   * changes current read-only status
   *
   * @param status read-only=true editable=false
   */
  public void setReadOnly(boolean status) {
    getSoarDocument().isReadOnly = status;
  }

  /** Watches for right clicks in order to pop up the context menu. */
  class PopupListener extends MouseAdapter {
    public void mouseReleased(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON3) {
        contextMenu.show(e.getComponent(), e.getX(), e.getY());
      }
    }
  } // class PopupListener

  public boolean getScrollableTracksViewportWidth() {
    if (getParent() instanceof JViewport) {
      JViewport port = (JViewport) getParent();
      TextUI ui = getUI();
      int w = port.getWidth();
      int wp = ui.getPreferredSize(this).width;
      return w > wp; // wrap
    }
    return false; // no wrap
  }

  /**
   * Stolen from JTextArea...
   *
   * @param line the line number to translate >= 0
   * @return the offset >= 0
   * @throws BadLocationException thrown if the line is less than zero or greater or equal to the
   *     number of lines contained in the document (as reported by getLineCount).
   * @see JTextArea
   */
  public int getLineStartOffset(int line) throws BadLocationException {
    return EditingUtils.getLineStartOffset(getDocument(), line);
  }

  /**
   * Stolen from JTextArea...
   *
   * @return the number of lines >= 0
   * @see JTextArea
   */
  public int getLineCount() {
    // There is an implicit break being modeled at the end of the
    // document to deal with boundry conditions at the end.  This
    // is not desired in the line count, so we detect it and remove
    // its effect if throwing off the count.
    Element map = getDocument().getDefaultRootElement();
    int n = map.getElementCount();
    Element lastLine = map.getElement(n - 1);
    if ((lastLine.getEndOffset() - lastLine.getStartOffset()) > 1) {
      return n;
    }
    return n - 1;
  }

  /** Returns the text of the specified line in the document */
  public String getLineText(int lineNum) throws BadLocationException {
    Document doc = getDocument();
    Element map = doc.getDefaultRootElement();
    int startOffset = getLineStartOffset(lineNum - 1);
    int endOffset = doc.getLength() - 1;

    if (lineNum + 1 < map.getElementCount()) {
      endOffset = getLineStartOffset(lineNum) - 1;
    }

    return doc.getText(startOffset, endOffset - startOffset);
  } // getLineText()

  /**
   * Stolen from JTextComponent...
   *
   * <p>Initializes the JEditorPane's document from a stream
   *
   * @param in The stream to read from
   * @throws IOException as thrown by the stream being used to initialize.
   * @see JTextComponent
   * @see JTextComponent#read
   * @see #setDocument
   */
  public void read(Reader in) throws IOException {
    EditorKit kit = getUI().getEditorKit(this);
    SoarDocument doc = new SoarDocument();

    try {
      kit.read(in, doc, 0);
      setDocument(doc);
    } catch (BadLocationException e) {
      throw new IOException(e.getMessage());
    }
  }

  @Override
  public void removeNotify() {
    super.removeNotify();

    // perform document cleanup
    SoarDocument doc = getSoarDocument();
    if (doc != null) {
      doc.close();
    }
  }

  /** Colors the syntax of the whole document */
  public void colorSyntax() {
    getSoarDocument().colorSyntax(new StringReader(getText()));
  }

  /** Auto Justifies the selected area. If none selected, justified entire document */
  public void justifyDocument() {
    getSoarDocument().justifyDocument(getSelectionStart(), getSelectionEnd());
  }

  class AutoJustifyAction extends AbstractAction {
    private static final long serialVersionUID = 20221225L;

    public void actionPerformed(ActionEvent e) {
      SoarDocument doc = getSoarDocument();

      String textTyped = e.getActionCommand();

      int caretPos = getCaretPosition();

      if (textTyped.equals("\n")) {
        (new DefaultEditorKit.InsertBreakAction()).actionPerformed(e);
        caretPos++;
      } else if (!textTyped.equals("\t")) {
        (new DefaultEditorKit.DefaultKeyTypedAction()).actionPerformed(e);
        caretPos++;
      }

      caretPos = doc.autoJustify(caretPos);

      if (caretPos > 0) {
        setCaretPosition(caretPos);
      }
    } // actionPerformed()
  } // class AutoJustifyAction
}
