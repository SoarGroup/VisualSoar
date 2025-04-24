package edu.umich.soar.visualsoar.components;

import javax.swing.*;
import java.awt.*;

/**
 * A button displayed with highlighted text
 */
public class HighlightedTextButton extends JButton {
  private Color textBackground;

  public HighlightedTextButton(String text, Color defaultColor) {
    super(text);
    this.textBackground = defaultColor;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g.create();

    // Calculate the text bounds
    FontMetrics fm = g2d.getFontMetrics();
    String text = getText();
    int textWidth = fm.stringWidth(text);
    int textX = (getWidth() - textWidth) / 2;
    // center on the text baseline
    int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

    // Draw the text background
    g2d.setColor(textBackground);
    g2d.fillRect(textX, textY - fm.getAscent(), textWidth, fm.getHeight());

    // Draw the text
    g2d.setColor(getForeground());
    g2d.drawString(text, textX, textY);

    g2d.dispose();
  }

  public void setTextBackground(Color color) {
    this.textBackground = color;
    repaint();
  }
}
