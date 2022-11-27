package edu.umich.soar.visualsoar.misc;
import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;

import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A class that is the FeedbackList window in the MainFrame
 * its job is to provide various forms of messages from 
 * Visual Soar to the user
 * @author Brad Jones
 * @version 0.5a 4 Aug 1999
 */


public class FeedbackList extends JList implements ActionListener
{
///////////////////////////////////////////////////////////////////
// Instance Variables
///////////////////////////////////////////////////////////////////

    DefaultListModel dlm = new DefaultListModel();
    FeedbackListObject selectedObj = null;  //currently selected object in the list
    JPopupMenu rightClickContextMenu;
    JMenuItem gotoSourceMenuItem = new JMenuItem("See Related Source Code or Datamap Entry");
    JMenuItem dmAddMenuItem = new JMenuItem("Add Non-Validated Support to Datamap");


///////////////////////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////////////////////


    public FeedbackList()
    {
        setModel(dlm);

        //Constuct the context menu when users right click on a feedback item
        rightClickContextMenu = new JPopupMenu();
        gotoSourceMenuItem.addActionListener(this);
        rightClickContextMenu.add(gotoSourceMenuItem);
        dmAddMenuItem.addActionListener(this);
        rightClickContextMenu.add(dmAddMenuItem);

        //handle double click and right click
        addMouseListener(
            new MouseAdapter() 
            {
                public void mouseClicked(MouseEvent e) 
                {
                    //record currently selected object
                    int index = locationToIndex(e.getPoint());
                    if (dlm.getElementAt(index) instanceof FeedbackListObject) {
                        selectedObj = (FeedbackListObject) dlm.getElementAt(index);
                    }

                    //double click:  go to associated source code
                    if (e.getClickCount() == 2)
                    {
                        loadAssociatedSourceCode();
                    }
                    //right click:  context menu
                    else if (SwingUtilities.isRightMouseButton(e)) {
                        dmAddMenuItem.setEnabled(! selectedObj.isDataMapObject());
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
        }
        else if (action.getSource().equals(dmAddMenuItem)) {
            OperatorWindow opWin = MainFrame.getMainFrame().getOperatorWindow();
            Vector vecErrors = new Vector();
            opWin.generateDataMapForOneError(selectedObj, vecErrors);
            MainFrame.getMainFrame().setFeedbackListData(vecErrors);
        }
    }

    /**
     * loadAssociatedSourceCode
     *
     * loads the associated source code file and highlights the line that
     * caused the feedback for the currently selected feedback list object
     */
    private void loadAssociatedSourceCode() {
        if (this.selectedObj == null) return;  //nothing to do

        ListModel dlm = getModel();
        if(!this.selectedObj.isDataMapObject())
        {
            this.selectedObj.DisplayFile();
        }
        else
        {
            // check to see if datamap already opened
            DataMap dm = MainFrame.getMainFrame().getDesktopPane().dmGetDataMap(this.selectedObj.getDataMapId());

            // Only open a new window if the window does not already exist
            if( dm != null)
            {
                try
                {
                    if (dm.isIcon())
                    dm.setIcon(false);
                    dm.setSelected(true);
                    dm.moveToFront();
                }
                catch (java.beans.PropertyVetoException pve)
                {
                    System.err.println("Guess we can't do that");
                }
            }
            else
            {
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
     *
     * BUG?:  Can this be done more efficiently???
     */
    public void setListData(Vector v)
    {
        dlm.removeAllElements();
        int vecSize = v.size();
        dlm.ensureCapacity(vecSize*2);
        for(int i = 0; i < vecSize; i++)
        {
            dlm.add(i, v.get(i));
        }

        //Make sure no-one's been fiddling with the list model
        if (getModel() != dlm)
        {
            setModel(dlm);
        }
    }//setListData
    
    /**
     * Remove all the data in the list.
     *
     */
    public void clearListData()
    {
        dlm.removeAllElements();
        
        //Make sure no-one's been fiddling with the list model
        if (getModel() != dlm)
        {
            setModel(dlm);
        }
    }//clearListData
    
    /**
     * Add an item to the list
     *
     */
    public void insertElementAt(Object obj, int index)
    {
        dlm.insertElementAt(obj, index);
        
        //Make sure no-one's been fiddling with the list model
        if (getModel() != dlm)
        {
            setModel(dlm);
        }
    }//setListData

    /**
     * Append a vector to the existing list content
     *
     */
    public void appendListData(Vector v)
    {
        dlm.ensureCapacity((v.size() + dlm.size())*2);
        
        for(int i = dlm.size(), j = 0;
            j < v.size();
            i++, j++)
        {
            dlm.add(i, v.get(j));
        }

        //Make sure no-one's been fiddling with the list model
        if (getModel() != dlm)
        {
            setModel(dlm);
        }
    }//setListData



    class FeedbackCellRenderer extends JLabel implements ListCellRenderer 
    {
        private Border lineBorder = BorderFactory.createLineBorder(Color.red,2),
            emptyBorder = BorderFactory.createEmptyBorder(2,2,2,2);
        private Color textColor;
        private Color floTextColor;
                       
        public FeedbackCellRenderer() 
        {
            setOpaque(true);
            textColor = Color.blue.darker();
            floTextColor = Color.green.darker().darker();
            setFont(new Font("SansSerif",Font.PLAIN,12));
        }
        
        public Component getListCellRendererComponent(JList list,Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) 
        {
            setText(value.toString());
            if(isSelected)
            {
                setForeground(list.getSelectionForeground());
                setBackground(list.getSelectionBackground());
            }
            else 
            {
                if(value instanceof FeedbackListObject) 
                {
                    if(((FeedbackListObject)value).isGenerated()) 
                    {
                        setForeground(floTextColor);
                    }
                    else 
                    {
                        if(((FeedbackListObject)value).isError())
                        setForeground(Color.red);
                        else
                        setForeground(textColor);
                    }
                }
                else
                setForeground(list.getForeground());
                setBackground(list.getBackground());
            }
            
            setBorder(emptyBorder);
            return this;
        }
    }
}
