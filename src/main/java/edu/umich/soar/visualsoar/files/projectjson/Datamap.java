package edu.umich.soar.visualsoar.files.projectjson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.stream.Collectors;

// TODO: create custom exception for document validation
// TODO: make sure to present JSON parsing errors kindly to the user somehow
public class Datamap {
  public final String rootId;
  public final List<Vertex> vertices;

  @JsonCreator
  public Datamap(
      @JsonProperty("rootId") String rootId,
      @JsonProperty("vertices") List<Vertex> vertices) {
    this.rootId = rootId;
    this.vertices =
        vertices.stream().sorted(Comparator.comparing(v -> v.id)).collect(Collectors.toList());
    Map<String, Vertex> id2Vertex = new HashMap<>();
    for (Vertex v : vertices) {
      if (id2Vertex.containsKey(v.id)) {
        throw new IllegalArgumentException(
            "The vertex ID " + v.id + " was used more than once. Vertex IDs must be unique.");
      }
      id2Vertex.put(v.id, v);
    }

    if (!id2Vertex.containsKey(rootId)) {
      throw new IllegalArgumentException(
          "rootId is " + rootId + " but no vertex with that ID can be found");
    }
    Vertex root = id2Vertex.get(rootId);
    //    TODO: this should probably be checked when we create the real datamap object, rather than
    // here
    if (root.type != Vertex.VertexType.SOAR_ID) {
      throw new IllegalArgumentException(
          "Root vertex must be of type "
              + Vertex.VertexType.SOAR_ID
              + ", but found "
              + root.type);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Datamap datamap = (Datamap) o;
    return Objects.equals(rootId, datamap.rootId)
        && Objects.equals(vertices, datamap.vertices);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rootId, vertices);
  }
}
