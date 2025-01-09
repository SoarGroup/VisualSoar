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

  Vertex(VertexType vertexType, Comment comment) {
    this.vertexType = vertexType;
    this.comment = comment;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "with")
  static class Builder {
    private Vertex.VertexType vertexType;
    private Comment comment;

    public Builder withVertexType(Vertex.VertexType vertexType) {
      this.vertexType = vertexType;
      return this;
    }

    public Builder withComment(Comment comment) {
      this.comment = comment;
      return this;
    }

    public Vertex build() {
      return new Vertex(vertexType, comment);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Vertex vertex = (Vertex) o;
    return vertexType == vertex.vertexType
        && Objects.equals(comment, vertex.comment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vertexType, comment);
  }
}
