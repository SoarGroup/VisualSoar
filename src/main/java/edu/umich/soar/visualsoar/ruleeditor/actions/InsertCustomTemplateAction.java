package edu.umich.soar.visualsoar.ruleeditor.actions;

import edu.umich.soar.visualsoar.ruleeditor.EditingUtils;
import edu.umich.soar.visualsoar.ruleeditor.EditorPane;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.function.Function;

/**
 * This class puts the contents of a given file into the text area.
 */
public class InsertCustomTemplateAction extends AbstractAction {
	private static final long serialVersionUID = 20230407L;

	private final File file;
	private final EditorPane editorPane;
	private final Function<String, String> lookupVariable;

	public InsertCustomTemplateAction(String initFN, EditorPane editorPane, Function<String, String> lookupVariable) {
		super(initFN);
		file = new File(initFN);
		this.editorPane = editorPane;
		this.lookupVariable = lookupVariable;
	}

	public void actionPerformed(ActionEvent e) {
		//Open the file
		Scanner scanner = null;
		String content = "#Error Loading Custom Template. ";
		boolean error = false;
		try {
			scanner = new Scanner(file, StandardCharsets.UTF_8);
		} catch (IOException ex) {
			error = true;
		}

		//Retrieve the data from the file
		if (!error) {
			content = scanner.useDelimiter("\\A").next();
			scanner.close();
			content = content + " "; //add a space to guarantee '$' isn't the last char
		}

		//Do macro replacements
		int startIndex = content.indexOf("$");
		while (startIndex >= 0) {
			int endIndex = content.indexOf("$", startIndex + 1);
			if (endIndex == -1) break;
			String macro = content.substring(startIndex + 1, endIndex);
			String replacement = lookupVariable.apply(macro);
			String before = content.substring(0, startIndex);
			String after = content.substring(endIndex + 1);
			content = before + replacement + after;
			startIndex = content.indexOf("$", startIndex);
		}

		//Remove the extra space that was added to the content (see above)
		content = content.substring(0, content.length() - 1);

		//Insert the contents into the RuleEditor
		int pos = editorPane.getCaretPosition();
		EditingUtils.insert(editorPane.getDocument(), content, pos);
		editorPane.setCaretPosition(pos + content.length());
	}
}
