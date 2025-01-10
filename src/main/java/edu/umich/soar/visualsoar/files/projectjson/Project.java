package edu.umich.soar.visualsoar.files.projectjson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Project {
  public final String version;

  public final Datamap datamap;
  public final Layout layout;

  @JsonCreator
  public Project(
      @JsonProperty("version") String version,
      @JsonProperty("datamap") Datamap datamap,
      @JsonProperty("layout") Layout layout) {
    this.version = version;
    this.datamap = datamap;
    this.layout = layout;
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
