package edu.umich.soar.visualsoar.components;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;

// WIP; need to draw out state diagram. Log 40 minutes.
public class AutocompletePopup extends JPopupMenu {
  public AutocompletePopup(
      JTextComponent parent, int position, List<String> suggestions, Consumer<String> onSelect) {
    super();
    int maxVisibleRows = Math.min(suggestions.size(), 10); // Limit to 10 rows max
    // Create the suggestion list
    JList<String> suggestionList = new JList<>(new Vector<>(suggestions));
    suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    suggestionList.setVisibleRowCount(maxVisibleRows);

    // single-click to accept selection
    suggestionList.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            int index = suggestionList.locationToIndex(e.getPoint());
            if (index >= 0) {
              String selected = suggestionList.getModel().getElementAt(index);
              onSelect.accept(selected);
              setVisible(false);
            }
          }
        });

    // highlight item hovered over with mouse
    suggestionList.addMouseMotionListener(
        new MouseMotionAdapter() {
          @Override
          public void mouseMoved(MouseEvent e) {
            int index = suggestionList.locationToIndex(e.getPoint());
            if (index >= 0) {
              suggestionList.setSelectedIndex(index); // Highlight the hovered item
            }
          }
        });

    suggestionList.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
              setVisible(false);
              e.consume();
              return;
            }
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
              int index = suggestionList.getSelectedIndex();
              if (index >= 0) {
                String selected = suggestionList.getModel().getElementAt(index);
                onSelect.accept(selected);
                setVisible(false);
                e.consume();
              }
            }
            if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
              int index = suggestionList.getSelectedIndex();
              if (index == -1) {
                index = 0;
              } else {
                index += (e.getKeyCode() == KeyEvent.VK_UP) ? -1 : 1;
              }
              if (index < 0) {
                index = suggestions.size() - 1;
              } else if (index >= suggestions.size()) {
                index = 0;
              }
              suggestionList.setSelectedIndex(index);
              suggestionList.ensureIndexIsVisible(index); // Ensure the selected item is visible
              e.consume();
            }
          }
        });

    // Calculate the preferred height based on the number of items
    int rowHeight = suggestionList.getFixedCellHeight();
    if (rowHeight == 0) {
      rowHeight = suggestionList.getFontMetrics(suggestionList.getFont()).getHeight();
    }
    int preferredHeight = rowHeight * maxVisibleRows;

    // Create the scroll pane and set its preferred size
    JScrollPane scrollPane = new JScrollPane(suggestionList);
    scrollPane.setFocusable(false); // Prevent the scroll pane from stealing focus
    scrollPane.setPreferredSize(new Dimension(300, preferredHeight)); // Adjust width as needed

    // Add a border to the popup
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(scrollPane, BorderLayout.CENTER);
    panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1)); // Add a gray border

    add(new JScrollPane(suggestionList));

    try {
      // Get the caret position and convert to component-relative coordinates
      Rectangle caretRect = parent.modelToView2D(position).getBounds();
      Point popupLocation = new Point(caretRect.x, caretRect.y + caretRect.height);

      // Show the popup menu
      show(parent, popupLocation.x, popupLocation.y);
    } catch (BadLocationException ex) {
      ex.printStackTrace();
    }
  }
}
