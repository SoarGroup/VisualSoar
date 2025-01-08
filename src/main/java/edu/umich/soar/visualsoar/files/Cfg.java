package edu.umich.soar.visualsoar.files;

import static edu.umich.soar.visualsoar.files.Util.saveToFile;

import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorRootNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;
import edu.umich.soar.visualsoar.operatorwindow.SoarOperatorNode;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.tree.TreeNode;

public class Cfg {
  private static final Logger LOGGER = Logger.getLogger(Cfg.class.getName());
  /**
   * readCfgFile
   *
   * <p>is called when the project is loaded to read information about what files were open last time
   * and restore them
   */
  public static void readCfgFile(MainFrame mainFrame) {
    OperatorWindow operatorWindow = mainFrame.getOperatorWindow();
    // Read file contents
    Path cfgFile = getCfgFile(operatorWindow);
    if ((cfgFile == null) || (!Files.exists(cfgFile))) {
      return; // nothing to do
    }
    Path projectRoot = cfgFile.getParent();
    List<String> cfgLines;
    try {
      cfgLines = Files.readAllLines(cfgFile);
    } catch (IOException e) {
      mainFrame.getFeedbackManager().setStatusBarMsg("Unable to read previous configuration from " + cfgFile.getFileName());
      return;
    }

    // Process each line
    for (String line : cfgLines) {
      String[] tokens = line.split(" ");
      if (tokens.length != 2) continue; // invalid line

      // Calculate the canonical path of the filename for equality comparison
      String target;
      try {
        target = projectRoot.resolve(Paths.get(tokens[1])).toRealPath().toString();
      } catch (IOException ioe) {
        continue; // fail quietly; should never happen
      }

      // Find the associated node and open a window for it
      Enumeration<TreeNode> bfe = operatorWindow.breadthFirstEnumeration();
      boolean applied = false;
      while (bfe.hasMoreElements()) {
        OperatorNode node = (OperatorNode) bfe.nextElement();
        if (node.getFileName() == null) continue; // skip the fluff

        // calculate the canonical path so equality comparison will work as intended
        String nodeFN;
        try {
          nodeFN = projectRoot.resolve(node.getFileName()).toRealPath().toString();
//          TODO next: relativize to cfgFile
//          nodeFN = (new File(node.getFileName())).getCanonicalPath();
        } catch (IOException ioe) {
          continue; // fail quietly; should never happen
        }

        if (nodeFN.equals(target)) {
          if (tokens[0].equals("RULEEDITOR")) {
            node.openRules(mainFrame);
            applied = true;
          } else if (tokens[0].equals("DATAMAP")) {
            node.openDataMap(operatorWindow.getDatamap(), mainFrame);
            applied = true;
          } else {
            LOGGER.warning("Unknown directive in cfg file: " + tokens[0]);
          }
        }
      }
      if (!applied) {
        LOGGER.warning("Unable to apply line from cfg file: " + line);
      }
    }
  }

  /**
   * writeCfgFile
   *
   * <p>saves information about what files are open in the project so that they can be restored when
   * the file is loaded.
   *
   * <p>Each line in the config file is one of the following: RULEEDITOR &lt;filename&gt; DATAMAP
   * &lt;filename&gt;
   */
  public static void writeCfgFile(MainFrame mainFrame) {
    // Sanity check:  is a project currently open?
    if (mainFrame.getOperatorWindow() == null) {
      return;
    }

    Path cfgFile = getCfgFile(mainFrame.getOperatorWindow());
    if (cfgFile == null) {
      return;
    }
    Path projectRoot = cfgFile.getParent();

    // Create the .cfg file contents
    ArrayList<String> cfgLines = new ArrayList<>();
    JInternalFrame[] jif = mainFrame.getDesktopPane().getAllFrames();
    for (JInternalFrame jInternalFrame : jif) {
      if (jInternalFrame instanceof RuleEditor) {
        RuleEditor re = (RuleEditor) jInternalFrame;
        String line = "RULEEDITOR " + getPathForWriting(projectRoot, re.getFile());
        cfgLines.add(line);
      } else if (jInternalFrame instanceof DataMap) {
        DataMap dm = (DataMap) jInternalFrame;
        int dmId = dm.getId();

        // Find the filename of the SoarOperatorNode associated with this id
        Enumeration<TreeNode> bfe = mainFrame.getOperatorWindow().breadthFirstEnumeration();
        while (bfe.hasMoreElements()) {
          OperatorNode node = (OperatorNode) bfe.nextElement();
          if (node instanceof SoarOperatorNode) {
            SoarOperatorNode son = (SoarOperatorNode) node;
            if (son.getDataMapIdNumber() == dmId) {
              String line = "DATAMAP " + getPathForWriting(projectRoot, son.getFileName());
              cfgLines.add(line);
              break;
            }
          }
        }
      }
    }

    // Write the .cfg file contents
    try {
      saveToFile(
          cfgFile,
          (OutputStream out) -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {
              for (String line : cfgLines) {
                writer.write(line);
                writer.write("\n");
              }
            }
          });
    } catch (IOException e) {
      // the .cfg file is not essential so just report it if it can't be written
      mainFrame
          .getFeedbackManager()
          .setStatusBarMsg("Unable to save current configuration to " + cfgFile.getFileName());
    }
  }

  private static String getPathForWriting(Path projectRoot, String fileName) {
    // write path relative to project root, and use / instead of \ for cross-platform support
    return projectRoot.relativize(Paths.get(fileName)).toString().replace("\\", "/");
  }

  /**
   * @return a File object representing where the project's .cfg file should be
   */
  private static Path getCfgFile(OperatorWindow operatorWindow) {
    Object root = operatorWindow.getModel().getRoot();
    if (root instanceof OperatorRootNode) {
      OperatorRootNode orn = (OperatorRootNode) root;
      Path projectDir = Paths.get(orn.getFolderName());
      return projectDir.resolve(orn.getName() + ".cfg");
    }
    LOGGER.warning("Attempted to get .cfg path when root was not an operator window");
    return null;
  }
}
