package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.parser.*;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.beans.PropertyVetoException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the  file node for the operator window
 *
 * @author Brad Jones
 * @version 0.5a 5 Aug 1999
 */
public class FileNode extends OperatorNode implements java.io.Serializable {
    private static final long serialVersionUID = 20221225L;

///////////////////////////////////////////////////////////////////
// Data Members
///////////////////////////////////////////////////////////////////
    /**
     * a string that is the path to the file which is associated with this file
     */
    protected String fileAssociation;

    /**
     * a reference to the rule editor, null if there isn't one
     */
    protected RuleEditor ruleEditor = null;

    ///////////////////////////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////////////////////////
    public FileNode(String inName, int inId, String inFile) {
        super(inName, inId);
        fileAssociation = inFile;
    }

///////////////////////////////////////////////////////////////////
// Methods
///////////////////////////////////////////////////////////////////

    /**
     * Use this getter function to get the path to the rule file
     *
     * @return the path to the datamap
     */
    public String getFileName() {
        OperatorNode parent = (OperatorNode) getParent();
        return parent.getFullPathName() + File.separator + fileAssociation;
    }

    /**
     * This is the function that gets called when you want to add a file to this
     * node
     *
     * @param operatorWindow pane associated with 'this'
     * @param newFileName    the name of the new operator to add
     */
    public void addFile(OperatorWindow operatorWindow, String newFileName) throws IOException {
        File file = new File(getFullPathName() + File.separator + newFileName + ".soar");

        // Check to make sure file does not exist
        if (checkCreateReplace(file)) {
            return;
        }

        //FileNode fn = operatorWindow.createFileNode(newFileName,file.getName());
        FileOperatorNode fon = operatorWindow.createFileOperatorNode(newFileName, file.getName());
        operatorWindow.addChild(this, fon);
        sourceChildren();
    }

    /**
     * The user wants to rename this node
     *
     * @param operatorWindow the pane associated with 'this'
     * @param newName        the new name that the user wants this node to be called
     */
    public void rename(OperatorWindow operatorWindow,
                       String newName) throws IOException {
        DefaultTreeModel model = (DefaultTreeModel) operatorWindow.getModel();
        File oldFile = new File(getFileName());
        File newFile = new File(oldFile.getParent() + File.separator + newName + ".soar");

        if (creationConflict(newFile)) {
            throw new IOException("Bad file name");
        }

        if (!oldFile.renameTo(newFile)) {
            throw new IOException("Unable to rename operator file.");
        } else {
            name = newName;
            fileAssociation = newFile.getName();
            if (ruleEditor != null) {
                ruleEditor.fileRenamed(newFile.getPath());
            }
        }
        model.nodeChanged(this);
    }

    /**
     * Given a Writer this writes out a description of the operator node
     * that can be read back in later
     *
     * @param w the writer
     * @throws IOException if there is an error writing to the writer
     */
    @Override
    public void write(Writer w) throws IOException {
        w.write("FILE " + name + " " + fileAssociation + " " + id);
    }

    /**
     * Given a Writer this writes out a description of the operator node
     * that can be read back later
     *
     * @param w where the description should be written to
     * @throws IOException if there is an error writing to the writer
     */
    public void exportDesc(Writer w) throws IOException {
        w.write("FILE " + name);
    }

    public void exportType(Writer w) throws IOException {
        w.write("IMPORT_TYPE " + VSEImporter.FILE + "\n");
    }

    /**
     * Given a Writer this writes out the rules as it is either in
     * the file or the rule editor
     *
     * @param w where the file should be written to
     * @throws IOException if there is an error writing to the writer
     */
    public void exportFile(Writer w, int id) throws IOException {
        w.write("RULE_FILE " + id + " ");
        if (ruleEditor == null) {
            StringWriter sw = new StringWriter();
            LineNumberReader lnr =
                    new LineNumberReader(new FileReader(getFileName()));
            int lines = 0;
            String s = lnr.readLine();
            while (s != null) {
                ++lines;
                sw.write(s + "\n");
                s = lnr.readLine();
            }
            w.write(lines + "\n");
            w.write(sw + "\n");
        } else {
            w.write(ruleEditor.getNumberOfLines() + "\n");
            w.write(ruleEditor.getAllText() + "\n");
        }
    }

    @Override
    protected void enableContextMenuItems() {
        super.enableContextMenuItems();
        addSubOperatorItem.setEnabled(false);
        addFileItem.setEnabled(false);
        addTopFolderItem.setEnabled(false);
        openDataMapItem.setEnabled(false);
        impasseSubMenu.setEnabled(false);
        checkChildrenAgainstDataMapItem.setEnabled(false);

        if (name.equals("elaborations")) {
            deleteItem.setEnabled(getParent().getChildCount() == 1);
            renameItem.setEnabled(false);
        }

        addTopFolderItem.setVisible(false);

    }//enableContextMenuItems

