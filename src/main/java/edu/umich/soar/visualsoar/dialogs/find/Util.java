package edu.umich.soar.visualsoar.dialogs.find;

import edu.umich.soar.visualsoar.datamap.DataMapTree;
import edu.umich.soar.visualsoar.datamap.FakeTreeNode;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackList;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;

public class Util {
  /**
   * @return The selected text from the currently focused window
   */
  @NotNull
  static String getSelectedText() {
    String initialText = null;
    Component focusedComponent =
        KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (focusedComponent instanceof JEditorPane) {
      // covers rule editor
      JEditorPane editorPane = (JEditorPane) focusedComponent;
      initialText = editorPane.getSelectedText();
    } else if (focusedComponent instanceof JTree) {
      // covers both OperatorWindow and DataMapTree
      initialText = getJtreeTextSelection((JTree) focusedComponent);
    } else if (focusedComponent instanceof FeedbackList) {
      FeedbackList fbList = (FeedbackList) focusedComponent;
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
