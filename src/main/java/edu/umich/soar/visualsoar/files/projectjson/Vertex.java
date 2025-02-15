package edu.umich.soar.visualsoar.files.projectjson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.*;
import java.util.stream.Collectors;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = Vertex.SoarIdVertex.class, name = "SOAR_ID"),
  @JsonSubTypes.Type(value = Vertex.EnumerationVertex.class, name = "ENUMERATION"),
  @JsonSubTypes.Type(value = Vertex.IntegerRangeVertex.class, name = "INTEGER"),
  @JsonSubTypes.Type(value = Vertex.FloatRangeVertex.class, name = "FLOAT"),
  @JsonSubTypes.Type(value = Vertex.class, name = "STRING"),
  @JsonSubTypes.Type(value = Vertex.ForeignVertex.class, name = "FOREIGN")
})
public class Vertex {

  public enum VertexType {
    SOAR_ID,
    ENUMERATION,
    INTEGER,
    FLOAT,
    STRING,
    FOREIGN,
  }

  public final String id;
  public final VertexType type;

  public Vertex(@JsonProperty("id") String id, @JsonProperty("type") VertexType type) {
    this.id = id;
    this.type = type;

    Objects.requireNonNull(id);
    Objects.requireNonNull(type, "Vertex " + id + " is missing its 'type' field");
  }

