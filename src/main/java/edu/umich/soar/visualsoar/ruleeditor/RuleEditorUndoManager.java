package edu.umich.soar.visualsoar.ruleeditor;

import edu.umich.soar.visualsoar.util.BooleanProperty;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.util.Objects;

/**
 * The AbstractDocument that SoarDocument inherits from provides the ability to undo/redo. In
 * particular, it has an UndoManager that manages a series of UndoableEvent objects. However we have
 * to subclass these two classes (technically UndoableEvent is an interface not a class) in order to
 * get the UndoManager to batch insignificant events together.
 *
 * <p>For example if you type "sp" you've created four undoable events: one for each character and
 * then one for each syntax highlight you've done to that character. If you hit Undo you'd like all
 * four of those events to be undone. Doing them one at a time is a chore.
 *
 * <p>Notably a more complex version of a custom UndoManager used to exist but had some buggy
 * behavior. In particular, it wasn't properly tracking syntax highlighting edits and creating
 * exceptions.
 *
 * <p>I (Nuxoll) couldn't figure out how to fix that implementation, so I replaced it with something
 * simpler (if uglier) below (and above) that seems to be working better. TODO: I wish this would
 * restore the selection start/end after undo/redo
 */
public class RuleEditorUndoManager extends UndoManager {
  private static final long serialVersionUID = 20221225L;

  private static final char[] SIG_CHARS = {' ', '.', '\n', '\t', '{', '}', '(', ')', '^', '*'};

  private final EditorPane editorPane;
  private final BooleanProperty lastActionWasSave;
  // private final boolean lastEditWasSignificant;
  private boolean inCompoundEdit;
  private boolean firstInCompoundEditComplete;
  // bookkeeping for determining if an edit is significant
  private boolean lastEditWasInsert = true;
  // user pasted multiple characters at once into the buffer
  private boolean lastEditWasMultiChar = false;
  private RuleEditorUndoableEdit lastEdit = null;

  public class CompoundModeManager implements AutoCloseable {
    @Override
    public void close() throws Exception {
      if (!RuleEditorUndoManager.this.inCompoundEdit) {
        System.err.println(
            "WARNING: CompoundModeManager closed after undo manager "
                + "had already exited compound mode");
      }
      RuleEditorUndoManager.this.endCompoundEdit();
    }
  }

  public RuleEditorUndoManager(EditorPane editorPane, BooleanProperty lastActionWasSave) {
    super();
    this.editorPane = editorPane;
    this.lastActionWasSave = lastActionWasSave;
    setLimit(10000); // This seems to be enough?
  }

  @Override
  public boolean addEdit(UndoableEdit anEdit) {
    RuleEditorUndoableEdit customEdit =
        new RuleEditorUndoableEdit(anEdit, editorPane.getSoarDocument());
    if (inCompoundEdit) {
      // we are in a compound edit and have already created the first edit in it
      // signal that this edit should be merged with the previous one
      // beginning a new compound edit, so force a new edit
      customEdit.significant = !firstInCompoundEditComplete;
      firstInCompoundEditComplete = true;
    }

    return super.addEdit(customEdit);
  }

  /**
   * When this class is in compound mode, all edits are combined into one single edit. Using undo()
   * or redo() immediately close compound mode.
   *
   * @return A manager for compound mode that is {@link AutoCloseable auto-closeable}, meaning that
   *     the client can restrict compound mode to a specific code block using a try-with-resources
   *     statement. Compound mode will end early if undo() or redo() are called.
   */
  public CompoundModeManager compoundMode() {
    startCompoundEdit();
    return new CompoundModeManager();
  }

  private void startCompoundEdit() {
    inCompoundEdit = true;
  }

  private void endCompoundEdit() {
    inCompoundEdit = false;
    firstInCompoundEditComplete = false;
  }

  // Clients are not expected to undo/redo inside a compound edit, but to prevent undesired addition
  // of edits to
  // a compound edit (i.e. for safety), we immediately close compound mode if they try.
  @Override
  public void undo() {
    super.undo();
    endCompoundEdit();

    // If the user clears the undo queue then treat the buffer as if
    // it has just been saved
    if (!this.canUndo()) {
      RuleEditorUndoManager.this.lastActionWasSave.set(true);
    }
  }

