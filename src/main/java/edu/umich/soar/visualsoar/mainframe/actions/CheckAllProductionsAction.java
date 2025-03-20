package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.ProjectModel;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.mainframe.UpdateThread;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.misc.PerformableAction;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeNode;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
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
    Vector<OperatorNode> vecNodes =
        getOperatorNodes(mainFrame.getOperatorWindow().getProjectModel());
    CheckProductionsThread cpt = new CheckProductionsThread(vecNodes, "Checking Productions...");
    cpt.start();
  }

  @NotNull
  private static Vector<OperatorNode> getOperatorNodes(ProjectModel pm) {
    // TODO: clearly bfe is assumed to contain only OperatorNodes. Refactor so we don't have to
    // cast.
    Vector<OperatorNode> vecNodes = new Vector<>(10, 50);
    Enumeration<TreeNode> bfe = pm.breadthFirstEnumeration();
    while (bfe.hasMoreElements()) {
      vecNodes.add((OperatorNode) bfe.nextElement());
    }
    return vecNodes;
  }

  public static List<FeedbackListEntry> checkAllProductions(ProjectModel pm) throws IOException {
    Vector<OperatorNode> vecNodes = getOperatorNodes(pm);
    Vector<FeedbackListEntry> vecErrors = new Vector<>();
    for (OperatorNode on : vecNodes) {
      on.checkAgainstDatamap(vecErrors, pm);
    }
    return new ArrayList<>(vecErrors);
  }

  public void actionPerformed(ActionEvent ae) {
    perform();
  }

  class CheckProductionsThread extends UpdateThread {
    public CheckProductionsThread(Vector<OperatorNode> v, String title) {
      super(mainFrame, v, title);
    }

    public boolean checkEntity(Object node) throws IOException {
      return ((OperatorNode) node)
          .checkAgainstDatamap(vecErrors, mainFrame.getOperatorWindow().getProjectModel());
    }
  }
} // class CheckAllProductionsAction
