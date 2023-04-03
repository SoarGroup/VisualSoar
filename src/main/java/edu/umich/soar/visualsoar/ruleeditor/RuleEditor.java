package edu.umich.soar.visualsoar.ruleeditor;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.dialogs.FindDialog;
import edu.umich.soar.visualsoar.dialogs.FindReplaceDialog;
import edu.umich.soar.visualsoar.graph.EnumerationVertex;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.graph.SoarVertex;
import edu.umich.soar.visualsoar.misc.*;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorRootNode;
import edu.umich.soar.visualsoar.parser.*;
import edu.umich.soar.visualsoar.util.ActionButtonAssociation;
import edu.umich.soar.visualsoar.util.EnumerationIteratorWrapper;
import edu.umich.soar.visualsoar.util.MenuAdapter;
import sml.Agent;

import javax.swing.Action;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * This is the rule editor window
 *
 * @author Brad Jones
 */
public class RuleEditor extends CustomInternalFrame {
    private static final long serialVersionUID = 20221225L;

    //********** Data Members  *****************
    private final OperatorNode associatedNode;
    private final EditorPane editorPane = new EditorPane();
    private final UndoManager undoManager = new CustomUndoManager();
    private String fileName;
    private final JLabel lineNumberLabel = new JLabel("Line:");
    private final JLabel modifiedLabel = new JLabel("");


    //For keeping track of find/replace operations
    private String findString = null;
    private String replaceString = null;
    private boolean findForward = true;
    private boolean matchCase = false;
    private boolean wrapSearch = false;

    //For highlighting a section of text (e.g., due to a Find command)
    private final Highlighter highlighter = editorPane.getHighlighter();
    private final Color hlColor = new Color(255, 255, 128); // a pale yellow
    private final DefaultHighlighter.DefaultHighlightPainter hlPainter
            = new DefaultHighlighter.DefaultHighlightPainter(hlColor);
    private Object lastHighlight = null;

    //Context Menu Items
    JMenuItem cutSelectedTextItem = new JMenuItem("Cut");
    JMenuItem copySelectedTextItem = new JMenuItem("Copy");
    JMenuItem pasteTextItem = new JMenuItem("Paste");
    JMenuItem deleteSelectedTextItem = new JMenuItem("Delete");

    JMenuItem openDataMapItem = new JMenuItem("Open Corresponding Datamap");


    // ********** Actions ***********
    private final Action saveAction = new SaveAction();
    private final Action revertToSavedAction = new RevertToSavedAction();
    private final Action closeAction = new CloseAction();

    private final Action undoAction = new UndoAction();
    private final Action redoAction = new RedoAction();
    private final Action cutAction = new DefaultEditorKit.CutAction();
    private final Action copyAction = new DefaultEditorKit.CopyAction();
    private final Action pasteAction = new PasteAction();
    private final Action insertTextFromFileAction = new InsertTextFromFileAction();

    private final Action commentOutAction = new CommentOutAction();
    private final Action uncommentOutAction = new UncommentOutAction();

    private final Action reDrawAction = new ReDrawAction();
    private final Action reJustifyAction = new ReJustifyAction();

    private final Action findAction = new FindAction();
    private final FindAgainAction findAgainAction = new FindAgainAction();
    private final ReplaceAction replaceAction = new ReplaceAction();
    private final ReplaceAndFindAgainAction replaceAndFindAgainAction = new ReplaceAndFindAgainAction();
    private final ReplaceAllAction replaceAllAction = new ReplaceAllAction();
    private final Action findAndReplaceAction = new FindAndReplaceAction();

    private final Action checkProductionsAction = new CheckProductionsAction();
    private final Action tabCompleteAction = new TabCompleteAction();


    // 3P
    // Menu item handlers for the STI operations in this window.
    private final Action sendProductionToSoarAction = new SendProductionToSoarAction();
    private final Action sendFileToSoarAction = new SendFileToSoarAction();
    private final Action sendAllFilesToSoarAction = new SendAllFilesToSoarAction();
    private final Action sendMatchesToSoarAction = new SendMatchesToSoarAction();
    private final Action sendExciseProductionToSoarAction = new SendExciseProductionToSoarAction();

    private final BackupThread backupThread;
    // Constructors

    /**
     * Constructs a new JInternalFrame, sets its size and
     * adds the editor pane to it
     *
     * @param inFileName the file to which this RuleEditor is associated
     */
    public RuleEditor(File inFileName, OperatorNode inNode) throws IOException {
        super(inNode.getUniqueName(), true, true, true, true);
        setType(RULE_EDITOR);

        // Initalize my member variables
        associatedNode = inNode;
        fileName = inFileName.getPath();
        getData(inFileName);
        editorPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        setBounds(0, 0, 250, 100);
        initMenuBar();
        initLayout();
        //editorPane.setLineWrap(false);
        addVetoableChangeListener(new CloseListener());

        // Retile the internal frames after closing a window
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addInternalFrameListener(
                new InternalFrameAdapter() {
                    public void internalFrameClosing(InternalFrameEvent e) {
                        if (isModified()) {
                            int answer = JOptionPane.showConfirmDialog(null, "Save Changes to " + fileName + "?",
                                    "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
                            if (answer == JOptionPane.CANCEL_OPTION) {
                                return;
                            } else if (answer == JOptionPane.YES_OPTION) {
                                try {
                                    write();
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                }
                            }
                        }
                        dispose();

                        MainFrame mf = MainFrame.getMainFrame();
                        if (Prefs.autoTileEnabled.getBoolean()) {
                            mf.getDesktopPane().performTileAction();
                        }

                        mf.selectNewInternalFrame();
                    }
                });

        registerDocumentListeners();
        backupThread = new BackupThread();
        backupThread.start();

        if (edu.umich.soar.visualsoar.misc.Prefs.autoSoarCompleteEnabled.getBoolean()) {
            Keymap keymap = editorPane.getKeymap();

            KeyStroke dot = KeyStroke.getKeyStroke('.');
            Action autoSoarCompleteAction = new AutoSoarCompleteAction();
            keymap.addActionForKeyStroke(dot, autoSoarCompleteAction);

            KeyStroke langle = KeyStroke.getKeyStroke('<');
            keymap.addActionForKeyStroke(langle, autoSoarCompleteAction);

            editorPane.setKeymap(keymap);
        }


        editorPane.addCaretListener(
                new CaretListener() {
                    public void caretUpdate(CaretEvent e) {
                        int offset = e.getDot();

                        try {
                            lineNumberLabel.setText("Line: " + (1 + editorPane.getLineOfOffset(offset)));
                            //editorPane.requestFocus();

                            //Remove any highlights
                            if (lastHighlight != null) {
                                highlighter.removeAllHighlights();
                                lastHighlight = null;
                            }
                        } catch (BadLocationException ble) {
                            ble.printStackTrace();
                        }
                    }
                });

        adjustKeymap();
        setupContextMenu();
    }//RuleEditor ctor

    /**
     * Constructs a new JInternalFrame, sets its size and
     * adds the editor pane to it.
     * This constructor is for opening external files not associated with the
     * project.
     *
     * @param inFileName the file to which this RuleEditor is associated
     */
    public RuleEditor(File inFileName) throws IOException {
        super(inFileName.getName(), true, true, true, true);
        setType(RULE_EDITOR);

        // Initalize my member variables
        associatedNode = null;
        fileName = inFileName.getPath();
        getData(inFileName);

        editorPane.setFont(new Font("Monospaced", Font.PLAIN, 14));

        setBounds(0, 0, 250, 100);
        initMenuBarExternFile();
        initLayout();

        addVetoableChangeListener(new CloseListener());

        registerDocumentListeners();
        backupThread = new BackupThread();
        backupThread.start();

        editorPane.addCaretListener(
                new CaretListener() {
                    public void caretUpdate(CaretEvent e) {
                        int offset = e.getDot();
                        try {
                            lineNumberLabel.setText("Line: " + (1 + editorPane.getLineOfOffset(offset)));
                            //editorPane.requestFocus();
                        } catch (BadLocationException ble) {
                            ble.printStackTrace();
                        }
                    }
                });
        adjustKeymap();
        setupContextMenu();

    }//RuleEditor ctor


    private void registerDocumentListeners() {
        Document doc = editorPane.getDocument();

        doc.addDocumentListener(
                new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) {
                        if (! isModified()) {
                            setModified(true);
                            modifiedLabel.setText("Modified");
                        }
                    }

                    public void removeUpdate(DocumentEvent e) {
                        if (! isModified()) {
                            setModified(true);
                            modifiedLabel.setText("Modified");
                        }
                    }

                    public void changedUpdate(DocumentEvent e) {
                    }
                });

