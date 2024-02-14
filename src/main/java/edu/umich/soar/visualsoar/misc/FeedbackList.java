package edu.umich.soar.visualsoar.misc;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

///////////////////////////////////////////////////////////////////
// Instance Variables
///////////////////////////////////////////////////////////////////

    DefaultListModel<FeedbackListEntry> dlm = new DefaultListModel<>();
    FeedbackListEntry selectedObj = null;  //currently selected object in the list
    JPopupMenu rightClickContextMenu;
    JMenuItem gotoSourceMenuItem = new JMenuItem("See Related Source Code or Datamap Entry");
    JMenuItem dmAddMenuItem = new JMenuItem("Add Non-Validated Support to Datamap");


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
                    public void mouseClicked(MouseEvent e) {
                        //record currently selected object
                        int index = locationToIndex(e.getPoint());
                        selectedObj = dlm.getElementAt(index);

                        //double click:  the feedback entry object handles this
                        if (e.getClickCount() == 2) {
                            selectedObj.react();
                        }

                        //right click:  context menu
                        else if (SwingUtilities.isRightMouseButton(e)) {
                            //There are two possible reasons for the "add to datamap" opton to be grayed out:

                            //1.  the selected entry is not associated with Soar source code
                            boolean isOpNodeEntry = (selectedObj instanceof FeedbackEntryOpNode);
                            dmAddMenuItem.setEnabled(isOpNodeEntry);

                            //2.  project is read-only
                            if (dmAddMenuItem.isEnabled()) {
                                dmAddMenuItem.setEnabled(!MainFrame.getMainFrame().isReadOnly());
                            }

                            rightClickContextMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                });

        setCellRenderer(new FeedbackCellRenderer());

    }
///////////////////////////////////////////////////////////////////
// Methods
///////////////////////////////////////////////////////////////////


    /**
     * this is called when the user selects an item on the context menu
     */
    @Override
    public void actionPerformed(ActionEvent action) {
        if (selectedObj != null) selectedObj.react();
        if (action.getSource().equals(gotoSourceMenuItem)) {
            selectedObj.react();
        } else if (action.getSource().equals(dmAddMenuItem)) {
            //This should only happen with "check against the datamap" errors
            if (! (selectedObj instanceof FeedbackEntryOpNode)) return; //should not happen!
            FeedbackEntryOpNode entry = (FeedbackEntryOpNode)selectedObj;

            //Create a non-validated datamap entry to resolve this issue
            OperatorWindow opWin = MainFrame.getMainFrame().getOperatorWindow();
            Vector<FeedbackListEntry> vecErrors = new Vector<>();
            opWin.generateDataMapForOneError(entry, vecErrors);
            MainFrame.getMainFrame().setFeedbackListData(vecErrors);
        }
    }

    /**
     * Override the default implementation.  We want to update the
     * DefaultListModel class we're using here.
     */
    public void setListData(Vector<? extends FeedbackListEntry> v) {
        dlm.removeAllElements();
        //dlm has no "addAll" method so roll our own
        dlm.ensureCapacity(v.size());
        for (FeedbackListEntry flobj : v) {
            dlm.addElement(flobj);
        }

        //Make sure no-one's been fiddling with the list model
        //TODO: I don't understand why we need this? -:AMN:
        if (getModel() != dlm) {
            setModel(dlm);
        }
    }//setListData

    /**
     * Remove all the data in the list.
     */
    public void clearListData() {
        dlm.removeAllElements();

        //Make sure no-one's been fiddling with the list model
        if (getModel() != dlm) {
            setModel(dlm);
        }
    }//clearListData


    /**
     * class FeedbackCellRenderer displays a FeedbackListEntry's text as a clickable label in an appropriate color
     */
    static class FeedbackCellRenderer extends JLabel implements ListCellRenderer<FeedbackListEntry> {
        private static final long serialVersionUID = 20221225L;

        private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        private static final Color FEEDBACK_ERROR_COLOR = Color.red;
        private static final Color FEEDBACK_MSG_COLOR = Color.blue.darker();

        public FeedbackCellRenderer() {
            setOpaque(true);
            setFont(new Font("SansSerif", Font.PLAIN, 12));
        }

        public Component getListCellRendererComponent(JList list,
                                                      FeedbackListEntry entry,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            setText(entry.toString());
            setForeground(list.getForeground());
            setBackground(list.getBackground());

            //special fonts and colors
            if (isSelected) {
                setForeground(list.getSelectionForeground());
                setBackground(list.getSelectionBackground());
            } else if (entry.isError()) {
                setForeground(FEEDBACK_ERROR_COLOR);
            }
            else if (entry instanceof FeedbackEntryOpNode) {
                setForeground(FEEDBACK_MSG_COLOR);
            }


            setBorder(EMPTY_BORDER);
            return this;
        }
    }//class FeedbackList
}