    /**
     * Removes the selected file from the tree if it is allowed
     */
    public void delete(OperatorWindow operatorWindow) {
        OperatorNode parent = (OperatorNode) getParent();

        if (name.equals("elaborations")) {
            JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
                    "The elaborations file may not be deleted",
                    "Delete Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        renameToRemove(new File(getFileName()));

        operatorWindow.removeNode(this);
        parent.notifyDeletionOfChild(operatorWindow, this);
    }

    /**
     * This will parse the productions for a given file node
     * if a rule editor is open for the file it just forwards the call to the
     * open rule editor, else it opens the file and attempts to parse the
     * productions
     */

    public Vector<SoarProduction> parseProductions() throws ParseException, java.io.IOException {
        if (name.startsWith("_")) return null;

        if (ruleEditor == null) {
            //This version is for files that are closed (:AMN: Sep 2022)
            SuppParseChecks.fixUnmatchedBraces(getFileName());

            java.io.Reader r = new java.io.FileReader(getFileName());
            SoarParser aParser = new SoarParser(r);

            Vector<SoarProduction> prods = aParser.VisualSoarFile();
            r.close();

            return prods;
        }

        //This version is for files that are open (:AMN: Sep 2022)
        ruleEditor.fixUnmatchedBraces();

        return ruleEditor.parseProductions();
    }

    /**
     * This will check the productions in this file node for datamap errors.
     * if a rule editor is open for the file it just forwards the call to the
     * open rule editor, else it opens the file and attempts to parse the
     * productions
     *
     * @param vecErrors any errors found are <em>added</em> to this vector
     */
    public boolean CheckAgainstDatamap(Vector<FeedbackListEntry> vecErrors) throws IOException {
        Vector<SoarProduction> parsedProds = new Vector<>();

        //First:  is the code syntactically correct?
        try {
            parsedProds = parseProductions();
        } catch (ParseException pe) {
            vecErrors.add(new FeedbackListEntry("Unable to check productions due to parse error"));
            vecErrors.add(this.parseParseException(pe));
            return true;
        } catch (TokenMgrError tme) {
            tme.printStackTrace();
        }

        //Now check for datamap issues
        if ((parsedProds != null) && (!parsedProds.isEmpty())) {
            //Use a temp vector so that vecErrors doesn't get cleared
            //TODO:  is temp vector really needed?
            Vector<FeedbackListEntry> tmpErrors = new Vector<>();
            OperatorWindow ow = MainFrame.getMainFrame().getOperatorWindow();
            ow.checkProductions((OperatorNode) getParent(), this, parsedProds, tmpErrors);
            if (!tmpErrors.isEmpty()) {
                vecErrors.addAll(tmpErrors);
            }
        }

        return (!vecErrors.isEmpty());


    }//CheckAgainstDatamap

    /**
     * retrieves the text of this FileNode.  If the file is open, the text
     * is retrieved from the associated RuleEditor.  Otherwise, it is
     * retrieved from the file.
     */
    public String getText() {
        String text;
        if (ruleEditor != null) {
            text = ruleEditor.getAllText();
        } else {
            try {
                Path path = Paths.get(getFileName());
                byte[] bytes = Files.readAllBytes(path);
                text = new String(bytes);
            } catch (IOException e) {
                //quiet fail.  The null return value
                return null;
            }
        }//else

        return text;
    }//getText

    /**
     * retrieves a list of the names of all productions in this file.
     * Each production name in the list is appended with the line number
     * in parens after it.
     *
     * @author Andrew Nuxoll (29 Sep 2022)
     */
    public Vector<String> getProdNames() {
        //These files won't have productions
        Vector<String> result = new Vector<>();
        if (getFileName().startsWith("_")) return result;

        //Get the text of the file
        String text = getText();

        //find the start position of each production
        Pattern prodPattern = Pattern.compile("[\n\r][\t ]*[sg]p[ \t\n\r]*\\{");
        Matcher prodMatch = prodPattern.matcher(text);
        while (prodMatch.find()) {
            int start = prodMatch.end();
            //search for the first letter of the production name
            char ch = text.charAt(start);
            while (Character.isWhitespace(ch)) {
                start++;
                ch = text.charAt(start);
            }

            //search for first char after the production name
            int end = start + 1;
            ch = text.charAt(end);
            while ((!Character.isWhitespace(ch)) && (ch != '#') && (ch != ';')) {
                end++;
                ch = text.charAt(end);
            }

            //Note:  the code above will miss the production name if you do
            // something unconventional like this.
            //        sp {   #some comment
            //               production*name*here
            // I didn't think this was worth checking for.  -:AMN: 29 Sep 2022

            String prodName = text.substring(start, end).trim();
            if (!prodName.isEmpty()) {
                result.add(prodName);
            }
        }//while

        return result;
    }//getProdNames

    /**
     * getLineNumForString
     * <p>
     * determines the line number of the first line of the file that contains
     * a given string
     *
     * @return -1 if not found
     */
    public int getLineNumForString(String target) {
        //Find the string
        String fileContent = getText();
        int index = fileContent.indexOf(target);

        //Count the lines up to that point
        int pos = fileContent.indexOf('\n');
        int lines = 1;
        while (pos < index) {
            pos++;
            if (pos >= fileContent.length()) break;  //shouldn't happen...
            pos = fileContent.indexOf('\n', pos);
            if (pos < 0) break; //also shouldn't happen
            lines++;
        }

        return lines;

    }//getLineNumForString

    /**
     * This opens/shows a rule editor with this node's associated file
     *
     * @param pw the MainFrame
     */
    @Override
    public RuleEditor openRules(MainFrame pw) {
        //If the rules are already open just bring it to the front
        if ( (ruleEditor != null) && (! ruleEditor.isClosed()) ) {
            pw.showRuleEditor(ruleEditor);
            return ruleEditor;
        }

        //Read the file contents
        try {
            ruleEditor = new RuleEditor(new java.io.File(getFileName()),
                    this);
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(pw,
                    "There was an error reading file: " + fileAssociation,
                    "I/O Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        //Create the window and show it to the user
        ruleEditor.setReadOnly(MainFrame.getMainFrame().isReadOnly());
        ruleEditor.setVisible(true);
        pw.addRuleEditor(ruleEditor);
        try {
            ruleEditor.setSelected(true);
        } catch (java.beans.PropertyVetoException pve) {
            //No sweat. This just means the new window failed to get focus.
        }

        return ruleEditor;

    }//openRules

    /**
     * This opens/shows a rule editor with this node's associated file
     * and places the caret on the given line number
     *
     * @param pw   the Project window
     * @param line the line number to place the caret on
     */
    public void openRules(MainFrame pw, int line) {
        openRules(pw);
        ruleEditor.setLine(line);
    }

    /**
     * This opens/shows a rule editor with this node's associated file
     * and displays a substring of the file starting on a given line
     *
     * @param pw          the Project window
     * @param line        the line number to place the caret on
     * @param assocString the substring to place the caret on
     */
    public void openRulesToString(MainFrame pw, int line, String assocString) {
        openRules(pw);
        ruleEditor.highlightString(line, assocString);
    }

    protected String getFullPathName() {
        return null;
    }

    public void exportDataMap(Writer w) throws IOException {
        w.write("NODATAMAP\n");
    }

    public void copyStructures(File folderToWriteTo) throws IOException {
        File copyOfFile = new File(folderToWriteTo.getPath() + File.separator + fileAssociation);
        Writer w = new FileWriter(copyOfFile);
        Reader r = new FileReader(getFileName());
        edu.umich.soar.visualsoar.util.ReaderUtils.copy(w, r);
        w.close();
        r.close();
    }

    public void source(Writer w) throws IOException {
        String LINE = System.lineSeparator();
        w.write("source " + fileAssociation + LINE);
    }

    public void sourceChildren() throws IOException {
    }

    public void sourceRecursive() throws IOException {
    }

    /** close the open editor window associated with this node (if there is one) */
    @Override
    public void closeEditors() {
        //Make sure there is a window to close
        if (ruleEditor == null) return;
        if (ruleEditor.isClosed()) return;

        //Save whatever is there to avoid the "are you sure?" popup
        try {
            ruleEditor.write();
        } catch (IOException e) {
            //Nothing I can do here. Just silently ignore
        }

        try {
            ruleEditor.setClosed(true);
        } catch (PropertyVetoException e) {
            /* if this window can't be closed at least make it invisible */
            ruleEditor.setVisible(false);
        }
    }

    public void searchTestDataMap(SoarWorkingMemoryModel swmm,
                                  Vector<FeedbackListEntry> errors) {
    }

    public void searchCreateDataMap(SoarWorkingMemoryModel swmm,
                                    Vector<FeedbackListEntry> errors) {
    }

    public void searchTestNoCreateDataMap(SoarWorkingMemoryModel swmm,
                                          Vector<FeedbackListEntry> errors) {
    }

    public void searchCreateNoTestDataMap(SoarWorkingMemoryModel swmm,
                                          Vector<FeedbackListEntry> errors) {
    }

    public void searchNoTestNoCreateDataMap(SoarWorkingMemoryModel swmm,
                                            Vector<FeedbackListEntry> errors) {
    }

}
