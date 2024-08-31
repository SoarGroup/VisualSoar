package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.misc.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;

import java.util.Vector;

/** Search for WMEs that are tested but never created */
public class SearchDataMapTestNoCreateAction extends SearchDataMapAction {
  private static final long serialVersionUID = 20221225L;

  public SearchDataMapTestNoCreateAction(MainFrame mainFrame) {
    super(mainFrame);
  }

  public void searchDatamap(OperatorNode opNode, Vector<FeedbackListEntry> v) {
    opNode.searchTestNoCreateDataMap(mainFrame.getOperatorWindow().getDatamap(), v);
  }
}
