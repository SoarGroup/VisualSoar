package edu.umich.soar.visualsoar.dialogs;

import edu.umich.soar.visualsoar.misc.Prefs;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;

/**
 * Panel containing editing facilities for selecting custom colors
 * for use in syntax highlighting
 *
 * @author Jon Bauman
 */
class AutoTilePanel extends JPanel {

    JCheckBox enable = new JCheckBox("Enable Auto-tiling");
    JLabel tileStyle = new JLabel("Default Tile Style:");
    JRadioButton horizontal = new JRadioButton("Horizontal");
    JRadioButton vertical = new JRadioButton("Vertical");
    ButtonGroup horizVert = new ButtonGroup();

    /**
     * Creates a titled border around the input field
     */
    public AutoTilePanel() {
        JPanel horizVertPanel = new JPanel();

        setLayout(new GridLayout(2, 1, 0, 5));

        horizVert.add(horizontal);
        horizVert.add(vertical);

        horizVertPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        horizVertPanel.add(tileStyle);
        horizVertPanel.add(Box.createHorizontalStrut(5));
        horizVertPanel.add(horizontal);
        horizVertPanel.add(vertical);

        enable.setSelected(Prefs.autoTileEnabled.getBoolean());
        if (Prefs.horizTile.getBoolean()) {
            horizontal.setSelected(true);
        } else {
            vertical.setSelected(true);
        }

        add(enable);
        add(horizVertPanel);

        setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder("Tiling"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
    } // constructor

    public boolean getAutoTile() {
        return enable.isSelected();
    }

    public boolean getHorizontalTile() {
        return horizontal.isSelected();
    }
}
