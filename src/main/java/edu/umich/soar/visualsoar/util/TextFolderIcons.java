package edu.umich.soar.visualsoar.util;


import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.metal.*;


/**
 * Author: Nobuo Tamemasa
 * http://www.codeguru.com/java/articles/187.shtml
 * @version 1.0 01/12/99
 */
public class TextFolderIcons extends MetalIconFactory.TreeFolderIcon {

  protected String label;
  private static Hashtable<String, String> labels;

  protected TextFolderIcons() {
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    super.paintIcon( c, g, x, y );
    if (label != null) {
      FontMetrics fm = g.getFontMetrics();
          
      int offsetX = (getIconWidth()  - fm.stringWidth(label)) /2;
      int offsetY;
      if( fm.toString().equals("F")) {
        offsetY = (getIconHeight() - fm.getHeight()) /2 - 3;
      }
      else {
        offsetY = (getIconHeight() - fm.getHeight()) /2 - 2;
      }
      g.drawString(label, x + offsetX ,
                          y + offsetY + fm.getHeight());
    }
  }
  
  public static Icon getIcon(String str) {

    if (labels == null) {
      labels = new Hashtable<>();
      setDefaultSet();
    }
    TextFolderIcons icon = new TextFolderIcons();
    icon.label = labels.get(str);

    return icon;
  }

  private static void setDefaultSet() {
    labels.put("impasse"     ,"I");
    labels.put("file"  ,"F");
    labels.put("operator"  ,"O");
    labels.put("link", "L");
  }

}