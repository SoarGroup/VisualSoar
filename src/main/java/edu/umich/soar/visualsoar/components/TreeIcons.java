package edu.umich.soar.visualsoar.components;

import javax.swing.*;
import javax.swing.plaf.metal.MetalIconFactory;
import java.awt.*;

public class TreeIcons {

  public enum IconType {
    IMPASSE,
    FILE,
    OPERATOR,
    LINK;

    public String getLabel() {
      return name().substring(0, 1);
    }
  }

  private static void paintIconLabel(
      Component c, Graphics g, int x, int y, Icon icon, IconType type) {
    // save for restoring later so we can isolate our font change here
    Font originalFont = g.getFont();
    Font fixedFont = new Font("SansSerif", Font.BOLD, 12);
    g.setFont(fixedFont);
    FontMetrics fm = g.getFontMetrics();

    int offsetX = (icon.getIconWidth() - fm.stringWidth(type.getLabel())) / 2;
    int offsetY;
    if (fm.toString().equals("F")) {
      offsetY = (icon.getIconHeight() - fm.getHeight()) / 2 - 3;
    } else {
      offsetY = (icon.getIconHeight() - fm.getHeight()) / 2 - 2;
    }
    g.drawString(type.getLabel(), x + offsetX, y + offsetY + fm.getHeight());
    g.setFont(originalFont);
  }

  private static class TextFileIcon extends MetalIconFactory.TreeLeafIcon {

    private final IconType type;

    private TextFileIcon(IconType type) {
      this.type = type;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
      super.paintIcon(c, g, x, y);
      paintIconLabel(c, g, x, y, this, type);
    }
  }

  private static class TextFolderIcon extends MetalIconFactory.TreeFolderIcon {

    private final IconType type;

    private TextFolderIcon(IconType type) {
      this.type = type;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
      super.paintIcon(c, g, x, y);
      paintIconLabel(c, g, x, y, this, type);
    }
  }

  public static Icon getFileIcon(IconType type) {
    return new TextFileIcon(type);
  }

  public static Icon getFolderIcon(IconType type) {
    return new TextFolderIcon(type);
  }
}
