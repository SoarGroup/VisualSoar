package edu.umich.soar.visualsoar.files.datamapjson;

import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static edu.umich.soar.visualsoar.files.datamapjson.Datamap.writeJsonToFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DatamapTest {
  // TODO: more test JSON files to test separate things
  // TODO: test that parsing fails for duplicate keys
  // TODO: test that foreign vertices are validated (failure check)

  @Test
  public void roundTripTestFile() throws IOException, URISyntaxException {
    String originalRawJson =
        Files.readString(Paths.get(DatamapTest.class.getResource("sample.json").toURI()));
    Datamap originalDatamap = Datamap.loadFromJson(new StringReader(originalRawJson));

    Path destination = Files.createTempFile("sample-roundtripped", ".json");
    writeJsonToFile(destination, originalDatamap);

    String roundTrippedRawJson = Files.readString(destination);
    Datamap roundTrippedDatamap = Datamap.loadFromJson(new StringReader(roundTrippedRawJson));

    assertEquals(originalDatamap, roundTrippedDatamap);
    assertEquals(originalRawJson, roundTrippedRawJson);
  }
}
