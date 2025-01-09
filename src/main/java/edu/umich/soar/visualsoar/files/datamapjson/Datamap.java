package edu.umich.soar.visualsoar.files.datamapjson;

import static edu.umich.soar.visualsoar.files.Util.JSON_OBJECT_MAPPER;
import static edu.umich.soar.visualsoar.files.Util.saveToFile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umich.soar.visualsoar.files.Util;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

// TODO: create custom exception for document validation
// TODO: make sure to present JSON parsing errors kindly to the user somehow
public class Datamap {
  public final String version;
  public final String rootId;
  public final Map<String, Vertex> vertices;

  @JsonCreator
  public Datamap(
      @JsonProperty("version") String version,
      @JsonProperty("rootId") String rootId,
      @JsonProperty("vertices") Map<String, Vertex> vertices) {
    this.version = version;
    this.rootId = rootId;
    this.vertices = vertices;

    if (!vertices.containsKey(rootId)) {
      throw new IllegalArgumentException(
          "rootId is " + rootId + " but no vertex with that ID can be found");
    }
    Vertex root = vertices.get(rootId);
//    TODO: this should probably be checked when we create the real datamap object, rather than here
    if (root.vertexType != Vertex.VertexType.SOAR_ID) {
      throw new IllegalArgumentException(
          "Root vertex must be of type "
              + Vertex.VertexType.SOAR_ID
              + ", but found "
              + root.vertexType);
    }
  }

  public static Datamap loadFromJson(Reader src) throws IOException {
    return JSON_OBJECT_MAPPER.readValue(src, Datamap.class);
  }

  private static class JsonWriter implements Util.Writer {
    private final Datamap json;

    private JsonWriter(Datamap json) {
      this.json = json;
    }

    @Override
    public void write(OutputStream out) throws IOException {
      try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
        JSON_OBJECT_MAPPER.writerFor(Datamap.class).writeValue(writer, json);
      }
    }
  }

  public static void writeJsonToFile(Path destination, Datamap json) throws IOException {
    saveToFile(destination, new JsonWriter(json));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Datamap datamap = (Datamap) o;
    return Objects.equals(version, datamap.version)
        && Objects.equals(rootId, datamap.rootId)
        && Objects.equals(vertices, datamap.vertices);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, rootId, vertices);
  }

  //  TODO NEXT: Continue reading SoarWorkingMemoryReader and implementing checks and parsings. Next is foreign vertex.
}