        doc.addUndoableEditListener(undoManager);
    }

    private void adjustKeymap() {
        Keymap keymap = editorPane.getKeymap();
        keymap.removeKeyStrokeBinding(KeyStroke.getKeyStroke("alt F"));
        editorPane.setKeymap(keymap);

    }

    private void setupContextMenu() {
        JPopupMenu contextMenu = editorPane.getContextMenu();

        cutSelectedTextItem.addActionListener(cutAction);
        contextMenu.add(cutSelectedTextItem);

        copySelectedTextItem.addActionListener(copyAction);
        contextMenu.add(copySelectedTextItem);

        pasteTextItem.addActionListener(pasteAction);
        contextMenu.add(pasteTextItem);

        deleteSelectedTextItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        editorPane.replaceSelection("");
                    }
                });
        contextMenu.add(deleteSelectedTextItem);

        contextMenu.addSeparator();

        openDataMapItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        MainFrame mf = MainFrame.getMainFrame();
                        SoarWorkingMemoryModel dataMap = mf.getOperatorWindow().getDatamap();
                        getDataMapNode().openDataMap(dataMap, mf);
                    }
                });
        contextMenu.add(openDataMapItem);

    }

    /**
     * A helper function to read in the data from a file into the editorPane so we don't have
     * catch it in the constructor
     *
     * @param fn the file name
     */
    private void getData(File fn) throws IOException {
        Reader fr =
                new edu.umich.soar.visualsoar.util.TabRemovingReader(new FileReader(fn));
        editorPane.read(fr);
        fr.close();
    }

    /**
     * Moves the caret to the beginning of the text in the pane.
     * (This is usually done in preparation for a find/replace operation.)
     */
    public void resetCaret() {
        editorPane.setCaretPosition(0);
    }

    /**
     * Highlights a specified section of the document and moves
     * the caret to the beginning of that position.
     */
    public void highlightSection(int startOffset, int endOffset) {
        try {
            editorPane.setCaretPosition(startOffset);
            highlighter.removeAllHighlights();
            lastHighlight = highlighter.addHighlight(startOffset,
                    endOffset,
                    hlPainter);
            highlighter.paint(editorPane.getGraphics());
        } catch (BadLocationException ble) {
            ble.printStackTrace();
        }
        editorPane.requestFocus();
    }//highlightSection()

    /**
     * Highlights a specified substring in the document that is
     * located on a specified line of the document.
     */
    public void highlightString(int lineNum, String assocString) {
        int startOffset;
        int endOffset;

        try {
            //Determine the extent of the given line
            startOffset = editorPane.getLineStartOffset(lineNum - 1);
            endOffset = editorPane.getLineStartOffset(lineNum) - 1;

            //Attempt to find the specified substring
            String text = editorPane.getLineText(lineNum);
            int index = text.indexOf(assocString);
            if (index >= 0) {
                startOffset += index;
                endOffset = startOffset + assocString.length();
            }
        } catch (BadLocationException ble) {
            ble.printStackTrace();
            return;
        }

        highlightSection(startOffset, endOffset);
    }//highlightString()

    /**
     * Sets the current line number in the editorPane, puts the caret at the
     * beginning of the line, highlights the line and requests focus.
     *
     * @param lineNum the desired lineNum to go to
     */
    public void setLine(int lineNum) {
        int startOffset;
        int endOffset;

        //Determine the extent of the given line
        try {
            startOffset = editorPane.getLineStartOffset(lineNum - 1);
            endOffset = editorPane.getLineStartOffset(lineNum) - 1;
        } catch (BadLocationException ble) {
            ble.printStackTrace();
            return;
        }

        highlightSection(startOffset, endOffset);
    }//setLine()

    public int getNumberOfLines() {
        return editorPane.getLineCount();
    }

    /**
     * Returns the node this rule editor is associated with
     *
     * @return the associated node
     */
    public OperatorNode getNode() {
        return associatedNode;
    }

    /**
     * Locates the OperatorNode containing the datamap used by the
     * productions in this node.
     *
     * @return the associated node
     */
    public OperatorNode getDataMapNode() {
        OperatorNode node = (OperatorNode) associatedNode.getParent();

        while (node.noDataMap()) {
            node = (OperatorNode) node.getParent();
        }

        return node;
    }


    /**
     * Finds the a string in the document past the caret, if it finds it, it
     * selects it
     *
     * @return whether or not it found the string
     */
    private boolean findForward() {
        String searchString;

        if (matchCase) {
            searchString = findString;
        } else {
            searchString = findString.toLowerCase();
        }
        Document doc = editorPane.getDocument();
        int caretPos = editorPane.getCaretPosition();

        int textlen = doc.getLength() - caretPos;
        try {
            String text = doc.getText(caretPos, textlen);
            if (!matchCase) {
                text = text.toLowerCase();
            }
            int start = text.indexOf(searchString);

            if ((start == -1) && (wrapSearch)) { // search the wrapped part
                text = doc.getText(0, caretPos);
                caretPos = 0;
                start = text.indexOf(searchString);
            }

            if (start != -1) {
                int end = start + findString.length();
                editorPane.setSelectionStart(caretPos + start);
                editorPane.setSelectionEnd(caretPos + end);
                return true;
            } else { // string not found
                return false;
            }
        } catch (BadLocationException ble) {
            ble.printStackTrace();
        }
        return false;
    }

    /**
     * Finds the a string in the document before the caret or before the
     * selection point, if it finds it, it selects it
     *
     * @return whether or not it found the string
     */
    private boolean findBackward() {
        String searchString;
        if (matchCase) {
            searchString = findString;
        } else {
            searchString = findString.toLowerCase();
        }
        Document doc = editorPane.getDocument();
        int caretPos = editorPane.getSelectionStart();
        try {
            String text = doc.getText(0, caretPos);
            if (!matchCase) {
                text = text.toLowerCase();
            }
            int start = text.lastIndexOf(searchString);

            if ((start == -1) && (wrapSearch)) {
                // seach the wrapped part
                int textlen = doc.getLength() - caretPos;
                text = doc.getText(caretPos, textlen);
                start = caretPos + text.lastIndexOf(searchString);
            }

            if (start != -1) {
                int end = start + findString.length();
                editorPane.setSelectionStart(start);
                editorPane.setSelectionEnd(end);
                return true;
            } else { // string not found
                return false;
            }
        } catch (BadLocationException ble) {
            ble.printStackTrace();
        }
        return false;
    }

    public String getAllText() {
        Document doc = editorPane.getDocument();
        String s = "";
        try {
            s = doc.getText(0, doc.getLength());
        } catch (BadLocationException ble) {
            ble.printStackTrace();
        }
        return s;
    }

    public void setFindReplaceData(String find,
                                   Boolean forward,
                                   Boolean caseSensitive,
                                   Boolean wrap) {
        setFindReplaceData(find, null, forward, caseSensitive, wrap);
    }

    public void setFindReplaceData(String find,
                                   String replace,
                                   Boolean forward,
                                   Boolean caseSensitive,
                                   Boolean wrap) {
        findString = find;
        replaceString = replace;
        findForward = forward;
        matchCase = caseSensitive;
        wrapSearch = wrap;

        if (findString != null) {
            findAgainAction.setEnabled(true);

            if (replaceString != null) {
                replaceAction.setEnabled(true);
                replaceAndFindAgainAction.setEnabled(true);
                replaceAllAction.setEnabled(true);
            }
        }
    }

    // 3P
    // This method returns the production that the cursor is currently over.
    // null is returned if a production cannot be found underneath the current
    // cursor position.
    public String GetProductionStringUnderCaret() {
        // Get the current position of the cursor in the editor pane
        int caretPos = editorPane.getCaretPosition();

        // Get the text in the editor
        String text = editorPane.getText();

        // Search backwards for the "sp " string which marks
        // the start of a production.
        int nProductionStartPos = text.lastIndexOf("sp ", caretPos);
        if (nProductionStartPos == -1) {
            return null;
        }

        // Now search for the first opening brace for this production
        int nFirstOpenBrace = text.indexOf('{', nProductionStartPos);
        if (nFirstOpenBrace == -1) {
            return null;
        }

        // Go through the string looking for the closing brace
        int nNumOpenBraces = 1;
        int nCurrentSearchPos = nFirstOpenBrace + 1;
        while (nCurrentSearchPos < text.length() && nNumOpenBraces > 0) {
            // Keep track of our open brace count
            if (text.charAt(nCurrentSearchPos) == '{') {
                nNumOpenBraces++;
            }
            if (text.charAt(nCurrentSearchPos) == '}') {
                nNumOpenBraces--;
            }

            // Advance to the next character
            nCurrentSearchPos++;
        }

        // We should have zero open braces
        if (nNumOpenBraces != 0) {
            return null;
        }

        // The last brace marks the end of the production
        int nProductionEndPos = nCurrentSearchPos;

        // Our cursor position should not be past the last brace.  If this is
        // the case, it means that our cursor is not really inside of a
        // production.
        if (nProductionEndPos < caretPos) {
            return null;
        }

        // Return the string to the caller
        return text.substring(nProductionStartPos, nProductionEndPos);
    }

    // 3P
    // This method returns the production name that the cursor is currently over.
    // null is returned if a production cannot be found underneath the current
    // cursor position.
    public String getProductionNameNearCaret() {
        // Get the current position of the cursor in the editor pane
        int caretPos = editorPane.getCaretPosition();

        // Get the text in the editor
        String text = editorPane.getText();

        int preSpPos = text.lastIndexOf("sp ", caretPos);
        int postSpPos = text.indexOf("sp ", caretPos);
        int nProductionStartPos;
        if ((preSpPos != -1)
                && ((postSpPos == -1)
                || (caretPos - preSpPos < postSpPos - caretPos))) {
            nProductionStartPos = preSpPos;
        } else if ((postSpPos != -1)
                && ((preSpPos == -1)
                || (caretPos - preSpPos >= postSpPos - caretPos))) {
            nProductionStartPos = postSpPos;
        } else {
            return null;
        }

        // Now search for the first opening brace for this production
        int nFirstOpenBrace = text.indexOf('{', nProductionStartPos);
        if (nFirstOpenBrace == -1) {
            return null;
        }

        // Get the start of the name position
        int nStartPos = nFirstOpenBrace + 1;
        if (nStartPos >= text.length()) {
            return null;
        }

        // Now go through the editor text trying to find the end
        // of the name.  Right now we currently define the end
        // to be a space, newline, or '('.
        //
        // TODO: Is this the correct way to find the name?
        int nCurrentSearchIndex = nFirstOpenBrace + 1;
        while (nCurrentSearchIndex < text.length()) {
            // See if we have found a character which ends the name
            if (text.charAt(nCurrentSearchIndex) == ' ' ||
                    text.charAt(nCurrentSearchIndex) == '\n' ||
                    text.charAt(nCurrentSearchIndex) == '(') {
                break;
            }

            // Go to the next character
            nCurrentSearchIndex++;
        }

        // Last character in the name
        int nEndPos = nCurrentSearchIndex;

        // Return the name to the caller
        return text.substring(nStartPos, nEndPos);
    }

    // 3P
    //This method returns the production name that the cursor is currently over.
    //null is returned if a production cannot be found underneath the current
    //cursor position.
    public String GetProductionNameUnderCaret() {
        // Get the current position of the cursor in the editor pane
        int caretPos = editorPane.getCaretPosition();

        // Get the text in the editor
        String text = editorPane.getText();

        // Search backwards for the "sp " string which marks
        // the start of a production.
        int nProductionStartPos = text.lastIndexOf("sp ", caretPos);
        if (nProductionStartPos == -1) {
            return null;
        }

        // Now search for the first opening brace for this production
        int nFirstOpenBrace = text.indexOf('{', nProductionStartPos);
        if (nFirstOpenBrace == -1) {
            return null;
        }

        // Get the start of the name position
        int nStartPos = nFirstOpenBrace + 1;
        if (nStartPos >= text.length()) {
            return null;
        }

        // Now go through the editor text trying to find the end
        // of the name.  Right now we currently define the end
        // to be a space, newline, or '('.
        //
        // TODO: Is this the correct way to find the name?
        int nCurrentSearchIndex = nFirstOpenBrace + 1;
        while (nCurrentSearchIndex < text.length()) {
            // See if we have found a character which ends the name
            if (text.charAt(nCurrentSearchIndex) == ' ' ||
                    text.charAt(nCurrentSearchIndex) == '\n' ||
                    text.charAt(nCurrentSearchIndex) == '(') {
                break;
            }

            // Go to the next character
            nCurrentSearchIndex++;
        }

        // Last character in the name
        int nEndPos = nCurrentSearchIndex;

        // Return the name to the caller
        return text.substring(nStartPos, nEndPos);
    }


    /**
     * Looks for the passed string in the document, if it is searching
     * forward, then it searches for and instance of the string after the caret and selects it,
     * if it is searching backwards then it selects the text either before the caret or
     * before the start of the selection
     */
    public void find() {
        boolean result;

        if (findForward) {
            result = findForward();
        } else {
            result = findBackward();
        }
        if (!result) {
            getToolkit().beep();
        }
    }

    /**
     * Similiar to void find(), but returns result of search as boolean
     */
    public boolean findResult() {
        if (findForward) {
            return (findForward());
        } else {
            return (findBackward());
        }
    }


    /**
     * Takes the selected text within the editorPane checks to see if that is the string
     * that we are looking for, if it is then it replaces it and selects the new text
     */
    public void replace() {
        String selString = editorPane.getSelectedText();
        if (selString == null) {
            getToolkit().beep();
            return;
        }
        boolean toReplace;
        if (matchCase) {
            toReplace = selString.equals(findString);
        } else {
            toReplace = selString.equalsIgnoreCase(findString);
        }
        if (toReplace) {
            editorPane.replaceSelection(replaceString);
            int end = editorPane.getCaretPosition();
            int start = end - replaceString.length();
            editorPane.setSelectionStart(start);
            editorPane.setSelectionEnd(end);
        } else {
            getToolkit().beep();
        }
    }

    /**
     * Replaces all instances (before/after) the caret with the specified string
     */
    public void replaceAll() {
        int count = 0;

        if (wrapSearch) {
            editorPane.setCaretPosition(0);
            while (findForward()) {
                editorPane.replaceSelection(replaceString);
            }
        } else {
            if (findForward) {
                while (findForward()) {
                    editorPane.replaceSelection(replaceString);
                    ++count;
                }
            } else {
                while (findBackward()) {
                    editorPane.replaceSelection(replaceString);
                    ++count;
                }
            }
        }

        MainFrame.getMainFrame().setStatusBarMsg("Replaced " + count + " occurrences of \"" + findString + "\" with \"" + replaceString + "\"");
    }

    /**
     * Writes the data in the editing window out to disk, the file that it writes
     * to is the same as it was at construction
     *
     * @throws IOException if there is a disk error
     */
    public void write() throws IOException {
        makeValidForParser();
        FileWriter fw = new FileWriter(fileName);
        editorPane.write(fw);
        setModified(false);
        modifiedLabel.setText("");
        fw.close();
    }

    /**
     * fixUnmatchedBraces
     * <p>
     * If you forget to put a close brace at the end of your production
     * VS will thoughtfully insert it for you.
     * <p>
     * Note:  There is a sister method {@link SuppParseChecks#fixUnmatchedBraces}
     * which does the same thing for files that are not currently open.  They
     * share {@link SuppParseChecks#findMissingBracePositions(String)} and
     * {@link SuppParseChecks#insertBraces} as helper methods.
     */
    public void fixUnmatchedBraces() {
        String text = editorPane.getText();

        //Add any required braces
        Vector<Integer> bracePositions = SuppParseChecks.findMissingBracePositions(text);
        if (bracePositions.isEmpty()) return; //nothing to do
        text = SuppParseChecks.insertBraces(text, bracePositions);

        //Adjust the caretPosition so cursor stays in the same relative location
        int caretPos = editorPane.getCaretPosition();
        for (int i : bracePositions) {
            if (i < caretPos) caretPos += 1;
        }
        caretPos = Math.min(text.length() - 2, caretPos); //don't go over the edge!
        editorPane.setCaretPosition(caretPos);


        //TODO: It would be nice to inform the user somehow that the
        //      code has been edited.  However the only way I see to do
        //      that without explicit support for such a message is to
        //      throw a ParseException which defeats the purpose of the
        //      edit.  So it's a silent edit.  -:AMN: 22 Sep 2022

        editorPane.setText(text);

    }//fixUnmatchedBraces

    /**
     * In order for the file to be valid for the parser
     * there must be a newline following
     */
    private void makeValidForParser() {
        String text = editorPane.getText();

        //Add a trailing newline if needed
        int pound = text.lastIndexOf("#");
        if (pound == -1) {
            return;
        }
        int nl = text.lastIndexOf("\n");
        if (nl < pound) {
            text += "\n";
            editorPane.setText(text);
        }

    }

    /**
     * Same as above but for a given string
     */
    private String makeStringValidForParser(String prod) {
        String text = editorPane.getText();
        int pound = text.lastIndexOf("#");
        int nl = text.lastIndexOf("\n");
        if ((pound != -1) && (nl < pound)) {
            prod += "\n";
        }
        return prod;
    }


    /**
     * Reverts the contents of the editor to it's saved copy
     */
    public void revert() throws IOException {
        Reader theReader =
                new edu.umich.soar.visualsoar.util.TabRemovingReader(
                        new FileReader(fileName));

        editorPane.read(theReader);
        registerDocumentListeners();
        theReader.close();

        modifiedLabel.setText("");
        setModified(false);

        editorPane.colorSyntax();
    }

    /**
     * @return returns the file that this window is associated with
     */
    public String getFile() {
        return fileName;
    }


    public Vector<SoarProduction> parseProductions() throws ParseException {
        makeValidForParser();
        SoarParser parser = new SoarParser(new StringReader(getAllText()));

        return parser.VisualSoarFile();
    }

    /**
     * The file underneath of us has been renamed
     *
     * @param newFileName what the new name is
     */
    public void fileRenamed(String newFileName) {
        setTitle(getNode().getUniqueName());
        fileName = newFileName;
    }

    /**
     * This lays out the Rule Editor according to specifications
     */
    private void initLayout() {
        // Take care of the panel to the south
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(lineNumberLabel, BorderLayout.WEST);
        southPanel.add(modifiedLabel, BorderLayout.EAST);

        // do the rest of the content pane
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(new JScrollPane(editorPane));
        contentPane.add(southPanel, BorderLayout.SOUTH);
    }

    /**
     * Initializes the menubar according to specifications
     */
    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        initFileMenu(menuBar);
        initEditMenu(menuBar);
        initSearchMenu(menuBar);
        initSoarMenu(menuBar);
        initSoarRuntimeMenu(menuBar);

        setJMenuBar(menuBar);
    }

    private void initMenuBarExternFile() {
        JMenuBar menuBar = new JMenuBar();

        initFileMenu(menuBar);
        initEditMenu(menuBar);
        initSearchMenu(menuBar);

        setJMenuBar(menuBar);
    }

    private void initFileMenu(JMenuBar menuBar) {
        JMenu fileMenu = new JMenu("File");

        // Save Action
        JMenuItem saveItem = new JMenuItem("Save this File");
        saveItem.addActionListener(saveAction);
        fileMenu.add(saveItem);

        JMenuItem revertToSavedItem = new JMenuItem("Revert To Saved");
        revertToSavedItem.addActionListener(revertToSavedAction);
        fileMenu.add(revertToSavedItem);

        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.addActionListener(closeAction);
        fileMenu.add(closeItem);

        fileMenu.setMnemonic(KeyEvent.VK_I);
        menuBar.add(fileMenu);
    }

    private void initEditMenu(JMenuBar menuBar) {
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo");
        editMenu.add(undoItem);
        undoItem.addActionListener(undoAction);
        undoAction.addPropertyChangeListener(
                new ActionButtonAssociation(undoAction, undoItem));

        JMenuItem redoItem = new JMenuItem("Redo");
        editMenu.add(redoItem);
        redoItem.addActionListener(redoAction);
        redoAction.addPropertyChangeListener(
                new ActionButtonAssociation(redoAction, redoItem));

        editMenu.addMenuListener(
                new MenuAdapter() {
                    public void menuSelected(MenuEvent e) {
                        undoAction.setEnabled(undoManager.canUndo());
                        redoAction.setEnabled(undoManager.canRedo());
                    }
                });

        editMenu.addSeparator();
        JMenuItem commentOutItem = new JMenuItem("Comment Out");
        commentOutItem.addActionListener(commentOutAction);
        editMenu.add(commentOutItem);

        JMenuItem uncommentOutItem = new JMenuItem("Uncomment Out");
        uncommentOutItem.addActionListener(uncommentOutAction);
        editMenu.add(uncommentOutItem);

        editMenu.addSeparator();

        JMenuItem cutItem = new JMenuItem("Cut");
        cutItem.addActionListener(cutAction);
        editMenu.add(cutItem);

        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.addActionListener(copyAction);
        editMenu.add(copyItem);

        JMenuItem pasteItem = new JMenuItem("Paste");
        pasteItem.addActionListener(pasteAction);
        editMenu.add(pasteItem);

        editMenu.addSeparator();

        JMenuItem insertTextFromFileItem = new JMenuItem("Insert Text From File...");
        insertTextFromFileItem.addActionListener(insertTextFromFileAction);
        editMenu.add(insertTextFromFileItem);

        JMenuItem reDrawItem = new JMenuItem("Redraw Color Syntax");
        reDrawItem.addActionListener(reDrawAction);
        editMenu.add(reDrawItem);

        JMenuItem reJustifyItem = new JMenuItem("ReJustify Text");
        reJustifyItem.addActionListener(reJustifyAction);
        editMenu.add(reJustifyItem);

        // register accel and remember thingys
        editMenu.setMnemonic('E');
        undoItem.setMnemonic(KeyEvent.VK_D);
        undoItem.setAccelerator(KeyStroke.getKeyStroke("control Z"));
        redoItem.setMnemonic(KeyEvent.VK_R);
        redoItem.setAccelerator(KeyStroke.getKeyStroke("control shift Z"));
        cutItem.setMnemonic(KeyEvent.VK_T);
        cutItem.setAccelerator(KeyStroke.getKeyStroke("control X"));
        copyItem.setMnemonic(KeyEvent.VK_C);
        copyItem.setAccelerator(KeyStroke.getKeyStroke("control C"));
        pasteItem.setMnemonic(KeyEvent.VK_P);
        pasteItem.setAccelerator(KeyStroke.getKeyStroke("control V"));
        reDrawItem.setMnemonic(KeyEvent.VK_D);
        reDrawItem.setAccelerator(KeyStroke.getKeyStroke("control D"));
        reJustifyItem.setMnemonic(KeyEvent.VK_J);
        reJustifyItem.setAccelerator(KeyStroke.getKeyStroke("control J"));

        commentOutItem.setMnemonic(KeyEvent.VK_SLASH);
        commentOutItem.setAccelerator(KeyStroke.getKeyStroke("control SLASH"));

        menuBar.add(editMenu);
    }

    private void initSearchMenu(JMenuBar menuBar) {
        JMenu searchMenu = new JMenu("Search");

        JMenuItem findItem = new JMenuItem("Find...");
        findItem.addActionListener(findAction);
        searchMenu.add(findItem);

        JMenuItem findAgainItem = new JMenuItem("Find Again");
        findAgainItem.addActionListener(findAgainAction);
        findAgainAction.addPropertyChangeListener(new ActionButtonAssociation(findAgainAction, findAgainItem));

        searchMenu.add(findAgainItem);

        JMenuItem findAndReplaceItem = new JMenuItem("Find & Replace...");
        findAndReplaceItem.addActionListener(findAndReplaceAction);
        searchMenu.add(findAndReplaceItem);

        JMenuItem replaceItem = new JMenuItem("Replace");
        replaceItem.addActionListener(replaceAction);
        replaceAction.addPropertyChangeListener(new ActionButtonAssociation(replaceAction, replaceItem));
        searchMenu.add(replaceItem);

        JMenuItem replaceAndFindAgainItem = new JMenuItem("Replace & Find Again");
        replaceAndFindAgainItem.addActionListener(replaceAndFindAgainAction);
        replaceAndFindAgainAction.addPropertyChangeListener(new ActionButtonAssociation(replaceAndFindAgainAction, replaceAndFindAgainItem));
        searchMenu.add(replaceAndFindAgainItem);

        JMenuItem replaceAllItem = new JMenuItem("Replace All");
        replaceAllItem.addActionListener(replaceAllAction);
        replaceAllAction.addPropertyChangeListener(new ActionButtonAssociation(replaceAllAction, replaceAllItem));
        searchMenu.add(replaceAllItem);

        // Register accel and mnemonics
        searchMenu.setMnemonic('S');
        findItem.setAccelerator(KeyStroke.getKeyStroke("control F"));
        findItem.setMnemonic(KeyEvent.VK_F);
        findAgainItem.setAccelerator(KeyStroke.getKeyStroke("control G"));
        findAndReplaceItem.setAccelerator(KeyStroke.getKeyStroke("control R"));
        replaceItem.setAccelerator(KeyStroke.getKeyStroke("control EQUALS"));
        replaceAndFindAgainItem.setAccelerator(KeyStroke.getKeyStroke("control H"));

        menuBar.add(searchMenu);
    }

    // 3P - Changes to this function were made to add the STI related menu items.
    //
    // Update, those changes have since been moved to CreateSoarRuntimeMenu.
    private void initSoarMenu(JMenuBar menuBar) {
        ///////////////////////////////////////
        // Soar menu
        JMenu soarMenu = new JMenu("Soar");

        // "Check Productions" menu item
        JMenuItem checkProductionsItem = new JMenuItem("Check Productions Against Datamap");
        checkProductionsItem.addActionListener(checkProductionsAction);
        soarMenu.add(checkProductionsItem);

        checkProductionsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7,
                0));
        checkProductionsItem.setMnemonic(KeyEvent.VK_P);

        // "Soar Complete" menu item
        JMenuItem tabCompleteItem = new JMenuItem("Soar Complete");
        tabCompleteItem.addActionListener(tabCompleteAction);
        soarMenu.add(tabCompleteItem);

        tabCompleteItem.setAccelerator(KeyStroke.getKeyStroke("control ENTER"));

        ///////////////////////////////////////
        // Insert Template menu
        JMenu templates = new JMenu("Insert Template");
        initTemplatesMenu(MainFrame.getMainFrame().getTemplateManager().getRootTemplate(),
                templates);

        soarMenu.setMnemonic(KeyEvent.VK_O);

        menuBar.add(soarMenu);
        menuBar.add(templates);
    }

    private void initTemplatesMenu(Template parentTemplate, JMenu parentMenu) {
        if (parentTemplate == null) {
            JMenuItem item = new JMenuItem("No templates found.");
            item.setEnabled(false);
            parentMenu.add(item);
            return;
        }

        // Add plain old templates...
        Iterator<Template> i = parentTemplate.getChildTemplates();
        while (i.hasNext()) {
            Template t = i.next();
            JMenuItem currentTemplateItem = new JMenuItem(t.getName());
            currentTemplateItem.addActionListener(new InsertTemplateAction(t));
            parentMenu.add(currentTemplateItem);
        }
    }

    // 3P
    // Initializes the "Runtime" menu item and adds it to the given menubar
    private void initSoarRuntimeMenu(JMenuBar menuBar) {
        // Create the Runtime menu
        JMenu runtimeMenu = new JMenu("Runtime");

        // "Send Production" menu item
        JMenuItem sendProductionToSoarItem = new JMenuItem("Send Production");
        sendProductionToSoarItem.addActionListener(sendProductionToSoarAction);
        runtimeMenu.add(sendProductionToSoarItem);

        // "Send File" menu item
        JMenuItem sendFileToSoarItem = new JMenuItem("Send File");
        sendFileToSoarItem.addActionListener(sendFileToSoarAction);
        runtimeMenu.add(sendFileToSoarItem);

        // "Send All Files" menu item
        JMenuItem sendAllFilesToSoarItem = new JMenuItem("Send All Files");
        sendAllFilesToSoarItem.addActionListener(sendAllFilesToSoarAction);
        runtimeMenu.add(sendAllFilesToSoarItem);

        // "Matches" menu item
        JMenuItem matchesItem = new JMenuItem("Matches Production");
        matchesItem.addActionListener(sendMatchesToSoarAction);
        runtimeMenu.add(matchesItem);

        // "Excise Production" menu item
        JMenuItem exciseProductionItem = new JMenuItem("Excise Production");
        exciseProductionItem.addActionListener(sendExciseProductionToSoarAction);
        runtimeMenu.add(exciseProductionItem);

        // Set the mnemonic and add the menu to the menu bar
        runtimeMenu.setMnemonic('R');
        menuBar.add(runtimeMenu);
    }

    /**
     * This class is meant to catch if the user closes an internal frame without saving
     * the file, it prompts them to save, or discard the file or cancel the close
     */
    class CloseListener implements VetoableChangeListener {
        public void vetoableChange(PropertyChangeEvent e) throws PropertyVetoException {
            String name = e.getPropertyName();
            if (name.equals(JInternalFrame.IS_CLOSED_PROPERTY)) {
                Component internalFrame = (Component) e.getSource();

                // note we need to check this or else when the property is vetoed
                // the option pane will come up twice see Graphic Java 2 Volume II pg 889
                // for more information
                Boolean oldValue = (Boolean) e.getOldValue(),
                        newValue = (Boolean) e.getNewValue();
                if (oldValue == Boolean.FALSE && newValue == Boolean.TRUE && isModified()) {
                    int answer = JOptionPane.showConfirmDialog(internalFrame,
                            "Save Changes to " + fileName + "?",
                            "Unsaved Changes",
                            JOptionPane.YES_NO_CANCEL_OPTION);
                    if (answer == JOptionPane.CANCEL_OPTION) {
                        throw new PropertyVetoException("close cancelled", e);
                    } else if (answer == JOptionPane.YES_OPTION) {
                        try {
                            write();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                }
            }
        }
    }


    ////////////////////////////////////////////////////////
    // ACTIONS
    ////////////////////////////////////////////////////////
    class InsertTextFromFileAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public InsertTextFromFileAction() {
            super("Insert Text From File");
        }

        public void actionPerformed(ActionEvent event) {
            JFileChooser fileChooser = new JFileChooser();
            if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(MainFrame.getMainFrame())) {
                try {
                    Reader r = new FileReader(fileChooser.getSelectedFile());
                    StringWriter w = new StringWriter();

                    int rc = r.read();
                    while (rc != -1) {
                        w.write(rc);
                        rc = r.read();
                    }
                    editorPane.insert(w.toString(), editorPane.getCaret().getDot());
                } catch (IOException ioe) {
                    JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                            "There was an error inserting the text",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);


                }
            }
        }
    }

    /**
     * Gets the currently selected rule editor and tells it to save itself
     */
    class SaveAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public SaveAction() {
            super("Save File");
        }

        public void actionPerformed(ActionEvent event) {
            try {
                write();
            } catch (java.io.IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /**
     * reverts the editor's contents to its last saved state
     */
    class RevertToSavedAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public RevertToSavedAction() {
            super("Revert To Saved");
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent event) {
            try {
                revert();
            } catch (java.io.IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Closes the current window
     */
    class CloseAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public CloseAction() {
            super("Close");
        }

        public void actionPerformed(ActionEvent event) {
            try {
                setClosed(true);

            } catch (PropertyVetoException pve) {
                // This is not an error
            }
            MainFrame mf = MainFrame.getMainFrame();
            if (Prefs.autoTileEnabled.getBoolean()) {
                mf.getDesktopPane().performTileAction();
            }

            mf.selectNewInternalFrame();
        }
    }

    class UndoAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public UndoAction() {
            super("Undo");
        }

        public void actionPerformed(ActionEvent e) {
            if (!undoManager.canUndo()) {
                getToolkit().beep();
                return;
            }

            undoManager.undo();
        }
    }

    class ReDrawAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public ReDrawAction() {
            super("Redraw");
        }

        public void actionPerformed(ActionEvent e) {
            editorPane.colorSyntax();
        }
    }

    class ReJustifyAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public ReJustifyAction() {
            super("ReJustify");
        }

        public void actionPerformed(ActionEvent e) {
            editorPane.justifyDocument();
        }
    }


    class RedoAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public RedoAction() {
            super("Redo");
        }

        public void actionPerformed(ActionEvent e) {
            if (undoManager.canRedo()) {
                undoManager.redo();
            } else {
                getToolkit().beep();
            }
        }
    }

    class PasteAction extends DefaultEditorKit.PasteAction {
        private static final long serialVersionUID = 20221225L;

        public PasteAction() {
            super();
        }

        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            editorPane.colorSyntax();
        }
    }

    class FindAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public FindAction() {
            super("Find");
        }

        public void actionPerformed(ActionEvent e) {
            FindDialog findDialog = new FindDialog(MainFrame.getMainFrame(), RuleEditor.this);
            findDialog.setVisible(true);
        }
    }

    class FindAndReplaceAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public FindAndReplaceAction() {
            super("Find And Replace");
        }

        public void actionPerformed(ActionEvent e) {
            FindReplaceDialog findReplaceDialog = new FindReplaceDialog(MainFrame.getMainFrame(), RuleEditor.this);
            findReplaceDialog.setVisible(true);
        }
    }

    class FindAgainAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public FindAgainAction() {
            super("Find Again");
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            find();
        }
    }

    class ReplaceAndFindAgainAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public ReplaceAndFindAgainAction() {
            super("Replace & Find Again");
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            replace();
            find();
        }
    }

    class ReplaceAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public ReplaceAction() {
            super("Replace");
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            replace();
        }
    }

    class ReplaceAllAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public ReplaceAllAction() {
            super("Replace All");
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            replaceAll();
        }
    }

    class CheckProductionsAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public CheckProductionsAction() {
            super("Check Productions");
        }

        public void actionPerformed(ActionEvent ae) {
            List<FeedbackListObject> errors = new LinkedList<>();
            Vector<FeedbackListObject> vecErrors = new Vector<>();

            try {
                Vector<SoarProduction> prodVec = parseProductions();
                MainFrame.getMainFrame().getOperatorWindow().checkProductions((OperatorNode) associatedNode.getParent(),
                        associatedNode,
                        prodVec,
                        errors);
            } catch (TokenMgrError tme) {
                String errMsg = "Could not check productions due to syntax Error:";
                errMsg += tme.getMessage();
                vecErrors.add(new FeedbackListObject(errMsg));
            } catch (ParseException pe) {
                String errMsg = "Could not check productions due to syntax Error:";
                vecErrors.add(new FeedbackListObject(errMsg));
                vecErrors.add(associatedNode.parseParseException(pe));
            }

            if ((errors.isEmpty()) && (vecErrors.isEmpty())) {
                String msg = "No errors detected in " + getFile();
                vecErrors.add(new FeedbackListObject(msg));
            } else {
                EnumerationIteratorWrapper e =
                        new EnumerationIteratorWrapper(errors.iterator());
                while (e.hasMoreElements()) {
                    try {
                        String errorString = e.nextElement().toString();
                        String numberString =
                                errorString.substring(errorString.indexOf("(") + 1,
                                        errorString.indexOf(")"));
                        vecErrors.add(new FeedbackListObject(associatedNode,
                                Integer.parseInt(numberString),
                                errorString,
                                true));
                    } catch (NumberFormatException nfe) {
                        System.out.println("Never happen");
                    }
                }
            }
            MainFrame.getMainFrame().setFeedbackListData(vecErrors);
        }
    }

    /**
     * This class puts the instantiated template in the text area.
     */
    class InsertTemplateAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        private final Template template;

        public InsertTemplateAction(Template t) {
            super(t.getName());
            template = t;
        }

        public void actionPerformed(ActionEvent e) {
            try {
                String s = template.instantiate(RuleEditor.this);
                int pos = editorPane.getCaretPosition();
                editorPane.insert(s, pos);
                editorPane.setCaretPosition(pos + template.getCaretOffset());
            } catch (TemplateInstantiationException tie) {
                JOptionPane.showMessageDialog(RuleEditor.this, tie.getMessage(),
                        "Template Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * This class comments (puts a # in the first position for every line) for the currently selected text
     * of the text area
     */
    class CommentOutAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public CommentOutAction() {
            super("Comment Out");
        }

        private boolean isCommentedOut(String text) {
            if (text.length() == 0) return false;
            String[] lines = text.split("[\r\n]+");
            for (String line : lines) {
                if (line.trim().length() == 0) return false;
                if (line.trim().charAt(0) != '#') return false;
            }
            return true;
        }

        public void actionPerformed(ActionEvent e) {
            //Get the text to be commented out
            try {
                editorPane.expandSelectionToEntireLines();
            } catch (BadLocationException ex) {
                return; //shouldn't happen...
            }
            String selectedText = editorPane.getSelectedText();
            if ((selectedText == null) || (selectedText.length() == 0)) {
                return; //also shouldn't happen
            }

            //If all the selected text is already commented out then
            //we want to uncomment instead (i.e., a toggle)
            if (isCommentedOut(selectedText)) {
                uncommentOutAction.actionPerformed(e);
                return;
            }

            //Save the current selection to restore later
            int selStart = editorPane.getSelectionStart();
            int selEnd = editorPane.getSelectionEnd();

            //comment out the text
            String commentText = "#" + selectedText;
            int nl = commentText.indexOf('\n');
            while (nl != -1) {
                commentText = commentText.substring(0, nl + 1) + "#" + commentText.substring(nl + 1);
                nl = (nl + 1) >= commentText.length() ? -1 : commentText.indexOf('\n', nl + 1);

                //increment selection end to accommodate added char
                selEnd++;
            }

            editorPane.replaceRange(commentText, editorPane.getSelectionStart(), editorPane.getSelectionEnd());

            //restore the selection
            editorPane.setSelectionStart(selStart);
            editorPane.setSelectionEnd(selEnd);

        }//actionPerformed
    }//class CommentOutAction

    /**
     * This class un-comments (takes out the # in the first position for every
     * line) from the currently selected text of the text area.
     */
    class UncommentOutAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public UncommentOutAction() {
            super("Uncomment Out");
        }

        public void actionPerformed(ActionEvent e) {
            String selectedText = editorPane.getSelectedText();
            if (selectedText != null) {
                //Save the current selection to restore later
                int selStart = editorPane.getSelectionStart();
                int selEnd = editorPane.getSelectionEnd();


                String uncommentText = selectedText;
                if (uncommentText.charAt(0) == '#') {
                    uncommentText = uncommentText.substring(1);

                    //decrease the selection range to accommodate missing char
                    selEnd--;
                }
                int nlp = uncommentText.indexOf("\n#");
                while (nlp != -1) {
                    uncommentText = uncommentText.substring(0, nlp + 1) + uncommentText.substring(nlp + 2);
                    nlp = uncommentText.indexOf("\n#", nlp + 1);

                    //decrease the selection range to accommodate missing char
                    selEnd--;
                }

                editorPane.replaceRange(uncommentText, editorPane.getSelectionStart(), editorPane.getSelectionEnd());

                //restore the selection
                if (selEnd > selStart) {
                    editorPane.setSelectionStart(selStart);
                    editorPane.setSelectionEnd(selEnd);
                }


            } else {
                getToolkit().beep();
            }
        }
    }


    /**
     * A simplified version of the TabCompleteAction that only displays
     * the next possible attribute in the feedback window following the user
     * typing a dot/period.
     * Unlike the TabCompleteAction, this action does not ever insert anything
     * into the rule editor, it only displays the attribute options in the feedback window.
     */
    class AutoSoarCompleteAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public AutoSoarCompleteAction() {
            super("Auto Soar Complete");
        }

        public void actionPerformed(ActionEvent e) {
            // Do character insertion and caret adjustment stuff
            SoarDocument doc = (SoarDocument) editorPane.getDocument();
            String textTyped = e.getActionCommand();
            int caretPos = editorPane.getCaretPosition();

            if (textTyped.equals("\n")) {
                (new DefaultEditorKit.InsertBreakAction()).actionPerformed(e);
                caretPos++;
            } else if (!textTyped.equals("\t")) {
                (new DefaultEditorKit.DefaultKeyTypedAction()).actionPerformed(e);
                caretPos++;
            }

            caretPos = doc.autoJustify(caretPos);
            if (caretPos > 0) {
                editorPane.setCaretPosition(caretPos);
            }


            // Advanced Soar Complete stuff
            int pos = editorPane.getCaretPosition();
            String text = editorPane.getText();
            int sp_pos = text.lastIndexOf("sp ", pos);
            if (sp_pos == -1) {
                getToolkit().beep();
                return;
            }
            String prodSoFar = text.substring(sp_pos, pos);
            int arrowPos = prodSoFar.indexOf("-->");
            String end;
            if (arrowPos == -1) {
                end = ") --> }";
            } else {
                end = " <$$$>)}";
            }
            int caret = prodSoFar.lastIndexOf("^");
            int period = prodSoFar.lastIndexOf(".");
            int space = prodSoFar.lastIndexOf(" ");

            // Guarantee that period is more relevant than space and caret
            if (period != -1 && caret != -1 && space != -1 && period > caret && period > space) {
                String userType = prodSoFar.substring(period + 1);
                prodSoFar = prodSoFar.substring(0, period + 1) + "<$$>" + end;
                attributeComplete(userType, prodSoFar);
            }
        } // end of actionPerformed()

        /**
         * uses the soar parser to generate all the possible attributes that can follow
         */
        private void attributeComplete(String userType, String prodSoFar) {
            List<String> completeMatches = getMatchingStrings(userType, prodSoFar);
            if (completeMatches == null) return;
            display(completeMatches);

        }   // end of attributeComplete()

        /**
         * Displays all the possible attributes that can follow the dot/period to the
         * feedback list.
         *
         * @param completeMatches List of Strings representing possible attributes to be displayed
         */
        private void display(List<String> completeMatches) {
            if (completeMatches.size() == 0) {
                MainFrame.getMainFrame().setStatusBarMsg("no auto-complete matches found");
            }
            else {
                MainFrame.getMainFrame().setStatusBarMsgList(completeMatches);
            }
        }    // end of display()


    }//class AutoSoarCompleteAction


    class TabCompleteAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public TabCompleteAction() {
            super("Tab Complete");
        }


        public void actionPerformed(ActionEvent e) {
            int pos = editorPane.getCaretPosition();
            String text = editorPane.getText();
            int sp_pos = text.lastIndexOf("sp ", pos);
            if (sp_pos == -1) {
                getToolkit().beep();
                return;
            }
            String prodSoFar = text.substring(sp_pos, pos);
            int arrowPos = prodSoFar.indexOf("-->");
            String end;
            if (arrowPos == -1) {
                end = ") --> }";
            } else {
                end = " <$$$>)}";
            }
            int caret = prodSoFar.lastIndexOf("^");
            int period = prodSoFar.lastIndexOf(".");
            int space = prodSoFar.lastIndexOf(" ");
            String userType;
            // The most relevant is the caret
            if ((period == -1 && caret != -1 && space != -1 && caret > space)
                    || (period != -1 && caret != -1 && space != -1 && period < caret && space < caret)) {
                userType = prodSoFar.substring(caret + 1);
                prodSoFar = prodSoFar.substring(0, caret + 1) + "<$$>" + end;
                attributeComplete(pos, userType, prodSoFar);
            }
            // The most relevant is the period
            else if (period != -1 && caret != -1 && space != -1 && period > caret && period > space) {
                userType = prodSoFar.substring(period + 1);
                prodSoFar = prodSoFar.substring(0, period + 1) + "<$$>" + end;
                attributeComplete(pos, userType, prodSoFar);
            }
            // The most relevant is the space
            else if ((period == -1 && caret != -1 && space != -1 && space > caret)
                    || (period != -1 && caret != -1 && space != -1 && space > caret && space > period)) {
                userType = prodSoFar.substring(space + 1);
                prodSoFar = prodSoFar.substring(0, space + 1) + "<$$>" + end;
                valueComplete(pos, userType, prodSoFar);
            }
            // Failure
            else {
                getToolkit().beep();
            }
        }//actionPerformed

        private void valueComplete(int pos, String userType, String prodSoFar) {
            try {
                prodSoFar = makeStringValidForParser(prodSoFar);
                SoarParser soarParser = new SoarParser(new StringReader(prodSoFar));
                SoarProduction sp = soarParser.soarProduction();
                OperatorNode on = getNode();
                OperatorNode parent = (OperatorNode) on.getParent();
                List<SoarVertex> matches;
                SoarIdentifierVertex siv = parent.getStateIdVertex();
                if (siv != null) {
                    matches = MainFrame.getMainFrame().getOperatorWindow().getDatamap().matches(siv, sp, "<$$>");
                } else {
                    SoarWorkingMemoryModel dataMap = MainFrame.getMainFrame().getOperatorWindow().getDatamap();
                    matches = dataMap.matches(dataMap.getTopstate(), sp, "<$$>");
                }
                List<String> completeMatches = new LinkedList<>();
                for (SoarVertex vertex : matches) {
                    if (vertex instanceof EnumerationVertex) {
                        EnumerationVertex ev = (EnumerationVertex) vertex;
                        Iterator<String> iter = ev.getEnumeration();
                        while (iter.hasNext()) {
                            String enumString = iter.next();
                            if (enumString.startsWith(userType)) {
                                completeMatches.add(enumString);
                            }
                        }
                    }
                }
                complete(pos, userType, completeMatches);
            } catch (ParseException pe) {
                getToolkit().beep();
            }
        }//valueComplete


        private void complete(int pos, String userType, List<String> completeMatches) {
            if (completeMatches.size() == 0) {
                return;
            }

            if (completeMatches.size() == 1) {
                String matched = completeMatches.get(0);
                editorPane.insert(matched.substring(userType.length()), pos);
                return;
            }

            //If we reach this point:  more than one match
            boolean stillGood = true;
            String addedCharacters = "";
            String matched = completeMatches.get(0);
            int curPos = userType.length();
            while (stillGood && curPos < matched.length()) {
                String newAddedCharacters = addedCharacters + matched.charAt(curPos);
                String potStartString = userType + newAddedCharacters;
                Iterator<String> j = completeMatches.iterator();
                while (j.hasNext()) {
                    String currentString = j.next();
                    if (!currentString.startsWith(potStartString)) {
                        stillGood = false;
                        break;
                    }
                }

                if (stillGood) {
                    addedCharacters = newAddedCharacters;
                    ++curPos;
                }
            }
            editorPane.insert(addedCharacters, pos);

            //report all matches to the user
            MainFrame.getMainFrame().setStatusBarMsgList(completeMatches);

        }//complete

        private void attributeComplete(int pos, String userType, String prodSoFar) {
            List<String> completeMatches = getMatchingStrings(userType, prodSoFar);
            if (completeMatches == null) return;
            complete(pos, userType, completeMatches);

        }//attributeComplete

    }//class TabCompleteAction

    /**
     * getMatchingStrings
     * <p>
     * is a helper method for {@link TabCompleteAction#attributeComplete}
     * and {@link AutoSoarCompleteAction#attributeComplete}.  It retrieves
     * the strings associated with entries in the datamap with attributes
     * that match the user's current production.
     *
     * @param userType  The characters the user has typed so far in the current expression
     * @param prodSoFar The content of the production so far
     * @return a list of possible completions (could be empty)
     */
    private List<String> getMatchingStrings(String userType, String prodSoFar) {
        //parse the code the user has written so far
        prodSoFar = makeStringValidForParser(prodSoFar);
        SoarParser soarParser = new SoarParser(new StringReader(prodSoFar));
        SoarProduction sp;
        try {
            sp = soarParser.soarProduction();
        } catch (ParseException pe) {
            return null;
        }

        //Find all matching string via the datamap
        OperatorNode on = getNode();
        List<SoarVertex> matches;
        SoarIdentifierVertex siv = ((OperatorNode) on.getParent()).getStateIdVertex();
        if (siv != null) {
            matches = MainFrame.getMainFrame().getOperatorWindow().getDatamap().matches(siv, sp, "<$$>");
        } else {
            SoarWorkingMemoryModel dataMap = MainFrame.getMainFrame().getOperatorWindow().getDatamap();
            matches = dataMap.matches(dataMap.getTopstate(), sp, "<$$>");
        }
        List<String> completeMatches = new LinkedList<>();
        //Warning: This iterator can't be given a parameter.  See my note
        // below and in DataMapMatcher.addConstraint() -:AMN:
        Iterator i = matches.iterator();
        while (i.hasNext()) {
            //This cast is wacky.  Let's take what *should* be a SoarVertex
            //and cast it to a String because String objects have been
            //inserted into the Set<SoarVertex> in 'matches'.
            String matched = (String) i.next();
            if (matched.startsWith(userType)) {
                completeMatches.add(matched);
            }
        }
        return completeMatches;
    }

    // 3P
    // Handles the "Runtime|Send Production" menu item
    class SendProductionToSoarAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public SendProductionToSoarAction() {
            super("Send Production");
        }

        public void actionPerformed(ActionEvent e) {
            // Get the production string that our caret is over
            String sProductionString = GetProductionStringUnderCaret();
            if (sProductionString == null) {
                getToolkit().beep();
                return;
            }

            // Get the agent
            Agent agent = MainFrame.getMainFrame().getActiveAgent();
            if (agent == null) {
                JOptionPane.showMessageDialog(RuleEditor.this, "Not connected to an agent.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Send the production to Soar
            String result = agent.ExecuteCommandLine(sProductionString, true);
            MainFrame.getMainFrame().reportResult(result);


        }//actionPerformed
    }//class SendProductionToSoarAction

    // 3P
    // Handles the "Runtime|Send File" menu item
    class SendFileToSoarAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public SendFileToSoarAction() {
            super("Send File");
        }

        public void actionPerformed(ActionEvent e) {
            // Get the agent
            Agent agent = MainFrame.getMainFrame().getActiveAgent();
            if (agent == null) {
                JOptionPane.showMessageDialog(RuleEditor.this, "Not connected to an agent.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }


            // Save the file
            try {
                write();
            } catch (java.io.IOException ioe) {
                ioe.printStackTrace();
            }

            // Call source in Soar
            if (fileName != null) {
                String result = agent.ExecuteCommandLine("source " + "\"" + fileName + "\"", true);
                MainFrame.getMainFrame().reportResult(result);
            }
        }
    }//class SendFileToSoarAction

    // 3P
    // Handles the "Runtime|Send All Files" menu item
    class SendAllFilesToSoarAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public SendAllFilesToSoarAction() {
            super("Send All Files");
        }

        public void actionPerformed(ActionEvent e) {
            // Get the agent
            Agent agent = MainFrame.getMainFrame().getActiveAgent();
            if (agent == null) {
                JOptionPane.showMessageDialog(RuleEditor.this, "Not connected to an agent.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }


            // Save the file
            try {
                write();
            } catch (java.io.IOException ioe) {
                ioe.printStackTrace();
            }

            // We want the name of the top level source file.
            // There may be a simpler way, but I'll walk up the tree of operator nodes
            // to the top and get the file name info from there.
            OperatorNode node = associatedNode;
            while (node != null && !(node instanceof OperatorRootNode)) {
                node = (OperatorNode) node.getParent();
            }

            if (node == null) {
                System.out.println("Couldn't find the top level project node");
                return;
            }

            // Generate the path to the top level source file
            OperatorRootNode root = (OperatorRootNode) node;
            String projectFilename = root.getProjectFile();    // Includes .vsa

            // Swap the extension from .vsa to .soar
            projectFilename = projectFilename.replaceFirst(".vsa", ".soar");

            // Call source in Soar
            String result = agent.ExecuteCommandLine("source " + "\"" + projectFilename + "\"", true);
            MainFrame.getMainFrame().reportResult(result);
        }
    }//class SendAllFilesToSoarAction

    // 3P
    // Handles the "Runtime|Matches Production" menu item
    class SendMatchesToSoarAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public SendMatchesToSoarAction() {
            super("Matches Production");
        }

        public void actionPerformed(ActionEvent e) {
            // Get the agent
            Agent agent = MainFrame.getMainFrame().getActiveAgent();
            if (agent == null) {
                JOptionPane.showMessageDialog(RuleEditor.this, "Not connected to an agent.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Call matches in Soar
            String sProductionName = GetProductionNameUnderCaret();
            if (sProductionName != null) {
                String result = agent.ExecuteCommandLine("matches " + sProductionName, true);
                MainFrame.getMainFrame().reportResult(result);
            }
        }
    }//SendMatchesToSoarAction

    // 3P
    // Handles the "Runtime|Excise Production" menu item
    class SendExciseProductionToSoarAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public SendExciseProductionToSoarAction() {
            super("Excise Production");
        }

        public void actionPerformed(ActionEvent e) {
            // Get the agent
            Agent agent = MainFrame.getMainFrame().getActiveAgent();
            if (agent == null) {
                JOptionPane.showMessageDialog(RuleEditor.this, "Not connected to an agent.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Call excise in Soar
            String sProductionName = GetProductionNameUnderCaret();
            if (sProductionName != null) {
                String result = agent.ExecuteCommandLine("excise " + sProductionName, true);
                MainFrame.getMainFrame().reportResult(result);
            }
        }
    }//class SendExciseProductionToSoarAction

    class BackupThread extends Thread {
        Runnable writeOutControl;
        boolean closed = false;

        public BackupThread() {
            writeOutControl = new Runnable() {
                public void run() {
                    if (!isClosed()) {
                        String modifiedLabelText = modifiedLabel.getText();
                        modifiedLabel.setText("AutoSaving...");
                        makeValidForParser();
                        try {
                            FileWriter fw = new FileWriter(fileName + "~");
                            editorPane.write(fw);
                            fw.close();
                        } catch (IOException ioe) { /* ignore */ }
                        modifiedLabel.setText(modifiedLabelText);
                    } else {
                        closed = true;
                    }
                }
            };

        }

        public void run() {
            try {
                while (!closed) {
                    // 3 minutes
                    sleep(60000 * 3);
                    SwingUtilities.invokeAndWait(writeOutControl);
                }
            } catch (InterruptedException | InvocationTargetException ie) {
                /* ignore */
            }
        }
    }//class BackupThread

    //These are characters are "significant" for the purpose of
    //class CustomUndoableEdit (below)
    private static final char[] SIG_CHARS = {' ', '.', '\n', '\t', '{', '}', '(', ')', '^', '*'};

    /**
     * class CustomUndoableEvent
     * <p>
     * We need to modify the isSignificant() method in
     * AbstractDocument.DefaultDocumentEvent. I don't want to subclass
     * AbstractDocument.DefaultDocumentEvent because I'd have to also subclass
     * AbstractDocument which seems like a can of works.  So, I've done a
     * kludge-y subclass this way (see the 'parent' instance variable).
     * If you see a clever-er solution please be my guest...
     *
     * @author Andrew Nuxoll
     * @version 29 Sep 2022
     */
    class CustomUndoableEdit implements UndoableEdit {

        private final UndoableEdit parent;
        private boolean significant = false;  // is this edit "significant"?

        public CustomUndoableEdit(UndoableEdit initParent) {
            this.parent = initParent;

            //style changes aren't significant
            if (this.parent.getPresentationName().equals("style change")) {
                return;
            }

            //Retrieve the text the user inserted for this edit
            SoarDocument doc = (SoarDocument) editorPane.getDocument();
            String lastText = doc.getLastInsertedText();

            //If the last edit was a remove it's not significant (I think)
            if (lastText == null) return;

            //If the last insertion was the end of a word/line/phrase or
            // a Soar coding element then it's significant  (see the SIG_CHARS
            //constant)
            for (char c : SIG_CHARS) {
                if (lastText.indexOf(c) != -1) {
                    this.significant = true;
                    return;
                }
            }
        }//ctor

        /**
         * this is the only method whose behavior I've actually changed
         */
        @Override
        public boolean isSignificant() {
            return this.significant;
        }

        //All the methods below just use the parent's functionality

        @Override
        public void undo() throws CannotUndoException {
            this.parent.undo();
        }

        @Override
        public boolean canUndo() {
            return this.parent.canUndo();
        }

        @Override
        public void redo() throws CannotRedoException {
            this.parent.redo();
        }

        @Override
        public boolean canRedo() {
            return this.parent.canRedo();
        }

        @Override
        public void die() {
            this.parent.die();
        }

        @Override
        public boolean addEdit(UndoableEdit anEdit) {
            return this.parent.addEdit(anEdit);
        }

        @Override
        public boolean replaceEdit(UndoableEdit anEdit) {
            return this.parent.replaceEdit(anEdit);
        }

        @Override
        public String getPresentationName() {
            return this.parent.getPresentationName();
        }

        @Override
        public String getUndoPresentationName() {
            return this.parent.getUndoPresentationName();
        }

        @Override
        public String getRedoPresentationName() {
            return this.parent.getRedoPresentationName();
        }
    }//class CustomUndoableEdit

    /**
     * class CustomUndoManager
     * <p>
     * The AbstractDocument that SoarDocument inherits from provides the ability
     * to undo/redo.  In particular, it has an UndoManager that manages a series of
     * UndoableEvent objects.  However we have to subclass these two classes
     * (technically UndoableEvent is an interface not a class) in order to get
     * the UndoManager to batch insigificant events together.
     * <p>
     * For example if you type "sp" you've created four undoable events: one for
     * each character and then one for each syntax highlight you've done to that
     * character.  If you hit Undo you'd like all four of those events to be
     * undone.  Doing them one at a time is a chore.
     * <p>
     * Notably a more complex version of CustomUndoManager used to exist but
     * had some buggy behavior.  In particular, it wasn't properly tracking
     * syntax highlighting edits and creating exceptions.
     * <p>
     * I (Nuxoll) couldn't figure out how to fix that implementation, so I
     * replaced it with something simpler (if uglier) version below (and above)
     * that seems to be working better.
     */

    class CustomUndoManager extends UndoManager {
        private static final long serialVersionUID = 20221225L;

        public CustomUndoManager() {
            super();
            setLimit(10000); //This seems to be enough?
        }

        public boolean addEdit(UndoableEdit anEdit) {
            CustomUndoableEdit customEdit = new CustomUndoableEdit(anEdit);

            return super.addEdit(customEdit);
        }
    }//CustomUndoManager


}//class RuleEditor

