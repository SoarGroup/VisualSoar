package edu.umich.soar.visualsoar.files.projectjson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.*;
import java.util.stream.Collectors;

/** Specifies the project datamap (working memory layout). */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = DMVertex.SoarIdVertex.class, name = "SOAR_ID"),
  @JsonSubTypes.Type(value = DMVertex.EnumerationVertex.class, name = "ENUMERATION"),
  @JsonSubTypes.Type(value = DMVertex.IntegerRangeVertex.class, name = "INTEGER"),
  @JsonSubTypes.Type(value = DMVertex.FloatRangeVertex.class, name = "FLOAT"),
  @JsonSubTypes.Type(value = DMVertex.class, name = "STRING"),
  @JsonSubTypes.Type(value = DMVertex.ForeignVertex.class, name = "FOREIGN")
})
public class DMVertex {

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

  public DMVertex(@JsonProperty("id") String id, @JsonProperty("type") VertexType type) {
    this.id = id;
    this.type = type;

    Objects.requireNonNull(id);
    Objects.requireNonNull(type, "Vertex " + id + " is missing its 'type' field");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DMVertex v = (DMVertex) o;
    return Objects.equals(id, v.id) && type == v.type;
  }

  public static class OutEdge {

    private final String name;
    public final String toId;
    public final String comment;
    public final boolean generated;

    public OutEdge(
        @JsonProperty("name") String name,
        @JsonProperty("toId") String toId,
        @JsonProperty("comment") String comment,
        @JsonProperty("generated") Boolean generated) {
      this.name = name;
      Objects.requireNonNull(name);
      this.toId = toId;
      Objects.requireNonNull(toId, "Edge named '" + name + "' is missing its toId field");
      // don't bother writing out empty comments
      if (comment != null && comment.isEmpty()) {
        comment = null;
      }
      this.comment = comment;
      this.generated = generated != null ? generated : false;
    }

    public String getName() {
      return name;
    }

    public String getToId() {
      return toId;
    }

    @JsonProperty("generated")
    @JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
    public boolean getGenerated() {
      return generated;
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

  public static class SoarIdVertex extends DMVertex {
    public final List<OutEdge> outEdges;

    public SoarIdVertex(
        @JsonProperty("id") String id, @JsonProperty("outEdges") List<OutEdge> outEdges) {
      super(id, VertexType.SOAR_ID);
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

  public static class EnumerationVertex extends DMVertex {
    public final List<String> choices;

    public EnumerationVertex(
        @JsonProperty("id") String id, @JsonProperty("choices") List<String> choices) {
      super(id, VertexType.ENUMERATION);
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

  public static class IntegerRangeVertex extends DMVertex {

    public final int min;
    public final int max;

    public IntegerRangeVertex(
        @JsonProperty("id") String id,
        @JsonProperty("min") Integer min,
        @JsonProperty("max") Integer max) {
      super(id, VertexType.INTEGER);
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

  public static class FloatRangeVertex extends DMVertex {
    public final double min;
    public final double max;

    public FloatRangeVertex(
        @JsonProperty("id") String id,
        @JsonProperty("min") Double min,
        @JsonProperty("max") Double max) {
      super(id, VertexType.FLOAT);
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

  public static class ForeignVertex extends DMVertex {
    public final String foreignDMPath;
    public final DMVertex importedVertex;

    public ForeignVertex(
        @JsonProperty("id") String id,
        @JsonProperty("foreignDMPath") String foreignDMPath,
        @JsonProperty("importedVertex") DMVertex importedVertex) {
      super(id, VertexType.FOREIGN);
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
      this.foreignDMPath = foreignDMPath.replace('\\', '/');
      this.importedVertex = importedVertex;
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
