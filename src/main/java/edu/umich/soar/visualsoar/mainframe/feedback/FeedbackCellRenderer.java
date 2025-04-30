package edu.umich.soar.visualsoar.mainframe.feedback;


import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;

/** Displays a FeedbackListEntry's text as a clickable label in an appropriate color */
class FeedbackCellRenderer extends JLabel implements ListCellRenderer<FeedbackListEntry> {
  private static final long serialVersionUID = 20221225L;

  private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);
  private static final Color FEEDBACK_ERROR_COLOR = Color.red;
  private static final Color FEEDBACK_MSG_COLOR = Color.blue.darker();

  public FeedbackCellRenderer() {
    setOpaque(true);
  }

  void setFontSize(int fontSize) {
    final Font newSizedFont = new Font(getFont().getName(), getFont().getStyle(), fontSize);
    setFont(newSizedFont);
  }

  @Override
  public Component getListCellRendererComponent(
      JList list, FeedbackListEntry entry, int index, boolean isSelected, boolean cellHasFocus) {
    setText(entry.toString());
    setForeground(list.getForeground());
    setBackground(list.getBackground());

    // special fonts and colors
    if (isSelected) {
      setForeground(list.getSelectionForeground());
      setBackground(list.getSelectionBackground());
    } else if (entry.isError()) {
      setForeground(FEEDBACK_ERROR_COLOR);
    } else if (entry instanceof FeedbackEntryOpNode) {
      if (((FeedbackEntryOpNode) entry).canFix()) {
        setForeground(FEEDBACK_MSG_COLOR);
      }
    }

    setBorder(EMPTY_BORDER);
    return this;
  }
}
