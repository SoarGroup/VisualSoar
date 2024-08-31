package edu.umich.soar.visualsoar.files;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorRootNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;
import edu.umich.soar.visualsoar.operatorwindow.SoarOperatorNode;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;

public class Cfg {
  /**
   * readCfgFile
   *
   * <p>is called when the project is loaded to read information about what files are open last time
   * and restore them
   */
  public static void readCfgFile(MainFrame mainFrame) {
    OperatorWindow operatorWindow = mainFrame.getOperatorWindow();
    // Read file contents
    File cfgFile = getCfgFile(operatorWindow);
    if ((cfgFile == null) || (!cfgFile.exists())) return; // nothing to do
    Scanner scan;
    ArrayList<String> cfgLines = new ArrayList<>();
    try {
      scan = new Scanner(cfgFile);
      while (scan.hasNextLine()) cfgLines.add(scan.nextLine());
      scan.close();
    } catch (FileNotFoundException fnfe) {
      mainFrame.setStatusBarMsg("Unable to read previous configuration from " + cfgFile.getName());
      return;
    }

    // Process each line
    for (String line : cfgLines) {
      String[] tokens = line.split(" ");
      if (tokens.length != 2) continue; // invalid line

      // Calculate the canonical path of the filename for equality comparison
      String target;
      try {
        target = (new File(tokens[1])).getCanonicalPath();
      } catch (IOException ioe) {
        continue; // fail quietly; should never happen
      }

      // Find the associated node
      Enumeration<TreeNode> bfe = operatorWindow.breadthFirstEnumeration();
      while (bfe.hasMoreElements()) {
        OperatorNode node = (OperatorNode) bfe.nextElement();
        if (node.getFileName() == null) continue; // skip the fluff

        // calculate the canonical path so equality comparison will work as intended
        String nodeFN;
        try {
          nodeFN = (new File(node.getFileName())).getCanonicalPath();
        } catch (IOException ioe) {
          continue; // fail quietly; should never happen
        }

        if (nodeFN.equals(target)) {
          if (tokens[0].equals("RULEEDITOR")) {
            node.openRules(mainFrame);
          } else if (tokens[0].equals("DATAMAP")) {
            node.openDataMap(operatorWindow.getDatamap(), mainFrame);
          }
        }
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
    if (mainFrame.getOperatorWindow() == null) return;

    // Create the .cfg file contents
    ArrayList<String> cfgLines = new ArrayList<>();
    JInternalFrame[] jif = mainFrame.getDesktopPane().getAllFrames();
    for (JInternalFrame jInternalFrame : jif) {
      if (jInternalFrame instanceof RuleEditor) {
        RuleEditor re = (RuleEditor) jInternalFrame;
        String line = "RULEEDITOR " + re.getFile();
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
              String line = "DATAMAP " + son.getFileName();
              cfgLines.add(line);
              break;
            }
          }
        }
      }
    }

    // Write the .cfg file contents
    File cfgFile = getCfgFile(mainFrame.getOperatorWindow());
    if ((cfgFile != null) && (cfgFile.exists())) {
      try {
        PrintWriter pw = new PrintWriter(cfgFile);
        for (String line : cfgLines) {
          pw.println(line);
        }
        pw.close();
      } catch (FileNotFoundException fnfe) {
        // the .cfg file is not essential so just report it if it can't be written
        mainFrame.setStatusBarMsg("Unable to save current configuration to " + cfgFile.getName());
      }
    }
  }

  /**
   * @return a File object representing where the project's .cfg file should be
   */
  private static File getCfgFile(OperatorWindow operatorWindow) {
    Object root = operatorWindow.getModel().getRoot();
    if (root instanceof OperatorRootNode) {
      OperatorRootNode orn = (OperatorRootNode) root;
      String cfgFN = orn.getFolderName() + File.separator + orn.getName() + ".cfg";
      return new File(cfgFN);
    }

    return null;
  }
}
