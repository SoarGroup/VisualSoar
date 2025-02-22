package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.misc.PerformableAction;
import edu.umich.soar.visualsoar.operatorwindow.OperatorRootNode;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * Export Agent Writes all the <operator>_source.soar files necessary for sourcing agent files
 * written in into the TSI
 */
public class ExportAgentAction extends PerformableAction {
  private static final long serialVersionUID = 20221225L;
  private final MainFrame mainFrame;

  public ExportAgentAction(MainFrame mainFrame) {
    super("Export Agent");
    this.mainFrame = mainFrame;
  }

  public void perform() {
    DefaultTreeModel tree = (DefaultTreeModel) mainFrame.getOperatorWindow().getModel();
    OperatorRootNode root = (OperatorRootNode) tree.getRoot();
    try {
      root.startSourcing();
    } catch (IOException exception) {
      JOptionPane.showMessageDialog(
          mainFrame, exception.getMessage(), "Agent Export Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  public void actionPerformed(ActionEvent event) {
    perform();
    mainFrame.getFeedbackManager().setStatusBarMsg("Export Finished");
  }
}
