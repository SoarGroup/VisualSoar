package edu.umich.soar.visualsoar.files.projectjson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

  public final String rootId;
  public final List<Node> nodes;

  @JsonCreator
  public Layout(@JsonProperty("rootId") String rootId, @JsonProperty("nodes") List<Node> nodes) {
    this.rootId = rootId;
    this.nodes = nodes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Layout layout = (Layout) o;
    return Objects.equals(rootId, layout.rootId) && Objects.equals(nodes, layout.nodes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rootId, nodes);
  }


  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXISTING_PROPERTY,
      property = "type",
      visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = Node.HighLevelOperator.class, name = "HLOPERATOR"),
    @JsonSubTypes.Type(value = Node.Operator.class, name = "OPERATOR"),
    @JsonSubTypes.Type(value = Node.HighLevelFileOperator.class, name = "HLFOPERATOR"),
    @JsonSubTypes.Type(value = Node.FileOperator.class, name = "FOPERATOR"),
    @JsonSubTypes.Type(value = Node.HighLevelOperator.class, name = "HLIOPERATOR"),
    @JsonSubTypes.Type(value = Node.ImpasseOperator.class, name = "IOPERATOR"),
    @JsonSubTypes.Type(value = Node.Folder.class, name = "FOLDER"),
    @JsonSubTypes.Type(value = Node.File.class, name = "FILE"),
    @JsonSubTypes.Type(value = Node.Root.class, name = "ROOT"),
    @JsonSubTypes.Type(value = Node.Link.class, name = "LINK"),
  })
  public static class Node {
    public final String id;
    public final NodeType type;
    public final List<Node> children;

    //    TODO: don't serialize empty children
    public Node(String id, NodeType type, List<Node> children) {
      this.id = id;
      this.type = type;
      this.children = Objects.requireNonNullElse(children, Collections.emptyList());
    }

    private void assertType(NodeType expectedType) {
      if (type != expectedType) {
        throw new IllegalArgumentException(
            "Node type for "
                + getClass().getSimpleName()
                + " should be "
                + expectedType
                + " but is "
                + type);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Node node = (Node) o;
      return Objects.equals(id, node.id)
          && type == node.type
          && Objects.equals(children, node.children);
    }

    //  TODO: copy remaining data/logic from makeNodeVersionFive
    //  TODO: NEXT: HighLevelOperator

    public static class HighLevelOperator extends Node {
      public HighLevelOperator(
          @JsonProperty("id") String id,
          @JsonProperty("type") NodeType type,
          @JsonProperty("children") List<Node> children) {
        super(id, type, children);
        super.assertType(NodeType.HLOPERATOR);
      }
    }

    public static class Operator extends Node {
      public Operator(
          @JsonProperty("id") String id,
          @JsonProperty("type") NodeType type,
          @JsonProperty("children") List<Node> children) {
        super(id, type, children);
        super.assertType(NodeType.OPERATOR);
      }
    }

    public static class HighLevelFileOperator extends Node {
      public HighLevelFileOperator(
          @JsonProperty("id") String id,
          @JsonProperty("type") NodeType type,
          @JsonProperty("children") List<Node> children) {
        super(id, type, children);
        super.assertType(NodeType.HLFOPERATOR);
      }
    }

    public static class FileOperator extends Node {
      public FileOperator(
          @JsonProperty("id") String id,
          @JsonProperty("type") NodeType type,
          @JsonProperty("children") List<Node> children) {
        super(id, type, children);
        super.assertType(NodeType.FOPERATOR);
      }
    }

    public static class ImpasseOperator extends Node {
      public ImpasseOperator(
          @JsonProperty("id") String id,
          @JsonProperty("type") NodeType type,
          @JsonProperty("children") List<Node> children) {
        super(id, type, children);
        super.assertType(NodeType.IOPERATOR);
      }
    }

    public static class Folder extends Node {
      public Folder(
          @JsonProperty("id") String id,
          @JsonProperty("type") NodeType type,
          @JsonProperty("children") List<Node> children) {
        super(id, type, children);
        super.assertType(NodeType.FOLDER);
      }
    }

    public static class File extends Node {
      public File(
          @JsonProperty("id") String id,
          @JsonProperty("type") NodeType type,
          @JsonProperty("children") List<Node> children) {
        super(id, type, children);
        super.assertType(NodeType.FILE);
      }
    }

    public static class Root extends Node {
      public Root(
          @JsonProperty("id") String id,
          @JsonProperty("type") NodeType type,
          @JsonProperty("children") List<Node> children) {
        super(id, type, children);
        super.assertType(NodeType.ROOT);
      }
    }

    public static class Link extends Node {
      public Link(
          @JsonProperty("id") String id,
          @JsonProperty("type") NodeType type,
          @JsonProperty("children") List<Node> children) {
        super(id, type, children);
        super.assertType(NodeType.LINK);
      }
    }
  }
}
