package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackManager;
import edu.umich.soar.visualsoar.misc.PerformableAction;
import edu.umich.soar.visualsoar.misc.Prefs;

import java.awt.event.ActionEvent;

public class CommitAction extends PerformableAction {
  private static final long serialVersionUID = 20221225L;

  private final MainFrame mainFrame;
  private final PerformableAction saveAllFilesAction;
  private final PerformableAction exportAgentAction;
  private final PerformableAction saveDatamapAndProjectAction;
  private final PerformableAction checkAllProductionsAction;

  public CommitAction(
      MainFrame mainFrame,
      PerformableAction saveAllFilesAction,
      PerformableAction exportAgentAction,
      PerformableAction saveDatamapAndProjectAction,
      PerformableAction checkAllProductionsAction) {
    super("Commit");
    this.mainFrame = mainFrame;
    this.saveAllFilesAction = saveAllFilesAction;
    this.exportAgentAction = exportAgentAction;
    this.saveDatamapAndProjectAction = saveDatamapAndProjectAction;
    this.checkAllProductionsAction = checkAllProductionsAction;
    setEnabled(false);
  }

  public void perform() {

    try (FeedbackManager.AtomicContext ignored =
        mainFrame.getFeedbackManager().beginAtomicContext()) {
      saveAllFilesAction.perform();
      if (mainFrame.projectIsOpen()) {
        exportAgentAction.perform();
        saveDatamapAndProjectAction.perform();
        if (Prefs.checkDmOnSave.getBoolean()) {
          checkAllProductionsAction.perform();
        }
      }
    }
  }

  public void actionPerformed(ActionEvent e) {
    perform();
    mainFrame.getFeedbackManager().setStatusBarMsg("Save Finished");
  }
}
