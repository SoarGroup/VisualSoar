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

  public final VertexType vertexType;
  public final Comment comment;
  public final String foreignDMPath;
  public final String foreignVertexId;
  public final Vertex foreignVertex;
  public final String[] enumChoices;

  Vertex(
    VertexType vertexType,
    Comment comment,
    String foreignDMPath,
    String foreignVertexId,
    Vertex foreignVertex, String[] enumChoices) {
    this.vertexType = vertexType;
    this.comment = comment;
    this.foreignDMPath = foreignDMPath;
    this.foreignVertexId = foreignVertexId;
    this.foreignVertex = foreignVertex;
    this.enumChoices = enumChoices;

    if (vertexType == VertexType.FOREIGN) {
      if (foreignVertexId == null) {
        throw new IllegalArgumentException(
            "vertices of type " + VertexType.FOREIGN + " must have a foreignVertexId defined");
      }
      if (foreignDMPath == null) {
        throw new IllegalArgumentException(
            "vertices of type " + VertexType.FOREIGN + " must have a foreignDMPath defined");
      }
      if (foreignVertex == null) {
        throw new IllegalArgumentException(
            "vertices of type " + VertexType.FOREIGN + " must have a foreignVertex defined");
      }
    }

    if(vertexType == VertexType.ENUMERATION) {
      if (enumChoices == null) {
        throw new IllegalArgumentException("vertices of type " + VertexType.ENUMERATION + " must have an enumChoices defined");
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

    public Builder withVertexType(Vertex.VertexType vertexType) {
      this.vertexType = vertexType;
      return this;
    }

    public Builder withComment(Comment comment) {
      this.comment = comment;
      return this;
    }

    public Vertex build() {
      return new Vertex(vertexType, comment, foreignDMPath, foreignVertexId, foreignVertex, enumChoices);
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
