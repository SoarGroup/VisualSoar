package edu.umich.soar.visualsoar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class VisualSoarTest {

  @Test
  void testMainMethodHelpOption() {
    // Arrange
    String[] args = {"--help"};
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(outputStream));

    try {
      // Act
      VisualSoar.main(args);

      // Assert
      String output = outputStream.toString().replace("\r\n", "\n").trim();
      assertTrue(output.contains("usage: VisualSoar"), "Output should contain usage information");
    } catch (Exception e) {
      fail("Exception occurred while running the main method: " + e.getMessage());
    } finally {
      // Cleanup
      System.setOut(originalOut);
    }
  }

  @Test
  void testMainMethodCheckProductionsAgainstDatamap() throws ParseException, URISyntaxException {
    /**
     * Mock out {@link VisualSoar#main(String[])}'s call to System.exit, which is wrapped in {@link
     * VisualSoar#systemExit(int)}. If you don't do this, then the test cannot complete, because we
     * would exit before we could check the results!
     */
    try (MockedStatic<VisualSoar> mockedVisualSoar =
        mockStatic(
            VisualSoar.class,
            invocation -> {
              if (invocation.getMethod().getName().equals("systemExit")) {
                return null;
              }
              return invocation.callRealMethod();
            })) {

      Path noErrorProjectPath =
          Path.of(getClass().getResource("test_projects/no-datamap-errors").toURI());
      Path errorProjectPath =
          Path.of(getClass().getResource("test_projects/has-datamap-errors").toURI());

      // redirect stdout so we can verify what is written
      PrintStream originalOut = System.out;
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      System.setOut(new PrintStream(outputStream));
      try {
        // First, test output for a project with no DM issues
        VisualSoar.main(
            new String[] {
              "--check",
              "productionsAgainstDatamap",
              "--project",
              noErrorProjectPath.resolve("no-datamap-errors.vsa.json").toString()
            });

        String actualOutput = outputStream.toString().replace("\r\n", "\n").trim();
        String expectedOutput = "✅ No datamap issues found!";
        assertEquals(expectedOutput, actualOutput);

        outputStream.reset();

        // Again, with JSON-formatted output
        VisualSoar.main(
            new String[] {
              "--check",
              "productionsAgainstDatamap",
              "--project",
              noErrorProjectPath.resolve("no-datamap-errors.vsa.json").toString(),
              "--json"
            });

        actualOutput = outputStream.toString().replace("\r\n", "\n").trim();
        expectedOutput =
            "{\"message\": \"✅ No datamap issues found!\", \"severity\": 3, \"source\": \"VisualSoar\"}";
        assertEquals(expectedOutput, actualOutput);

        outputStream.reset();

        // Second, test project with DM errors
        VisualSoar.main(
            new String[] {
              "--check",
              "productionsAgainstDatamap",
              "--project",
              errorProjectPath.resolve("has-datamap-errors.vsa.json").toString()
            });
        mockedVisualSoar.verify(() -> VisualSoar.systemExit(1)); // Verify systemExit(1) was called

        // normalize to \ for simplicity here
        actualOutput = outputStream.toString().replace("\r\n", "\n").replace("/", "\\").trim();
        expectedOutput =
            "❌ propose*initialize-has-datamap-errors: initialize-has-datamap-errors(6): could not match constraint (<o>,name,wrong-name) in production\n"
                + "❌ Unable to check productions due to parse error\n"
                + "❌ elaborations\\top-state(10): parser.ParseException: Encountered \" <VARIABLE> \"<op> \"\" at line 10, column 4.\n"
                + "Was expecting:\n"
                + "    \"-->\" ...";
        assertEquals(expectedOutput, actualOutput);

        outputStream.reset();
        mockedVisualSoar.reset();

        // Again, with JSON-formatted output
        VisualSoar.main(
            new String[] {
              "--check",
              "productionsAgainstDatamap",
              "--project",
              errorProjectPath.resolve("has-datamap-errors.vsa.json").toString(),
              "--json"
            });
        mockedVisualSoar.verify(() -> VisualSoar.systemExit(1)); // Verify systemExit(1) was called

        actualOutput = outputStream.toString().replace("\r\n", "\n").replace("\\r\\n", "\\n").trim();
        expectedOutput =
            ("{\"message\": \"Operator node diagnostic\", \"severity\": 1, \"relatedInformation\": [{\"message\": \"could not match constraint (<o>,name,wrong-name) in production\", \"location\": {\"uri\": \"file://"
                    + jsonPathString(
                        errorProjectPath.resolve(
                            "has-datamap-errors/initialize-has-datamap-errors.soar"))
                    + "\", \"range\": {\"start\": {\"line\": 6, \"character\": 0}, \"end\": {\"line\": 6, \"character\": 0}}}}], \"source\": \"VisualSoar\"}\n"
                    + "{\"message\": \"Unable to check productions due to parse error\", \"severity\": 1, \"source\": \"VisualSoar\"}\n"
                    + "{\"message\": \"Operator node diagnostic\", \"severity\": 1, \"relatedInformation\": [{\"message\": \"parser.ParseException: Encountered \\\" <VARIABLE> \\\"<op> \\\"\\\" at line 10, column 4.\\nWas expecting:\\n    \\\"-->\\\" ...\\n    \", \"location\": {\"uri\": \"file://"
                    + jsonPathString(
                        errorProjectPath.resolve("has-datamap-errors/elaborations/top-state.soar"))
                    + "\", \"range\": {\"start\": {\"line\": 10, \"character\": 0}, \"end\": {\"line\": 10, \"character\": 0}}}}], \"source\": \"VisualSoar\"}")
                .replace("\r\n", "\n")
                .trim();

        assertEquals(expectedOutput, actualOutput);

      } finally {
        System.setOut(originalOut);
      }
    }
  }

  private static String jsonPathString(Path path) {
    return String.valueOf(
        JsonStringEncoder.getInstance().quoteAsString(path.toAbsolutePath().toString()));
  }
}
