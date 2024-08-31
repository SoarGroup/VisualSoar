package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.files.Vsa;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.misc.SoarFileFilter;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Open Project Action a filechooser is created to determine project file Opens a project by
 * creating a new OperatorWindow
 *
 * @see OperatorWindow
 * @see SoarFileFilter
 */
public class OpenProjectAction extends AbstractAction {
  private static final long serialVersionUID = 20221225L;

  private final MainFrame mainFrame;

  public OpenProjectAction(MainFrame mainFrame) {
    super("Open Project...");
    this.mainFrame = mainFrame;
  }

  public void actionPerformed(ActionEvent event) {
    File vsaFile = Vsa.selectVsaFile(mainFrame);
    if (vsaFile == null) {
      return;
    }
    boolean readOnly = event.getActionCommand().contains("Read-Only");
    mainFrame.openProject(vsaFile, readOnly);
  }
}