  private void assertType(VertexType expectedType) {
    if (type != expectedType) {
      throw new IllegalArgumentException(
          "Vertex type for "
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
    Vertex v = (Vertex) o;
    return Objects.equals(id, v.id) && type == v.type;
  }

  public static class OutEdge {

    private final String name;
    public final String toId;
    public final String comment;

    public OutEdge(
        @JsonProperty("name") String name,
        @JsonProperty("toId") String toId,
        @JsonProperty("comment") String comment) {
      this.name = name;
      Objects.requireNonNull(name);
      this.toId = toId;
      Objects.requireNonNull(toId, "Edge named '" + name + "' is missing its toId field");
      // don't bother writing out empty comments
      if (comment != null && comment.isEmpty()) {
        comment = null;
      }
      this.comment = comment;
    }

    public String getName() {
      return name;
    }

    public String getToId() {
      return toId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      OutEdge outEdge = (OutEdge) o;
      return Objects.equals(toId, outEdge.toId) && Objects.equals(comment, outEdge.comment);
    }

    @Override
    public int hashCode() {
      return Objects.hash(toId, comment);
    }
  }

  public static class SoarIdVertex extends Vertex {
    public final List<OutEdge> outEdges;

    public SoarIdVertex(String id, List<OutEdge> outEdges) {
      this(id, VertexType.SOAR_ID, outEdges);
    }

    SoarIdVertex(
        @JsonProperty("id") String id,
        @JsonProperty("type") VertexType type,
        @JsonProperty("outEdges") List<OutEdge> outEdges) {
      super(id, type);
      super.assertType(VertexType.SOAR_ID);
      if (outEdges == null) {
        outEdges = Collections.emptyList();
      }
      this.outEdges =
          outEdges.stream()
              .sorted(Comparator.comparing(OutEdge::getName).thenComparing(OutEdge::getToId))
              .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public boolean equals(Object o) {
      if (!super.equals(o)) {
        return false;
      }

      SoarIdVertex vertex = (SoarIdVertex) o;
      return outEdges.equals(vertex.outEdges);
    }
  }

  public static class EnumerationVertex extends Vertex {
    public final List<String> choices;

    public EnumerationVertex(String id, List<String> choices) {
      this(id, VertexType.ENUMERATION, choices);
    }

    EnumerationVertex(
        @JsonProperty("id") String id,
        @JsonProperty("type") VertexType type,
        @JsonProperty("choices") List<String> choices) {
      super(id, type);
      super.assertType(VertexType.ENUMERATION);
      if (choices == null) {
        throw new IllegalArgumentException(
            "Vertex "
                + id
                + " is of type "
                + type
                + " and therefore must have an enumChoices defined");
      }
      this.choices = choices.stream().sorted().collect(Collectors.toUnmodifiableList());
    }

    @Override
    public boolean equals(Object o) {
      if (!super.equals(o)) {
        return false;
      }

      EnumerationVertex vertex = (EnumerationVertex) o;
      return choices.equals(vertex.choices);
    }
  }

  public static class IntegerRangeVertex extends Vertex {

    public final int min;
    public final int max;

    public IntegerRangeVertex(String id, Integer min, Integer max) {
      this(id, VertexType.INTEGER, min, max);
    }

    IntegerRangeVertex(
        @JsonProperty("id") String id,
        @JsonProperty("type") VertexType type,
        @JsonProperty("min") Integer min,
        @JsonProperty("max") Integer max) {
      super(id, type);
      super.assertType(VertexType.INTEGER);
      if (min == null) {
        min = Integer.MIN_VALUE;
      }
      this.min = min;
      if (max == null) {
        max = Integer.MAX_VALUE;
      }
      this.max = max;
    }

    @JsonProperty("min")
    public Integer getMin() {
      if (min == Integer.MIN_VALUE) {
        return null;
      }
      return min;
    }

    @JsonProperty("max")
    public Integer getMax() {
      if (max == Integer.MAX_VALUE) {
        return null;
      }
      return max;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      IntegerRangeVertex vertex = (IntegerRangeVertex) o;
      return super.equals(vertex) && min == vertex.min && max == vertex.max;
    }
  }

  public static class FloatRangeVertex extends Vertex {
    public final double min;
    public final double max;

    public FloatRangeVertex(String id, Double min, Double max) {
      this(id, VertexType.FLOAT, min, max);
    }

    FloatRangeVertex(
        @JsonProperty("id") String id,
        @JsonProperty("type") VertexType type,
        @JsonProperty("min") Double min,
        @JsonProperty("max") Double max) {
      super(id, type);
      super.assertType(VertexType.FLOAT);
      if (min == null) {
        min = Double.NEGATIVE_INFINITY;
      }
      this.min = min;

      if (max == null) {
        max = Double.POSITIVE_INFINITY;
      }
      this.max = max;
    }

    @JsonProperty("min")
    public Double getMin() {
      if (min == Double.NEGATIVE_INFINITY) {
        return null;
      }
      return min;
    }

    @JsonProperty("max")
    public Double getMax() {
      if (max == Double.POSITIVE_INFINITY) {
        return null;
      }
      return max;
    }

    @Override
    public boolean equals(Object o) {
      if (!super.equals(o)) {
        return false;
      }
      FloatRangeVertex vertex = (FloatRangeVertex) o;
      return min == vertex.min && max == vertex.max;
    }
  }

  public static class ForeignVertex extends Vertex {
    public final String foreignDMPath;
    public final Vertex importedVertex;

    public ForeignVertex(String id, String foreignDMPath, Vertex importedVertex) {
      this(id, VertexType.FOREIGN, foreignDMPath, importedVertex);
    }

    ForeignVertex(
        @JsonProperty("id") String id,
        @JsonProperty("type") VertexType type,
        @JsonProperty("foreignDMPath") String foreignDMPath,
        @JsonProperty("importedVertex") Vertex importedVertex) {
      super(id, type);
      super.assertType(VertexType.FOREIGN);
      this.foreignDMPath = foreignDMPath;
      this.importedVertex = importedVertex;
      if (foreignDMPath == null) {
        throw new IllegalArgumentException(
            "Vertex "
                + id
                + " is of type "
                + type
                + " and therefore must have a foreignDMPath defined");
      }
      if (importedVertex == null) {
        throw new IllegalArgumentException(
            "Vertex "
                + id
                + " is of type "
                + type
                + " and therefore must have a importedVertex defined");
      }
    }

    public VertexType getType() {
      return VertexType.FOREIGN;
    }

    @Override
    public boolean equals(Object o) {
      if (!super.equals(o)) {
        return false;
      }
      ForeignVertex vertex = (ForeignVertex) o;
      return Objects.equals(foreignDMPath, vertex.foreignDMPath)
          && Objects.equals(importedVertex, vertex.importedVertex);
    }
  }
}
