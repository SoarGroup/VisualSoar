package edu.umich.soar.visualsoar.dialogs;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.misc.Prefs;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

public class EditCustomTemplatesDialog extends JFrame implements ActionListener {
    private static final long serialVersionUID = 20221225L;

    //parent window
    MainFrame owner;

    //Widgets that are on this dialog
    JButton addButton = new JButton("Add");
    JButton removeButton = new JButton("Remove");
    JButton editButton = new JButton("Edit");
    JButton closeButton = new JButton("Close");
    DefaultListModel<String> listModel = new DefaultListModel<>();
    JList<String> jlistTemplates = new JList<>(listModel);

    //default template string
    String defaultText = "# Any text you write in a template file is automatically inserted when\n# the template is selected from the Insert Template menu.  This\n# comment has been inserted to help you.  You should probably\n# remove it from this file after your template is written.\n#\n# Certain macros can be placed in your text and they will be\n# automatically replaced with the relevant text.\n#\n# The macros are:\n# - $super-operator$ = the super operator\n# - $operator$ = the current operator\n# - $production$ = the name of the production nearest to the cursor\n# - $date$ = today's date\n# - $time$ = the time right now\n# - $project$ or $agent$ = the name of the project\n# - $user$ = the current user name\n# - $caret$ or $cursor$ = indicates where where the cursor should be\n#   after the template is inserted (to be implemented...)\n#\n\nsp {$super-operator$*custom-template\n   (state <s> ^foo bar) \n-->\n   (<s> ^baz qux)\n}\n";

    public EditCustomTemplatesDialog(final MainFrame initOwner) {
        this.owner = initOwner;
        Dimension margin = new Dimension(10, 10);

        //Fill the Frame with a panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        this.add(contentPanel);

        //Put a vertical box in the panel that contains two horiz boxes
        Box mainBox = Box.createHorizontalBox();
        Box closeButtonBox = Box.createHorizontalBox();
        contentPanel.add(Box.createRigidArea(margin));  //top margin
        contentPanel.add(mainBox);
        contentPanel.add(closeButtonBox);
        contentPanel.add(Box.createRigidArea(margin));  //bottom margin

        //The main box contains the template list on left-hand side
        ScrollPane sp = new ScrollPane();
        sp.setSize(new Dimension(200, 100));
        sp.add(jlistTemplates);
        mainBox.add(Box.createRigidArea(margin));  //left margin
        mainBox.add(sp);
        mainBox.add(Box.createRigidArea(margin));  //center divider

        //The main box contains the three action buttons on right-hand side
        //These are arranged in GridLayout so they are all the same size
        JPanel buttonPanel = new JPanel(new GridLayout(7, 1));
        buttonPanel.add(addButton);
        buttonPanel.add(Box.createRigidArea(margin));
        buttonPanel.add(removeButton);
        buttonPanel.add(Box.createRigidArea(margin));
        buttonPanel.add(editButton);
        mainBox.add(buttonPanel);
        mainBox.add(Box.createRigidArea(margin));  //right margin

        //the bottom of the dialog is just a close button flush right
        closeButtonBox.add(Box.createHorizontalGlue());
        closeButtonBox.add(closeButton);
        closeButtonBox.add(Box.createRigidArea(margin)); //right margin

        //Load the data into the list
        fillTemplateList();

        //Set position and size
        this.pack();
        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent we) {
                setLocationRelativeTo(initOwner);
            }
        });

        //listen for events
        addButton.addActionListener(this);
        removeButton.addActionListener(this);
        editButton.addActionListener(this);
        closeButton.addActionListener(this);

    }//ctor

    /**
     * retrieves the current template list and displays it for the user
     */
    public void fillTemplateList() {
        Vector<String> templates = Prefs.getCustomTemplates();
        listModel.clear();
        for(String tp : templates) {
            listModel.addElement(tp);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == closeButton) {
            this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
        else if (e.getSource() == addButton) {
            addTemplate();
        }
        else if (e.getSource() == editButton) {
            String filename = jlistTemplates.getSelectedValue();
            editTemplate(filename);
        }
        else if (e.getSource() == removeButton) {
            removeTemplate();
        }
    }

    /** responds to the Add button */
    private void addTemplate() {
        //user selects a .vsoart file for the new template
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Visual Soar Template Files (*.vsoart)", "vsoart", "text");
        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) return;

        //Make sure this template doesn't already exist
        File file = fileChooser.getSelectedFile();
        if (Prefs.getCustomTemplates().contains(file.getPath())) {
            JOptionPane.showMessageDialog(this,
                    "Error:  " + file.getPath() + " is already in this list.");
            return;
        }

        //if the file is a new file, create it
        if (!file.exists()) {
            //create it
            try {
                file.createNewFile();

                //Add some default content to help the user
                PrintWriter pw = new PrintWriter(file);
                pw.print(defaultText);
                pw.close();
            }
            catch(IOException ioe) {
                JOptionPane.showMessageDialog(this,
                        "Error:  " + file.getPath() + " could not be created.");
                return;
            }
        }//file didn't exist

        //Add the new template to the list and prefs
        Prefs.addCustomTemplate(file.getPath());
        this.listModel.addElement(file.getPath());

        //TODO:  add this template to the menu for all open RuleEditors

        //open an editor for the new template and close this dialog
        editTemplate(file.getPath());
    }//addTemplate

    /** responds to Edit button */
    private void editTemplate(String filename) {
        //create a File object for the selected file
        if (filename == null) return; //ignore silly user
        File file = new File(filename);

        //Sanity check:  file exists?
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this,
                    "Error:  " + file.getPath() + " doesn't exist!");
            return;
        }

        //Sanity check:  file can be opened?
        if (!file.canRead()) {
            JOptionPane.showMessageDialog(this,
                    "Error:  " + file.getPath() + " can't be read.");
            return;
        }

        //ok, open it and close this dialog
        this.owner.OpenFile(file);
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//editTemplate

    /** responds to Remove button */
    private void removeTemplate() {
        String filename = jlistTemplates.getSelectedValue();
        if (filename == null) return; //ignore silly user
        Prefs.removeCustomTemplate(filename);
        this.listModel.removeElement(filename);

        //TODO: update all RuleEditor menus
    }//removeTemplate

}//class EditCustomTemplatesDialog
