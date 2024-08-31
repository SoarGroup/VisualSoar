package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.mainframe.UpdateThread;
import edu.umich.soar.visualsoar.misc.FeedbackListEntry;
import edu.umich.soar.visualsoar.misc.PerformableAction;
import edu.umich.soar.visualsoar.operatorwindow.FileNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;

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
    Enumeration<TreeNode> bfe = mainFrame.getOperatorWindow().breadthFirstEnumeration();
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

      // We lie and say there are errors no matter what so that
      // the "there were no errors..." message won't appear.
      return true;
    }
  }
}
