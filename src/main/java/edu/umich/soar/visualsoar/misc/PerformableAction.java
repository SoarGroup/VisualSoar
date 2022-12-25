package edu.umich.soar.visualsoar.misc;

public abstract class PerformableAction extends javax.swing.AbstractAction {
    private static final long serialVersionUID = 20221225L;

    public PerformableAction(String desc) {
        super(desc);
    }

    public abstract void perform();

}
