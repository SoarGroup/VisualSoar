package edu.umich.soar.visualsoar.util;


import javax.swing.*;
import javax.swing.plaf.metal.MetalIconFactory;
import java.awt.*;
import java.util.Hashtable;


/**
 * Author: Nobuo Tamemasa
 * http://www.codeguru.com/java/articles/187.shtml
 *
 * @version 1.0 01/12/99
 * <p>
 * TODO:  This class and TextFolderIcons are basically the same.
 * They should be merged.
 */
public class TextIcons extends MetalIconFactory.TreeLeafIcon {

    protected String label;
    private static Hashtable<String, String> labels;

    protected TextIcons() {
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        super.paintIcon(c, g, x, y);
        if (label != null) {
            FontMetrics fm = g.getFontMetrics();

            int offsetX = (getIconWidth() - fm.stringWidth(label)) / 2;
            int offsetY = (getIconHeight() - fm.getHeight()) / 2 - 2;

            g.drawString(label, x + offsetX,
                    y + offsetY + fm.getHeight());
        }
    }

    public static Icon getIcon(String str) {

        if (labels == null) {
            labels = new Hashtable<>();
            setDefaultSet();
        }
        TextIcons icon = new TextIcons();
        icon.label = labels.get(str);
        return icon;
    }

    private static void setDefaultSet() {
        labels.put("impasse", "I");
        labels.put("file", "F");
        labels.put("operator", "O");
        labels.put("link", "L");
    }

}
