package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.mainframe.UpdateThread;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This action provides a framework for searching all datamaps for errors. It is intended to be
 * subclassed. Operation status is displayed in a progress bar. Results are displayed in the
 * feedback list Double-clicking on an item in the feedback list should display the rogue node in
 * the datamap.
 */
public abstract class SearchDataMapAction extends AbstractAction {
  private static final long serialVersionUID = 20221225L;

  protected final MainFrame mainFrame;
  protected int numNodes = 0; // number of operator nodes in the project
  protected int numChecks = 0; // number of nodes scanned so far

  public SearchDataMapAction(MainFrame mainFrame) {
    super("Check All Productions");
    this.mainFrame = mainFrame;
    setEnabled(false);
  }

  public void actionPerformed(ActionEvent ae) {
    initializeEdges();
    numNodes = 0;
    numChecks = 0;

    Enumeration<TreeNode> bfe = mainFrame.getOperatorWindow().breadthFirstEnumeration();
    Vector<OperatorNode> vecNodes = new Vector<>(10, 50);
    while (bfe.hasMoreElements()) {
      vecNodes.add((OperatorNode) bfe.nextElement());
      numNodes++;
    }

    // Add the nodes a second time because we'll be scanning them twice,
    // once to check productions against the datamap and again to check
    // the datamap for untested WMEs.  (See checkEntity() below.)
    bfe = mainFrame.getOperatorWindow().breadthFirstEnumeration();
    while (bfe.hasMoreElements()) {
      vecNodes.add((OperatorNode) bfe.nextElement());
    }

    (new DatamapTestThread(vecNodes, "Scanning Datamap...")).start();
  }

  /**
   * This initializes the status of all the edges to zero, which means that the edges have not been
   * used by a production in any way.
   */
  public void initializeEdges() {
    Enumeration<NamedEdge> edges = mainFrame.getOperatorWindow().getDatamap().getEdges();
    while (edges.hasMoreElements()) {
      NamedEdge currentEdge = edges.nextElement();
      currentEdge.resetTestedStatus();
      currentEdge.resetErrorNoted();
      // initialize the output-link as already tested
      if (currentEdge.getName().equals("output-link")) {
        currentEdge.setOutputLinkTested(mainFrame.getOperatorWindow().getDatamap());
      }
    }
  }

  // This function performs the actual error check
  // The datamap associated with the given operator node is scanned and a
  // list of errors is placed in the given Vector.
  public abstract void searchDatamap(OperatorNode opNode, Vector<FeedbackListEntry> v);

  class DatamapTestThread extends UpdateThread {
    public DatamapTestThread(Vector<OperatorNode> v, String title) {
      super(mainFrame, v, title);
    }

    /**
     * Search through the datamap and look for extra WMEs by looking at the status of the named edge
     * (as determined by the check nodes function) and the edge's location within the datamap. Extra
     * WMEs are classified in this action by never being tested by a production, not including any
     * item within the output-link.
     */
    public boolean checkEntity(Object node) throws IOException {
      OperatorNode opNode = (OperatorNode) node;

      // For the first run, do a normal production check
      if (numChecks < numNodes) {
        Vector<FeedbackListEntry> v = new Vector<>();
        boolean rc = opNode.CheckAgainstDatamap(v);
        if (rc) {
          String msg =
              "WARNING:  datamap errors were found in "
                  + opNode.getFileName()
                  + "'s productions.  This may invalidate the current scan.";
          vecErrors.add(new FeedbackListEntry(msg));
        }

        numChecks++;
        return rc;
      } // if

      // For the second run, do the requested datamap scan
      Vector<FeedbackListEntry> v = new Vector<>();
      searchDatamap(opNode, v);
      numChecks++;

      if (!v.isEmpty()) {
        vecErrors.addAll(v);
        return true;
      }

      return false;
    }
  }
}
