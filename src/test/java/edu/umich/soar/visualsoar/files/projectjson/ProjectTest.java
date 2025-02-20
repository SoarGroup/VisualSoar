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

import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProjectTest {
  private static Path sampleJsonPath;
//  TODO: get rid of this after we stop requiring number IDs
  private static Path sampleNumberedJsonPath;
  private static String sampleJsonRaw;
  private static String sampleNumberedJsonRaw;

  @BeforeAll
  public static void setup() throws URISyntaxException, IOException {
    sampleJsonPath = Paths.get(ProjectTest.class.getResource("sample.json").toURI());
    sampleJsonRaw = Files.readString(sampleJsonPath);
    sampleNumberedJsonPath =
        Paths.get(ProjectTest.class.getResource("sample_numbered_ids.json").toURI());
    sampleNumberedJsonRaw = Files.readString(sampleNumberedJsonPath);
  }

  // TODO: separate round-trip stringification and structure tests
  // TODO: more test JSON files to test separate things
  // TODO: test that parsing fails for duplicate keys
  // TODO: test that foreign vertices are validated (failure check)
  // TODO: test that enumchoices are validated (failure check)

  /**
   * Test that a read/write roundtrip of JSON (de)serialization is lossless.
   *
   * @throws IOException
   */
  @Test
  public void roundTripSerialization() throws IOException {
    Project originalProject = loadFromJson(new StringReader(sampleJsonRaw), Project.class);

    Path destination = Files.createTempFile("sample-roundtripped", ".json");
    writeJsonToFile(destination, originalProject);

    String roundTrippedRawJson = Files.readString(destination);
    Project roundTrippedProject =
      loadFromJson(new StringReader(roundTrippedRawJson), Project.class);

    assertEquals(sampleJsonRaw, roundTrippedRawJson);
    assertEquals(originalProject, roundTrippedProject);
  }
//  TODO: remove
  @Test
  public void roundTripNumberedSerialization() throws IOException {
    Project originalProject = loadFromJson(new StringReader(sampleNumberedJsonRaw), Project.class);

    Path destination = Files.createTempFile("sample-roundtripped", ".json");
    writeJsonToFile(destination, originalProject);

    String roundTrippedRawJson = Files.readString(destination);
    Project roundTrippedProject =
      loadFromJson(new StringReader(roundTrippedRawJson), Project.class);

    assertEquals(sampleNumberedJsonRaw, roundTrippedRawJson);
    assertEquals(originalProject, roundTrippedProject);
  }
//
//  @Test
//  public void roundTripSoarWorkingMemoryModel() throws IOException {
//    SoarWorkingMemoryModel swmm = SoarWorkingMemoryReader.loadSWMM(sampleJsonPath.toFile());
//    //    write to temp file, then read and compare just the datamap somehow.
//  }

  /**
   * Test that a round-trip serialization between JSON and V-S internal project representation is
   * lossless.
   */
  @Test
  public void roundTripOperatorWindow() throws IOException {
    OperatorWindow operatorWindow = new OperatorWindow("sample", sampleNumberedJsonPath, false);
    operatorWindow.openHierarchy(sampleNumberedJsonPath.toFile());
    Path tempDir = Files.createTempDirectory("roundTripOperatorWindow");
    operatorWindow.writeOutHierarchy(tempDir.resolve("sample.vsa").toFile(), null, null, true);
    String roundTrippedJson = Files.readString(tempDir.resolve("sample.vsa.json"));
    assertEquals(sampleNumberedJsonRaw, roundTrippedJson);
    //    SoarWorkingMemoryModel swmm = SoarWorkingMemoryReader.loadSWMM(sampleJsonPath.toFile());
  }
}
