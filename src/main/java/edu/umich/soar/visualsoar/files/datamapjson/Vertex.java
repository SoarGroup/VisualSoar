package edu.umich.soar.visualsoar.files.datamapjson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

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

//  todo: rename to "type"
  public final VertexType vertexType;
  public final Comment comment;
  public final String foreignDMPath;
  public final String foreignVertexId;
  public final Vertex foreignVertex;
  public final String[] enumChoices;
  public final Number min;
  public final Number max;

//  TODO: should take ID as well so that it can be used in error messages
  Vertex(
    VertexType vertexType,
    Comment comment,
    String foreignDMPath,
    String foreignVertexId,
    Vertex foreignVertex, String[] enumChoices, Number min, Number max) {
    this.vertexType = vertexType;
    this.comment = comment;
    this.foreignDMPath = foreignDMPath;
    this.foreignVertexId = foreignVertexId;
    this.foreignVertex = foreignVertex;
    this.enumChoices = enumChoices;
    this.min = min;
    this.max = max;

    if (vertexType == VertexType.FOREIGN) {
      if (foreignVertexId == null) {
        throw new IllegalArgumentException(
            "vertices of type " + vertexType + " must have a foreignVertexId defined");
      }
      if (foreignDMPath == null) {
        throw new IllegalArgumentException(
            "vertices of type " + vertexType + " must have a foreignDMPath defined");
      }
      if (foreignVertex == null) {
        throw new IllegalArgumentException(
            "vertices of type " + vertexType + " must have a foreignVertex defined");
      }
    }

    if(vertexType == VertexType.ENUMERATION) {
      if (enumChoices == null) {
        throw new IllegalArgumentException("vertices of type " + vertexType + " must have an enumChoices defined");
      }
    }

//    TODO: there has to be a cleaner way than taking a Number and checking if it's an int
    if (vertexType == VertexType.INTEGER_RANGE) {
      if (min == null) {
        throw new IllegalArgumentException("vertices of type " + vertexType + " must have a min defined");
      }
      if (!(min instanceof Integer)) {
        throw new IllegalArgumentException("min value must be an integer");
      }
      if (max == null) {
        throw new IllegalArgumentException("vertices of type " + vertexType + " must have a max defined");
      }
      if (!(max instanceof Integer)) {
        throw new IllegalArgumentException("max value must be an integer");
      }
    }
    // TODO: as above; I want to use BigDecimal for exact precision instead of regular float, etc.
    if (vertexType == VertexType.FLOAT_RANGE) {
      if (min == null) {
        throw new IllegalArgumentException("vertices of type " + vertexType + " must have a min defined");
      }
      if (max == null) {
        throw new IllegalArgumentException("vertices of type " + vertexType + " must have a max defined");
      }
    }

  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "with")
  static class Builder {
    private Vertex.VertexType vertexType;
    private Comment comment;
    private Vertex foreignVertex;
    private String foreignDMPath;
    private String foreignVertexId;
    private String[] enumChoices;
    private Number min;
    private Number max;

    public Builder withVertexType(Vertex.VertexType vertexType) {
      this.vertexType = vertexType;
      return this;
    }

    public Builder withComment(Comment comment) {
      this.comment = comment;
      return this;
    }

    public Vertex build() {
      return new Vertex(vertexType, comment, foreignDMPath, foreignVertexId, foreignVertex, enumChoices, min, max);
    }

    public void withForeignVertex(Vertex foreignVertex) {
      this.foreignVertex = foreignVertex;
    }

    public void withForeignDMPath(String foreignDMPath) {
      this.foreignDMPath = foreignDMPath;
    }

    public void withForeignVertexId(String foreignVertexId) {
      this.foreignVertexId = foreignVertexId;
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
