package edu.umich.soar.visualsoar.files.projectjson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Project {
  public static final String SUPPORTED_VERSION = "6";

  public final String version;

  public final Datamap datamap;
  public final Layout layout;

  @JsonCreator
  public Project(
      @JsonProperty("version") String version,
      @JsonProperty("datamap") Datamap datamap,
      @JsonProperty("layout") Layout layout) {
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
    return Objects.equals(datamap, project.datamap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(datamap);
  }
}
