package edu.umich.soar.visualsoar.files.datamapjson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Comment {
  public final String contents;
  public final boolean generated;

  @JsonCreator
  public Comment(
      @JsonProperty("contents") String contents, @JsonProperty("generated") boolean generated) {
    this.contents = contents;
    this.generated = generated;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Comment comment = (Comment) o;
    return generated == comment.generated && Objects.equals(contents, comment.contents);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contents, generated);
  }
}
