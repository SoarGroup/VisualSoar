package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.misc.PerformableAction;
import edu.umich.soar.visualsoar.misc.Prefs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class TryOpenProjectAction extends PerformableAction {
  private final MainFrame mainFrame;
  private final Prefs.RecentProjInfo projInfo;

  public TryOpenProjectAction(MainFrame mainFrame, Prefs.RecentProjInfo rpi) {
    super("Attempt to load project file: " + rpi);
    this.mainFrame = mainFrame;
    this.projInfo = rpi;
  }

  @Override
  public void perform() {
    // Get rid of the old project (if it exists)
    if (mainFrame.getOperatorWindow() != null) {
      mainFrame.closeProject();
    }

    try {
      mainFrame.tryOpenProject(projInfo.file, projInfo.isReadOnly);
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(mainFrame, "Unable to open file: " + projInfo);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    perform();
  }
}
