package edu.umich.soar.visualsoar.ruleeditor;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.misc.Prefs;
import edu.umich.soar.visualsoar.misc.SyntaxColor;
import edu.umich.soar.visualsoar.parser.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;
import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class SoarDocument extends DefaultStyledDocument {
    private static final long serialVersionUID = 20221225L;

    //These are enforced by the preferences dialog
    public static final int MIN_FONT_SIZE = 6;
    public static final int DEFAULT_FONT_SIZE = 12;
    public static final int MAX_FONT_SIZE = 100;


    AbstractElement root = (AbstractElement) getDefaultRootElement();
    SyntaxColor[] colorTable;
    boolean inRHS = false; // Are we in the RHS of a production?
    private static int fontSize = DEFAULT_FONT_SIZE;

    // A SoarDocument logs each last inserted/removed text so that
    // RuleEditorUndoManager can decide whether that text is
    // "significant" for the purposes of merging edits.
    // Only one of these variables should be non-null at any time
    private String lastInsertedText = null;
    private String lastRemovedText = null;

    /** to support Read-Only mode */
    public boolean isReadOnly = false;
    private Prefs.PrefsChangeListener fontSizeListener;


  public SoarDocument() {
        colorTable = Prefs.getSyntaxColors().clone();

        //set font size and style
        Style defaultStyle = this.getStyle("default");
        MutableAttributeSet attributeSet = new SimpleAttributeSet();
        StyleConstants.setFontSize(attributeSet, Prefs.editorFontSize.getInt());
        defaultStyle.addAttributes(attributeSet);

        fontSizeListener = newVal -> {
          try {
            int newFontSize = (int) newVal;
            setFontSize(newFontSize);
          } catch (ClassCastException e) {
            e.printStackTrace();
          }
        };

        Prefs.editorFontSize.addChangeListener(fontSizeListener);
    }

  public void setFontSize(int size) {
    SoarDocument.fontSize = size;

    // Create or update a style for the font size
    StyleContext context = StyleContext.getDefaultStyleContext();
    Style fontStyle = context.getStyle(StyleContext.DEFAULT_STYLE);

    // Update the font size in the style
    MutableAttributeSet attributeSet = new SimpleAttributeSet();
    StyleConstants.setFontSize(attributeSet, size);
    fontStyle.addAttributes(attributeSet);

    // Apply the updated style to the entire document
    SwingUtilities.invokeLater(() -> {
      setCharacterAttributes(0, getLength(), attributeSet, false);

      // Notify observers to repaint the editor
      fireChangedUpdate(new DefaultDocumentEvent(0, getLength(), DocumentEvent.EventType.CHANGE));
    });
  }

    public String getLastInsertedText() {
        return this.lastInsertedText;
    }

    public String getLastRemovedText() {
        return this.lastRemovedText;
    }

    public void insertString(int offset,
                             String str,
                             AttributeSet a) throws BadLocationException {

        //Enforce read-only mode
        if (isReadOnly) {
            MainFrame.getMainFrame().rejectForReadOnly();
            return;
        }

        //Please see the big ass comment on this variable above
        this.lastInsertedText = str;
        this.lastRemovedText = null;

        try {
            super.insertString(offset, str, a);
        } catch (BadLocationException ble) {
            this.lastInsertedText = null;
            ble.printStackTrace();
        }

        if (!Prefs.highlightingEnabled.getBoolean()) {
            return;
        }

        int length = str.length();

        if (length == 1) {
            colorSyntax(offset);
        } else {
            colorSyntax(offset, new StringReader(str));
        }//else

    }//insertString()

    public void remove(int offs, int len) throws BadLocationException {
        //Enforce read-only mode
        if (isReadOnly) {
            MainFrame.getMainFrame().rejectForReadOnly();
            return;
        }

        //please see the big ass comment on this variable above
        this.lastRemovedText = this.getText(offs, len);
        this.lastInsertedText = null;

        super.remove(offs, len);

        colorSyntax(offs);
    }


    void replaceRange(String str, int start, int end) {
        //Enforce read-only mode
        if (isReadOnly) {
            MainFrame.getMainFrame().rejectForReadOnly();
            return;
        }

        try {
            remove(start, end - start);
            insertString(start, str, null);
        } catch (BadLocationException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    } // replaceRange()

    String getElementString(int elementIndex, Content content) {
        String theLine = null;

        AbstractElement element;

        if (elementIndex > -1) {
            element = (AbstractElement) root.getElement(elementIndex);
        } else {
            return null;
        }

        try {
            theLine = content.getString(element.getStartOffset(),
                    element.getEndOffset() - element.getStartOffset() - 1);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        return theLine;
    } // getElementString()

    String getElementString(Element element, Content content) {
        String theLine = null;

        try {
            theLine = content.getString(element.getStartOffset(),
                    element.getEndOffset() - element.getStartOffset() - 1);
        } catch (BadLocationException | StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        //Weird bug:  Sometimes the line has a non-ASCII char at the front.
        //            This was confusing justifyDocument() so I kludged
        //            over it here.  Is there a better solution?  idk.  -:AMN:
        if (theLine.length() > 0) {
            char c = theLine.charAt(0);
            while (c > 127) {
                theLine = theLine.substring(1);
                if (theLine.length() == 0) break;
                c = theLine.charAt(0);
            }
        }
        return theLine;
    } // getElementString()

    String getElementString(Element element, Content content, int startOffset) {
        String theLine = null;

        try {
            theLine = content.getString(startOffset,
                    element.getEndOffset() - startOffset - 1);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        return theLine;
    } // getElementString()

    void colorRange(int begPos, int length, int kind) {
        Color theColor;
        try {
           theColor = colorTable[kind];
        }
        catch(ArrayIndexOutOfBoundsException e) {
            //Should never happen but just in case...
            System.err.println("Syntax coloring failed for kind=" + kind + ". Using black.");
            theColor = Color.black;
        }
        SimpleAttributeSet attrib = new SimpleAttributeSet();

        //Check for bad location
        try {
            getText(begPos, length);
        } catch (BadLocationException ble) {
            ble.printStackTrace();
        }

        StyleConstants.setForeground(attrib, theColor);
        setCharacterAttributes(begPos, length, attrib, false);
    }//colorRange()

    /*
     * If the parser barfs on the input we want to casually just keep reading
     * the file.  So we catch the thrown error and and substitute a dummy token.
     * :AMN: 31 Oct '03  (Boo!)
     */
    Token carefullyGetNextToken(SoarParserTokenManager mgr) {
        try {
            return mgr.getNextToken();
        } catch (TokenMgrError tme) {
            return new Token();
        }

    }//carefullyGetNextToken()

    void evaluateToken(Token currToken,
                       int startOffset,
                       SoarParserTokenManager mgr) {
        int begin;
        int length;
        Token startToken = currToken;
        switch (currToken.kind) {
            case SoarParserConstants.RARROW:
                begin = startOffset + currToken.beginColumn;
                length = 3;
                colorRange(begin, length, currToken.kind);
                inRHS = true;
                break;

            case SoarParserConstants.SP:
            case SoarParserConstants.GP:
                begin = startOffset + currToken.beginColumn;
                length = 2;
                colorRange(begin, length, currToken.kind);
                break;

            case SoarParserConstants.CARET: // followed by a STRING
                begin = startOffset + currToken.beginColumn;
                colorRange(begin, 1, SoarParserConstants.DEFAULT);

                currToken = carefullyGetNextToken(mgr);
                begin += 1;
                if ((currToken.kind == SoarParserConstants.SYMBOLIC_CONST)
                        || (currToken.kind == SoarParserConstants.INTEGER_CONST)
                        || (currToken.kind == SoarParserConstants.FLOATING_POINT_CONST)) {

                    length = currToken.image.length();
                    colorRange(begin, length, SoarParserConstants.CARET);

                    currToken = carefullyGetNextToken(mgr);
                    while (currToken.kind == SoarParserConstants.PERIOD) {
                        begin += length + 1; // don't color period

                        currToken = carefullyGetNextToken(mgr);
                        length = currToken.image.length();

                        if ((currToken.kind == SoarParserConstants.SYMBOLIC_CONST)
                                || (currToken.kind == SoarParserConstants.INTEGER_CONST)
                                || (currToken.kind == SoarParserConstants.FLOATING_POINT_CONST)) {

                            colorRange(begin, length, SoarParserConstants.CARET);
                        } else if (currToken.kind == SoarParserConstants.VARIABLE) {
                            colorRange(begin, length, SoarParserConstants.VARIABLE);
                        }

                        currToken = carefullyGetNextToken(mgr);
                    }

                } else if (currToken.kind == SoarParserConstants.VARIABLE) {

                    length = currToken.image.length();
                    colorRange(begin, length, SoarParserConstants.VARIABLE);

                    currToken = carefullyGetNextToken(mgr);
                    while (currToken.kind == SoarParserConstants.PERIOD) {
                        begin += length + 1; // don't color period

                        currToken = carefullyGetNextToken(mgr);
                        length = currToken.image.length();

                        if ((currToken.kind == SoarParserConstants.SYMBOLIC_CONST)
                                || (currToken.kind == SoarParserConstants.INTEGER_CONST)
                                || (currToken.kind == SoarParserConstants.FLOATING_POINT_CONST)) {

                            colorRange(begin, length, SoarParserConstants.CARET);
                        } else if (currToken.kind == SoarParserConstants.VARIABLE) {
                            colorRange(begin, length, SoarParserConstants.VARIABLE);
                        }

                        currToken = carefullyGetNextToken(mgr);
                    }

                }
                // We need to make sure that this token is on the same line as the first
                // token
                if (startToken.beginLine != currToken.beginLine) {
                    Element rootElement = getDefaultRootElement();
                    Element childElement = rootElement.getElement(currToken.beginLine);
                    evaluateToken(currToken, childElement.getStartOffset() - 1, mgr);
                } else {
                    evaluateToken(currToken, startOffset, mgr);
                }
                break;

            case SoarParserConstants.VARIABLE:
                begin = startOffset + currToken.beginColumn;
                // XXX Assumes that tokens do not cross line barriers
                length = currToken.image.length();
                colorRange(begin, length, currToken.kind);
                break;

            case SoarParserConstants.SYMBOLIC_CONST:
                begin = startOffset + currToken.beginColumn;

                //If the token has no image then no highlighting to do
                if (currToken.image == null) break;

                //NOTE:  This assumes that tokens do not cross line barriers
                length = currToken.image.length();

                //Since the syntax highlighter has to guess which lexical state
                //to start in, me may see an "sp" here being misinterpreted as
                //a symbolic constant.  So we catch that here.
                if (currToken.image.equals("sp")) {
                    currToken.kind = SoarParserConstants.SP;
                    colorRange(begin, 2, currToken.kind);
                    inRHS = false;
                } else if (currToken.image.equals("gp")) {
                    currToken.kind = SoarParserConstants.GP;
                    colorRange(begin, 2, currToken.kind);
                    inRHS = false;
                } else {
                    //This is really a symbolic constant
                    colorRange(begin, length, currToken.kind);
                }
                break;

            case SoarParserConstants.RBRACE:
                if (inRHS) {
                    //A closing brace in the RHS indicates we've finished
                    //a Soar production and should be back in the
                    //DEFAULT lexical state.  The parser does not do
                    //this for us because we're using it strictly as
                    //a tokenizer right now.
                    mgr.SwitchTo(SoarParserConstants.DEFAULT);
                    inRHS = false;
                }
                break;

            case SoarParserConstants.LPAREN:
            case SoarParserConstants.RPAREN:
            case SoarParserConstants.AMPERSAND:
            case SoarParserConstants.ATSIGN:
            case SoarParserConstants.COMMA:
            case SoarParserConstants.EQUAL:
            case SoarParserConstants.EMARK:
            case SoarParserConstants.GREATER:
            case SoarParserConstants.HYPHEN:
            case SoarParserConstants.LESS:
            case SoarParserConstants.PERIOD:
            case SoarParserConstants.PLUS:
            case SoarParserConstants.QMARK:
            case SoarParserConstants.TILDE:
            case SoarParserConstants.LSQBRACKET:
            case SoarParserConstants.RSQBRACKET:
            case SoarParserConstants.EXPONENT:
                begin = startOffset + currToken.beginColumn;
                colorRange(begin, 1, SoarParserConstants.DEFAULT);
                break;

            case SoarParserConstants.LDISJUNCT:
            case SoarParserConstants.RDISJUNCT:
            case SoarParserConstants.GEQUAL:
            case SoarParserConstants.LEQUAL:
            case SoarParserConstants.NEQUAL:
                begin = startOffset + currToken.beginColumn;
                colorRange(begin, 2, SoarParserConstants.DEFAULT);
                break;


            default:
                break;

        } // token cases

    } // evaluateToken()

    public void colorSyntax(Reader r) {
        (new ColorSyntaxThread(r)).start();
    }

    class ColorSyntaxThread extends Thread {
        Reader r;

        public ColorSyntaxThread(Reader inReader) {
            r = inReader;
        }

        public void run() {
            colorSyntax();
        }

        //Color the syntax of an entire file
        public void colorSyntax() {
            Token currToken;
            Element currElem;
            int currLineNum;
            int offset;
            SoarParserTokenManager mgr =
                    new SoarParserTokenManager(new SimpleCharStream(r, 0, 0));

            try {
                currToken = mgr.getNextToken();
            } catch (TokenMgrError tme) {
                /* this just means the syntax wasn't valid at
                 * the current state of entry we assume that more
                 * is coming and give up for now
                 */
                return;
            }

            currLineNum = currToken.beginLine;
            currElem = root.getElement(currLineNum);

            while (currToken.kind != SoarParserConstants.EOF) {

                if (currLineNum != currToken.beginLine) {
                    currLineNum = currToken.beginLine;
                    currElem = root.getElement(currLineNum);
                }

                if (currLineNum == 0) {
                    offset = currElem.getStartOffset();
                } else {
                    offset = currElem.getStartOffset() - 1;
                }

                evaluateToken(currToken, offset, mgr);

                try {
                    currToken = mgr.getNextToken();
                } catch (TokenMgrError tme) {
                    /* this just means the syntax wasn't valid at
                     * the current state of entry we assume that more
                     * is coming and give up for now
                     */
                    return;
                }

            }  // iterate through tokens
        } // colorSyntax() (whole file)

    } // ColorSyntaxThread

    /**
     * The Token class created by JavaCC does not have a copy ctor.
     * Since we can't edit that class we improvise a copy ctor here.
     *
     * @param dst Token to copy to
     * @param src Token to copy from
     * @author Andrew Nuxoll
     * 24 Nov 03
     */
    void copyToken(Token dst, Token src) {
        dst.kind = src.kind;
        dst.beginLine = src.beginLine;
        dst.beginColumn = src.beginColumn;
        dst.endLine = src.endLine;
        dst.endColumn = src.endColumn;
        dst.image = src.image;
        dst.next = src.next;
        dst.specialToken = src.specialToken;
    }


    /**
     * Given a reader, this function attempts to guess the correct lexical state
     * to start the parser in.  Once guessed, the correct first token and
     * token mamager is returned.
     *
     * @param r   Reader
     * @param tok Token
     * @return a SoarParserTokenManager
     * @author Andrew Nuxoll
     * 17 Nov 03
     */
    protected SoarParserTokenManager guessLexicalState(Reader r,
                                                       Token tok) {
        SoarParserTokenManager mgr;
        Token ispTok = new Token(); // Token retrieved using IN_SOAR_PRODCTION


        //Save our position in the reader
        try {
            r.mark(4096);

            //Since we don't know what lexical state we're in, guess
            //the most likely state: IN_SOAR_PRODUCTION
            mgr = new SoarParserTokenManager(new SimpleCharStream(r, 0, 0));
            mgr.SwitchTo(SoarParserConstants.IN_SOAR_PRODUCTION);

            //Get a token
            try {
                ispTok = mgr.getNextToken();
            } catch (TokenMgrError tme) {
                //Set the kind so that the other lexical state is tried.
                ispTok.kind = SoarParserConstants.SYMBOLIC_CONST;
            }

            //If the token is anything other than SYMBOLIC_CONST then
            //we probably picked the right lexical state.
            if (ispTok.kind != SoarParserConstants.SYMBOLIC_CONST) {
                copyToken(tok, ispTok);
                return mgr;
            }

            //Reset the reader and try the DEFAULT lexical state
            r.reset();
            mgr = new SoarParserTokenManager(new SimpleCharStream(r, 0, 0));

            //Get a token
            Token defTok; // Token retrieved using DEFAULT
            try {
                defTok = mgr.getNextToken();
            } catch (TokenMgrError tme) {
                //Guess we'd better stick with IN_SOAR_PRODUCTION
                mgr = new SoarParserTokenManager(new SimpleCharStream(r, 0, 0));
                mgr.SwitchTo(SoarParserConstants.IN_SOAR_PRODUCTION);
                copyToken(tok, ispTok);
                return mgr;
            }

            //Looks like the default state worked
            copyToken(tok, defTok);
        } catch (IOException ioe) {
            //This exception occurs when we call reset() on a closed reader
            //In this case we should stick with IN_SOAR_PRODUCTION
            mgr = new SoarParserTokenManager(new SimpleCharStream(r, 0, 0));
            mgr.SwitchTo(SoarParserConstants.IN_SOAR_PRODUCTION);
            copyToken(tok, ispTok);
            return mgr;
        }

        return mgr;
    }//guessLexicalState()

    //Color the syntax of a single line
    public void colorSyntax(int caretPos) {
        Content data = getContent();
        int lineNum = root.getElementIndex(caretPos);
        Element currElem = root.getElement(lineNum);
        Token currToken = new Token();

        //If the element has zero size no work need be done
        if (currElem == null) return;
        if (currElem.getEndOffset() <= currElem.getStartOffset()) return;

        String currLine;
        int offset = currElem.getStartOffset();

        currLine = getElementString(currElem, data);

        //Create a token manager. Since we don't know what lexical state we're
        //in, guess the most likely state.
        Reader r = new StringReader(currLine);
        SoarParserTokenManager mgr = guessLexicalState(r, currToken);

        // init all the text to black
        colorRange(offset, currLine.length(), SoarParserConstants.DEFAULT);

        while (currToken.kind != SoarParserConstants.EOF) {
            evaluateToken(currToken, offset, mgr);

            try {
                currToken = mgr.getNextToken();
            } catch (TokenMgrError tme) {
                /* this just means the syntax wasn't valid at
                 * the current state of entry we assume that more
                 * is coming and give up for now
                 */
                return;
            }

        }  // iterate through tokens


    } // colorSyntax (one line)


    //Color the syntax of a specified region
    public void colorSyntax(int caretPos, Reader r) {
        int startLineNum = root.getElementIndex(caretPos);
        Token currToken = new Token();

        //Create a token manager with our best guess for the current lexical
        //state and get the first token.
        SoarParserTokenManager mgr = guessLexicalState(r, currToken);

        //To make inline comments work we need track the end position of the
        // previous token.
        while (currToken.kind != SoarParserConstants.EOF) {
            int currLineNum = startLineNum + currToken.beginLine;
            Element currElem = root.getElement(currLineNum);

            if (currToken.beginLine == 0) {
                evaluateToken(currToken, currElem.getStartOffset(), mgr);
            } else {
                evaluateToken(currToken, currElem.getStartOffset() - 1, mgr);
            }

            try {
                currToken = mgr.getNextToken();
            } catch (TokenMgrError tme) {
                /* this just means the syntax wasn't valid at
                 * the current state of entry we assume that more
                 * is coming and give up for now
                 */
                return;
            }

        }  // iterate through tokens
    } // colorSyntax() (specific section)

    /**
     * Justifies a chunk of text from in the rule editor.
     * If nothing is highlighted, then justifies the entire document
     *
     * TODO:  This method should be refactored to make more manageable
     *
     *  @param selectionStart the position of the beginning of the highlighted text
     * @param selectionEnd   the position of the end of the highlighted text
     */
    public void justifyDocument(int selectionStart, int selectionEnd) {

        //Enforce read-only mode
        if (isReadOnly) {
            MainFrame.getMainFrame().rejectForReadOnly();
            return;
        }

        Content data = getContent();

        String prevLine = null;
        String currLine;
        String newCurrLine;
        String indentString;

        int elemIndex;
        int prevLineIndex;
        int endIndex;
        int numSpaces;
        int currLineBegin;
        int currLineEnd;

        char lastChar;
        char[] indentChars;

        boolean firstProduction = true;

        AbstractElement currLineElem;

        // check if block of text is selected or not
        if (selectionStart == selectionEnd) {
            // nothing selected, start from beginning of file and do every line
            selectionStart = 0;
            selectionEnd = getLength() - 1;
            firstProduction = false;    // makes sure vs doesn't justify stuff before
            // the first production (ie echo)
        }

        elemIndex = root.getElementIndex(selectionStart);
        prevLineIndex = elemIndex - 1;
        endIndex = root.getElementIndex(selectionEnd);

        // endIndex is one less if last line just a line feed
        String lastLine = getElementString(root.getElement(endIndex), data);
        if (lastLine.trim().length() == 0) {
            endIndex--;
        }

        // This while loop goes through every line of selected text for
        // justification
        while ((elemIndex <= endIndex) && (selectionStart <= (getLength() - 1))) {
            currLineElem = (AbstractElement) root.getElement(elemIndex);
            currLineBegin = currLineElem.getStartOffset();
            currLineEnd = currLineElem.getEndOffset() - 1;
            currLine = getElementString(currLineElem, data);
            indentString = "";
            numSpaces = 0;

            // Get last prevLine that isn't a blank line or comment
            if (elemIndex > 0) {
                boolean goodLine = false;
                while ((prevLineIndex > -1) && !goodLine) {
                    prevLine = getElementString(prevLineIndex, data);
                    if ((prevLine == null)
                            || (prevLine.trim().startsWith("#"))
                            || (prevLine.trim().length() == 0)) {
                        prevLineIndex--;
                    } else {
                        goodLine = true;
                    }
                }   // end of while getting last previous line of Soar code
            }
            newCurrLine = currLine.trim();

            if ((newCurrLine.length() != 0)
                    && ((newCurrLine.charAt(0) == '}')
                    || (newCurrLine.startsWith("-->"))
                    || (newCurrLine.startsWith("sp"))
                    || (newCurrLine.startsWith("gp")))) {
                if (newCurrLine.startsWith("sp") || newCurrLine.startsWith("gp")) {
                    firstProduction = true;
                }
            } else if ((newCurrLine.length() != 0)
                    && (newCurrLine.charAt(0) == '#')) {
                numSpaces = currLine.indexOf('#');    // don't move comment.
            } else if (prevLine == null) {
                if (newCurrLine.startsWith("sp") || newCurrLine.startsWith("gp")) {
                    firstProduction = true;
                }
            }
            // already returned if prevLine == null
            else if (prevLine.trim().length() != 0) {
                prevLine = cropComments(prevLine);  // omit comments from the end of the string
                lastChar = prevLine.charAt(prevLine.length() - 1);

                if (prevLine.startsWith("sp") || prevLine.startsWith("gp") || prevLine.endsWith("-->")) {
                    numSpaces = 3;
                } else if ((newCurrLine.startsWith("^")
                        || newCurrLine.startsWith("-^"))) {
                    String currentLine = prevLine;
                    int currentElementIndex = prevLineIndex;
                    boolean done = false;

                    while (!done && currentLine != null) {
                        int upPos = currentLine.indexOf('^');
                        if (upPos != -1) {
                            numSpaces = upPos;
                            done = true;
                        } else {
                            --currentElementIndex;
                            currentLine = getElementString(currentElementIndex,
                                    data);
                        }
                    } // while searching previous lines for last instance of '^'

                    // if couldn't find a previous '^', set numspaces to end of
                    // last line
                    if (!done) {
                        prevLine = cropComments(prevLine);
                        // tells us where the code actually starts (skips
                        // leading whitespace)
                        int firstCharLoc = prevLine.indexOf((prevLine.trim()).charAt(0));
                        numSpaces = prevLine.trim().length() + firstCharLoc + 1;
                    }
                } else if (lastChar == ')') {
                    int currentElementIndex = prevLineIndex;
                    boolean done = false;
                    numSpaces = 3;
                    int parenCount = 0;
                    String currentLine = prevLine;

                    while ((!done)
                            && (currentLine != null)
                            && (currentElementIndex > -1)) {
                        for (int i = currentLine.length() - 1; i >= 0; --i) {
                            if (currentLine.charAt(i) == ')') {
                                ++parenCount;
                            } else if (currentLine.charAt(i) == '(') {
                                --parenCount;
                                if (parenCount <= 0) {
                                    numSpaces = i;
                                    done = true;
                                    break;
                                }
                            }
                        }
                        --currentElementIndex;

                        // Get the next previous line of valid Soar code
                        boolean soarLine = false;
                        while ((currentElementIndex > -1) && !soarLine) {
                            currentLine = getElementString(currentElementIndex, data);
                            if ((currentLine == null) || currentLine.trim().startsWith("#") || (currentLine.length() == 0)) {
                                currentElementIndex--;
                            } else {
                                soarLine = true;
                            }
                        }   // end of while getting last previous line of Soar code

                    } // end of while
                } // end of else if last char == ')'
                else if (lastChar == '{') {
                    numSpaces = prevLine.indexOf('{');
                } else if (newCurrLine.startsWith("(") && lastChar == '}') {
                    numSpaces = 3;
                }
                else if (newCurrLine.startsWith("-(") && lastChar == '}') {
                    numSpaces = 3;
                } else if (prevLineIndex >= 0) {
                    // look for a ^ on previous line
                    String fullPrevLine = getElementString(prevLineIndex, data);  // get the full previous line
                    int caretLocation = fullPrevLine.indexOf('^');
                    if (caretLocation != -1) {
                        // Get position past string that follows the caret (ie ^string )
                        while (fullPrevLine.charAt(caretLocation) != ' ') {
                            caretLocation++;
                        }
                        // look to see if string past ^string is << or { and if
                        // so, use that position
                        while (fullPrevLine.charAt(caretLocation) == ' ') {
                            caretLocation++;
                        }
                        if (fullPrevLine.charAt(caretLocation) == '{') {
                            numSpaces = caretLocation + 2;
                        } else if ((fullPrevLine.charAt(caretLocation) == '<')
                                && (fullPrevLine.charAt(caretLocation + 1) == '<')) {
                            numSpaces = caretLocation + 3;
                        } else {
                            numSpaces = caretLocation;
                        }
                    } // end of prevLine contains a '^' and thus currLine
                    // is considered a value
                    else {
                        // else line up with previous line
                        numSpaces = fullPrevLine.indexOf((fullPrevLine.trim()).charAt(0));
                    }
                }    // end of else if prevlineindex > 1


                //  Does not fit a previous constraint and therefore is possibly
                //  considered a VALUE of a wme

            }   // end of if previous line was not length zero


            //Now that we have the desired position, line up line based on that position
            // properly line up negated conditions
            if ((newCurrLine.length() != 0)
                    && (newCurrLine.charAt(0) == '-')
                    && (numSpaces > 0)) {
                numSpaces--;
            }
            //  Empty line feed found, remove is within production
            if ((!(prevLine == null))
                    && (newCurrLine.length() == 0)
                    && (prevLine.trim().length() != 0)
                    && firstProduction) {
                if (!(prevLine.trim().charAt(prevLine.trim().length() - 1) == '}')) {
                    indentString = "";
                    newCurrLine = "";
                    try {
                        remove(currLineBegin - 1,
                                currLineEnd - currLineBegin + 1);
                    } catch (BadLocationException e) {
                        throw new IllegalArgumentException(e.getMessage());
                    }
                }
            }
            // no line up char found, or coincidental 3 space indent
            else if ((numSpaces == 3) || (numSpaces < 0)) {
                indentString = "   ";
            }
            // variable indent to line up chars vertically
            else if (numSpaces > 0) {
                indentChars = new char[numSpaces];

                for (int i = 0; i < numSpaces; i++) {
                    indentChars[i] = ' ';
                }
                indentString = new String(indentChars);
            }
            // no indent for normal chars, or coincidental 0 space indent
            newCurrLine = indentString + newCurrLine;
            if (!newCurrLine.equals(currLine)
                    && (newCurrLine.length() != 0)) {
                // not already justified
                replaceRange(newCurrLine, currLineBegin, currLineEnd);
            }

            currLineEnd = currLineElem.getEndOffset() - 1;
            selectionStart = currLineEnd + 1;
            if (selectionStart <= (getLength() - 1)) {
                elemIndex = root.getElementIndex(selectionStart);
                prevLineIndex = elemIndex - 1;
            }
        }     // end of while going through every line

    } // justifyDocument()

    public int autoJustify(int caretPos) {
        if (!edu.umich.soar.visualsoar.misc.Prefs.autoIndentingEnabled.getBoolean()) {
            return -1;
        }

        Content data = getContent();
        String prevLine = null;
        String indentString = "";
        int elemIndex = root.getElementIndex(caretPos);
        int prevLineIndex = elemIndex - 1;
        int numSpaces = 0;
        char lastChar;
        char[] indentChars;
        boolean leftOfText = true;

        AbstractElement currLineElem = (AbstractElement) root.getElement(elemIndex);
        int currLineBegin = currLineElem.getStartOffset();
        int currLineEnd = currLineElem.getEndOffset() - 1;
        String currLine = getElementString(currLineElem, data);
        String afterCaretString = getElementString(currLineElem, data, caretPos);


        // Gets the last line of Soar code (skips blanks lines and comment lines)
        if (elemIndex > 0) {
            boolean goodLine = false;
            while ((prevLineIndex > -1) && !goodLine) {
                prevLine = getElementString(prevLineIndex, data);
                if ((prevLine == null) || prevLine.trim().startsWith("#") || (prevLine.trim().length() == 0)) {
                    prevLineIndex--;
                } else {
                    goodLine = true;
                }
            }   // end of while getting last previous line of Soar code
        } else { // no indent for 1st line
            return -1;
        }
        if (prevLine == null) {
            return -1;
        }
        String newCurrLine = currLine.trim();

        if ((newCurrLine.length() == 0)) {
            //Attempt to indent the appropriate number of spaces
            String trimmed = prevLine.trim();
            if (((trimmed.startsWith("sp")) && (trimmed.contains("{")))
                    || ((trimmed.startsWith("gp")) && (trimmed.contains("{")))
                    || ((trimmed.startsWith("(")) && (trimmed.endsWith(")")))
                    || ((trimmed.startsWith("^")) && (trimmed.endsWith(")")))
                    || (trimmed.startsWith("-->"))) {
                numSpaces = 3;
            } else if ((trimmed.startsWith("(") || trimmed.startsWith("^"))
                    && (!trimmed.contains(")"))
                    && (trimmed.contains("^"))) {
                numSpaces = prevLine.indexOf("^");
            }
        } else if (newCurrLine.charAt(0) == '}' || newCurrLine.startsWith("-->")) {
            //this is deliberately empty
        }
        // already returned if prevLine == null
        else if (prevLine.trim().length() != 0) {
            prevLine = cropComments(prevLine);  // omit comments from the end of the string
            lastChar = prevLine.charAt(prevLine.length() - 1);

            if (prevLine.startsWith("sp") || prevLine.startsWith("gp") || prevLine.endsWith("-->")) {
                numSpaces = 3;
            } else if (newCurrLine.startsWith("^") || newCurrLine.startsWith("-^")) {
                String currentLine = prevLine;
                int currentElementIndex = elemIndex;
                boolean done = false;
                while (!done && currentLine != null) {
                    int upPos = currentLine.indexOf('^');
                    if (upPos != -1) {
                        numSpaces = upPos;
                        done = true;
                    } else {
                        --currentElementIndex;
                        currentLine = getElementString(currentElementIndex, data);
                    }
                }
                // if couldn't find a previous '^', set numspaces to end of last line
                if (!done) {
                    prevLine = cropComments(prevLine);
                    int firstCharLoc = prevLine.indexOf((prevLine.trim()).charAt(0));
                    numSpaces = prevLine.trim().length() + firstCharLoc;
                }
            } else if (newCurrLine.startsWith("<")) {
                String currentLine = prevLine;
                int currentElementIndex = elemIndex;
                boolean done = false;
                while (!done && currentLine != null) {
                    int upPos = currentLine.indexOf('<');
                    if (upPos != -1) {
                        numSpaces = upPos;
                        done = true;
                    } else {
                        --currentElementIndex;
                        currentLine = getElementString(currentElementIndex, data);
                    }
                }
                // if couldn't find a previous '<', set numspaces to end of last line
                if (!done) {
                    prevLine = cropComments(prevLine);
                    int firstCharLoc = prevLine.indexOf((prevLine.trim()).charAt(0));
                    numSpaces = prevLine.trim().length() + firstCharLoc;
                }
            } else if (lastChar == ')') {
                int currentElementIndex = elemIndex - 1;
                boolean done = false;
                int count = 0;
                String currentLine = prevLine;

                while ((!done && currentLine != null)
                        && (currentElementIndex > -1)) {
                    for (int i = currentLine.length() - 1; i >= 0; --i) {
                        if (currentLine.charAt(i) == ')') {
                            ++count;
                        } else if (currentLine.charAt(i) == '(') {
                            --count;
                            if (count <= 0) {
                                numSpaces = i;
                                done = true;
                                break;
                            }
                        }
                    }
                    --currentElementIndex;

                    // Get the last previous line of valid Soar code
                    boolean soarLine = false;
                    while ((currentElementIndex > -1) && !soarLine) {
                        currentLine = getElementString(currentElementIndex, data);
                        if ((currentLine == null)
                                || (currentLine.trim().startsWith("#"))
                                || (currentLine.length() == 0)) {
                            currentElementIndex--;
                        } else {
                            soarLine = true;
                        }
                    }

                }   // while finding the matching parentheses
            }   // end of else if last char == ')'
            else if (lastChar == '{') {
                numSpaces = prevLine.indexOf('{');
            } else if (newCurrLine.startsWith("(") && lastChar == '}') {
                numSpaces = 3;
            } else if (prevLineIndex >= 0) {
                // look for a ^ on previous line
                String fullPrevLine = getElementString(prevLineIndex, data);  // get the full previous line
                int caretLocation = fullPrevLine.indexOf('^');
                if (caretLocation != -1) {
                    // Get position past string that follows the caret (ie ^string )
                    while (fullPrevLine.charAt(caretLocation) != ' ') {
                        caretLocation++;
                    }
                    // look to see if string past ^string is << or { and if so, use that position
                    while (fullPrevLine.charAt(caretLocation) == ' ') {
                        caretLocation++;
                    }
                    if (fullPrevLine.charAt(caretLocation) == '{') {
                        numSpaces = caretLocation + 2;
                    } else if ((fullPrevLine.charAt(caretLocation) == '<')
                            && (fullPrevLine.charAt(caretLocation + 1) == '<')) {
                        numSpaces = caretLocation + 3;
                    } else {
                        numSpaces = caretLocation;
                    }
                } // end of prevLine contains a '^' and thus currLine considered
                // a value
                else {
                    // else line up with previous line
                    numSpaces = fullPrevLine.indexOf((fullPrevLine.trim()).charAt(0));
                }
            }    // end of else if prevlineindex > 1

            // Does not fit a previous constraint and therefore is possibly
            // considered a VALUE of a wme
        }
        // else { numSpaces = 0; } , already initialized

        // properly line up negated conditions
        if ((newCurrLine.length() != 0) && (newCurrLine.charAt(0) == '-') &&
                (numSpaces > 0)) {

            numSpaces--;
        }

        // no line up char found, or coincidental 3 space indent
        if ((numSpaces == 3) || (numSpaces < 0)) {
            indentString = "   ";
            numSpaces = 3;
        }
        // variable indent to line up chars vertically
        else if (numSpaces > 0) {
            indentChars = new char[numSpaces];

            for (int i = 0; i < numSpaces; i++) {
                indentChars[i] = ' ';
            }

            indentString = new String(indentChars);
        }
        // no indent for normal chars, or coincidental 0 space indent

        newCurrLine = indentString + newCurrLine;

        if (!newCurrLine.equals(currLine)) { // not already justified
            replaceRange(newCurrLine, currLineBegin, currLineEnd);
        }

        /* find out if if the caret is left of all the text on the line
         * check for non-whitespace left of the caret
         */
        for (int i = caretPos - currLineBegin - 1; (i >= 0) && leftOfText; i--) {
            if (!Character.isWhitespace(currLine.charAt(i))) {
                leftOfText = false;
            }
        }

        if (leftOfText) {
            return (currLineBegin + numSpaces);
        } else { // caret is in the middle, or after text
            return (newCurrLine.lastIndexOf(afterCaretString)
                    + currLineBegin);
        }

    } // autoJustify()


    /**
     * Function removes any trailing comments from the end of a string.
     * Comments are defined as anything following a '#', ";#", "; #" or ";  #"
     * Note:  the use of semicolons denoting comments is no longer used in Soar, but
     * still supported.
     *
     * @param prevLine the string that is to be cropped.
     */
    public String cropComments(String prevLine) {
        // omit comments from the end of the previous line for testing
        if (!prevLine.startsWith("#")) {
            if (prevLine.contains(";#")) {
                prevLine = prevLine.substring(0, prevLine.indexOf(";#") - 1);
            } else if (prevLine.contains("; #")) {
                prevLine = prevLine.substring(0, prevLine.indexOf("; #") - 1);
            } else if (prevLine.contains(";  #")) {
                prevLine = prevLine.substring(0, prevLine.indexOf(";  #") - 1);
            } else if (prevLine.indexOf('#') != -1) {
                prevLine = prevLine.substring(0, prevLine.indexOf('#') - 1);
            }
        }

        //remove any trailing whitespace
        prevLine = prevLine.replaceAll("\\s+$", "");

        return prevLine;
    }

  /** Cleanup method to be called when the document is closed. */
  public void close() {
    // Remove the font size listener so that we don't accumulate them every time a new window
    // is opened and closed
    if (fontSizeListener != null) {
      Prefs.editorFontSize.removeChangeListener(fontSizeListener);
      fontSizeListener = null;
    }
  }
} // class SoarDocument
