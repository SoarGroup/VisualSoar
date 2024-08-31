package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * class LoadTopStateDatamapAction
 *
 * <p>This action loads the top-state datamap
 *
 * @author Andrew Nuxoll
 * @version 08 Sep 2022
 */
public class LoadTopStateDatamapAction extends AbstractAction {
  private static final long serialVersionUID = 20221225L;

  private final MainFrame mainFrame;

  public LoadTopStateDatamapAction(MainFrame mainFrame) {
    super("Load Top-State Data Map");
    this.mainFrame = mainFrame;
    setEnabled(false);
  }

  public void actionPerformed(ActionEvent ae) {
    mainFrame.openTopStateDatamap();
  }
}
