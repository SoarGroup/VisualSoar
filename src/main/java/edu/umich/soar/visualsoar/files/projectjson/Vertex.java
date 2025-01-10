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
  @JsonSubTypes.Type(value = Vertex.SoarIdVertex.class, name = "SOAR_ID"),
  @JsonSubTypes.Type(value = Vertex.EnumerationVertex.class, name = "ENUMERATION"),
  @JsonSubTypes.Type(value = Vertex.IntegerRangeVertex.class, name = "INTEGER_RANGE"),
  @JsonSubTypes.Type(value = Vertex.IntegerVertex.class, name = "INTEGER"),
  @JsonSubTypes.Type(value = Vertex.FloatRangeVertex.class, name = "FLOAT_RANGE"),
  @JsonSubTypes.Type(value = Vertex.FloatVertex.class, name = "FLOAT"),
  @JsonSubTypes.Type(value = Vertex.StringVertex.class, name = "STRING"),
  @JsonSubTypes.Type(value = Vertex.ForeignVertex.class, name = "FOREIGN")
})
public abstract class Vertex {

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

  Vertex(String id, VertexType type, Comment comment) {
    this.id = id;
    this.type = type;
    this.comment = comment;
  }

  private void assertType(VertexType expectedType) {
    if (type != expectedType) {
      throw new IllegalArgumentException(
          "Vertex type for " + getClass().getSimpleName() + " should be " + expectedType + " but is " + type);
    }
  }

  private boolean baseEquals(Vertex other) {
    return Objects.equals(id, other.id)
        && type == other.type
        && Objects.equals(comment, other.comment);
  }

  public static class SoarIdVertex extends Vertex {
    SoarIdVertex(
        @JsonProperty("id") String id,
        @JsonProperty("type") VertexType type,
        @JsonProperty("comment") Comment comment) {
      super(id, type, comment);
      super.assertType(VertexType.SOAR_ID);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SoarIdVertex vertex = (SoarIdVertex) o;
      return super.baseEquals(vertex);
    }
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
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      EnumerationVertex vertex = (EnumerationVertex) o;
      return super.baseEquals(vertex) && Arrays.equals(enumChoices, vertex.enumChoices);
    }
  }

  public static class IntegerVertex extends Vertex {
    public final int value;

    IntegerVertex(
        @JsonProperty("id") String id,
        @JsonProperty("type") VertexType type,
        @JsonProperty("comment") Comment comment,
        @JsonProperty("value") int value) {
      super(id, type, comment);
      super.assertType(VertexType.INTEGER);
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      IntegerVertex vertex = (IntegerVertex) o;
      return super.baseEquals(vertex) && value == vertex.value;
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
      return super.baseEquals(vertex) && min == vertex.min && max == vertex.max;
    }
  }

  public static class FloatVertex extends Vertex {
    public final double value;

    FloatVertex(
        @JsonProperty("id") String id,
        @JsonProperty("type") VertexType type,
        @JsonProperty("comment") Comment comment,
        @JsonProperty("value") double value) {
      super(id, type, comment);
      super.assertType(VertexType.FLOAT);
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FloatVertex vertex = (FloatVertex) o;
      return super.baseEquals(vertex) && value == vertex.value;
    }
  }

  public static class FloatRangeVertex extends Vertex {
    public final double min;
    public final double max;

    FloatRangeVertex(
        @JsonProperty("id") String id,
        @JsonProperty("type") VertexType type,
        @JsonProperty("comment") Comment comment,
        @JsonProperty("min") double min,
        @JsonProperty("max") double max) {
      super(id, type, comment);
      super.assertType(VertexType.FLOAT_RANGE);
      this.min = min;
      this.max = max;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FloatRangeVertex vertex = (FloatRangeVertex) o;
      return super.baseEquals(vertex) && min == vertex.min && max == vertex.max;
    }
  }

  public static class StringVertex extends Vertex {
    public final String value;

    StringVertex(
        @JsonProperty("id") String id,
        @JsonProperty("type") VertexType type,
        @JsonProperty("comment") Comment comment,
        @JsonProperty("value") String value) {
      super(id, type, comment);
      super.assertType(VertexType.STRING);
      this.value = value;
      if (value == null) {
        throw new IllegalArgumentException(
            "Vertex " + id + " is of type " + type + " and therefore must have a value defined");
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      StringVertex vertex = (StringVertex) o;
      return super.baseEquals(vertex) && Objects.equals(value, vertex.value);
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
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ForeignVertex vertex = (ForeignVertex) o;
      return super.baseEquals(vertex)
          && Objects.equals(foreignDMPath, vertex.foreignDMPath)
          && Objects.equals(importedVertex, vertex.importedVertex);
    }
  }
}
