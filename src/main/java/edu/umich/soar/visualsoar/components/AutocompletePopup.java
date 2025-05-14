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
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/** Popup menu for auto-completing text in a document */
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
  private final AutocompleteContext autocompleteContext;

  /**
   * @param parent text component where completion will be performed
   * @param position position to perform completion at
   * @param onCompletion to pass the final selected completion string to
   */
  public AutocompletePopup(
      @NotNull JTextComponent parent,
      int position,
      @NotNull AutocompleteContext autocompleteContext,
      @NotNull Consumer<String> onCompletion) {
    super();
    this.autocompleteContext = autocompleteContext;
    int maxVisibleRows =
        Math.min(autocompleteContext.unfilteredSuggestionsSize(), 10); // Limit to 10 rows max
    updateSuggestionList();
    suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    suggestionList.setVisibleRowCount(maxVisibleRows);

    // single-click to accept selection
    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            executeCompletion(suggestionList.locationToIndex(e.getPoint()), onCompletion);
          }
        });

    // highlight item hovered over with mouse
    addMouseMotionListener(
        new MouseMotionAdapter() {
          @Override
          public void mouseMoved(MouseEvent e) {
            int index = suggestionList.locationToIndex(e.getPoint());
            if (index >= 0) {
              suggestionList.setSelectedIndex(index); // Highlight the hovered item
            }
          }
        });

    addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            handleKeyPressed(e, parent, onCompletion);
          }

          @Override
          public void keyTyped(KeyEvent e) {
            char typedChar = e.getKeyChar();
            handleKeyTyped(e, typedChar, parent);
          }
        });

    // Ensure the suggestion list gains focus
    // Without this, on Windows the popup shows but the keyboard becomes unresponsive
    addPopupMenuListener(
        new PopupMenuListener() {
          @Override
          public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            SwingUtilities.invokeLater(() -> requestFocusInWindow());
          }

          @Override
          public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

          @Override
          public void popupMenuCanceled(PopupMenuEvent e) {}
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
      setVisible(false);
    }
  }

  private void handleKeyPressed(
      @NotNull KeyEvent e, @NotNull JTextComponent parent, @NotNull Consumer<String> onCompletion) {
    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
      handleBackspace(e, parent);
      return;
    }
    // ENTER triggers the current selected completion
    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
      executeCompletion(suggestionList.getSelectedIndex(), onCompletion);
      e.consume();
      return;
    }
    if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
      handleUpDownKeys(e);
      return;
    }
    // Unambiguous cursor movement keys are passed through to parent after closing
    if (CURSOR_MOVEMENT_PASSTHROUGH_KEYS.contains(e.getKeyCode())) {
      setVisible(false);
      parent.dispatchEvent(e);
      return;
    }
    // Close the popup if a meta key (Ctrl, Alt, Cmd) is pressed with another key, as this is likely
    // the invocation of some other shortcut such as undo/redo, and we would need more logic to
    // handle that gracefully
    if ((e.getModifiersEx()
                & (KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK | KeyEvent.META_DOWN_MASK))
            != 0
        && e.getKeyCode() != KeyEvent.VK_CONTROL
        && e.getKeyCode() != KeyEvent.VK_ALT
        && e.getKeyCode() != KeyEvent.VK_META) {
      setVisible(false);
      parent.dispatchEvent(e);
      return;
    }
    parent.dispatchEvent(e);
  }

  /**
   * When the user types, pass the typing through to the parent and update the suggestion list. If
   * no more suggestions are available, close this popup.
   */
  private void handleKeyTyped(KeyEvent e, char typedChar, @NotNull JTextComponent parent) {
    if (Character.isDefined(typedChar) && !Character.isISOControl(typedChar)) {
      autocompleteContext.appendInput(String.valueOf(typedChar));
    }
    if (autocompleteContext.filteredSuggestions().isEmpty()) {
      // Close the popup if no suggestions match
      setVisible(false);
    }
    updateSuggestionList();
    // enter the chars in the document
    parent.dispatchEvent(e);
  }

  /**
   * Update auto-complete context and pass on to parent. Hide if nothing could be deleted from the
   * auto-complete context.
   */
  private void handleBackspace(KeyEvent e, @NotNull JTextComponent parent) {
    if (autocompleteContext.canDelete()) {
      autocompleteContext.deleteInput();
      updateSuggestionList();
    } else {
      setVisible(false);
    }

    parent.dispatchEvent(e);
  }

  private void executeCompletion(int suggestionIndex, @NotNull Consumer<String> onCompletion) {
    if (suggestionIndex >= 0) {
      String selected = autocompleteContext.getCompletion(suggestionIndex);
      onCompletion.accept(selected);
      setVisible(false);
    }
  }

  /**
   * UP/DOWN keys navigate selection; custom handling here allows wrapping between start/end of list
   */
  private void handleUpDownKeys(KeyEvent e) {
    int index = suggestionList.getSelectedIndex();
    if (index == -1) {
      index = 0;
    } else {
      index += (e.getKeyCode() == KeyEvent.VK_UP) ? -1 : 1;
    }
    int numSuggestions = suggestionList.getModel().getSize();
    if (index < 0) {
      index = numSuggestions - 1;
    } else if (index >= numSuggestions) {
      index = 0;
    }
    suggestionList.setSelectedIndex(index);
    suggestionList.ensureIndexIsVisible(index);
    e.consume();
  }

  private void updateSuggestionList() {
    suggestionList.setListData(new Vector<>(autocompleteContext.filteredSuggestions()));
    suggestionList.setSelectedIndex(0);
  }

  public String shortInstructions() {
    return "UP/DOWN to select; ENTER to confirm";
  }
}
