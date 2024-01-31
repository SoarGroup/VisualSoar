package edu.umich.soar.visualsoar.datamap;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * class CheckBoxDataMap
 *
 * is specifically for managing datamaps that are displayed with checkboxes
 * for selecting items to import from a foreign datamap.  This is as opposed
 * a regular datamap window used for viewing and editing a datamap.
 *
 * @see CheckBoxDataMapTree
 * @see CheckBoxDataMapTreeRenderer
 */
public class CheckBoxDataMap extends DataMap {

    public CheckBoxDataMap(SoarWorkingMemoryModel swmm, String title) {
        super(swmm.getTopstate(), title);

        TreeModel soarTreeModel = new SoarWMTreeModelWrapper(swmm, swmm.getTopstate(), title);
        this.dataMapTree = new CheckBoxDataMapTree(this, soarTreeModel, swmm);

        //layout the window contents:  a tree with buttons underneath
        Box contents = Box.createVerticalBox();
        getContentPane().add(contents);
        JScrollPane scrollPane = new JScrollPane(dataMapTree);
        contents.add(scrollPane);
        setupImportButtons(contents);
    }//ctor

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
                //TODO:  TBD do crazy stuff here.  :o
            }
        });
    }//setupImportButtons


}//class CheckBoxDataMap
