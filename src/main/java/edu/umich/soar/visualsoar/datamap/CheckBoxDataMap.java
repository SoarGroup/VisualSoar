package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.operatorwindow.OperatorRootNode;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * class CheckBoxDataMap
 *
 * is specifically for managing datamaps that are displayed with checkboxes
 * for selecting items to import from a foreign datamap.  This is as opposed
 * a regular datamap window used for viewing and editing a datamap.
 *
 * @see CheckBoxDataMapTree
 * @see CheckBoxDataMapTreeRenderer
 *
 * @author Andrew Nuxoll
 * @version Created: Jan 2024
 */
public class CheckBoxDataMap extends DataMap {

    // This is the _relative_ path of the .dm file from this project's root folder
    // A relative path is used so that VS can be more flexible searching for the file
    // on different computers.
    private final String foreignDMFilename;

    /**
     * CheckBoxDataMap ctor
     *
     * @param swmm  the SWMM for the foreign datamap
     * @param foreignDMPath the path where the foreign datamap was loaded from
     */
    public CheckBoxDataMap(SoarWorkingMemoryModel swmm, Path foreignDMPath) {
        super(swmm.getTopstate(), "External Datamap: " + foreignDMPath);
        this.foreignDMFilename = calcRelativePath(foreignDMPath);

        TreeModel soarTreeModel = new SoarWMTreeModelWrapper(swmm, swmm.getTopstate(), title);
        this.dataMapTree = new CheckBoxDataMapTree(this, soarTreeModel, swmm);

        //layout the window contents:  a tree with buttons underneath
        Box contents = Box.createVerticalBox();
        getContentPane().add(contents);
        JScrollPane scrollPane = new JScrollPane(dataMapTree);
        contents.add(scrollPane);
        setupImportButtons(contents);
    }//ctor

    /** calculate the relative path from this project's root to a given filename */
    private String calcRelativePath(Path foreignDMPath) {
        OperatorWindow operatorWindow = MainFrame.getMainFrame().getOperatorWindow();
        OperatorRootNode root = (OperatorRootNode)(operatorWindow.getModel().getRoot());
        if (root == null) {
            System.err.println("Error: Couldn't find the top level project node"); //should never happen
            return null;
        }
        String projectFilename = root.getProjectFile() ;	// Includes .vsa

        //Calculate the root folder of this project
        File localVSA = new File(projectFilename);
        int fnLen = localVSA.getName().length();
        String basePathName = localVSA.getAbsolutePath();
        basePathName = basePathName.substring(0, basePathName.length() - fnLen);

        //Now calculate the relative path to the foreign DM
        Path basePath = Paths.get(basePathName);
        Path relPath = basePath.relativize(foreignDMPath);

        return relPath.toString();
    }//calcRelativePath

    /** accessor */
    public String getForeignDMFilename() { return this.foreignDMFilename; }

    /**
     * setupImportButtons
     *
     * When using a datamap window to import from a foreign datamap, there are
     * buttons at the bottom to confirm or cancel the operation.  These are set up
     * in this method which is called from the ctor.
     *
     * @param contents  the buttons are added to this component
     */
    private void setupImportButtons(Box contents) {
        //Add the buttons
        Box buttonBox = Box.createHorizontalBox();
        JButton selectAllButton = new JButton("Select All");
        JButton selectNoneButton = new JButton("Select None");
        JButton cancelButton = new JButton("Cancel");
        JButton importButton = new JButton("Import Selected Items");
        buttonBox.add(selectAllButton);
        buttonBox.add(selectNoneButton);
        buttonBox.add(Box.createRigidArea(new Dimension(20,20)));
        buttonBox.add(cancelButton);
        buttonBox.add(importButton);
        contents.add(buttonBox);

        //listener for Select All button
        selectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (! (dataMapTree instanceof CheckBoxDataMapTree)) {
                    return; // should never happen!
                }
                CheckBoxDataMapTree cbdmt = (CheckBoxDataMapTree)dataMapTree;
                cbdmt.selectAll();
                repaint();
            }
        });

        //listener for Select None button
        selectNoneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (! (dataMapTree instanceof CheckBoxDataMapTree)) {
                    return; // should never happen!
                }
                CheckBoxDataMapTree cbdmt = (CheckBoxDataMapTree)dataMapTree;
                cbdmt.selectNone();
                repaint();
            }
        });

        //listener for Cancel button
        cancelButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeMyselfAndReTile();
            }
        });

        //Listener for Import button
        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (! (dataMapTree instanceof CheckBoxDataMapTree)) {
                    return; // should never happen!
                }
                CheckBoxDataMapTree cbdmt = (CheckBoxDataMapTree)dataMapTree;
                cbdmt.importFromForeignDataMap();
                closeMyselfAndReTile();
            }
        });
    }//setupImportButtons


}//class CheckBoxDataMap
