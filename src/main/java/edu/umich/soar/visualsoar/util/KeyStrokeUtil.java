package edu.umich.soar.visualsoar.util;

import javax.swing.*;

public class KeyStrokeUtil {
  public static KeyStroke getPlatformKeyStroke(String key) {
    String modifier =
        System.getProperty("os.name").toLowerCase().contains("mac") ? "meta" : "control";
    return KeyStroke.getKeyStroke(modifier + " " + key);
  }
}
