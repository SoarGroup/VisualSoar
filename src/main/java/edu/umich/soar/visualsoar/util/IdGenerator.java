package edu.umich.soar.visualsoar.util;

import java.util.UUID;

public class IdGenerator {

  /**
   * @return Random string suitable for use as a unique ID.
   */
  public static String getId() {
    // removing the dashes to make the ID selectable with a double-click in most editors
    return UUID.randomUUID().toString().replace("-", "");
  }

}
