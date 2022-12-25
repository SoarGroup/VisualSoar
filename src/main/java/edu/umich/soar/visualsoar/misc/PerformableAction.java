package edu.umich.soar.visualsoar.misc;

public abstract class PerformableAction extends javax.swing.AbstractAction {
    public PerformableAction(String desc) {
        super(desc);
    }

    public abstract void perform();

}
