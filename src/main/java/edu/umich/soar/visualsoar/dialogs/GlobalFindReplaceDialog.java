package edu.umich.soar.visualsoar.dialogs;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

/**
 * Dialog which takes input for, and initiates a find or replace
 * operation
 * @author Jon Bauman
 * @see RuleEditor#find
 * @see RuleEditor#replace
 */
public class GlobalFindReplaceDialog extends JDialog {

	/**
	 * panel which contains the find input field and option buttons
	 */
	FindPanel 				findPanel = new FindPanel();
	
	/**
	 * the rule editor this find was excecuted from, null if this is
	 * a project-wide search
	 */
	RuleEditor 				d_ruleEditor;
	
	/**
	 * panel which contians all the replace input field
	 */
	ReplacePanel 			replacePanel = new ReplacePanel();
		
	FindReplaceButtonPanel 	buttonPanel = new FindReplaceButtonPanel();
	
	/**
	 * @param owner Frame which owns the dialog
	 * @param ruleEditor a reference to the RuleEditor
	 */
	public GlobalFindReplaceDialog(final Frame owner,RuleEditor ruleEditor) {
		super(owner, "Find & Replace", false);
		
		d_ruleEditor = ruleEditor;
		setResizable(false);
		Container contentPane = getContentPane();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		contentPane.setLayout(gridbag);
		
		// specifies component as last one on the row
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		contentPane.add(findPanel, c);
		contentPane.add(replacePanel, c);
		contentPane.add(buttonPanel, c);
		pack();		
		getRootPane().setDefaultButton(buttonPanel.findButton);			
		findPanel.findField.setNextFocusableComponent(
											replacePanel.replaceField);
		replacePanel.replaceField.setNextFocusableComponent(
												findPanel.findField);
		
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent we) {
				setLocationRelativeTo(owner);
				findPanel.requestFocus();
			}
		});
		
		buttonPanel.cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		buttonPanel.replaceAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object[] theData = findPanel.getData();
				String toReplace = replacePanel.getText();
				String toFind = (String)theData[0];
				Boolean forward = (Boolean)theData[1];
				Boolean caseSensitive = (Boolean)theData[2];
				Boolean wrap = (Boolean)theData[3];
				
				d_ruleEditor.setFindReplaceData(toFind,
								toReplace,forward,caseSensitive,wrap);
				d_ruleEditor.replaceAll();
				if (! buttonPanel.keepDialog.isSelected()) {
					dispose();
				}				
			}
		});
		
		buttonPanel.replaceButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object[] theData = findPanel.getData();
				String toReplace = replacePanel.getText();
				String toFind = (String)theData[0];
				Boolean forward = (Boolean)theData[1];
				Boolean caseSensitive = (Boolean)theData[2];
				Boolean wrap = (Boolean)theData[3];
				
				d_ruleEditor.setFindReplaceData(toFind,
								toReplace,forward,caseSensitive,wrap);
				d_ruleEditor.replace();
				if (! buttonPanel.keepDialog.isSelected()) {
					dispose();
				}
			}
		});	
		
		buttonPanel.findButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object[] theData = findPanel.getData();
				String toReplace = replacePanel.getText();
				String toFind = (String)theData[0];
				Boolean forward = (Boolean)theData[1];
				Boolean caseSensitive = (Boolean)theData[2];
				Boolean wrap = (Boolean)theData[3];

				d_ruleEditor.setFindReplaceData(toFind,
								toReplace,forward,caseSensitive,wrap);
				d_ruleEditor.find();
				if (! buttonPanel.keepDialog.isSelected()) {
					dispose();
				}
			}
		});		
	}	
	
}
