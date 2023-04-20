package edu.umich.soar.visualsoar.dialogs;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.misc.Prefs;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Vector;

/**
 * class EditCustomTemplatesDialog
 *
 * Allows the user to add/remove custom code templates.  This dialog is invoked
 * by the Insert Template->Edit Custom Templates... option in the Rule Editor menu.
 *
 * @see edu.umich.soar.visualsoar.misc.Template
 *
 */
public class EditCustomTemplatesDialog extends JFrame implements ActionListener {
    private static final long serialVersionUID = 20221225L;

    //parent window
    MainFrame owner;

    //Widgets that are on this dialog
    JButton addButton = new JButton("Add");
    JButton removeButton = new JButton("Remove");
    JButton editButton = new JButton("Edit");
    JButton dirButton = new JButton("Set Template Folder");
    JButton closeButton = new JButton("Close");
    DefaultListModel<String> listModel = new DefaultListModel<>();
    JList<String> jlistTemplates = new JList<>(listModel);

    public EditCustomTemplatesDialog(final MainFrame initOwner) {
        this.owner = initOwner;
        Dimension margin = new Dimension(10, 10);
        Dimension buttonSpacing = new Dimension(0, 5);

        //Fill the Frame with a panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        this.add(contentPanel);

        //Put a vertical box in the panel that contains two horiz boxes
        Box mainBox = Box.createHorizontalBox();
        Box closeButtonBox = Box.createHorizontalBox();
        contentPanel.add(Box.createRigidArea(margin));  //top margin
        contentPanel.add(mainBox);
        contentPanel.add(Box.createRigidArea(margin));
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
        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 0, 10));
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(editButton);
        mainBox.add(buttonPanel);
        mainBox.add(Box.createRigidArea(margin));  //right margin

        //the bottom of the dialog is just a close button flush right
        closeButtonBox.add(Box.createRigidArea(margin)); //left margin
        closeButtonBox.add(dirButton);
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
        dirButton.addActionListener(this);
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
        else if (e.getSource() == dirButton) {
            setTemplateFolder();
        }
    }
    /**
     * resets the Insert Template menu for all open RuleEditor windows
     * This is a helper method for the button event handler methods (below)
     */
    private void resetMenus() {
        JInternalFrame[] jif = owner.getDesktopPane().getAllFrames();
        for (JInternalFrame jInternalFrame : jif) {
            if (jInternalFrame instanceof RuleEditor) {
                RuleEditor re = (RuleEditor) jInternalFrame;
                re.reinitTemplatesMenu();
            }
        }
    }//resetMenus


    /** responds to the Add button */
    private void addTemplate() {
        String name = JOptionPane.showInputDialog("Template Name:");
        if ((name == null) || (name.length() == 0)) return; //empty input is treated as "Cancel"

        //Make sure this template doesn't already exist
        if (Prefs.getCustomTemplates().contains(name)) {
            JOptionPane.showMessageDialog(this,
                    "Error:  '" + name + "' already exists.");
            return;
        }

        //Create a File to store the template
        File templateFile = Prefs.addCustomTemplate(name);
        if (templateFile == null) {
            JOptionPane.showMessageDialog(this,
                    "Error:  '" + name + "' could not be created.");
            return;
        }

        //Add the new template to the list in the dialog
        this.listModel.addElement(name);

        //update all RuleEditor menus
        resetMenus();

        //open an editor for the new template and close this dialog
        editTemplate(name);
    }//addTemplate

    /** responds to Edit button */
    private void editTemplate(String name) {
        //create a File object for the selected file
        if (name == null) return; //ignore silly user
        File file = Prefs.getCustomTemplateFile(name);

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

        //update all RuleEditor menus
        resetMenus();
    }//removeTemplate

    /** responds to 'Set Template Folder' button.  The user changes
     * what folder contains the current custom templates.
     */
    private void setTemplateFolder() {
        //Ask the user to select a new folder
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.setCurrentDirectory(Prefs.getCustomTemplatesFolder());
        int result = dirChooser.showDialog(this, "Select");

        //Verify the user's selection
        if (result == JFileChooser.APPROVE_OPTION) {
            File newFolder = dirChooser.getSelectedFile();
            if (! newFolder.canWrite()) {
                JOptionPane.showMessageDialog(this,"Unable to write to selected folder.");
                return;
            }

            //set the new value
            String newFolderStr = newFolder.getAbsolutePath();
            Prefs.customTemplateFolder.set(newFolderStr);

            //Update templates list to reflect the new folder's contents
            Prefs.loadCustomTemplates();
            fillTemplateList();
            resetMenus();

        }//if
    }//setTemplateFolder

}//class EditCustomTemplatesDialog
