package edu.umich.soar.visualsoar.ruleeditor;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.dialogs.EditCustomTemplatesDialog;
import edu.umich.soar.visualsoar.graph.EnumerationVertex;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.graph.SoarVertex;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackEntryOpNode;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.misc.*;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorRootNode;
import edu.umich.soar.visualsoar.parser.*;
import edu.umich.soar.visualsoar.ruleeditor.actions.*;
import edu.umich.soar.visualsoar.util.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Action;
import javax.swing.event.*;
import javax.swing.text.*;

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
    private String fileName;
    private final JLabel lineNumberLabel = new JLabel("Line:");
    private final JLabel modifiedLabel = new JLabel("");

    //This needs to be updated periodically, so it's an instance variable
    private final JMenu templateMenu = new JMenu("Insert Template");


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

	// last thing the user did was save the document; bookkeeping used by undo manager
	private final BooleanProperty lastActionWasSave = new BooleanProperty(true);

	private final CompoundUndoManager undoManager = new CompoundUndoManager(editorPane, lastActionWasSave);

    // ********** Actions ***********
    private final Action saveAction = new SaveAction(this);
    private final Action revertToSavedAction = new RevertToSavedAction(editorPane, modifiedLabel);
    private final Action closeAction = new CloseAction(this);

    private final Action undoAction = new UndoAction(undoManager, getToolkit());
    private final Action redoAction = new RedoAction(undoManager, getToolkit());
    private final Action cutAction = new DefaultEditorKit.CutAction();
    private final Action copyAction = new DefaultEditorKit.CopyAction();
    private final Action pasteAction = new PasteAction(editorPane);
	private final Action selectAllAction = new SelectAllAction(editorPane);
    private final Action insertTextFromFileAction = new InsertTextFromFileAction(editorPane);

    private final Action uncommentOutAction = new UncommentOutAction(editorPane, undoManager, getToolkit());
	private final Action commentOutAction = new CommentOutAction(editorPane, undoManager, uncommentOutAction);

    private final Action reDrawAction = new ReDrawAction(editorPane);
    private final Action reJustifyAction = new ReJustifyAction(editorPane);

    private final Action findAction = new FindAction(this);
    private final FindAgainAction findAgainAction = new FindAgainAction(this);
    private final ReplaceAction replaceAction = new ReplaceAction(this);
    private final ReplaceAndFindAgainAction replaceAndFindAgainAction = new ReplaceAndFindAgainAction(this);
    private final ReplaceAllAction replaceAllAction = new ReplaceAllAction(this);
    private final Action findAndReplaceAction = new FindAndReplaceAction(this);

    private final Action checkProductionsAction = new CheckProductionsAction();
    private final Action editCustomTemplatesAction = new EditCustomTemplatesAction();
    private final Action tabCompleteAction = new TabCompleteAction();


    // 3P
    // Menu item handlers for the STI operations in this window.
    private final Action sendProductionToSoarAction = new SendProductionToSoarAction();
    private final Action sendFileToSoarAction = new SendFileToSoarAction();
    private final Action sendAllFilesToSoarAction = new SendAllFilesToSoarAction();
    private final Action sendMatchesToSoarAction = new SendMatchesToSoarAction(this, getToolkit());
    private final Action sendExciseProductionToSoarAction = new SendExciseProductionToSoarAction(this, getToolkit());

    private BackupThread backupThread;

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
                            int answer = JOptionPane.showConfirmDialog(MainFrame.getMainFrame(), "Save Changes to " + fileName + "?",
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

        //Autobackup
        if (! MainFrame.getMainFrame().isReadOnly()) {
            backupThread = new BackupThread();
            backupThread.start();
        }

        if (edu.umich.soar.visualsoar.misc.Prefs.autoSoarCompleteEnabled.getBoolean()) {
            Keymap keymap = editorPane.getKeymap();

            KeyStroke dot = KeyStroke.getKeyStroke('.');
            Action autoSoarCompleteAction = new AutoSoarCompleteAction(editorPane, getToolkit());
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
                            lineNumberLabel.setText("Line: " + (1 + EditingUtils.getLineOfOffset(
								editorPane.getDocument(), offset)));
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
        //Autobackup
        if (! MainFrame.getMainFrame().isReadOnly()) {
            backupThread = new BackupThread();
            backupThread.start();
        }

        editorPane.addCaretListener(
                new CaretListener() {
                    public void caretUpdate(CaretEvent e) {
                        int offset = e.getDot();
                        try {
                            lineNumberLabel.setText("Line: " + (1 + EditingUtils.getLineOfOffset(
								editorPane.getDocument(), offset)));
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

        //To support read-only mode
        readOnlyDisabledMenuItems.add(cutSelectedTextItem);
        readOnlyDisabledMenuItems.add(pasteTextItem);
        readOnlyDisabledMenuItems.add(deleteSelectedTextItem);

    }

    /**
     * configures the editor in/out of read-only mode
     * @param status  read-only=true  editable=false
     */
    @Override
    public void setReadOnly(boolean status) {

        //enable/disable menu actions that change the contents
        for(JMenuItem item : readOnlyDisabledMenuItems) {
            item.setEnabled(! status);
        }

        //set same status for the associated soar document
        editorPane.setReadOnly(status);

        //Update the title bar to show the status
        String title = getTitle();
        if (status) {
            title = MainFrame.RO_LABEL + title;
        }
        else {
            title = title.replace(MainFrame.RO_LABEL, "");
        }
        setTitle(title);
    }//setReadOnly

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

    @NotNull
    public String getSelectedText() {
      String selectedText = editorPane.getSelectedText();
      if (selectedText == null) {
        return "";
      }
      return selectedText;
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
     * TODO: what if the substring occurs multiple times on the line? We should be using line/column spans, not lineNum + assocString
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

        int nEndPos = findEndOfName(nFirstOpenBrace, text);

        // Return the name to the caller
        return text.substring(nStartPos, nEndPos);
    }

    /**
     * Go through the editor text trying to find the end of the name.
     * Right now we currently define the end to be a space, newline, or '('.
     *
     * TODO: Is this the correct/best way to do this??
     */

    private static int findEndOfName(int nFirstOpenBrace, String text) {
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
        return nCurrentSearchIndex;
    }//findEndOfName

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
        int nEndPos = findEndOfName(nFirstOpenBrace, text);

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
     * This method is used by the Find/Replace dialog.  It takes the
     * selected text within the editorPane checks to see if that is
     * the string that we are looking for, if it is then it replaces
     * it and selects the new text
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

        MainFrame.getMainFrame().getFeedbackManager().setStatusBarMsg("Replaced " + count + " occurrences of \"" + findString + "\" with \"" + replaceString + "\"");
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

        //Delete the associated temp file if it exists
        File tempFile = new File(fileName + "~");
        if (tempFile.exists()) tempFile.delete();

        //for the Undo manager
        lastActionWasSave.set(true);
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

        //To support read-only mode
        readOnlyDisabledMenuItems.add(saveItem);
        readOnlyDisabledMenuItems.add(revertToSavedItem);
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

		JMenuItem selectAllItem = new JMenuItem("Select All");
		selectAllItem.addActionListener(selectAllAction);
		editMenu.add(selectAllItem);

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
        undoItem.setMnemonic(KeyEvent.VK_Z);
        undoItem.setAccelerator(KeyStroke.getKeyStroke("control Z"));
        redoItem.setMnemonic(KeyEvent.VK_R & KeyEvent.SHIFT_DOWN_MASK);
        redoItem.setAccelerator(KeyStroke.getKeyStroke("control shift Z"));
        cutItem.setMnemonic(KeyEvent.VK_T);
        cutItem.setAccelerator(KeyStroke.getKeyStroke("control X"));
        copyItem.setMnemonic(KeyEvent.VK_C);
        copyItem.setAccelerator(KeyStroke.getKeyStroke("control C"));
        pasteItem.setMnemonic(KeyEvent.VK_P);
        pasteItem.setAccelerator(KeyStroke.getKeyStroke("control V"));
		selectAllItem.setMnemonic(KeyEvent.VK_A);
		selectAllItem.setAccelerator(KeyStroke.getKeyStroke("control A"));
        reDrawItem.setMnemonic(KeyEvent.VK_D);
        reDrawItem.setAccelerator(KeyStroke.getKeyStroke("control D"));
        reJustifyItem.setMnemonic(KeyEvent.VK_J);
        reJustifyItem.setAccelerator(KeyStroke.getKeyStroke("control J"));

        commentOutItem.setMnemonic(KeyEvent.VK_SLASH);
        commentOutItem.setAccelerator(KeyStroke.getKeyStroke("control SLASH"));


        menuBar.add(editMenu);

        //To support read-only mode
        readOnlyDisabledMenuItems.add(undoItem);
        readOnlyDisabledMenuItems.add(redoItem);
        readOnlyDisabledMenuItems.add(cutItem);
        readOnlyDisabledMenuItems.add(pasteItem);
        readOnlyDisabledMenuItems.add(commentOutItem);
        readOnlyDisabledMenuItems.add(uncommentOutItem);
        readOnlyDisabledMenuItems.add(insertTextFromFileItem);
        readOnlyDisabledMenuItems.add(reJustifyItem);
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

        //To support read-only mode
        readOnlyDisabledMenuItems.add(findAndReplaceItem);
        readOnlyDisabledMenuItems.add(replaceItem);
        readOnlyDisabledMenuItems.add(replaceAndFindAgainItem);
        readOnlyDisabledMenuItems.add(replaceAllItem);
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
        initTemplatesMenu(
                MainFrame.getMainFrame().getTemplateManager().getRootTemplate(),
                templateMenu);

        soarMenu.setMnemonic(KeyEvent.VK_O);

        menuBar.add(soarMenu);
        menuBar.add(templateMenu);

        //To support read-only mode
        readOnlyDisabledMenuItems.add(tabCompleteItem);
    }

    private void initTemplatesMenu(Template parentTemplate, JMenu parentMenu) {
        if (parentTemplate == null) {
            JMenuItem item = new JMenuItem("No templates found.");
            item.setEnabled(false);
            parentMenu.add(item);
            return;
        }

        // Add default templates...
        Iterator<Template> i = parentTemplate.getChildTemplates();
        while (i.hasNext()) {
            Template t = i.next();
            JMenuItem currentTemplateItem = new JMenuItem(t.getName());
            currentTemplateItem.addActionListener(new InsertTemplateAction(t));
            parentMenu.add(currentTemplateItem);
        }

        parentMenu.addSeparator();
        //user template list
        Vector<String> customTemplates = Prefs.getCustomTemplates();
        if (!customTemplates.isEmpty()) {
            for (String templateName : customTemplates) {
                File ctFile = Prefs.getCustomTemplateFile(templateName);
                JMenuItem currentTemplateItem = new JMenuItem(templateName);
                currentTemplateItem.addActionListener(new InsertCustomTemplateAction(ctFile.getAbsolutePath(), editorPane, (macro) -> {
					try {
						return Template.lookupVariable(macro, this);
					} catch (TemplateInstantiationException e) {
						return "##invalid_template_macro##";
					}
				}));
                parentMenu.add(currentTemplateItem);
            }
            parentMenu.addSeparator();
        }


        JMenuItem customTemplatesItem = new JMenuItem("Edit Custom Templates...");
        customTemplatesItem.addActionListener(editCustomTemplatesAction);
        parentMenu.add(customTemplatesItem);

        //To support read-only mode
        readOnlyDisabledMenuItems.add(parentMenu);

    }//initTemplatesMenu

    /** this is called when custom templates are added/removed so the
     * Insert Template menu can be rebuilt to reflect the change. */
    public void reinitTemplatesMenu() {
        this.templateMenu.removeAll();
        initTemplatesMenu(
                MainFrame.getMainFrame().getTemplateManager().getRootTemplate(),
                templateMenu);
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


	/**
     * reverts the editor's contents to its last saved state
     */
    class RevertToSavedAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;
		private final EditorPane editorPane;
		private final JLabel modifiedLabel;

		public RevertToSavedAction(EditorPane editorPane, JLabel modifiedLabel) {
            super("Revert To Saved");
			this.editorPane = editorPane;
			this.modifiedLabel = modifiedLabel;
			setEnabled(true);
        }

        public void actionPerformed(ActionEvent event) {
            try {
                revert();
            } catch (java.io.IOException ioe) {
                ioe.printStackTrace();
            }
        }

		/**
		 * Reverts the contents of the editor to it's saved copy
		 */
		private void revert() throws IOException {
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
    }


//	TODO: fails if there's a ≈ character in a comment
	class CheckProductionsAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public CheckProductionsAction() {
            super("Check Productions");
        }

        public void actionPerformed(ActionEvent ae) {
            List<FeedbackListEntry> errors = new LinkedList<>();
            Vector<FeedbackListEntry> vecErrors = new Vector<>();

            try {
                Vector<SoarProduction> prodVec = parseProductions();
                MainFrame.getMainFrame()
                    .getOperatorWindow()
                    .getProjectModel()
                    .checkProductions(
                        (OperatorNode) associatedNode.getParent(), associatedNode, prodVec, errors);
            } catch (TokenMgrError tme) {
                String errMsg = "Could not check productions due to syntax Error:";
                errMsg += tme.getMessage();
                vecErrors.add(new FeedbackListEntry(errMsg));
            } catch (ParseException pe) {
                String errMsg = "Could not check productions due to syntax Error:";
                vecErrors.add(new FeedbackListEntry(errMsg));
                vecErrors.add(associatedNode.parseParseException(pe));
            }

            if ((errors.isEmpty()) && (vecErrors.isEmpty())) {
                String msg = "No errors detected in " + getFile();
                vecErrors.add(new FeedbackListEntry(msg));
            } else {
                EnumerationIteratorWrapper e =
                        new EnumerationIteratorWrapper(errors.iterator());
                while (e.hasMoreElements()) {
                    try {
                        String errorString = e.nextElement().toString();
                        String numberString =
                                errorString.substring(errorString.indexOf("(") + 1,
                                        errorString.indexOf(")"));
                        vecErrors.add(new FeedbackEntryOpNode(associatedNode,
                                                              Integer.parseInt(numberString),
                                                              errorString,
                                                       true));
                    } catch (NumberFormatException nfe) {
                        System.out.println("Never happen");
                    }
                }
            }
            MainFrame.getMainFrame().getFeedbackManager().showFeedback(vecErrors);
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
            insertTemplate(template);  //see below
        }
    }

    /** inserts a given template into this file.
     *
     * @param template  the template to insert
     */
    public void insertTemplate(Template template) {
        String text = "";
        try {
            text = template.instantiate(RuleEditor.this);
        } catch (TemplateInstantiationException tie) {
            JOptionPane.showMessageDialog(RuleEditor.this, tie.getMessage(),
                    "Template Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int pos = editorPane.getCaretPosition();
        EditingUtils.insert(editorPane.getDocument(), text, pos);
        editorPane.setCaretPosition(pos + template.getCaretOffset());

        //add a newline because it's likely helpful
		EditingUtils.insert(editorPane.getDocument(), "\n", pos);
    }

    static class EditCustomTemplatesAction extends AbstractAction {
        private static final long serialVersionUID = 20230407L;

        public EditCustomTemplatesAction() {
            super("Edit Custom Templates...");
        }

        public void actionPerformed(ActionEvent e) {
            EditCustomTemplatesDialog ectDialog = new EditCustomTemplatesDialog(MainFrame.getMainFrame());
            ectDialog.setVisible(true);
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
		private final EditorPane editorPane;
		private final Toolkit toolkit;

		public AutoSoarCompleteAction(EditorPane editorPane, Toolkit toolkit) {
            super("Auto Soar Complete");
			this.editorPane = editorPane;
			this.toolkit = toolkit;
		}

        public void actionPerformed(ActionEvent e) {
            // Do character insertion and caret adjustment stuff
            SoarDocument doc = editorPane.getSoarDocument();
            String textTyped = e.getActionCommand();
            int caretPos = editorPane.getCaretPosition();

            //If there is text currently selected, adjust the caret position back
            //to the beginning of the selection since it is about to be deleted
            //(replaced) by what the user just typed.
            String selText = editorPane.getSelectedText();
            if (selText != null) {
                caretPos -= selText.length();
                if (caretPos < 0) {
                    caretPos = 0;  //should never happen...but just in case
                }
            }

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
                toolkit.beep();
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
            if (completeMatches.isEmpty()) {
                MainFrame.getMainFrame().getFeedbackManager().setStatusBarMsg("no auto-complete matches found");
            }
            else {
                MainFrame.getMainFrame().getFeedbackManager().setStatusBarMsgList(completeMatches);
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
                SoarWorkingMemoryModel dataMap = MainFrame.getMainFrame().getOperatorWindow().getDatamap();
                prodSoFar = makeStringValidForParser(prodSoFar);
                SoarParser soarParser = new SoarParser(new StringReader(prodSoFar));
                SoarProduction sp = soarParser.soarProduction();
                OperatorNode on = getNode();
                OperatorNode parent = (OperatorNode) on.getParent();
                List<SoarVertex> matches;
                SoarIdentifierVertex siv = parent.getStateIdVertex(dataMap);
                if (siv != null) {
                    matches = dataMap.matches(siv, sp, "<$$>");
                } else {
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
            if (completeMatches.isEmpty()) {
                return;
            }

            if (completeMatches.size() == 1) {
                String matched = completeMatches.get(0);
				EditingUtils.insert(editorPane.getDocument(), matched.substring(userType.length()), pos);
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
			EditingUtils.insert(editorPane.getDocument(), addedCharacters, pos);

            //report all matches to the user
            MainFrame.getMainFrame().getFeedbackManager().setStatusBarMsgList(completeMatches);

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
        SoarWorkingMemoryModel dataMap = MainFrame.getMainFrame().getOperatorWindow().getDatamap();
        SoarIdentifierVertex siv = ((OperatorNode) on.getParent()).getStateIdVertex(dataMap);
        if (siv != null) {
            matches = dataMap.matches(siv, sp, "<$$>");
        } else {
            matches = dataMap.matches(dataMap.getTopstate(), sp, "<$$>");
        }
        List<String> completeMatches = new LinkedList<>();
        //Ignore Compiler Warning: This iterator can't be given a parameter.  See my note
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
              MainFrame.getMainFrame().getFeedbackManager()
                  .setStatusBarError(
                      "I don't know which production you wish to source; " +
                        "please click inside of it before attempting to send it to Soar again.");
              getToolkit().beep();
              return;
            }

            // Send the production to Soar
            SoarUtils.executeCommandLine(sProductionString, RuleEditor.this, true);
        }
    }//class SendProductionToSoarAction

    // 3P
    // Handles the "Runtime|Send File" menu item
    class SendFileToSoarAction extends AbstractAction {
        private static final long serialVersionUID = 20221225L;

        public SendFileToSoarAction() {
            super("Send File");
        }

        public void actionPerformed(ActionEvent e) {
            // Save the file
            try {
                write();
            } catch (java.io.IOException ioe) {
                ioe.printStackTrace();
            }
            SoarUtils.sourceFile(fileName, RuleEditor.this);
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
              MainFrame.getMainFrame()
                  .reportResult("VisualSoar error: Couldn't find the top level project node");
              return;
            }

            // Generate the path to the top level source file
            OperatorRootNode root = (OperatorRootNode) node;
            String projectFilename = root.getProjectFile();    // Includes .vsa

            // Swap the extension from .vsa to .soar
          projectFilename = projectFilename.replaceFirst("\\.vsa(\\.json)?", ".soar");

          SoarUtils.sourceFile(projectFilename, RuleEditor.this);
        }
    }//class SendAllFilesToSoarAction

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
                    SwingUtilities.invokeAndWait(writeOutControl);
                    // 3 minutes
                    sleep(60000 * 3);
                }
            } catch (InterruptedException | InvocationTargetException ie) {
                /* ignore */
            }
        }
    }//class BackupThread
}//class RuleEditor

