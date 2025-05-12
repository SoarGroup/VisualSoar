package edu.umich.soar.visualsoar.components;

import org.jetbrains.annotations.NotNull;

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
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

public class AutocompletePopup extends JPopupMenu {

  private static final Set<Integer> CURSOR_MOVEMENT_PASSTHROUGH_KEYS =
      Set.of(
          KeyEvent.VK_LEFT,
          KeyEvent.VK_RIGHT,
          KeyEvent.VK_PAGE_UP,
          KeyEvent.VK_PAGE_DOWN,
          KeyEvent.VK_HOME,
          KeyEvent.VK_END);
  private final JList<String> suggestionList = new JList<>();
  private final AutocompleteData autocompleteData;

  public AutocompletePopup(
      @NotNull JTextComponent parent,
      int initialPosition,
      @NotNull String inputSoFar,
      @NotNull List<String> suggestions,
      @NotNull Consumer<String> onCompletion) {
    super();
    this.autocompleteData = new AutocompleteData(inputSoFar, suggestions);

    int maxVisibleRows = Math.min(suggestions.size(), 10); // Limit to 10 rows max
    updateSuggestionList();
    suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    suggestionList.setVisibleRowCount(maxVisibleRows);

    // single-click to accept selection
    suggestionList.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            int index = suggestionList.locationToIndex(e.getPoint());
            if (index >= 0) {
              String selected = autocompleteData.getCompletion(index);
              onCompletion.accept(selected);
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
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
              int index = suggestionList.getSelectedIndex();
              if (index >= 0) {
                String selected = autocompleteData.getCompletion(index);
                onCompletion.accept(selected);
                setVisible(false);
                e.consume();
                return;
              }
            }
            // UP/DOWN keys navigate selection; custom handling here allows wrapping between
            // start/end of list
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
              return;
            }
            // Unambiguous cursor movement keys are passed through to parent after closing this
            // popup
            if (CURSOR_MOVEMENT_PASSTHROUGH_KEYS.contains(e.getKeyCode())) {
              setVisible(false);
              parent.dispatchEvent(e); // / TODO: is this needed?
              return;
            }
          }

          @Override
          public void keyTyped(KeyEvent e) {
            char typedChar = e.getKeyChar();
            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
              if (!autocompleteData.canDelete()) {
                setVisible(false);
                return;
              }
              autocompleteData.deleteInput();
            } else {
              autocompleteData.appendInput(typedChar);
            }
            if (autocompleteData.filteredSuggestions().isEmpty()) {
              // Close the popup if no suggestions match
              setVisible(false);
            }
            updateSuggestionList();
            // enter the chars in the document
            parent.dispatchEvent(e);
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
      Rectangle caretRect = parent.modelToView2D(initialPosition).getBounds();
      Point popupLocation = new Point(caretRect.x, caretRect.y + caretRect.height);

      // Show the popup menu
      show(parent, popupLocation.x, popupLocation.y);
    } catch (BadLocationException ex) {
      ex.printStackTrace();
    }
  }

  private void updateSuggestionList() {
    suggestionList.setListData(new Vector<>(autocompleteData.filteredSuggestions()));
    suggestionList.setSelectedIndex(0);
  }

  public String shortInstructions() {
    return "UP/DOWN to select; ENTER to confirm";
  }
}
