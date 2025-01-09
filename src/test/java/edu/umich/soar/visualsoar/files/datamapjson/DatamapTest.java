package edu.umich.soar.visualsoar.files.datamapjson;

import static edu.umich.soar.visualsoar.files.datamapjson.Json.loadFromJson;
import static edu.umich.soar.visualsoar.files.datamapjson.Json.writeJsonToFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class DatamapTest {
  // TODO: more test JSON files to test separate things
  // TODO: test that parsing fails for duplicate keys
  // TODO: test that foreign vertices are validated (failure check)
  // TODO: test that enumchoices are validated (failure check)

  @Test
  public void roundTripTestFile() throws IOException, URISyntaxException {
    String originalRawJson =
        Files.readString(Paths.get(DatamapTest.class.getResource("sample.json").toURI()));
    Datamap originalDatamap = loadFromJson(new StringReader(originalRawJson), Datamap.class);

    Path destination = Files.createTempFile("sample-roundtripped", ".json");
    writeJsonToFile(destination, originalDatamap);

    String roundTrippedRawJson = Files.readString(destination);
    Datamap roundTrippedDatamap =
        loadFromJson(new StringReader(roundTrippedRawJson), Datamap.class);

    assertEquals(originalDatamap, roundTrippedDatamap);
    assertEquals(originalRawJson, roundTrippedRawJson);
  }
}
