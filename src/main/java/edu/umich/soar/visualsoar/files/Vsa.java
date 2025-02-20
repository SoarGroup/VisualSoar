package edu.umich.soar.visualsoar.files;

import edu.umich.soar.visualsoar.misc.Prefs;

import java.awt.*;
import java.io.File;

public class Vsa {
  /**
   * @return the file selected (or null if none)
   */
  public static File selectVsaFile(Frame parent) {
    FileDialog fileChooser = new FileDialog(parent, "Open Project", FileDialog.LOAD);
    File dir = new File(Prefs.openFolder.get());
    if ((dir.exists()) && (dir.canRead())) {
      fileChooser.setDirectory(dir.getAbsolutePath());
    }
    fileChooser.setFilenameFilter(
        (dir1, name) ->
            name.toLowerCase().endsWith(".vsa") || name.toLowerCase().endsWith(".vsa.json"));
    fileChooser.setVisible(true);
    if (fileChooser.getFile() == null) {
      return null;
    }
    return new File(fileChooser.getDirectory(), fileChooser.getFile());
  }
}
