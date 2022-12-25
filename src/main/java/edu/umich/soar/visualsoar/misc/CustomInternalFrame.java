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
        
    /*======================================================================
      Public Methods
      ----------------------------------------------------------------------
     */

    /**
     * This method returns true if the content of the pane has been edited but
     * not saved.  This default implementation should be overriden in any
     * subclass if the window will contain data that needs to be saved.
     */
    public boolean isModified() {
        return false;
    }

    /**
     * Subclass can override this method to allow a user to mark the
     * frame as modified.
     */
    public void setModified(boolean c) {
    }


}//CustomInternalFrame
