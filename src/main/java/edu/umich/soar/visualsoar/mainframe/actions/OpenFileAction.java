package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.misc.Prefs;
import edu.umich.soar.visualsoar.misc.TextFileFilter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Open a text file unrelated to the project in a rule editor Opened file is not necessarily part of
 * project and not soar formatted
 */
public class OpenFileAction extends AbstractAction {
  private static final long serialVersionUID = 20221225L;
  private final MainFrame mainFrame;

  public OpenFileAction(MainFrame mainFrame) {
    super("Open File...");
    this.mainFrame = mainFrame;
  }

  public void actionPerformed(ActionEvent event) {
    try {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setFileFilter(new TextFileFilter());
      File dir = new File(Prefs.openFolder.get());
      if ((dir.exists()) && (dir.canRead())) {
        fileChooser.setCurrentDirectory(dir);
      }
      int state = fileChooser.showOpenDialog(mainFrame);
      File file = fileChooser.getSelectedFile();
      if (file != null && state == JFileChooser.APPROVE_OPTION) {
        mainFrame.OpenFile(file);
      }

    } catch (NumberFormatException nfe) {
      nfe.printStackTrace();
      JOptionPane.showMessageDialog(
          mainFrame,
          "Error Reading File, Data Incorrectly Formatted",
          "Bad File",
          JOptionPane.ERROR_MESSAGE);
    }
  }
}
