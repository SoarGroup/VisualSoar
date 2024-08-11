package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.ruleeditor.EditingUtils;
import edu.umich.soar.visualsoar.ruleeditor.EditorPane;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

////////////////////////////////////////////////////////
// ACTIONS
////////////////////////////////////////////////////////
public class InsertTextFromFileAction extends AbstractAction {
	private static final long serialVersionUID = 20221225L;
	private final EditorPane editorPane;

	public InsertTextFromFileAction(EditorPane editorPane) {
		super("Insert Text From File");
		this.editorPane = editorPane;
	}

	public void actionPerformed(ActionEvent event) {
		JFileChooser fileChooser = new JFileChooser();
		if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(MainFrame.getMainFrame())) {
			try {
				Reader r = new FileReader(fileChooser.getSelectedFile());
				StringWriter w = new StringWriter();

				int rc = r.read();
				while (rc != -1) {
					w.write(rc);
					rc = r.read();
				}
				EditingUtils.insert(editorPane.getDocument(), w.toString(), editorPane.getCaret().getDot());
			} catch (IOException ioe) {
				JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
					"There was an error inserting the text",
					"Error",
					JOptionPane.ERROR_MESSAGE);


			}
		}
	}
}
