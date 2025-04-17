package edu.umich.soar.visualsoar.dialogs;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackEntryOpNode;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 * Dialog which takes input for, and initiates a find or replace operation over multiple project
 * files.
 *
 * @author Brian Harleton
 * @see FindInProjectDialog
 */
public class ReplaceInProjectDialog extends JDialog {
  private static final long serialVersionUID = 20221225L;

  /** These keep track of place in directory tree that search is currently being performed. */
  OperatorNode root;

  Enumeration<TreeNode> bfe;
  boolean
      searchingRuleEditor; // is the current file being searched open in a rule editor window right
                           // now?
  boolean stringFound;
  boolean stringSelected;
  OperatorNode current;
  String fn;
  String lastToFind;

  /** panel which contains the find input field and match case option */
  FindInProjectPanel findPanel = new FindInProjectPanel();

  /** Holds the current rule editor that find/replace is currently in */
  RuleEditor d_ruleEditor = null;

  OperatorWindow opWin;

  /** panel which contains all the "replace input" fields */
  ReplacePanel replacePanel = new ReplacePanel();

  FindReplaceButtonPanel buttonPanel = new FindReplaceButtonPanel();
  Vector<FeedbackListEntry> v = new Vector<>();

  /**
   * Dialog that searches through all the files within a project for a string and replaces that
   * string with another string. Replace button replaces currently selected string with replacer
   * string and then moves to the next matching string in the project. Find Next button highlights
   * finds the next matching string in the project and highlights that string.
   *
   * @param owner Frame which owns the dialog
   * @param operators a reference to the OperatorWindow
   * @param opNode operator tree to search (a null value indicates the entire project should be
   *     searched)
   */
  public ReplaceInProjectDialog(final Frame owner, OperatorWindow operators, OperatorNode opNode) {
    super(owner, "Find and Replace In Project", false);

    opWin = operators;
    setResizable(false);
    Container contentPane = getContentPane();
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    contentPane.setLayout(gridbag);

    root = opNode;
    bfe = root.breadthFirstEnumeration();
    searchingRuleEditor = false;
    stringFound = false;
    current = null;
    lastToFind = null;
    stringSelected = false;

    // specifies component as last one on the row
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.fill = GridBagConstraints.HORIZONTAL;

    contentPane.add(findPanel, c);
    contentPane.add(replacePanel, c);
    contentPane.add(buttonPanel, c);
    pack();
    getRootPane().setDefaultButton(buttonPanel.findButton);

    // Set the match case as un-focusable so user can
    // quickly tab between the find & replace fields
    findPanel.optionsPanel.matchCase.setFocusable(false);

    DialogUtils.closeOnEscapeKey(this, owner);
    addWindowListener(
        new WindowAdapter() {
          public void windowOpened(WindowEvent we) {
            setLocationRelativeTo(owner);
            findPanel.requestFocus();
          }
        });

    buttonPanel.cancelButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            dispose();
          }
        });

    /*
     *   Replace all replaces all requested strings with the replacement string.
     *   All instances of replacement are sent to the feedback list.
     */
    buttonPanel.replaceAllButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Object[] theData = findPanel.getData();
            String toFind = (String) theData[0];
            String toReplace = replacePanel.getText();
            Boolean caseSensitive = (Boolean) theData[1];

            // If user changed the 'To Find' string, reset search to beginning of tree
            if (!toFind.equals(lastToFind)) {
              lastToFind = toFind;
              // Reset tree to beginning of project
              bfe = root.breadthFirstEnumeration();

              stringSelected = false;
              searchingRuleEditor = false;
            }

            //  Do an initial search to get the line numbers of all the replaced components
            if (!caseSensitive) {
              toFind = toFind.toLowerCase();
            }

            while (bfe.hasMoreElements()) {
              OperatorNode current = (OperatorNode) bfe.nextElement();
              String fn = current.getFileName();

              if (fn != null) {
                try {
                  LineNumberReader lnr = new LineNumberReader(new FileReader(fn));
                  String line = lnr.readLine();
                  while (line != null) {
                    if (!caseSensitive) {
                      line = line.toLowerCase();
                    }
                    if (line.contains(toFind)) {
                      v.add(
                          new FeedbackEntryOpNode(
                              current,
                              lnr.getLineNumber(),
                              "Replaced " + toFind + " with " + toReplace + ".",
                              toReplace));
                    }
                    line = lnr.readLine();
                  }
                  lnr.close();
                } catch (FileNotFoundException fnfe) {
                  System.err.println("Couldn't find: " + fn);
                } catch (IOException ioe) {
                  System.err.println("Error reading from file " + fn);
                }
              }
            }

            if (v.isEmpty()) {
              String msg = toFind + " not found in project";
              v.add(new FeedbackListEntry(msg));
            }

            // Special case, if toReplace contains toFind as a substring,
            // then VisualSoar can get into an infinite loop.
            // So, we handle that with a two-step replace.  Yes, this
            // is a kludge.  To really fix this the whole find/replace
            // system needs to be re-architected and that seems not
            // worth the time investment.  -:AMN: 20 Apr 2024
            if (toReplace.contains(toFind)) {
              String unique = uniqueStr(toFind);
              bfe = root.breadthFirstEnumeration();
              while (bfe.hasMoreElements() || searchingRuleEditor) {
                if (stringSelected) {
                  d_ruleEditor.replace();
                }
                findInProject(toFind, unique, caseSensitive, true);
              }
              toFind = unique; // set up for stage 2 below
              caseSensitive = true;
            }

            // Do the main replacement
            bfe = root.breadthFirstEnumeration();
            while (bfe.hasMoreElements() || searchingRuleEditor) {
              if (stringSelected) {
                d_ruleEditor.replace();
              }

              // General replace
              findInProject(toFind, toReplace, caseSensitive, true);
            }

            MainFrame.getMainFrame().getFeedbackManager().showFeedback(v);
          }
        });

    buttonPanel.replaceButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Object[] theData = findPanel.getData();
            String toFind = (String) theData[0];
            String toReplace = replacePanel.getText();
            Boolean caseSensitive = (Boolean) theData[1];

            // If user changed the 'To Find' string
            if (!toFind.equals(lastToFind)) {
              lastToFind = toFind;
              // Reset tree to beginning of project
              bfe = root.breadthFirstEnumeration();

              stringSelected = false;
              searchingRuleEditor = false;
            }

            if (stringSelected) {
              // replace
              d_ruleEditor.replace();
            }

            findInProject(toFind, toReplace, caseSensitive, false);
          }
        });

    buttonPanel.findButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Object[] theData = findPanel.getData();
            String toFind = (String) theData[0];
            String toReplace = replacePanel.getText();
            Boolean caseSensitive = (Boolean) theData[1];

            // If user changed the 'To Find' string
            if (!toFind.equals(lastToFind)) {
              lastToFind = toFind;
              // Reset tree to beginning of project
              bfe = root.breadthFirstEnumeration();

              searchingRuleEditor = false;
            }

            findInProject(toFind, toReplace, caseSensitive, false);
          }
        });
  } // ReplaceInProjectDialog ctor

  /**
   * Generate a random string of non-whitespace ASCII characters that is very unlikely to exist in
   * the project and does not contain the given string as a substring.
   */
  private String uniqueStr(String toFind) {
    Random rand = new Random();
    StringBuilder unique = new StringBuilder();
    while (unique.length() < 30) { // 30 characters seems more than sufficient

      // add a random non-whitespace char
      int nonWhitespaceIndex = 33 + rand.nextInt(90);
      char someChar = (char) nonWhitespaceIndex;

      // Make sure toFind isn't a substring of this
      if (!(unique.toString() + someChar).contains(toFind)) {
        unique.append(someChar);
      }
    }

    return unique.toString();
  } // uniqueStr

  private void findInProject(
      String toFind, String toReplace, Boolean caseSensitive, boolean outputToFeedbackList) {
    boolean matchCase = caseSensitive;
    boolean foundInFile = false;
    stringFound = false;
    String reFileName;

    while (!stringFound && (bfe.hasMoreElements() || searchingRuleEditor)) {
      if (!searchingRuleEditor) {
        current = (OperatorNode) bfe.nextElement();
        fn = current.getFileName();

        // See if Rule Editor is already open for that file.  If it is, then start searching Rule
        // Editor
        JInternalFrame[] bif =
            MainFrame.getMainFrame().getDesktopPane().getAllFrames(); // Get all open Rule Editors
        for (JInternalFrame jInternalFrame : bif) {
          if (jInternalFrame instanceof RuleEditor) {
            RuleEditor be = (RuleEditor) jInternalFrame;
            reFileName = be.getFile();

            if (reFileName.equals(fn)) {
              d_ruleEditor = be;
              d_ruleEditor.resetCaret();
              searchingRuleEditor = true;
              fn = null;
            } // found rule editor that matches filename where bfe is currently at
          }
        } // end of for (going through open RE's looking for correct one)

        if (fn != null) {
          try {
            LineNumberReader lnr = new LineNumberReader(new FileReader(fn));
            String line = lnr.readLine();

            while ((line != null) && !foundInFile) {
              if (!matchCase) {
                line = line.toLowerCase();
              }
              if (line.contains(toFind)) {
                // Found a matching string in this line
                foundInFile = true;
                current.openRules(MainFrame.getMainFrame());
                // set correct rule editor

                JInternalFrame[] jif =
                    MainFrame.getMainFrame()
                        .getDesktopPane()
                        .getAllFrames(); // Get all open Rule Editors
                for (JInternalFrame jInternalFrame : jif) {
                  if (jInternalFrame instanceof RuleEditor) {
                    RuleEditor re = (RuleEditor) jInternalFrame;
                    reFileName = re.getFile();

                    if (reFileName.equals(fn)) {
                      d_ruleEditor = re;
                      d_ruleEditor.resetCaret();
                      searchingRuleEditor = true;
                    } // found rule editor that matches filename where bfe is currently at
                  }
                } // end of for (going through open RE's looking for correct one)
              } // end of If(found the string on current line
              line = lnr.readLine(); // get next line
            } // end of while searching for a match in current file
            lnr.close();
          } // end of try reading a line in a file
          catch (FileNotFoundException fnfe) {
            System.err.println("Couldn't find: " + fn);
          } catch (IOException ioe) {
            System.err.println("Error reading from file " + fn);
          }
        } // if fn is a valid file
      } // end of not searching in a rule editor
      if (searchingRuleEditor) {
        // Do rule editor stuff
        d_ruleEditor.setFindReplaceData(
            toFind, toReplace, Boolean.TRUE, caseSensitive, Boolean.TRUE);
        if (d_ruleEditor.findResult()) {
          stringFound = true;
          stringSelected = true;
        } else {
          searchingRuleEditor = false;
        }
      } // end of searching within a rule editor
    } // end of while, either found string or no more strings in project

    if (!bfe.hasMoreElements() && !searchingRuleEditor && !outputToFeedbackList) {
      JOptionPane.showMessageDialog(
          MainFrame.getMainFrame(),
          "No more instances of " + toFind + " found in project",
          "End of Search",
          JOptionPane.INFORMATION_MESSAGE);
    }
  } // end of findInProject()
} // end of ReplaceInProjectDialog class
