package edu.umich.soar.visualsoar.files.projectjson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Specifies the project source layout. */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = LayoutNode.File.class, name = "FILE"),
  @JsonSubTypes.Type(value = LayoutNode.FileOperator.class, name = "FILE_OPERATOR"),
  @JsonSubTypes.Type(value = LayoutNode.Folder.class, name = "FOLDER"),
  @JsonSubTypes.Type(
      value = LayoutNode.HighLevelFileOperator.class,
      name = "HIGH_LEVEL_FILE_OPERATOR"),
  @JsonSubTypes.Type(
      value = LayoutNode.HighLevelImpasseOperator.class,
      name = "HIGH_LEVEL_IMPASSE_OPERATOR"),
  @JsonSubTypes.Type(value = LayoutNode.HighLevelOperator.class, name = "HIGH_LEVEL_OPERATOR"),
  @JsonSubTypes.Type(value = LayoutNode.ImpasseOperator.class, name = "IMPASSE_OPERATOR"),
  @JsonSubTypes.Type(value = LayoutNode.Link.class, name = "LINK"),
  @JsonSubTypes.Type(value = LayoutNode.Operator.class, name = "OPERATOR"),
  @JsonSubTypes.Type(value = LayoutNode.OperatorRoot.class, name = "OPERATOR_ROOT"),
})
public class LayoutNode {
  public enum NodeType {
    FILE,
    FILE_OPERATOR,
    FOLDER,
    HIGH_LEVEL_FILE_OPERATOR,
    HIGH_LEVEL_IMPASSE_OPERATOR,
    HIGH_LEVEL_OPERATOR,
    IMPASSE_OPERATOR,
    LINK,
    OPERATOR,
    OPERATOR_ROOT,
  }

  public final NodeType type;
  public final List<LayoutNode> children;
  public final String id;

  public LayoutNode(NodeType type, List<LayoutNode> children, String id) {
    this.type = type;
    this.children = Objects.requireNonNullElse(children, Collections.emptyList());
    this.id = id;
  }

  /** Returns null if children is empty so that we don't serialize the field at all */
  @JsonProperty("children")
  public LayoutNode[] getChildrenArray() {
    if (children.isEmpty()) {
      return null;
    }
    return children.toArray(new LayoutNode[0]);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LayoutNode node = (LayoutNode) o;
    return type == node.type
        && Objects.equals(children, node.children)
        && Objects.equals(id, node.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, children, id);
  }

  // We specify property ordering through because children can be quite large and
  // hinders reading if it comes at the beginning (as it would with the alphabetical default)
  @JsonPropertyOrder({"file", "id", "name", "type", "children"})
  public static class File extends LayoutNode {
    public final String name;
    public final String file;

    public File(
        @JsonProperty("children") List<LayoutNode> children,
        @JsonProperty("name") String name,
        @JsonProperty("id") String id,
        @JsonProperty("file") String file) {
      super(NodeType.FILE, children, id);
      this.name = name;
      this.file = file;
    }
  }

  @JsonPropertyOrder({"file", "id", "name", "type", "children"})
  public static class FileOperator extends LayoutNode {
    public final String name;
    public final String file;

    public FileOperator(
        @JsonProperty("children") List<LayoutNode> children,
        @JsonProperty("name") String name,
        @JsonProperty("id") String id,
        @JsonProperty("file") String file) {
      super(NodeType.FILE_OPERATOR, children, id);
      this.name = name;
      this.file = file;
    }
  }

  @JsonPropertyOrder({"dmId", "file", "folder", "id", "name", "type", "children"})
  public static class HighLevelFileOperator extends LayoutNode {
    public final String name;
    public final String file;
    public final String dmId;
    public final String folder;

    public HighLevelFileOperator(
        @JsonProperty("children") List<LayoutNode> children,
        @JsonProperty("name") String name,
        @JsonProperty("id") String id,
        @JsonProperty("file") String file,
        @JsonProperty("folder") String folder,
        @JsonProperty("dmId") String dmId) {
      super(NodeType.HIGH_LEVEL_FILE_OPERATOR, children, id);
      this.name = name;
      this.file = file;
      this.dmId = dmId;
      this.folder = folder;
    }
  }

