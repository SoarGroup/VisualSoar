package edu.umich.soar.visualsoar.mainframe.actions;

import edu.umich.soar.visualsoar.datamap.CheckBoxDataMap;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryReader;
import edu.umich.soar.visualsoar.files.Vsa;
import edu.umich.soar.visualsoar.mainframe.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * class LinkDataMapAction
 *
 * <p>This action loads a datamap from another project and allows the user to import items from it.
 * These items are "linked" can not be edited via this project but must be edited via the project
 * they are imported from.
 *
 * @author Andrew Nuxoll
 * @version 21 Jan 2024
 */
public class LinkDataMapAction extends AbstractAction {
  private static final long serialVersionUID = 20240121L;

  private final MainFrame mainFrame;

  public LinkDataMapAction(MainFrame mainFrame) {
    super("Link Items from Another Datamap");
    this.mainFrame = mainFrame;
    setEnabled(false);
  }

  public void actionPerformed(ActionEvent ae) {
    // Only one datamap window can be open at a time
    if (mainFrame.getDesktopPane().numDataMaps() > 0) {
      mainFrame
          .getFeedbackManager()
          .setStatusBarError(
              "Cannot import from foreign datamap while local datamap is being edited.");
      return;
    }

    // The user selects a datamap file to import from
    File vsaFile = Vsa.selectVsaFile(mainFrame);
    if (vsaFile == null) return;

    // read the data from the foreign datamap into a local SWMM object
    SoarWorkingMemoryModel swmm = SoarWorkingMemoryReader.readDataIntoSWMMfromVSA(vsaFile);
    if (swmm == null) {
      return;
    }
    // Create a datamap with checkboxes
    CheckBoxDataMap dataMap = new CheckBoxDataMap(swmm, swmm.getDmPath());
    dataMap.setVisible(true);
    mainFrame.addDataMap(dataMap);
  } // actionPerformed
} // class LinkDataMapAction
