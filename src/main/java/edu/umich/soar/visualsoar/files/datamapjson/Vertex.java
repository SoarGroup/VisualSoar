package edu.umich.soar.visualsoar.files.datamapjson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Arrays;
import java.util.Objects;

@JsonDeserialize(builder = Vertex.Builder.class)
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
  //  todo: rename to "type"
  public final VertexType vertexType;
  public final Comment comment;
  public final String foreignDMPath;
  public final Vertex foreignVertex;
  public final String[] enumChoices;
  public final Number min;
  public final Number max;

  Vertex(
      String id,
      VertexType vertexType,
      Comment comment,
      String foreignDMPath,
      Vertex foreignVertex,
      String[] enumChoices,
      Number min,
      Number max) {
    this.id = id;
    this.vertexType = vertexType;
    this.comment = comment;
    this.foreignDMPath = foreignDMPath;
    this.foreignVertex = foreignVertex;
    if (enumChoices != null) {
      this.enumChoices = Arrays.copyOf(enumChoices, enumChoices.length);
      Arrays.sort(this.enumChoices);
    } else {
      this.enumChoices = null;
    }
    this.min = min;
    this.max = max;

    if (id == null) {
      //      TODO: would rather do this validation during parsing so that a line number can be
      // provided.
      // try https://github.com/FasterXML/jackson-databind/issues/781; might mean we have to not use
      // a builder
      // and instead use a creator constructor, with id marked @JsonProperty(required = true)
      throw new IllegalArgumentException("Vertex missing ID");
    }

    if (vertexType == VertexType.FOREIGN) {
      if (foreignDMPath == null) {
        throw new IllegalArgumentException(
            "Vertex "
                + id
                + " is of type "
                + vertexType
                + " and therefore must have a foreignDMPath defined");
      }
      if (foreignVertex == null) {
        throw new IllegalArgumentException(
            "Vertex "
                + id
                + " is of type "
                + vertexType
                + " and therefore must have a foreignVertex defined");
      }
    }

    if (vertexType == VertexType.ENUMERATION) {
      if (enumChoices == null) {
        throw new IllegalArgumentException(
            "Vertex "
                + id
                + " is of type "
                + vertexType
                + " and therefore must have an enumChoices defined");
      }
    }

    //    TODO: there has to be a cleaner way than taking a Number and checking if it's an int
    if (vertexType == VertexType.INTEGER_RANGE) {
      if (min == null) {
        throw new IllegalArgumentException(
            "Vertex "
                + id
                + " is of type "
                + vertexType
                + " and therefore must have a min defined");
      }
      if (!(min instanceof Integer)) {
        throw new IllegalArgumentException("min value of vertex " + id + " is not an integer");
      }
      if (max == null) {
        throw new IllegalArgumentException(
            "Vertex "
                + id
                + " is of type "
                + vertexType
                + " and therefore must have a max defined");
      }
      if (!(max instanceof Integer)) {
        throw new IllegalArgumentException("max value of vertex " + id + " is not an integer");
      }
    }
    // TODO: as above; I want to use BigDecimal for exact precision instead of regular float, etc.
    if (vertexType == VertexType.FLOAT_RANGE) {
      if (min == null) {
        throw new IllegalArgumentException(
            "Vertex "
                + id
                + " is of type "
                + vertexType
                + " and therefore must have a min defined");
      }
      if (max == null) {
        throw new IllegalArgumentException(
            "Vertex "
                + id
                + " is of type "
                + vertexType
                + " and therefore must have a max defined");
      }
    }
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "with")
  static class Builder {
    private Vertex.VertexType vertexType;
    private Comment comment;
    private Vertex foreignVertex;
    private String foreignDMPath;
    private String[] enumChoices;
    private Number min;
    private Number max;
    private String id;

    public Builder withVertexType(Vertex.VertexType vertexType) {
      this.vertexType = vertexType;
      return this;
    }

    public Builder withComment(Comment comment) {
      this.comment = comment;
      return this;
    }

    public Vertex build() {
      return new Vertex(
          id, vertexType, comment, foreignDMPath, foreignVertex, enumChoices, min, max);
    }

    public void withId(String id) {
      this.id = id;
    }

    public void withForeignVertex(Vertex foreignVertex) {
      this.foreignVertex = foreignVertex;
    }

    public void withForeignDMPath(String foreignDMPath) {
      this.foreignDMPath = foreignDMPath;
    }

    public void withEnumChoices(String[] enumChoices) {
      this.enumChoices = enumChoices;
    }

    public void withMin(Number min) {
      this.min = min;
    }

    public void withMax(Number max) {
      this.max = max;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Vertex vertex = (Vertex) o;
    return vertexType == vertex.vertexType && Objects.equals(comment, vertex.comment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vertexType, comment);
  }
}
