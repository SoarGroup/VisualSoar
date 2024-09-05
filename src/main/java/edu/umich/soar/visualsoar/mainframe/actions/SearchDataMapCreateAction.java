package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;

import java.util.Vector;

/** Search for WMEs that are never created */
public class SearchDataMapCreateAction extends SearchDataMapAction {
  private static final long serialVersionUID = 20221225L;

  public SearchDataMapCreateAction(MainFrame mainFrame) {
    super(mainFrame);
  }

  public void searchDatamap(OperatorNode opNode, Vector<FeedbackListEntry> v) {
    opNode.searchCreateDataMap(mainFrame.getOperatorWindow().getDatamap(), v);
  }
}
