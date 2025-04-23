package edu.umich.soar.visualsoar.mainframe;

import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import org.jetbrains.annotations.Nullable;

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
  protected int value, min, numEntities;
  protected JProgressBar progressBar;
  protected JDialog progressDialog;
  protected Vector<OperatorNode> vecEntities;
  protected Vector<FeedbackListEntry> vecErrors = new Vector<>();
  protected int entityNum = 0;

  private boolean anyErrors;

  public UpdateThread(MainFrame mainFrame, Vector<OperatorNode> v, String title) {
    this.mainFrame = mainFrame;
    vecEntities = v;
    numEntities = v.size();
    progressBar = new JProgressBar(0, numEntities);
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
    postAction();
  }

  /**
   * This will be called when {@link #run()} is finished.
   */
  public void postAction(){}

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
    anyErrors = false;
    for (int i = 0; i < numEntities; i++) {
      try {
        boolean errDetected = checkEntity(vecEntities.elementAt(i));
        if (errDetected) {
          anyErrors = true;
        }
      } catch(IOException e) {
        e.printStackTrace();
        vecErrors.add(new FeedbackListEntry(e.getMessage(), true));
        anyErrors = true;
      }
      updateProgressBar(++entityNum);
      SwingUtilities.invokeLater(update);
    }

    if (!anyErrors) {
      String message = getSuccessMessage();
      if (message != null) {
        vecErrors.add(new FeedbackListEntry(message));
      }
    } else if (vecErrors.isEmpty()) {
      // This should never happen, as errors should be added to vecErrors.
      // TODO: return vecErrors from checkEntities instead so we don't have
      // two separate indicators for errors.
      vecErrors.add(new FeedbackListEntry("Unknown error occurred"));
    }
    mainFrame.getFeedbackManager().showFeedback(vecErrors);
    SwingUtilities.invokeLater(finish);
  }

  /**
   * @return true if the call to {@link #checkEntities()} found any errors, false otherwise
   */
  public boolean foundAnyErrors() {
    return anyErrors;
  }

  /**
   *
   * @return If not null, show the message to the user on success. Otherwise, show no message.
   */
  @Nullable
  protected String getSuccessMessage() {
    return "There were no errors detected in this project.";
  }
}
