package edu.umich.soar.visualsoar.components;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class FontUtils {

  /** Menu fonts are set a bit larger than in other components */
  private static final int MENU_BAR_FONT_SIZE_INCREASE = 4;

  //  TODO: take JComponent instead
  @NotNull
  public static Font getResizedFont(@NotNull Font oldFont, int fontSize) {
    return new Font(oldFont.getName(), oldFont.getStyle(), fontSize);
  }

  public static void setFontSize(@NotNull JComponent component, int fontSize) {
    Font oldFont = component.getFont();
    Font newFont = new Font(oldFont.getName(), oldFont.getStyle(), fontSize);
    component.setFont(newFont);
  }

  public static void setMenuBarFontSize(JMenuBar menuBar, int fontSize) {
    for (int i = 0; i < menuBar.getMenuCount(); i++) {
      JMenu menu = menuBar.getMenu(i);
      setFontSize(menu, fontSize + MENU_BAR_FONT_SIZE_INCREASE);
      setMenuFontSize(menu, fontSize);
    }
  }

  public static void setMenuFontSize(JMenu menu, int fontSize) {
    for (int i = 0; i < menu.getItemCount(); i++) {
      JMenuItem menuItem = menu.getItem(i);
      if (menuItem != null) {
        setFontSize(menuItem, fontSize + MENU_BAR_FONT_SIZE_INCREASE);
        if (menuItem instanceof JMenu) {
          setMenuFontSize((JMenu) menuItem, fontSize);
        }
      }
    }
  }

  public static void setMenuItemFontSize(JMenuItem menuItem, int fontSize) {
    setFontSize(menuItem, fontSize + MENU_BAR_FONT_SIZE_INCREASE);
    if (menuItem instanceof JMenu) {
      setMenuFontSize((JMenu) menuItem, fontSize);
    }
  }
}
