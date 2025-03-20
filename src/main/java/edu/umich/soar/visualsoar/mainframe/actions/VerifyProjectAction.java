package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.mainframe.UpdateThread;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.misc.PerformableAction;
import edu.umich.soar.visualsoar.operatorwindow.FileNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This action verifies that a project is intact. Specifically it checks that all the project's
 * files are present and can be loaded.
 */
public class VerifyProjectAction extends PerformableAction {
  private static final long serialVersionUID = 20221225L;

  private final MainFrame mainFrame;

  public VerifyProjectAction(MainFrame mainFrame) {
    super("Verify Project Integrity");
    this.mainFrame = mainFrame;
    setEnabled(false);
  }

  public void perform() {
    Enumeration<TreeNode> bfe = mainFrame.getOperatorWindow().getProjectModel().breadthFirstEnumeration();
    Vector<OperatorNode> vecNodes = new Vector<>(10, 50);
    while (bfe.hasMoreElements()) {
      Object obj = bfe.nextElement();
      if (obj instanceof OperatorNode) {
        OperatorNode node = (OperatorNode) obj;
        vecNodes.add(node);
      }
    }
    (new VerifyProjectThread(vecNodes, "Verifiying Project...")).start();
  }

  public void actionPerformed(ActionEvent ae) {
    perform();
  }

  class VerifyProjectThread extends UpdateThread {
    public VerifyProjectThread(Vector<OperatorNode> v, String title) {
      super(mainFrame, v, title);
    }

    public boolean checkEntity(Object node) {
      OperatorNode opNode = (OperatorNode) node;

      // Only file nodes need to be examined
      if (!(opNode instanceof FileNode)) {
        return false;
      }

      File f = new File(opNode.getFileName());
      if (!f.canRead()) {
        String msg = "Error!  Project Corrupted:  Unable to open file: " + opNode.getFileName();
        vecErrors.add(new FeedbackListEntry(msg));
        return true;
      }

      if (!f.canWrite()) {
        String msg = "Error!  Unable to write to file: " + opNode.getFileName();
        vecErrors.add(new FeedbackListEntry(msg));
        return true;
      }

      return false;
    }

    @Override
    public @Nullable String getSuccessMessage() {
      // User doesn't need to know that files are all present and RW-able
      return null;
    }
  }
}
