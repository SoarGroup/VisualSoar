package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.dialogs.prefs.PreferencesDialog;
import edu.umich.soar.visualsoar.mainframe.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;

/** Creates and shows the preferences dialog */
public class PreferencesAction extends AbstractAction {
  private static final long serialVersionUID = 20221225L;
  private final MainFrame mainFrame;

  public PreferencesAction(MainFrame mainFrame) {
    super("Preferences Action");
    this.mainFrame = mainFrame;
  }

  public void actionPerformed(ActionEvent e) {
    PreferencesDialog theDialog = new PreferencesDialog(mainFrame);
    theDialog.setVisible(true);
  } // actionPerformed()
}
