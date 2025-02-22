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

import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.operatorwindow.OperatorWindow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProjectTest {
  private static Path sampleJsonPath;
  private static String sampleJsonRaw;

  @BeforeAll
  public static void setup() throws URISyntaxException, IOException {
    sampleJsonPath = Paths.get(ProjectTest.class.getResource("sample.json").toURI());
    // line-ending fix required in Windows for some reason
    sampleJsonRaw = Files.readString(sampleJsonPath).replaceAll("\r\n", "\n");
  }

  /**
   * Test that a read/write round-trip of JSON (de)serialization is lossless.
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

    assertEquals(
        sampleJsonRaw,
        roundTrippedRawJson,
        "Expected:\n" + sampleJsonRaw + "\nActual: " + roundTrippedRawJson);
    assertEquals(originalProject, roundTrippedProject);
  }

  /**
   * Test that a round-trip serialization between JSON and V-S internal project representation is
   * lossless.
   */
  @Test
  public void roundTripOperatorWindow() throws IOException {
    MainFrame.setMainFrame(new MainFrame("Test"));
    MainFrame.getMainFrame().openProject(sampleJsonPath.toFile(), false);
    Path tempDir = Files.createTempDirectory("roundTripOperatorWindow");
    OperatorWindow.getOperatorWindow()
        .writeOutHierarchy(
            tempDir.resolve("sample.vsa").toFile()
        );
    String roundTrippedJson = Files.readString(tempDir.resolve("sample.vsa.json"));
    assertEquals(
        sampleJsonRaw,
        roundTrippedJson,
        "Expected:\n" + sampleJsonRaw + "\nActual: " + roundTrippedJson);
  }
}
