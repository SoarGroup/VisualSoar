package edu.umich.soar.visualsoar.files.projectjson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class Project {
  public static final String SUPPORTED_VERSION = "6";

  public final String version;

  public final Datamap datamap;
  public final LayoutNode layout;

  public Project(Datamap datamap, LayoutNode layout) {
    this(SUPPORTED_VERSION, datamap, layout);
  }

  @JsonCreator
  public Project(
      @JsonProperty("version") String version,
      @JsonProperty("datamap") Datamap datamap,
      @JsonProperty("layout") LayoutNode layout) {
    Objects.requireNonNull(version, "Project 'version' cannot be null or missing.");
    Objects.requireNonNull(datamap, "Project 'datamap' cannot be null or missing.");
    Objects.requireNonNull(layout, "Project 'layout' cannot be null or missing.");
    this.version = version;
    this.datamap = datamap;
    this.layout = layout;

    if (!version.equals(SUPPORTED_VERSION)) {
      throw new IllegalArgumentException(
          "Only version " + SUPPORTED_VERSION + " is currently supported for this project format");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Project project = (Project) o;
    return version.equals(project.version)
        && Objects.equals(layout, project.layout)
        && Objects.equals(datamap, project.datamap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(datamap);
  }

  @Override
  public String toString() {
    try {
      return Json.serializeToJson(this);
    } catch (IOException e) {
      return "Failed to stringify Project: " + e;
    }
  }

  public static Project loadJsonFile(Path jsonPath) throws IOException {
    try (Reader fileReader = Files.newBufferedReader(jsonPath)) {
      return Json.loadFromJson(fileReader, Project.class);
    }
  }
}
