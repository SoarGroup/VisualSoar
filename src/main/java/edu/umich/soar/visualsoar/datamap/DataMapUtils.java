package edu.umich.soar.visualsoar.datamap;

public class DataMapUtils {
  public static boolean attributeNameIsValid(String theName) {
    if (theName.isBlank()) {
      return false;
    }
    for (int i = 0; i < theName.length(); i++) {
      char testChar = theName.charAt(i);
      if (!(Character.isLetterOrDigit(testChar) || (testChar == '-') || (testChar == '_'))) {
        return false;
      }
    }
    return true;
  }
}
