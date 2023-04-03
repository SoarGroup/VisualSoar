package edu.umich.soar.visualsoar.misc;

import javax.swing.*;


/**
 * This class extends JInternalFrame so we can keep some VisualSoar specific
 * information with it.
 */
public class CustomInternalFrame extends JInternalFrame {
    private static final long serialVersionUID = 20221225L;

    /*======================================================================
      Constants
      ----------------------------------------------------------------------
     */
    public static int UNKNOWN = 0;
    public static int RULE_EDITOR = 1;
    public static int DATAMAP = 2;

    /*======================================================================
      Data Members
      ----------------------------------------------------------------------
     */
    protected int type;
    private boolean change = false;  //have the contents of this window been changed?
    private static boolean everChanged = false; //once set to true, stays true until a new project is loaded


    public CustomInternalFrame(String title, boolean resizable, boolean closable,
                               boolean maximizable, boolean iconifiable) {
        super(title, resizable, closable, maximizable, iconifiable);

        type = UNKNOWN;
    }

    /*======================================================================
      Accessor Methods
      ----------------------------------------------------------------------
     */
    public int getType() {
        return type;
    }

    protected void setType(int t) {
        type = t;
    }


    public boolean isModified() {
        return change;
    }

    //Allow user to mark a document as unchanged.
    public void setModified(boolean b) {
        change = b;
        if (change) everChanged = true;
    }


    //has any file in this project ever been changed?
    public static boolean hasEverChanged() { return CustomInternalFrame.everChanged; }
    public static void resetEverchanged() { CustomInternalFrame.everChanged = false; }

    /*======================================================================
      Public Methods
      ----------------------------------------------------------------------
     */


}//CustomInternalFrame
