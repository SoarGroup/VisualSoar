package edu.umich.soar.visualsoar.mainframe.feedback;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.misc.Prefs;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;

/**
 * A class that is the FeedbackList window in the MainFrame
 * its job is to provide various forms of messages from
 * Visual Soar to the user
 *
 * @author Brad Jones
 * @version 0.5a 4 Aug 1999
 */


public class FeedbackList extends JList<FeedbackListEntry> implements ActionListener {
  private static final long serialVersionUID = 20221225L;
  private static final int ROW_TEXT_MARGIN = 7;

///////////////////////////////////////////////////////////////////
// Instance Variables
///////////////////////////////////////////////////////////////////

    private final DefaultListModel<FeedbackListEntry> dlm = new DefaultListModel<>();
    private FeedbackListEntry selectedObj = null;  //currently selected object in the list
    private final JPopupMenu rightClickContextMenu;
    private final JMenuItem gotoSourceMenuItem = new JMenuItem("See Related Source Code or Datamap Entry");
    private final JMenuItem dmAddMenuItem = new JMenuItem("Add Non-Validated Support to Datamap");

    private final FeedbackCellRenderer cellRenderer = new FeedbackCellRenderer();

///////////////////////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////////////////////


    public FeedbackList() {
        setModel(dlm);

        //Constuct the context menu when users right click on a feedback item
        rightClickContextMenu = new JPopupMenu();
        gotoSourceMenuItem.addActionListener(this);
        rightClickContextMenu.add(gotoSourceMenuItem);
        dmAddMenuItem.addActionListener(this);
        rightClickContextMenu.add(dmAddMenuItem);

        //handle double click and right click
        addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        //record currently selected object
                        int index = locationToIndex(e.getPoint());
                        if (index == -1) return; //no selected object
                        selectedObj = dlm.getElementAt(index);

                        //double click:  the feedback entry object handles this
                        if (e.getClickCount() == 2) {
                            selectedObj.react();
                        }

                        //right click:  context menu
                        else if (SwingUtilities.isRightMouseButton(e)) {
                            //There are three possible reasons for the "add to datamap" option to be grayed out:
                            //1.  the selected entry is not associated with Soar source code
                            boolean isOpNodeEntry = (selectedObj instanceof FeedbackEntryOpNode);
                            dmAddMenuItem.setEnabled(isOpNodeEntry);
                            //2.  project is read-only
                            if (dmAddMenuItem.isEnabled()) {
                                dmAddMenuItem.setEnabled(!MainFrame.getMainFrame().isReadOnly());
                            }
                            //3.  the entry is not fixable
                            if (dmAddMenuItem.isEnabled() && (isOpNodeEntry)) {
                                dmAddMenuItem.setEnabled(((FeedbackEntryOpNode)selectedObj).canFix());
                            }

                            gotoSourceMenuItem.setEnabled(selectedObj.canGoto());


                            rightClickContextMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                });

        setCellRenderer(cellRenderer);

      setFontSize(Prefs.editorFontSize.getInt());
      Prefs.editorFontSize.addChangeListener(
        newValue -> {
          setFontSize((int) newValue);
        });
    }


  private void setFontSize(int fontSize) {
    final Font newSizedFont = new Font(getFont().getName(), getFont().getStyle(), fontSize);
    setFont(newSizedFont);
    cellRenderer.setFontSize(fontSize);
  }
///////////////////////////////////////////////////////////////////
// Methods
///////////////////////////////////////////////////////////////////


    /**
     * this is called when the user selects an item on the context menu
     */
    @Override
    public void actionPerformed(ActionEvent action) {
        if (selectedObj == null) return;  //should not happen

        if (action.getSource().equals(gotoSourceMenuItem)) {
            selectedObj.react();
        }
        else if (action.getSource().equals(dmAddMenuItem)) {
            //This should only happen with "check against the datamap" errors
            if (!(selectedObj instanceof FeedbackEntryOpNode)) return; //should not happen
            FeedbackEntryOpNode entry = (FeedbackEntryOpNode) selectedObj;

            //open datamap to display this result
            OperatorNode opNode = entry.getNode();
            OperatorNode parent = (OperatorNode) opNode.getParent();
            OperatorWindow opWin = MainFrame.getMainFrame().getOperatorWindow();
            parent.openDataMap(opWin.getDatamap(), MainFrame.getMainFrame());

            //Create a non-validated datamap entry to resolve this issue
            Vector<FeedbackListEntry> vecErrors = new Vector<>();
            opWin.generateDataMapForOneError(entry, vecErrors);


            //create a new feedback list that replaces the error with fix result
            Enumeration<? extends FeedbackListEntry> iter = this.dlm.elements();
            boolean found = false;
            while(iter.hasMoreElements()) {
                FeedbackListEntry fle = iter.nextElement();
                if (fle == entry) {
                    found = true;
                }
                else {
                    if (!found) {
                        vecErrors.add(0, fle);  //prepend
                    }
                    else {
                        vecErrors.add(fle);  //append
                    }
                }
            }
            MainFrame.getMainFrame().getFeedbackManager().showFeedback(vecErrors);
        }
    }

    /**
     * Override the default implementation.  We want to update the
     * DefaultListModel class we're using here.
     */
    @Override
    public void setListData(Vector<? extends FeedbackListEntry> v) {
        dlm.removeAllElements();
        dlm.addAll(v);
    }//setListData

    public void appendListData(Collection<? extends FeedbackListEntry> v) {
      dlm.addAll(v);
    }

    /**
     * Remove all the data in the list.
     */
    public void clearListData() {
        dlm.removeAllElements();
    }//clearListData

    public int getListSize() {
      return dlm.getSize();
    }

}
