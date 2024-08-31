package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.misc.PerformableAction;

import java.awt.event.ActionEvent;

/** Close Project Action Closes all open windows in the desktop pane */
public class CloseProjectAction extends PerformableAction {
  private static final long serialVersionUID = 20221225L;

  private final MainFrame mainFrame;

  public CloseProjectAction(MainFrame mainFrame) {
    super("Close Project");
    this.mainFrame = mainFrame;
    setEnabled(false);
  }

  public void perform() {
    mainFrame.closeProject();
  }

  public void actionPerformed(ActionEvent event) {
    perform();
  }
}
