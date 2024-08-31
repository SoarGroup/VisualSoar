package edu.umich.soar.visualsoar.util;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import sml.Agent;

import javax.swing.*;
import java.awt.*;

public class SoarUtils {

  public static void sourceFile(String path, Component parent) {
    if (path == null) {
      JOptionPane.showMessageDialog(
          parent,
          "sourceFile called with null path. This is a bug. Please report to developers.",
          "Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }
    // Soar commands lose backslashes, so we use forward slashes instead
    path = path.replace("\\\\", "/");
    String command = "source " + "\"" + path + "\"";
    executeCommandLine(command, parent, true);
  }

  public static void executeCommandLine(
      String command, Component parent, boolean reportToMainframe) {
    Agent agent = getAgent(parent);
    if (agent == null) {
      return;
    }
    String result = agent.ExecuteCommandLine(command, true);
    if (agent.GetKernel().HadError()) {
      result += "\n" + agent.GetLastErrorDescription();
    }

    if (reportToMainframe) {
      MainFrame.getMainFrame().setStatusBarMsg("Sent command: " + command);
      MainFrame.getMainFrame().reportResult(result);
    }
  }

  private static Agent getAgent(Component parent) {
    Agent agent = MainFrame.getMainFrame().getActiveAgent();
    if (agent == null) {
      JOptionPane.showMessageDialog(
          parent,
          "Not connected to an agent. Please connect via the \"Soar Runtime\" menu.",
          "Error",
          JOptionPane.ERROR_MESSAGE);
      return null;
    }
    return agent;
  }
}
