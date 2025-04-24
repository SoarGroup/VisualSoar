package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.misc.Prefs;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static edu.umich.soar.visualsoar.ruleeditor.SoarDocument.MAX_FONT_SIZE;

public class IncreaseFontSizeAction extends AbstractAction {
  private final MainFrame mainFrame;

  public IncreaseFontSizeAction(MainFrame mainFrame) {
    this.mainFrame = mainFrame;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    int currentFontSize = Prefs.editorFontSize.getInt();
    if (currentFontSize < MAX_FONT_SIZE) {
      int newFontSize = currentFontSize + 1;
      Prefs.editorFontSize.setInt(newFontSize);
      mainFrame.getFeedbackManager().setStatusBarMsg("Font size increased to " + newFontSize);
    }
  }
}
