package edu.umich.soar.visualsoar.ruleeditor;

import edu.umich.soar.visualsoar.misc.Prefs;

import javax.swing.*;
import javax.swing.plaf.TextUI;
import javax.swing.text.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * This is the EditorPane visual soar uses, it adds some functionality to make some actions nicer,
 * it isn't especially relevant now
 */
public class EditorPane extends javax.swing.JEditorPane {
    private static final long serialVersionUID = 20221225L;

    private final JPopupMenu contextMenu;

    /**
     * @serial a reference to the DropTargetListener for Drag and Drop operations, may be deleted in future
     */
    private final DropTargetListener dtListener = new EPDropTargetListener();

    /**
     * @serial a reference to the DropTarget for Drag and Drop operations, may be deleted in future
     */


    class EPDropTargetListener implements DropTargetListener {
        public void dragEnter(DropTargetDragEvent dtde) {
        }

        public void dragExit(DropTargetEvent dte) {
        }

        public void dragOver(DropTargetDragEvent dtde) {
        }

        public void dropActionChanged(DropTargetDragEvent dtde) {
        }

        // The Drag operation has terminated with a Drop on this DropTarget
        public void drop(DropTargetDropEvent e) {
            // Get Trivial Data
            // Get a reference to the DataStructure we are modifying

            // Get the action
            int action = e.getDropAction();
            if (action != DnDConstants.ACTION_COPY) {
                e.rejectDrop();
                return;
            }


            // Validate
            System.out.println("Drop called!");
            DataFlavor[] flavors = e.getCurrentDataFlavors();
            if (e.isLocalTransfer() == false) {
                e.rejectDrop();
                return;
            }
            boolean found = false;
            for (int i = 0; i < flavors.length && !found; ++i) {
                if (flavors[i] == DataFlavor.stringFlavor) {
                    found = true;
                }
            }
            if (found) {
                e.acceptDrop(action);
            } else {
                e.rejectDrop();
                return;
            }

            DataFlavor chosen = DataFlavor.stringFlavor;
            // Get the data

            String data = null;

            try {
                data = (String) e.getTransferable().getTransferData(chosen);
            } catch (Throwable t) {
                t.printStackTrace();
                e.dropComplete(false);
                return;
            }

            insert(data, getCaretPosition());
            e.dropComplete(true);
        }
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

        //Setup the context menu
        contextMenu = new JPopupMenu();
        MouseListener popupListener = new PopupListener();
        addMouseListener(popupListener);

    }//EditorPane ctor

    public JPopupMenu getContextMenu() {
        return contextMenu;
    }

    /**
     * changes current read-only status
     *
     * @param status read-only=true  editable=false
     */
    public void setReadOnly(boolean status) {
        ((SoarDocument)getDocument()).isReadOnly = status;
    }

    /**
     * Watches for right clicks in order to pop up the context menu.
     */
    class PopupListener extends MouseAdapter {
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) {
                contextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }//class PopupListener


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
     * @param str the text to insert
     * @param pos the position at which to insert >= 0
     * @throws IllegalArgumentException if pos is an
     *                                  invalid position in the model
     * @see JTextArea
     * @see java.awt.TextComponent#setText
     */
    public void insert(String str, int pos) {
        Document doc = getDocument();
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
    public void replaceRange(String str, int start, int end) {
        if (end < start) {
            throw new IllegalArgumentException("end before start");
        }
        Document doc = getDocument();

        if (doc != null) {
            try {
                doc.remove(start, end - start);
                doc.insertString(start, str, null);
            } catch (BadLocationException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
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
    public int getLineOfOffset(int offset) throws BadLocationException {
        Document doc = getDocument();
        if (offset < 0) {
            throw new BadLocationException("Can't translate offset to line", -1);
        } else if (offset > doc.getLength()) {
            throw new BadLocationException("Can't translate offset to line", doc.getLength() + 1);
        } else {
            Element map = getDocument().getDefaultRootElement();
            return map.getElementIndex(offset);
        }
    }

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
    public int getLineStartOffset(int line) throws BadLocationException {
        Element map = getDocument().getDefaultRootElement();
        if (line < 0) {
            throw new BadLocationException("Negative line", -1);
        } else if (line >= map.getElementCount()) {
            throw new BadLocationException("No such line", getDocument().getLength() + 1);
        } else {
            Element lineElem = map.getElement(line);
            return lineElem.getStartOffset();
        }
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

    /**
     * Returns the text of the specified line in the document
     */
    public String getLineText(int lineNum) throws BadLocationException {
        Document doc = getDocument();
        Element map = doc.getDefaultRootElement();
        int startOffset = getLineStartOffset(lineNum - 1);
        int endOffset = doc.getLength() - 1;

        if (lineNum + 1 < map.getElementCount()) {
            endOffset = getLineStartOffset(lineNum) - 1;
        }

        return doc.getText(startOffset, endOffset - startOffset);

    }//getLineText()


    /**
     * sets the selection to the current line
     *
     * @author Andrew Nuxoll
     * @version 22 Sep 2022
     */
    public void selectCurrLIne() throws BadLocationException {
        int lineNum = 1 + getLineOfOffset(getCaretPosition());
        Document doc = getDocument();
        Element map = doc.getDefaultRootElement();
        int startOffset = getLineStartOffset(lineNum - 1);
        int endOffset = doc.getLength() - 1;

        if (lineNum + 1 < map.getElementCount()) {
            endOffset = getLineStartOffset(lineNum) - 1;
        }

        setSelectionStart(startOffset);
        setSelectionEnd(endOffset);
    }//selectCurrLIne

    /**
     * Expands the current selection to include only entire lines
     * If nothing is currently selected, then the current line is
     * selected.
     */
    public void expandSelectionToEntireLines() throws BadLocationException {
        String selectedText = getSelectedText();
        if ((selectedText == null) || (selectedText.length() == 0)) {
            selectCurrLIne();
            return;
        }

        //set new selection start position
        Document doc = getDocument();
        Element map = doc.getDefaultRootElement();
        int lineNum = 1 + getLineOfOffset(getSelectionStart());
        int startOffset = getLineStartOffset(lineNum - 1);
        setSelectionStart(startOffset);

        //set new selection end position
        lineNum = 1 + getLineOfOffset(getSelectionEnd());
        int endOffset = doc.getLength() - 1;
        if (lineNum + 1 < map.getElementCount()) {
            endOffset = getLineStartOffset(lineNum) - 1;
        }
        setSelectionEnd(endOffset);

    }//expandSelectionToEntireLines

    /**
     * Stolen from JTextComponent...
     * <p>
     * Initializes the JEditorPane's document from a stream
     *
     * @param in The stream to read from
     * @throws IOException as thrown by the stream being
     *                     used to initialize.
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

    /**
     * Colors the syntax of the whole document
     */
    public void colorSyntax() {
        ((SoarDocument) getDocument()).colorSyntax(new StringReader(getText()));
    }

    /**
     * Auto Justifies the selected area.  If none selected, justified entire
     * document
     */
    public void justifyDocument() {
        ((SoarDocument) getDocument()).justifyDocument(getSelectionStart(),
                getSelectionEnd());
    }

    class AutoJustifyAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;


        public void actionPerformed(ActionEvent e) {
            SoarDocument doc = (SoarDocument) getDocument();

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
