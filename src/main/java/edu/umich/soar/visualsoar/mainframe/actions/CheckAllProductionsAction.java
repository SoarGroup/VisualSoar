package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.mainframe.UpdateThread;
import edu.umich.soar.visualsoar.misc.PerformableAction;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;

import javax.swing.tree.TreeNode;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This class is responsible for comparing all productions in the project with the project's model
 * of working memory - the datamap. Operation status is displayed in a progress bar. Results are
 * displayed in the feedback list
 */
public class CheckAllProductionsAction extends PerformableAction {
  private static final long serialVersionUID = 20221225L;

  private final MainFrame mainFrame;

  public CheckAllProductionsAction(MainFrame mainFrame) {
    super("Check All Productions");
    this.mainFrame = mainFrame;
    setEnabled(false);
  }

  // Same as actionPerformed() but this function waits for the thread to
  // complete before returning (i.e., it's effectively not threaded)
  public void perform() {
    Vector<OperatorNode> vecNodes = new Vector<>(10, 50);
    Enumeration<TreeNode> bfe = mainFrame.getOperatorWindow().getProjectModel().breadthFirstEnumeration();
    while (bfe.hasMoreElements()) {
      vecNodes.add((OperatorNode) bfe.nextElement());
    }

    CheckProductionsThread cpt = new CheckProductionsThread(vecNodes, "Checking Productions...");
    cpt.start();
  }

  public void actionPerformed(ActionEvent ae) {
    perform();
  }

  class CheckProductionsThread extends UpdateThread {
    public CheckProductionsThread(Vector<OperatorNode> v, String title) {
      super(mainFrame, v, title);
    }

    public boolean checkEntity(Object node) throws IOException {
      return ((OperatorNode) node).CheckAgainstDatamap(vecErrors);
    }
  }
} // class CheckAllProductionsAction
