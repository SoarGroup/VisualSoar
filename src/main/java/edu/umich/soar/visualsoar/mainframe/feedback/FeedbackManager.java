package edu.umich.soar.visualsoar.mainframe.feedback;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class FeedbackManager {

  private final FeedbackList feedbackList;
  private final JLabel statusBar;
  private boolean inAtomicFeedback;

  public FeedbackManager(FeedbackList feedbackList, JLabel statusBar) {
    this.feedbackList = feedbackList;
    this.statusBar = statusBar;
  }

  /**
   * @see #beginAtomicContext()
   */
  public class AtomicContext implements AutoCloseable {
    private AtomicContext() {
      inAtomicFeedback = true;
    }

    @Override
    public void close() {
      if (!inAtomicFeedback) {
        System.err.println(
            "WARNING: "
                + getClass().getName()
                + " closed after "
                + FeedbackManager.this.getClass().getName()
                + " had already exited atomic feedback mode");
      }
      inAtomicFeedback = false;
    }
  }

  /**
   * Use with try-with-resources syntax to start and end an atomic feedback context
   * automatically. While the mode is active, {@link #showFeedback(Vector)} will append
   * feedback rather than replacing the entire list with its argument. This is useful for
   * complex actions where nested calls may register feedback multiple times.
   * @return an autocloseable
   */
  public AtomicContext beginAtomicContext() {
    return new AtomicContext();
  }

  /**
   * Method updates the FeedBack list window.  If your message is a single string
   * consider using the status bar to display your message instead.
   *
   * @param v the vector list of feedback data; {@code null} either clears or doesn't modify the list.
   */
  public void showFeedback(
    @NotNull Collection<? extends FeedbackListEntry> v) {
    if (inAtomicFeedback) {
      feedbackList.appendListData(v);
    } else {
      feedbackList.setListData(new Vector<>(v));
    }
  }

  /**
   * @see #showFeedback(Collection)
   */
  public void showFeedback(@NotNull FeedbackListEntry e) {
    showFeedback(List.of(e));
  }

  public void clearFeedback() {
    feedbackList.clearListData();
  }

  /**
   * Method updates the status bar text with a message
   */
  public void setStatusBarMsg(String text) {
    //Extra spaces make text align better with feedback window above it
    statusBar.setForeground(Color.black);
    statusBar.setText("  " + text);
  }

  /**
   * Method updates the status bar text with list of strings.  These
   * are displayed on a single line.
   */
  public void setStatusBarMsgList(List<String> msgs) {
    if (msgs.isEmpty()) return; //nop
    StringBuilder sb = new StringBuilder();
    for (String match : msgs) {
      sb.append("   ");
      sb.append(match);
    }
    statusBar.setText(sb.toString());
  }

  /**
   * Method updates the status bar text with a message that indicates a user error
   */
  public void setStatusBarError(String text)
  {
    //Extra spaces make text align better with feedback window above it
    statusBar.setForeground(Color.red);
    statusBar.setText("  " + text);
    statusBar.getToolkit().beep();
  }
}
