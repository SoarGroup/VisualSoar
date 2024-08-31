package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;

public class CloseAllWindowsAction extends AbstractAction {
  private static final long serialVersionUID = 20240426L;

  private final MainFrame mainFrame;

  public CloseAllWindowsAction(MainFrame mainFrame) {
    super("Close All Windows");
    this.mainFrame = mainFrame;
  }

  public void actionPerformed(ActionEvent e) {
    JInternalFrame[] frames = mainFrame.getDesktopPane().getAllFrames();
    for (JInternalFrame jif : frames) {
      try {
        jif.setClosed(true);
      } catch (PropertyVetoException ex) {
        /* should not happen. nbd.*/
        ex.printStackTrace();
      }
    }
  }
}
