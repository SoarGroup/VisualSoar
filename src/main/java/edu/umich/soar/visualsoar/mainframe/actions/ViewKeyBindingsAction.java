package edu.umich.soar.visualsoar.mainframe.actions;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

/**
 * This is where the user wants a list of keybindings. The action points the user to the online
 * documentation.
 */
public class ViewKeyBindingsAction extends AbstractAction {
  private static final long serialVersionUID = 20221225L;
  private static final String KEY_BINDINGS_HELP_URL =
      "https://soar.eecs.umich.edu/reference/VisualSoarKeyboardAndMouseControls";

  private final Component parentComponent;

  public ViewKeyBindingsAction(Component parentComponent) {
    super("VisualSoar Keybindings");
    this.parentComponent = parentComponent;
  }

  public void actionPerformed(ActionEvent e) {
    final JTextPane textPane = new JTextPane();
    textPane.setContentType("text/html");
    textPane.setText(
        "<html>View VisualSoar key bindings help "
            + "on the Soar website: <a href=\""
            + KEY_BINDINGS_HELP_URL
            + "\">"
            + KEY_BINDINGS_HELP_URL
            + "</a>.</html>");
    textPane.setEditable(false);
    // get rid of white background
    textPane.setBackground(UIManager.getColor("OptionPane.background"));
    // make the link clickable
    textPane.addHyperlinkListener(
        he -> {
          if (HyperlinkEvent.EventType.ACTIVATED.equals(he.getEventType())) {
            try {
              Desktop.getDesktop().browse(new URI(he.getURL().toString()));
            } catch (IOException | URISyntaxException ex) {
              ex.printStackTrace();
            }
          }
        });
    JOptionPane.showMessageDialog(
        parentComponent, textPane, "Key Bindings Help", JOptionPane.INFORMATION_MESSAGE);
  }
}