  @JsonPropertyOrder({"folder", "id", "name", "type", "children"})
  public static class Folder extends LayoutNode {
    public final String name;
    public final String folder;

    public Folder(
        @JsonProperty("children") List<LayoutNode> children,
        @JsonProperty("name") String name,
        @JsonProperty("id") String id,
        @JsonProperty("folder") String folder) {
      super(NodeType.FOLDER, children, id);
      this.name = name;
      this.folder = folder;
    }
  }

  @JsonPropertyOrder({"file", "id", "linkedNodeId", "name", "type", "children"})
  public static class Link extends LayoutNode {
    public final String name;
    public final String file;
    public final String linkedNodeId;

    public Link(
        @JsonProperty("children") List<LayoutNode> children,
        @JsonProperty("name") String name,
        @JsonProperty("id") String id,
        @JsonProperty("file") String file,
        @JsonProperty("linkedNodeId") String linkedNodeId) {
      super(NodeType.LINK, children, id);
      this.name = name;
      this.file = file;
      this.linkedNodeId = linkedNodeId;
    }
  }

  @JsonPropertyOrder({"file", "id", "name", "type", "children"})
  public static class Operator extends LayoutNode {
    public final String name;
    public final String file;

    public Operator(
        @JsonProperty("children") List<LayoutNode> children,
        @JsonProperty("name") String name,
        @JsonProperty("id") String id,
        @JsonProperty("file") String file) {
      super(NodeType.OPERATOR, children, id);
      this.name = name;
      this.file = file;
    }
  }

  @JsonPropertyOrder({"dmId", "file", "folder", "id", "name", "type", "children"})
  public static class HighLevelOperator extends LayoutNode {
    public final String name;
    public final String file;
    public final String dmId;
    public final String folder;

    public HighLevelOperator(
        @JsonProperty("children") List<LayoutNode> children,
        @JsonProperty("name") String name,
        @JsonProperty("id") String id,
        @JsonProperty("file") String file,
        @JsonProperty("folder") String folder,
        @JsonProperty("dmId") String dmId) {
      super(NodeType.HIGH_LEVEL_OPERATOR, children, id);
      this.name = name;
      this.file = file;
      this.dmId = dmId;
      this.folder = folder;
    }
  }

  @JsonPropertyOrder({"folder", "id", "name", "type", "children"})
  public static class OperatorRoot extends LayoutNode {
    public final String name;
    public final String folder;

    public OperatorRoot(
        @JsonProperty("children") List<LayoutNode> children,
        @JsonProperty("name") String name,
        @JsonProperty("id") String id,
        @JsonProperty("folder") String folder) {
      super(NodeType.OPERATOR_ROOT, children, id);
      this.name = name;
      this.folder = folder;
    }
  }

  @JsonPropertyOrder({"file", "id", "name", "type", "children"})
  public static class ImpasseOperator extends LayoutNode {
    public final String name;
    public final String file;

    public ImpasseOperator(
        @JsonProperty("children") List<LayoutNode> children,
        @JsonProperty("name") String name,
        @JsonProperty("id") String id,
        @JsonProperty("file") String file) {
      super(NodeType.IMPASSE_OPERATOR, children, id);
      this.name = name;
      this.file = file;
    }
  }

  @JsonPropertyOrder({"dmId", "file", "folder", "id", "name", "type", "children"})
  public static class HighLevelImpasseOperator extends LayoutNode {
    public final String name;
    public final String file;
    public final String dmId;
    public final String folder;

    public HighLevelImpasseOperator(
        @JsonProperty("children") List<LayoutNode> children,
        @JsonProperty("name") String name,
        @JsonProperty("id") String id,
        @JsonProperty("file") String file,
        @JsonProperty("folder") String folder,
        @JsonProperty("dmId") String dmId) {
      super(NodeType.HIGH_LEVEL_IMPASSE_OPERATOR, children, id);
      this.name = name;
      this.file = file;
      this.dmId = dmId;
      this.folder = folder;
    }
  }
}
