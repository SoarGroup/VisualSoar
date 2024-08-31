package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class NewProjectAction extends AbstractAction {
  private static final long serialVersionUID = 20221225L;

  private final MainFrame mainFrame;

  public NewProjectAction(MainFrame mainFrame) {
    super("New Project...");
    this.mainFrame = mainFrame;
  }

  public void actionPerformed(ActionEvent event) {
    mainFrame.newProject();
  }
}
