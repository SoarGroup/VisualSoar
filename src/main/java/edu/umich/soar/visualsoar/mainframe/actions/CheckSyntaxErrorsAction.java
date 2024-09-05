package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.mainframe.UpdateThread;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackEntryOpNode;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import edu.umich.soar.visualsoar.parser.ParseException;
import edu.umich.soar.visualsoar.parser.SoarProduction;
import edu.umich.soar.visualsoar.parser.SuppParseChecks;
import edu.umich.soar.visualsoar.parser.TokenMgrError;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This action searches all productions in the project for syntax errors only. Operation status is
 * displayed in a progress bar. Results are displayed in the feedback list
 */
public class CheckSyntaxErrorsAction extends AbstractAction {
  private static final long serialVersionUID = 20221225L;

  private final MainFrame mainFrame;
  // a list of all production names seen is stored here so that duplicates can be found
  private final Vector<String> allProdNames = new Vector<>();

  public CheckSyntaxErrorsAction(MainFrame mainFrame) {
    super("Check All Productions for Syntax Errors");
    this.mainFrame = mainFrame;
    setEnabled(false);
  }

  public void actionPerformed(ActionEvent ae) {
    // reset the list for the new duplicate name check
    this.allProdNames.clear();

    Enumeration<TreeNode> bfe = mainFrame.getOperatorWindow().breadthFirstEnumeration();
    Vector<OperatorNode> vecNodes = new Vector<>(10, 50);
    while (bfe.hasMoreElements()) {
      vecNodes.add((OperatorNode) bfe.nextElement());
    }
    (new CheckSyntaxThread(vecNodes, "Checking Productions...")).start();
  }

  class CheckSyntaxThread extends UpdateThread {
    public CheckSyntaxThread(Vector<OperatorNode> v, String title) {
      super(mainFrame, v, title);
    }

    /** Check for duplicate production names */
    private void checkDuplicateProdNames(OperatorNode opNode) {
      Vector<String> prodNames = opNode.getProdNames();
      for (String prodName : prodNames) {
        for (String allName : CheckSyntaxErrorsAction.this.allProdNames) {
          if (allName.startsWith(prodName)) {
            // We *may* have a name conflict, but it's possible that
            // allName has a longer name.
            // trim allName to just the name and check for match
            String allNameOnly = allName.trim();
            int spaceIndex = allNameOnly.indexOf(" ");
            if (spaceIndex > 0) {
              allNameOnly = allName.substring(0, spaceIndex);
            }

            // now check for equality
            if (allNameOnly.equals(prodName)) {
              // Construct and add a FeedbackListObj
              String errStr =
                  "Warning: "
                      + allName
                      + " name conflicts with "
                      + prodName
                      + " in "
                      + opNode.getFileName();
              int lineNo = opNode.getLineNumForString(prodName);
              FeedbackListEntry flobj = new FeedbackEntryOpNode(opNode, lineNo, errStr);
              vecErrors.add(flobj);
            }
          }
        }
        // save each name in this file to check against future files
        allProdNames.add(prodName + " in " + opNode.getFileName());
      }
    }

    public boolean checkEntity(Object node) throws IOException {
      OperatorNode opNode = (OperatorNode) node;

      // do this check first since it only generates warnings
      checkDuplicateProdNames(opNode);

      try {
        // This is the main parsing here
        Vector<SoarProduction> prods = opNode.parseProductions();

        // Check for Supplemental Errors and Warnings
        if ((prods != null) && (!prods.isEmpty())) {

          // Variable on RHS never created or tested
          for (SoarProduction sprod : prods) {
            FeedbackListEntry flobj = SuppParseChecks.checkUndefinedVarRHS(opNode, sprod);
            if (flobj != null) {
              vecErrors.add(flobj);
              return true;
            }
          }

          // angle brackets used in constants
          FeedbackListEntry flobj = SuppParseChecks.warnSuspiciousConstants(opNode, prods);
          if (flobj != null) {
            vecErrors.add(flobj);
            return true;
          }
        }

      } catch (ParseException pe) {
        vecErrors.add(opNode.parseParseException(pe));
        return true;
      } catch (TokenMgrError tme) {
        vecErrors.add(opNode.parseTokenMgrError(tme));
        return true;
      }

      return false;
    }
  }
}
