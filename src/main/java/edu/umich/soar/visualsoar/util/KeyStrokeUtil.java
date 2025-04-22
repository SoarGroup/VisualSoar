package edu.umich.soar.visualsoar.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class KeyStrokeUtil {
  public static KeyStroke getPlatformKeyStroke(String keyString) {
    String modifier =
      Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() == KeyEvent.META_DOWN_MASK ? "meta" : "control";
    KeyStroke keyStroke = KeyStroke.getKeyStroke(modifier + " " + keyString);
    if (keyStroke == null) {
      throw new IllegalArgumentException("Invalid key stroke name: " + keyString);
    }
    return keyStroke;
  }
}
