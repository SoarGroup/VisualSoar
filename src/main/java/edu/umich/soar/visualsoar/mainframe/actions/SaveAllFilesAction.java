package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.files.Backup;
import edu.umich.soar.visualsoar.files.Cfg;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.misc.PerformableAction;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;

/** Runs through all the Rule Editors in the Desktop Pane and tells them to save themselves. */
public class SaveAllFilesAction extends PerformableAction {
  private static final long serialVersionUID = 20221225L;
  private final MainFrame mainFrame;

  public SaveAllFilesAction(MainFrame mainFrame) {
    super("Save All");
    this.mainFrame = mainFrame;
    setEnabled(false);
  }

  public void perform() {
    // Save the list of currently open windows
    Cfg.writeCfgFile(mainFrame);

    try {
      JInternalFrame[] jif = mainFrame.getDesktopPane().getAllFrames();
      for (JInternalFrame jInternalFrame : jif) {
        if (jInternalFrame instanceof RuleEditor) {
          RuleEditor re = (RuleEditor) jInternalFrame;
          re.write();
        }
      }
    } catch (java.io.IOException ioe) {
      JOptionPane.showMessageDialog(
          mainFrame, ioe.getMessage(), "I/O Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Since save was successful, discard auto-backup files
    Backup.deleteAutoBackupFiles(mainFrame);
  }

  public void actionPerformed(ActionEvent event) {
    perform();
  }
}
