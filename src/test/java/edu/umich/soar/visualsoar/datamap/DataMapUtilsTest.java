package edu.umich.soar.visualsoar.datamap;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DataMapUtilsTest {

  @Test
  void testValidAttributeName() {
    assertTrue(DataMapUtils.attributeNameIsValid("validName"));
    assertTrue(DataMapUtils.attributeNameIsValid("valid-name"));
    assertTrue(DataMapUtils.attributeNameIsValid("valid_name"));
    assertTrue(DataMapUtils.attributeNameIsValid("validName123"));
  }

  @Test
  void testInvalidCharacters() {
    assertFalse(DataMapUtils.attributeNameIsValid("invalid name"));
    assertFalse(DataMapUtils.attributeNameIsValid("invalid@name"));
    assertFalse(DataMapUtils.attributeNameIsValid("invalid#name"));
    assertFalse(DataMapUtils.attributeNameIsValid("invalid$name"));
  }

  @Test
  void testEmptyAttributeName() {
    assertFalse(DataMapUtils.attributeNameIsValid(""));
  }

  @Test
  void testWhitespaceAttributeName() {
    assertFalse(DataMapUtils.attributeNameIsValid("   \t\n"));
  }
}