  // tried and failed: save last edit and set it to isSignificant=true; significant sentinel; insignificant sentinel.
	// UndoManager seems to be designed with assumption that insignificant changes will always be followed by a significant one.
	// The docs say <quote>
	// Invoking redo results in invoking redo on all edits between the index of the next edit and the next significant
	// edit (or the end of the list). Continuing with the previous example if redo were invoked, redo would in turn be
	// invoked on A, b and c. In addition the index of the next edit is set to 3 (as shown in figure 2).
	// </quote>
	// However, the actual behavior appears to be that A would be redone but not b or c. To fix this we have to override
	// editToBeRedone, and we need access to indexOfNextAdd, but unfortunately that can only be retrieved through the
	// toString() (barf!). Attempting reflection results in a security error.
	// This implementation finds the next significant edit and then all of the following insignificant edits and redoes
	// them. This forms a correct inverse with undo().
	// Probably a more robust way to do this would be to use a CompoundEdit instead of the isSignificant flag. See
	// https://github.com/tips4java/tips4java/blob/main/source/CompoundUndoManager.java#L171.
	// TODO: test the undo manager!
	/**
	 *
	 * @return
	 */
	@Override
  protected UndoableEdit editToBeRedone() {
    String stringified = toString();
    String searchFor = "indexOfNextAdd: ";
    int indexOfNextAdd =
        Integer.parseInt(
            stringified.substring(stringified.indexOf(searchFor) + searchFor.length()));
    int count = edits.size();
    int i = indexOfNextAdd;

    UndoableEdit previousEdit = null;
    boolean foundSignificantEdit = false;
    while (i < count) {
      UndoableEdit edit = edits.elementAt(i++);
      if (edit.isSignificant()) {
        if (foundSignificantEdit) {
          return previousEdit;
        }
        foundSignificantEdit = true;
      }
      previousEdit = edit;
    }

    return previousEdit;
  }

  @Override
  public void redo() {
    super.redo();
    endCompoundEdit();
  }

  /**
   * We need to modify the isSignificant() method in AbstractDocument.DefaultDocumentEvent. I don't
   * want to subclass AbstractDocument.DefaultDocumentEvent because I'd have to also subclass
   * AbstractDocument which seems like a can of works. So, I've done a kludge-y subclass this way
   * (see the 'parent' instance variable). If you see a clever-er solution please be my guest...
   *
   * @author Andrew Nuxoll
   * @version 29 Sep 2022
   */
  class RuleEditorUndoableEdit implements UndoableEdit {

    private final UndoableEdit parent;
    private boolean significant = false; // is this edit "significant"?

    public RuleEditorUndoableEdit(UndoableEdit initParent, SoarDocument doc) {
      this.parent = initParent;

      // style changes aren't significant
      if (Objects.equals(this.parent.getPresentationName(), "style change")) {
        return;
      }

      // Retrieve the text the user inserted or removed for this edit
      // Also take note if it's an insert or delete
      String lastText = doc.getLastInsertedText();
      boolean wasInsert = (lastText != null);
      if (!wasInsert) {
        lastText = doc.getLastRemovedText();
      }

      // If there has been neither a preceding insert nor a preceding remove
      // then this edit is significant (I think)
      if (lastText == null) {
        this.significant = true;
        return;
      }

      // If the last insert/remove was a multi-character paste then
      // this new insert/remove is significant
      boolean sig = RuleEditorUndoManager.this.lastEditWasMultiChar;
      RuleEditorUndoManager.this.lastEditWasMultiChar = (lastText.length() > 1);
      if (sig) {
        this.significant = true;
        return;
      }

      // If the user just switched from insert to delete (or vice versa)
      // then this edit is significant
      boolean switched = (RuleEditorUndoManager.this.lastEditWasInsert != wasInsert);
      RuleEditorUndoManager.this.lastEditWasInsert = wasInsert;
      if (switched) {
        this.significant = true;
        return;
      }

      // the first edit after a user saves the document is significant
      if (RuleEditorUndoManager.this.lastActionWasSave.get()) {
        this.significant = true;
        RuleEditorUndoManager.this.lastActionWasSave.set(false);
        return;
      }

      // If the last insertion/deletion was the end of a word/line/phrase or
      // a Soar coding element then it's significant  (see the SIG_CHARS
      // constant)
      for (char c : SIG_CHARS) {
        if (lastText.indexOf(c) != -1) {
          this.significant = true;
          return;
        }
      }
    }

    /** these are methods whose behavior I've actually changed */
    @Override
    public boolean isSignificant() {
      return this.significant;
    }

    // Always allow undo.  This feels a bit dangerous but seems to be working.
    // When I used the parent's version it was rejecting valid undoes sometimes.
    @Override
    public boolean canUndo() {
      return true; // If this creates problems try going back to "return super.canUndo();"
    }

    // All the methods below just use the parent's functionality

    @Override
    public void undo() throws CannotUndoException {
      this.parent.undo();
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
  }
}
