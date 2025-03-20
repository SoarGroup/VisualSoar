package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This class is responsible for comparing all productions in the project with the project's
 * datamaps and 'fixing' any discrepancies by adding missing productions to the datamap. Operation
 * status is displayed in a progress bar. Add productions in the datamap are displayed as green
 * until the user validates them. Results are displayed in the feedback list
 */
public class GenerateDataMapAction extends AbstractAction {
  private static final long serialVersionUID = 20221225L;

  private final MainFrame mainFrame;
  JProgressBar progressBar;
  JDialog progressDialog;

  public GenerateDataMapAction(MainFrame mainFrame) {
    super("Generate Datamap from Operator Hierarchy");
    this.mainFrame = mainFrame;
    setEnabled(false);
  }

  public void actionPerformed(ActionEvent ae) {
    int numNodes = 0;
    Enumeration<TreeNode> bfe = mainFrame.getOperatorWindow().getProjectModel().breadthFirstEnumeration();
    while (bfe.hasMoreElements()) {
      numNodes++;
      bfe.nextElement();
    }
    System.out.println("Nodes: " + numNodes);
    progressBar = new JProgressBar(0, numNodes * 7);
    progressDialog = new JDialog(mainFrame, "Generating Datamap from Productions");
    progressDialog.getContentPane().setLayout(new FlowLayout());
    progressDialog.getContentPane().add(progressBar);
    progressBar.setStringPainted(true);
    progressDialog.setLocationRelativeTo(mainFrame);
    progressDialog.pack();
    progressDialog.setVisible(true);
    (new UpdateThread()).start();
  }

  class UpdateThread extends Thread {
    Runnable update, finish;
    int value, min;

    Vector<FeedbackListEntry> errors = new Vector<>();
    int repCount = 0;
    Enumeration<TreeNode> bfe = mainFrame.getOperatorWindow().getProjectModel().breadthFirstEnumeration();
    OperatorNode current;
    Vector<FeedbackListEntry> vecErrors = new Vector<>();

    public UpdateThread() {
      progressBar.getMaximum();
      progressBar.getMinimum();

      update =
          new Runnable() {
            public void run() {
              value = progressBar.getValue() + 1;
              updateProgressBar(value);
              // System.out.println("Value is " + value);
            }
          };
      finish =
          new Runnable() {
            public void run() {
              updateProgressBar(min);
              System.out.println("Done");
              progressDialog.dispose();
            }
          };
    }

    public void run() {
      checkNodes();
      repCount = 0;

      JOptionPane.showMessageDialog(
          mainFrame,
          "DataMap Generation Completed",
          "DataMap Generator",
          JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateProgressBar(int value) {
      progressBar.setValue(value);
    }

    public void checkNodes() {
      do {
        repCount++;
        errors.clear();
        bfe = mainFrame.getOperatorWindow().getProjectModel().breadthFirstEnumeration();

        while (bfe.hasMoreElements()) {
          current = (OperatorNode) bfe.nextElement();

          mainFrame.getOperatorWindow().generateDataMap(current, errors, vecErrors);

          mainFrame.getFeedbackManager().showFeedback(vecErrors);
          value = progressBar.getValue() + 1;
          updateProgressBar(value);
          SwingUtilities.invokeLater(update);
        } // while parsing operator nodes

      } while (!(errors.isEmpty()) && repCount < 5);

      // Instruct all open datamap windows to display
      // the newly generated nodes
      JInternalFrame[] jif = mainFrame.getDesktopPane().getAllFrames();
      for (JInternalFrame jInternalFrame : jif) {
        if (jInternalFrame instanceof DataMap) {
          DataMap dm = (DataMap) jInternalFrame;
          dm.displayGeneratedNodes();
        }
      }

      SwingUtilities.invokeLater(finish);
    }
  }
}
