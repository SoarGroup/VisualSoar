package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that contains the buttons for the find dialogs
 * @author Jon Bauman
 * @see FindDialog
 * @see FindReplaceDialog
 */
class FindButtonPanel extends JPanel {

	JCheckBox 	keepDialog;
	JButton 	cancelButton = new JButton("Cancel");
	JButton 	findButton = new JButton("Find");

	/**
	 * The 'find in project' version
	 *
	 * @param findInProject is not used but needed to distinguish this ctor
	 *                      from the single file version.  This could be done
	 *                      a cleaner way but *shrug*
	 */
	public FindButtonPanel(boolean findInProject) {
		cancelButton.setMnemonic('c');
		findButton.setMnemonic('f');
	
		setLayout(new FlowLayout());
		add(findButton);
		add(cancelButton);
	}


	/**
	 * The default single-file find version
	 */
	public FindButtonPanel() {
		keepDialog = new JCheckBox("Keep Dialog", false);	
	
		cancelButton.setMnemonic('c');
		findButton.setMnemonic('f');
		keepDialog.setMnemonic('k');
	
		setLayout(new FlowLayout());
		add(keepDialog);
		add(findButton);
		add(cancelButton);
	}
}
