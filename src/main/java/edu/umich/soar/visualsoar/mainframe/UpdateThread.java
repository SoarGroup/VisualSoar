package edu.umich.soar.visualsoar.mainframe;

import edu.umich.soar.visualsoar.misc.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Vector;

/**
 * This is a generic class for scanning a set of entities for errors in a separate thread and
 * providing a progress dialog while you do so. You must subclass this class to use it.
 */
public abstract class UpdateThread extends Thread {
  private final MainFrame mainFrame;
  protected Runnable update, finish;
  protected int value, min, max;
  protected JProgressBar progressBar;
  protected JDialog progressDialog;
  protected Vector<OperatorNode> vecEntities;
  protected Vector<FeedbackListEntry> vecErrors = new Vector<>();
  protected int entityNum = 0;

  public UpdateThread(MainFrame mainFrame, Vector<OperatorNode> v, String title) {
    this.mainFrame = mainFrame;
    vecEntities = v;
    max = v.size();
    progressBar = new JProgressBar(0, max);
    progressDialog = new JDialog(mainFrame, title);
    progressDialog.getContentPane().setLayout(new FlowLayout());
    progressDialog.getContentPane().add(progressBar);
    progressBar.setStringPainted(true);
    progressDialog.setLocationRelativeTo(mainFrame);
    progressDialog.pack();
    progressDialog.setVisible(true);
    progressBar.getMaximum();
    progressBar.getMinimum();

    update =
        () -> {
          value = progressBar.getValue() + 1;
          updateProgressBar(value);
        };
    finish =
        () -> {
          updateProgressBar(min);
          progressDialog.dispose();
        };
  }

  public void run() {
    checkEntities();
  }

  private void updateProgressBar(int value) {
    progressBar.setValue(value);
  }

  /**
   * Override this function in your subclass. It scans the given entity for errors and places them
   * in the vecErrors vector. vecErrors can either contain Strings or FeedbackListEntry objects
   *
   * @param o object to scan
   * @return true if any errors were found
   */
  public abstract boolean checkEntity(Object o) throws IOException;

  public void checkEntities() {
    try {
      boolean anyErrors = false;
      for (int i = 0; i < max; i++) {
        boolean errDetected = checkEntity(vecEntities.elementAt(i));
        if (errDetected) {
          anyErrors = true;
        }
        updateProgressBar(++entityNum);
        SwingUtilities.invokeLater(update);
      }

      if (!anyErrors) {
        vecErrors.add(new FeedbackListEntry("There were no errors detected in this project."));
      }
      mainFrame.setFeedbackListData(vecErrors);
      SwingUtilities.invokeLater(finish);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  } // checkEntities()
} // class UpdateThread
