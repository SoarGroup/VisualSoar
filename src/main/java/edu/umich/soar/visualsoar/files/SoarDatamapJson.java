package edu.umich.soar.visualsoar.files;

import static edu.umich.soar.visualsoar.files.Util.JSON_OBJECT_MAPPER;
import static edu.umich.soar.visualsoar.files.Util.saveToFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

@JsonDeserialize(builder = SoarDatamapJson.Builder.class)
public class SoarDatamapJson {

  private enum VertexType {
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
  public final VertexType vertexType;

  SoarDatamapJson(String id, VertexType vertexType) {
    this.id = id;
    this.vertexType = vertexType;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "with")
  static class Builder {
    private String id;
    private SoarDatamapJson.VertexType vertexType;

    public Builder withId(String id) {
      this.id = id;
      return this;
    }

    public Builder withVertexType(SoarDatamapJson.VertexType vertexType) {
      this.vertexType = vertexType;
      return this;
    }

    public SoarDatamapJson build() {
      return new SoarDatamapJson(id, vertexType);
    }
  }

  public static SoarDatamapJson loadFromJson(String json) throws JsonProcessingException {
    return JSON_OBJECT_MAPPER.readValue(json, SoarDatamapJson.class);
  }

  private static class JsonWriter implements Util.Writer {
    private final SoarDatamapJson json;

    private JsonWriter(SoarDatamapJson json) {
      this.json = json;
    }

    @Override
    public void write(OutputStream out) throws IOException {
      JSON_OBJECT_MAPPER.writerFor(SoarDatamapJson.class).writeValue(out, json);
    }
  }

  public static void writeJsonToFile(Path destination, SoarDatamapJson json) throws IOException {
    saveToFile(destination, new JsonWriter(json));
  }

  public static void main(String[] args) throws IOException {
    SoarDatamapJson json =
        SoarDatamapJson.loadFromJson("{\"id\":\"XYZ\", \"vertexType\":\"FLOAT\"}");
    System.out.println(json.id);
    System.out.println(json.vertexType);
    assert json.id.equals("XYZ");
    assert json.vertexType.equals(VertexType.FLOAT);
    Path destination = Paths.get("sample-output.json");
    writeJsonToFile(destination, json);
//    TODO: NEXT: output file contains empty object!
    System.err.println("Please inspect " + destination + " for JSON output");
  }
}
