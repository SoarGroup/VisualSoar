package edu.umich.soar.visualsoar.files;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.misc.CustomInternalFrame;
import edu.umich.soar.visualsoar.operatorwindow.OperatorRootNode;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import java.io.File;

public class Backup {
  /** VS periodically creates auto backups of open files.  This method can be called to delete all of them. */
  public static void deleteAutoBackupFiles(MainFrame mainFrame) {

    //Check for any auto-backups of rule files
    CustomInternalFrame[] frames = mainFrame.getDesktopPane().getAllCustomFrames();
    for (CustomInternalFrame frame : frames) {
      if (frame instanceof RuleEditor) {
        RuleEditor re = (RuleEditor) frame;
        String tempFN = re.getFile() + "~";
        File f = new File(tempFN);
        if (f.exists()) f.delete();
      }
    }

    //Check for auto-backups of the project files
    if (mainFrame.getOperatorWindow() != null) {
      Object root = mainFrame.getOperatorWindow().getModel().getRoot();
      if (root instanceof OperatorRootNode) {
        OperatorRootNode orn = (OperatorRootNode) root;
        File projectBackupFile = new File(orn.getProjectFile() + "~");
        if (projectBackupFile.exists()) projectBackupFile.delete();
        File dataMapBackupFile = new File(orn.getDataMapFile() + "~");
        if (dataMapBackupFile.exists()) dataMapBackupFile.delete();
        File commentBackupFile = new File(dataMapBackupFile.getParent() + File.separator + "comment.dm~");
        if (commentBackupFile.exists()) commentBackupFile.delete();
      }
    }
  }
}
