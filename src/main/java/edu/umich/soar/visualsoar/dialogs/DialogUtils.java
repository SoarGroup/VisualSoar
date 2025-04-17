package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DialogUtils {
  /**
   * Register a listener for the {@link KeyEvent#VK_ESCAPE escape key} to close {@code dialog}. The listener is global, so
   * even if the dialog has lost focus, it will close when the key is pressed.
   * <p>
   * The dialog's default close operation will be set to {@link JDialog#DISPOSE_ON_CLOSE}
   *
   * @param dialog to close
   * @param owner  of the dialog box
   */
  public static void closeOnEscapeKey(final JDialog dialog, final Component owner) {
    // Add a global KeyEventDispatcher for ESC key
    KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    KeyEventDispatcher escDispatcher =
        e -> {
          if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
            return true; // Consume the event
          }
          return false; // Let other events proceed
        };

    // Ensure the dialog's default close operation is set to dispose; without this, the key handler
    // would work once but then never work again for subsequent dialog opens
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    // Register the dispatcher when the dialog is shown
    dialog.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowOpened(WindowEvent we) {
            dialog.setLocationRelativeTo(owner);
            dialog.requestFocus();
            focusManager.addKeyEventDispatcher(escDispatcher);
          }

          @Override
          public void windowClosed(WindowEvent we) {
            // Remove the dispatcher when the dialog is closed
            focusManager.removeKeyEventDispatcher(escDispatcher);
          }
        });
  }
}
