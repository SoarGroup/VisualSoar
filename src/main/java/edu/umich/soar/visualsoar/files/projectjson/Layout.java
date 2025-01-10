package edu.umich.soar.visualsoar.files.projectjson;

public class Layout {
  enum NodeType {
    HLOPERATOR,
    OPERATOR,
    HLFOPERATOR,
    FOPERATOR,
    HLIOPERATOR,
    IOPERATOR,
    FOLDER,
    FILE,
    ROOT,
    LINK,
  }
//  TODO: NEXT continue reading makeNodeVersionFive and adding fields used by all node types
  public static class Node {
    public final String id;
    public final NodeType type;
    public final String parentId;

    public Node(String id, NodeType type, String parentId) {
      this.id = id;
      this.type = type;
      this.parentId = parentId;
    }
  }
}
