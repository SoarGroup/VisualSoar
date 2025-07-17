package edu.umich.soar.visualsoar.dialogs.find;

import edu.umich.soar.visualsoar.datamap.FakeTreeNode;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackList;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class FindUtils {
  /**
   * We store the most recently focused (text-providing) component so that we can autofill find
   * dialog boxes from them; using the currently focused component works for CTRL+F, etc. shortcuts,
   * but not for using the main menu Search -> Find in project, etc. because the menu itself takes
   * the focus.
   */
  private static Component lastFocusedTextComponent = null;

  private static void registerTextComponentFocusInternal(Component comp) {
    comp.addFocusListener(
        new FocusListener() {
          @Override
          public void focusGained(FocusEvent e) {
            lastFocusedTextComponent = comp;
          }

          @Override
          public void focusLost(FocusEvent e) {}
        });
  }

  /**
   * Registers a focus listener on {@code tree} so that text can be copied from it into a find box.
   */
  public static void registerTextComponentFocus(JTree tree) {
    registerTextComponentFocusInternal(tree);
  }

  /**
   * Registers a focus listener on {@code editorPane} so that text can be copied from it into a find
   * box.
   */
  public static void registerTextComponentFocus(JEditorPane editorPane) {
    registerTextComponentFocusInternal(editorPane);
  }

  /**
   * Registers a focus listener on {@code feedbackList} so that text can be copied from it into a
   * find box.
   */
  public static void registerTextComponentFocus(FeedbackList feedbackList) {
    registerTextComponentFocusInternal(feedbackList);
  }

  /**
   * @return The selected text from the currently focused window, or last focused text component
   */
  @NotNull
  static String getSelectedText() {
    String initialText = null;
    Component focusedComponent =
        KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    Component targetComponent = focusedComponent;
    // If focus is not on a text component, use last focused text component
    if (!(focusedComponent instanceof JEditorPane
        || focusedComponent instanceof JTree
        || focusedComponent instanceof FeedbackList)) {
      targetComponent = lastFocusedTextComponent;
    }
    if (targetComponent instanceof JEditorPane) {
      JEditorPane editorPane = (JEditorPane) targetComponent;
      initialText = editorPane.getSelectedText();
    } else if (targetComponent instanceof JTree) {
      initialText = getJtreeTextSelection((JTree) targetComponent);
    } else if (targetComponent instanceof FeedbackList) {
      FeedbackList fbList = (FeedbackList) targetComponent;
      FeedbackListEntry entry = fbList.getSelectedValue();
      if (entry != null) {
        initialText = entry.getMessage();
      }
    }
    if (initialText == null) {
      initialText = ""; // Default to empty if no text is selected
    }
    return initialText;
  }

  private static String getJtreeTextSelection(JTree jTree) {
    String initialText = null;
    TreePath path = jTree.getSelectionPath();
    if (path != null) {
      Object lastPathComponent = path.getLastPathComponent();
      if (lastPathComponent instanceof FakeTreeNode) {
        FakeTreeNode ftn = (FakeTreeNode) lastPathComponent;
        if (ftn.getEdge() != null) {
          initialText = ftn.getEdge().getName();
        }
      }
      if (initialText == null) {
        initialText = path.getLastPathComponent().toString();
      }
    }
    return initialText;
  }
}
