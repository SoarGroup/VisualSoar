package edu.umich.soar.visualsoar.files.projectjson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Arrays;
import java.util.Objects;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = Vertex.class, name = "SOAR_ID"),
  @JsonSubTypes.Type(value = Vertex.EnumerationVertex.class, name = "ENUMERATION"),
  @JsonSubTypes.Type(value = Vertex.IntegerRangeVertex.class, name = "INTEGER_RANGE"),
  @JsonSubTypes.Type(value = Vertex.class, name = "INTEGER"),
  @JsonSubTypes.Type(value = Vertex.FloatRangeVertex.class, name = "FLOAT_RANGE"),
  @JsonSubTypes.Type(value = Vertex.class, name = "FLOAT"),
  @JsonSubTypes.Type(value = Vertex.class, name = "STRING"),
  @JsonSubTypes.Type(value = Vertex.ForeignVertex.class, name = "FOREIGN")
})
public class Vertex {

  enum VertexType {
    SOAR_ID,
    ENUMERATION,
    INTEGER_RANGE,
    INTEGER,
    FLOAT_RANGE,
    FLOAT,
    STRING,
    FOREIGN,
  }

  public final String id;
  public final VertexType type;
  public final Comment comment;

  Vertex(
      @JsonProperty("id") String id,
      @JsonProperty("type") VertexType type,
      @JsonProperty("comment") Comment comment) {
    this.id = id;
    this.type = type;
    this.comment = comment;
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
    return Objects.equals(id, v.id) && type == v.type && Objects.equals(comment, v.comment);
  }

  public static class EnumerationVertex extends Vertex {
    public final String[] enumChoices;

    EnumerationVertex(
        @JsonProperty("id") String id,
        @JsonProperty("type") VertexType type,
        @JsonProperty("comment") Comment comment,
        @JsonProperty("enumChoices") String[] enumChoices) {
      super(id, type, comment);
      super.assertType(VertexType.ENUMERATION);
      if (enumChoices == null) {
        throw new IllegalArgumentException(
            "Vertex "
                + id
                + " is of type "
                + type
                + " and therefore must have an enumChoices defined");
      }
      this.enumChoices = Arrays.copyOf(enumChoices, enumChoices.length);
      Arrays.sort(this.enumChoices);
    }

    @Override
    public boolean equals(Object o) {
      if (!super.equals(o)) {
        return false;
      }

      EnumerationVertex vertex = (EnumerationVertex) o;
      return Arrays.equals(enumChoices, vertex.enumChoices);
    }
  }

  public static class IntegerRangeVertex extends Vertex {

    public final int min;
    public final int max;

    IntegerRangeVertex(
        @JsonProperty("id") String id,
        @JsonProperty("type") VertexType type,
        @JsonProperty("comment") Comment comment,
        @JsonProperty("min") int min,
        @JsonProperty("max") int max) {
      super(id, type, comment);
      super.assertType(VertexType.INTEGER_RANGE);
      this.min = min;
      this.max = max;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      IntegerRangeVertex vertex = (IntegerRangeVertex) o;
      return super.equals(vertex) && min == vertex.min && max == vertex.max;
    }
  }

  //  We have to use strings in the JSON because JSON floats don't support infinities; Jackson
  // does support it, but we want to write JSON that is easily parseable by other tools.
  public static class FloatRangeVertex extends Vertex {
    public final double min;
    public final double max;

    FloatRangeVertex(
        @JsonProperty("id") String id,
        @JsonProperty("type") VertexType type,
        @JsonProperty("comment") Comment comment,
        @JsonProperty("min") String min,
        @JsonProperty("max") String max) {
      super(id, type, comment);
      super.assertType(VertexType.FLOAT_RANGE);
      this.min = Double.parseDouble(min);
      this.max = Double.parseDouble(max);

      if (Double.isNaN(this.min)) {
        throw new IllegalArgumentException(
            "Float range vertex " + id + " has unparseable min value");
      }
      if (Double.isNaN(this.max)) {
        throw new IllegalArgumentException(
            "Float range vertex " + id + " has unparseable max value");
      }
    }

    @JsonProperty("min")
    public String getMinStringified() {
      return Double.toString(min);
    }

    @JsonProperty("max")
    public String getMaxStringified() {
      return Double.toString(max);
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

    ForeignVertex(
        @JsonProperty("id") String id,
        @JsonProperty("type") VertexType type,
        @JsonProperty("comment") Comment comment,
        @JsonProperty("foreignDMPath") String foreignDMPath,
        @JsonProperty("importedVertex") Vertex importedVertex) {
      super(id, type, comment);
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
