package edu.umich.soar.visualsoar.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that contains the button for the 'About' dialog
 * @author Jon Bauman
 * @see AboutDialog
 */
class AboutButtonPanel extends JPanel {

	JButton greatButton = new JButton("Great");
	 
	public AboutButtonPanel() {
		greatButton.setMnemonic('g');
	
		setLayout(new FlowLayout());
		add(greatButton);
	}
}
