package edu.umich.soar.visualsoar.ruleeditor;

import org.junit.jupiter.api.Test;

class EditingUtilsTest {

  @Test
  void testGetLineStartOffset() {
    //   TODO
  }

  @Test
  void selectCurrLineNonemptyLine() {
    //   TODO
  }

  // should not select anything
  // hello
  // hello
  //
  // hello
  //
  @Test
  void selectCurrLineOnEmptyLine() {
    //   TODO
  }

  @Test
  void selectCurrLineOnEndOfDocument() {
    //   TODO
  }

  // should not select anything
  // hello
  // hello
  //
  // hello
  @Test
  void selectCurrLineOnEmptyLineEndOfDocument() {
    //   TODO
  }

  @Test
  void replaceRange() {
    //   TODO
    // TODO: isn't there a built-in for this? Do we really need our own?
  }
}