package edu.umich.soar.visualsoar.files.projectjson;

import static edu.umich.soar.visualsoar.files.projectjson.Json.loadFromJson;
import static edu.umich.soar.visualsoar.files.projectjson.Json.writeJsonToFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class ProjectTest {
  // TODO: more test JSON files to test separate things
  // TODO: test that parsing fails for duplicate keys
  // TODO: test that foreign vertices are validated (failure check)
  // TODO: test that enumchoices are validated (failure check)

  @Test
  public void roundTripTestFile() throws IOException, URISyntaxException {
    String originalRawJson =
        Files.readString(Paths.get(ProjectTest.class.getResource("sample.json").toURI()));
    Project originalProject = loadFromJson(new StringReader(originalRawJson), Project.class);

    Path destination = Files.createTempFile("sample-roundtripped", ".json");
    writeJsonToFile(destination, originalProject);

    String roundTrippedRawJson = Files.readString(destination);
    Project roundTrippedProject =
        loadFromJson(new StringReader(roundTrippedRawJson), Project.class);

    assertEquals(originalProject, roundTrippedProject);
    assertEquals(originalRawJson, roundTrippedRawJson);
  }
}
