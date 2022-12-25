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


public class FeedbackList extends JList<FeedbackListObject> implements ActionListener {
///////////////////////////////////////////////////////////////////
// Instance Variables
///////////////////////////////////////////////////////////////////

    DefaultListModel<FeedbackListObject> dlm = new DefaultListModel<>();
    FeedbackListObject selectedObj = null;  //currently selected object in the list
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
                        if (dlm.getElementAt(index).hasNode()) {
                            //user may not select messages with no associated node
                            //as context operations would fail on such objects
                            selectedObj = dlm.getElementAt(index);
                        }

                        //double click:  go to associated source code
                        if (e.getClickCount() == 2) {
                            loadAssociatedSourceCode();
                        }
                        //right click:  context menu
                        else if (SwingUtilities.isRightMouseButton(e)) {
                            dmAddMenuItem.setEnabled(!selectedObj.isDataMapObject());
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
        if (action.getSource().equals(gotoSourceMenuItem)) {
            loadAssociatedSourceCode();
        } else if (action.getSource().equals(dmAddMenuItem)) {
            OperatorWindow opWin = MainFrame.getMainFrame().getOperatorWindow();
            Vector<FeedbackListObject> vecErrors = new Vector<>();
            opWin.generateDataMapForOneError(selectedObj, vecErrors);
            MainFrame.getMainFrame().setFeedbackListData(vecErrors);
        }
    }

    /**
     * loadAssociatedSourceCode
     * <p>
     * loads the associated source code file and highlights the line that
     * caused the feedback for the currently selected feedback list object
     */
    private void loadAssociatedSourceCode() {
        if (this.selectedObj == null) return;  //nothing to do

        if (!this.selectedObj.isDataMapObject()) {
            this.selectedObj.DisplayFile();
        } else {
            // check to see if datamap already opened
            DataMap dm = MainFrame.getMainFrame().getDesktopPane().dmGetDataMap(this.selectedObj.getDataMapId());

            // Only open a new window if the window does not already exist
            if (dm != null) {
                try {
                    if (dm.isIcon()) {
                        dm.setIcon(false);
                    }
                    dm.setSelected(true);
                    dm.moveToFront();
                } catch (java.beans.PropertyVetoException pve) {
                    System.err.println("Guess we can't do that");
                }
            } else {
                dm = this.selectedObj.createDataMap(MainFrame.getMainFrame().getOperatorWindow().getDatamap());
                MainFrame mf = MainFrame.getMainFrame();
                mf.addDataMap(dm);
                mf.getDesktopPane().dmAddDataMap(this.selectedObj.getDataMapId(), dm);
                dm.setVisible(true);
            }
            // Highlight the proper node within the datamap
            dm.selectEdge(this.selectedObj.getEdge());
        }

    }//loadAssociatedSourceCode

    /**
     * Override the default implementation.  We want to update the
     * DefaultListModel class we're using here.
     */
    public void setListData(Vector<? extends FeedbackListObject> v) {
        dlm.removeAllElements();
        //dlm has no "addAll" method so roll our own
        dlm.ensureCapacity(v.size());
        for (FeedbackListObject flobj : v) {
            dlm.addElement(flobj);
        }

        //Make sure no-one's been fiddling with the list model
        //:AMN:  I don't understand why we need this?
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
     * class FeedbackCellRenderer displays a FeedbackListObject's text as a clickable label in an appropriate color
     */
    static class FeedbackCellRenderer extends JLabel implements ListCellRenderer<FeedbackListObject> {
        private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        private static final Color FEEDBACK_ERROR_COLOR = Color.red;
        private static final Color FEEDBACK_MSG_COLOR = Color.blue.darker();

        public FeedbackCellRenderer() {
            setOpaque(true);
            setFont(new Font("SansSerif", Font.PLAIN, 12));
        }

        public Component getListCellRendererComponent(JList list,
                                                      FeedbackListObject value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            setText(value.toString());
            if (isSelected) {
                setForeground(list.getSelectionForeground());
                setBackground(list.getSelectionBackground());
            } else if (value.hasNode()) {
                if (value.isError()) {
                    setForeground(FEEDBACK_ERROR_COLOR);
                } else {
                    setForeground(FEEDBACK_MSG_COLOR);
                }
            } else {
                setForeground(list.getForeground());
                setBackground(list.getBackground());
            }


            setBorder(EMPTY_BORDER);
            return this;
        }
    }//class FeedbackList
}
