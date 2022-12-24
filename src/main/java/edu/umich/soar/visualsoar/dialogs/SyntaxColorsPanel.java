package edu.umich.soar.visualsoar.dialogs;

import edu.umich.soar.visualsoar.misc.Prefs;
import edu.umich.soar.visualsoar.misc.SyntaxColor;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeMap;

/**
 * Panel containing editing facilities for selecting custom colors
 * for use in syntax highlighting
 * @author Jon Bauman
 */
class SyntaxColorsPanel extends JPanel {

	JCheckBox		enable = new JCheckBox("Enable Syntax Highlighting");

	/**
	 * The array of buttons representing syntax colors
	 */
	JButton[] 		swatches;
	
	/**
	 * Labels to inform the user that rule editors must be reopened
	 * to reflect preference changes
	 */
	JLabel				mustReopen = new JLabel(
						"Rule editors must be reopened to realize changes");

	/**
	 * Map of the colors to be changed
	 */
	TreeMap<Integer, Color> colorsToChange = new TreeMap<>();

	/**
	 * The syntax colors
	 */
	SyntaxColor[]	colorTable = Prefs.getSyntaxColors();

	/** ctor */
	public SyntaxColorsPanel(SyntaxColor[] oldColors) {
		GridLayout 	layout = new GridLayout();
		JPanel		enablePanel = new JPanel();
		JPanel		buttonPanel = new JPanel();
		JPanel 		noticePanel = new JPanel();
		int			numButtons = 0;
		
		swatches = new JButton[oldColors.length];
		
		enablePanel.setLayout(new GridLayout());
		enable.setSelected(Prefs.highlightingEnabled.getBoolean());
		enablePanel.add(enable);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		for (int i = 0; i < oldColors.length; i++) {
			SyntaxColor		theColor = oldColors[i];
			String			name = (theColor != null) ? oldColors[i].getName() : null;
		
			if (name == null) {
				continue;
			}

			numButtons++;

			swatches[i] = new JButton(name);
			swatches[i].setForeground(oldColors[i]);
			buttonPanel.add(swatches[i]);

			swatches[i].addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					Color c = JColorChooser.showDialog(SyntaxColorsPanel.this,
										"Select Color", null);
					if (c != null) {
						colorToChange(e.getActionCommand(), c);
						((JButton)e.getSource()).setForeground(c);
					}
				}
			});

		}//for
		
		layout.setRows(numButtons / 2);
		buttonPanel.setLayout(layout);

		noticePanel.add(mustReopen);		
		
		add(enablePanel);
		add(buttonPanel);
		add(noticePanel);	
           
		setBorder(new CompoundBorder(
			BorderFactory.createTitledBorder("Syntax Colors"),
			BorderFactory.createEmptyBorder(10,10,10,10)));
	} // constructor
	
	void colorToChange(String name, Color c) {
		
		for (int i = 0; i < colorTable.length; i++) {
			if ((colorTable[i] != null) && (colorTable[i].equals(name))) {
				colorsToChange.put(i, c);
				break;				
			}
		}
	}
	
	public boolean getEnableHighlighting() {
		return enable.isSelected();
	}
	
	public TreeMap getChanges() {
		return colorsToChange;
	}
		
}
