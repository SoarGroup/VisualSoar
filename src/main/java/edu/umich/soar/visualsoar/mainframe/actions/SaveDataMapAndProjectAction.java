package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.misc.PerformableAction;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * Attempts to save the datamap
 *
 * @see OperatorWindow#saveHierarchy()
 */
public class SaveDataMapAndProjectAction extends PerformableAction {
  private static final long serialVersionUID = 20221225L;
  private final MainFrame mainFrame;

  public SaveDataMapAndProjectAction(MainFrame mainFrame) {
    super("Save DataMap And Project Action");
    this.mainFrame = mainFrame;
  }

  public void perform() {
    if (mainFrame.getOperatorWindow() != null) {
      try {
        mainFrame.getOperatorWindow().saveHierarchy();
      } catch (IOException e) {
        JOptionPane.showMessageDialog(
          mainFrame,
          e.getMessage(),
          "DataMap/Project Save Error",
          JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
        mainFrame.getFeedbackManager().setStatusBarError("Failed to save DataMap and project");
      }
    }
  }

  public void actionPerformed(ActionEvent event) {
    perform();
    mainFrame.getFeedbackManager().setStatusBarMsg("DataMap and Project Saved");
  }
}
