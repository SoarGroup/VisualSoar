package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.misc.PerformableAction;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;

import java.awt.event.ActionEvent;

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
      mainFrame.getOperatorWindow().saveHierarchy();
    }
  }

  public void actionPerformed(ActionEvent event) {
    perform();
    mainFrame.getFeedbackManager().setStatusBarMsg("DataMap and Project Saved");
  }
}
