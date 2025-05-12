package edu.umich.soar.visualsoar.components;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Manages the suggestion list for auto-completion of a user's typing. */
public class AutocompleteContext {
  private String currentInput;
  private final List<String> allSuggestions;
  private List<String> currentSuggestions = Collections.emptyList();

  /**
   * @param currentInput The user's input so far this auto-completion.
   * @param allSuggestions All suggestions to be filtered down by the user's input
   */
  public AutocompleteContext(@NotNull String currentInput, @NotNull List<String> allSuggestions) {
    this.currentInput = currentInput;
    this.allSuggestions = Collections.unmodifiableList(allSuggestions);
    updateSuggestions();
  }

  /**
   * @return The number of total (unfiltered) suggestions available
   */
  public int unfilteredSuggestionsSize() {
    return allSuggestions.size();
  }

  /**
   * Append to current input string and re-filter the suggestion list to those matching the current
   * input.
   *
   * @param newInput character to append to the current input
   */
  public void appendInput(char newInput) {
    currentInput += newInput;
    updateSuggestions();
  }

  /**
   * @return true iff current input is not empty
   */
  public boolean canDelete() {
    return !currentInput.isEmpty();
  }

  /**
   * Delete one character from current input string and re-filter the suggestion list to those
   * matching the current input.
   *
   * @return newly-filtered suggestion list
   * @throws IllegalArgumentException if current input is empty
   */
  public void deleteInput() {
    if (currentInput.isEmpty()) {
      throw new IllegalArgumentException("inputSoFar is empty, so deletion is not possible");
    }
    currentInput = currentInput.substring(0, currentInput.length() - 1);
    updateSuggestions();
  }

  private void updateSuggestions() {
    currentSuggestions =
        allSuggestions.stream()
            .filter(suggestion -> suggestion.startsWith(currentInput))
            .collect(Collectors.toUnmodifiableList());
  }

  /**
   * @return The list of suggestions that are prefixed by the current user input
   */
  public List<String> filteredSuggestions() {
    return currentSuggestions;
  }

  /**
   * @param index Index of selection in current filtered suggestions
   * @return completion using that index and the current input
   */
  public String getCompletion(int index) {
    String selection = currentSuggestions.get(index);
    return selection.substring(currentInput.length());
  }
}
