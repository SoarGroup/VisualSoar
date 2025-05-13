package edu.umich.soar.visualsoar.components;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutocompleteContextTest {
  private final List<String> allSuggestions = Arrays.asList("apple", "banana", "apricot", "blueberry", "blackberry");;


  @Test
  void testConstructorNoInputTextNoFiltering() {
    AutocompleteContext autocompleteContext = new AutocompleteContext("", allSuggestions);
    assertEquals(allSuggestions, autocompleteContext.filteredSuggestions());
  }

  @Test
  void testConstructorSomeSomeInputTextFiltering() {
    AutocompleteContext autocompleteContext = new AutocompleteContext("ap", allSuggestions);
    assertEquals(Arrays.asList("apple", "apricot"), autocompleteContext.filteredSuggestions());
  }

  @Test
  void testAppendInputUpdatesSuggestions() {
    AutocompleteContext autocompleteContext = new AutocompleteContext("", allSuggestions);
    autocompleteContext.appendInput("a");
    List<String> filtered = autocompleteContext.filteredSuggestions();
    assertEquals(Arrays.asList("apple", "apricot"), filtered);

    autocompleteContext.appendInput("p");
    autocompleteContext.appendInput("p");
    filtered = autocompleteContext.filteredSuggestions();
    assertEquals(Collections.singletonList("apple"), filtered);
  }

  @Test
  void testUnfilteredSuggestionsSize() {
    AutocompleteContext autocompleteContext = new AutocompleteContext("ap", allSuggestions);
    assertEquals(allSuggestions.size(), autocompleteContext.unfilteredSuggestionsSize());

    autocompleteContext.appendInput("p");
    assertEquals(allSuggestions.size(), autocompleteContext.unfilteredSuggestionsSize());

    autocompleteContext.appendInput("p");
    assertEquals(allSuggestions.size(), autocompleteContext.unfilteredSuggestionsSize());
  }

  @Test
  void testCanDelete() {
    AutocompleteContext autocompleteContext = new AutocompleteContext("", allSuggestions);
    assertFalse(autocompleteContext.canDelete());

    autocompleteContext.appendInput("a");
    assertTrue(autocompleteContext.canDelete());


    autocompleteContext = new AutocompleteContext("app", allSuggestions);
    assertTrue(autocompleteContext.canDelete());
  }

  @Test
  void testDeleteInputUpdatesSuggestions() {
    AutocompleteContext autocompleteContext = new AutocompleteContext("ap", allSuggestions);
    autocompleteContext.appendInput("p");
    assertEquals(Collections.singletonList("apple"), autocompleteContext.filteredSuggestions());

    autocompleteContext.deleteInput();
    assertEquals(Arrays.asList("apple", "apricot"), autocompleteContext.filteredSuggestions());

    autocompleteContext.deleteInput();
    autocompleteContext.deleteInput();
    assertEquals(allSuggestions, autocompleteContext.filteredSuggestions());
  }

  @Test
  void testDeleteInputThrowsExceptionWhenEmpty() {
    AutocompleteContext autocompleteContext = new AutocompleteContext("", allSuggestions);
    assertThrows(IllegalArgumentException.class, autocompleteContext::deleteInput);
  }

  @Test
  void testGetCompletion() {
    AutocompleteContext autocompleteContext = new AutocompleteContext("b", allSuggestions);
    autocompleteContext.appendInput("l");
    String completion = autocompleteContext.getCompletion(1);
    assertEquals("ackberry", completion);
  }

  @Test
  void testGetCompletionThrowsExceptionForInvalidIndex() {
    AutocompleteContext autocompleteContext = new AutocompleteContext("", allSuggestions);
    assertThrows(IndexOutOfBoundsException.class, () -> autocompleteContext.getCompletion(50));
  }

  @Test
  void testNoMatchingSuggestions() {
    AutocompleteContext autocompleteContext = new AutocompleteContext("", allSuggestions);
    autocompleteContext.appendInput("z");
    List<String> filtered = autocompleteContext.filteredSuggestions();
    assertEquals(Collections.emptyList(), filtered);
  }

  @Test
  void testCaseSensitivity() {
    AutocompleteContext autocompleteContext = new AutocompleteContext("", allSuggestions);
    autocompleteContext.appendInput("A");
    List<String> filtered = autocompleteContext.filteredSuggestions();
    assertEquals(Collections.emptyList(), filtered);
  }

  @Test
  void testAppendCommonPrefixWithMatchingSuggestions() {
    List<String> suggestions = List.of("apple", "applet", "application");
    AutocompleteContext context = new AutocompleteContext("ap", suggestions);

    String appended = context.appendCommonPrefix();

    assertEquals("pl", appended);
    assertEquals(List.of("apple", "applet", "application"), context.filteredSuggestions());

    context.appendInput("e");
    assertEquals(List.of("apple", "applet"), context.filteredSuggestions());
  }
}
