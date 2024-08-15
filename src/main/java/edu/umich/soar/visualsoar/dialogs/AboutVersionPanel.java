package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Panel that displays the Version number of Visual Soar for the 'About' dialog
 *
 * @author Brian Harleton
 * @see AboutDialog
 */
class AboutVersionPanel extends JPanel {
    private static final long serialVersionUID = 20221225L;


    JLabel versionLabel =
            new JLabel("Visual Soar");

    String version = loadVersionString();
    JLabel versionLabel2 =
            new JLabel("    " + version);

    public AboutVersionPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        add(versionLabel);
        add(versionLabel2);
    }

  private String loadVersionString() {
    // this file is generated at build-time
    String resourcePath = "versionString.txt";
    getClass().getClassLoader();
    try (InputStream inputStream = ClassLoader.getSystemResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        throw new IOException("Could not find resource file 'versionString.txt'");
      }
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
        String line = reader.readLine();
        if (line == null) {
          throw new IOException("No lines found in version.txt");
        }
        return line;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return e.getMessage();
    }
  }
}
