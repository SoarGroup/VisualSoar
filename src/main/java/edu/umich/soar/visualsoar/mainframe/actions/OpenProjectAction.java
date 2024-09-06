package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.files.Vsa;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackManager;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.*;

public class OpenProjectAction extends AbstractAction {
  private final MainFrame mainFrame;
  private final File vsaFile;
  private final boolean readOnly;

  public OpenProjectAction(MainFrame mainFrame, File vsaFile, boolean readOnly) {
    super(getActionName(vsaFile, readOnly));
    this.mainFrame = mainFrame;
    this.vsaFile = vsaFile;
    this.readOnly = readOnly;
  }

  /**
   * The VSA file will be selected from a chooser window by the user, and the read-only value
   * will be determined from event data.
   */
  public OpenProjectAction(MainFrame mainFrame) {
    super(getActionName(null, false));
    this.mainFrame = mainFrame;
    this.vsaFile = null;
    this.readOnly = false;
  }

  private static String getActionName(File vsaFile, boolean readOnly) {
    StringBuilder sb = new StringBuilder();
    sb.append("Load");
    if (readOnly) {
      sb.append(" read-only");
    }
    sb.append(" project");
    if (vsaFile != null) {
      sb.append(" ");
      sb.append(vsaFile);
    } else {
      sb.append("...");
    }
    return sb.toString();
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    final File vsaFile = this.vsaFile != null ? this.vsaFile : Vsa.selectVsaFile(mainFrame);
    if (vsaFile == null) {
      return;
    }
    final boolean readOnly = this.readOnly || event.getActionCommand().contains("Read-Only");
    try (FeedbackManager.AtomicContext ignored = mainFrame.getFeedbackManager().beginAtomicContext()) {
      mainFrame.openProject(vsaFile, readOnly);
    }
  }
}
