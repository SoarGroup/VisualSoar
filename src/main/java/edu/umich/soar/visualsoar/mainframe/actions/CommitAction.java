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
  private final CheckAllProductionsAction checkAllProductionsAction;

  public CommitAction(
      MainFrame mainFrame,
      PerformableAction saveAllFilesAction,
      PerformableAction exportAgentAction,
      PerformableAction saveDatamapAndProjectAction,
      CheckAllProductionsAction checkAllProductionsAction) {
    super("Commit");
    this.mainFrame = mainFrame;
    this.saveAllFilesAction = saveAllFilesAction;
    this.exportAgentAction = exportAgentAction;
    this.saveDatamapAndProjectAction = saveDatamapAndProjectAction;
    this.checkAllProductionsAction = checkAllProductionsAction;
    setEnabled(false);
  }

  public void perform() {
    perform(true);
  }

  /**
   * Save the project and all Soar source files.
   * Same as {@link #perform()}, but allows skipping the datamap check to avoid infinite recursion.
   *
   * @param checkDm perform the project datamap check if true and set in preferences, skip it if
   *     false
   */
  public void perform(boolean checkDm) {
    try (FeedbackManager.AtomicContext ignored =
        mainFrame.getFeedbackManager().beginAtomicContext()) {
      saveAllFilesAction.perform();
      if (mainFrame.projectIsOpen()) {
        exportAgentAction.perform();
        saveDatamapAndProjectAction.perform();
        if (checkDm && Prefs.checkDmOnSave.getBoolean()) {
          checkAllProductionsAction.perform(false);
        }
      }
    }
  }

  public void actionPerformed(ActionEvent e) {
    perform();
    mainFrame.getFeedbackManager().setStatusBarMsg("Save Finished");
  }
}
