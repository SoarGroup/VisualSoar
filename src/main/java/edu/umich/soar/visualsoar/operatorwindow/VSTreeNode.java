package edu.umich.soar.visualsoar.operatorwindow;

/**
 * This is an abstract class that is the basis for all tree nodes
 * within visual soar
 * @author Brad Jones
 * @version 0.5a 5 Aug 1999
 */
public abstract class VSTreeNode extends javax.swing.tree.DefaultMutableTreeNode  {
///////////////////////////////////////////////////////////////////
// Methods
///////////////////////////////////////////////////////////////////
	/**
	 * Given a Writer you must write out a representation of yourself
	 * that can be read back in later
	 * Note: implementing class shoudl throw an IOException if there is an
	 *       error writing to the writer
	 *
	 * @param w the writer
	 */
	abstract public void write(java.io.Writer w) throws java.io.IOException;
}
