package edu.umich.soar.visualsoar.files;

import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_JAVA_COMMENTS;
import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS;
import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS;
import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS;
import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS;
import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_SINGLE_QUOTES;
import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_TRAILING_COMMA;
import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS;
import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class Util {
  private static final Logger LOGGER = Logger.getLogger(Util.class.getName());
  public static final JsonFactory JSON_FACTORY =
      JsonFactory.builder()
          // configure, if necessary:
          .enable(ALLOW_JAVA_COMMENTS)
          .enable(ALLOW_UNQUOTED_FIELD_NAMES)
          .enable(ALLOW_TRAILING_COMMA)
          .enable(ALLOW_SINGLE_QUOTES)
          // TODO: are we sure that NaN's could be used for anything?
          .enable(ALLOW_NON_NUMERIC_NUMBERS)
          .enable(ALLOW_LEADING_ZEROS_FOR_NUMBERS)
          .enable(ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
          .enable(ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
          .enable(ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
          .build();

  //  TODO: ensure array values are all on separate lines
  //  TODO: ensure always uses \n to terminate lines (or use
  // https://github.com/FasterXML/jackson-databind/issues/585#issuecomment-643163524)
  public static final ObjectMapper JSON_OBJECT_MAPPER =
      JsonMapper.builder(JSON_FACTORY)
          .enable(INDENT_OUTPUT)
          .disable(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature())
          .enable(ORDER_MAP_ENTRIES_BY_KEYS)
          .enable(SORT_PROPERTIES_ALPHABETICALLY)
          .defaultPrettyPrinter(
              new DefaultPrettyPrinter() {
                @Override
                public DefaultPrettyPrinter createInstance() {
                  return this;
                }

                @Override
                public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException {
                  super.writeEndObject(g, nrOfEntries);
                  //                  place newline at end of file
                  if (g.getOutputContext().getNestingDepth() == 1) {
                    g.writeRaw('\n');
                  }
                }

                @Override
                public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException {
                  super.writeEndArray(g, nrOfValues);
                  //                  place newline at end of file
                  if (g.getOutputContext().getNestingDepth() == 1) {
                    g.writeRaw('\n');
                  }
                }
              })
          .build();

  @FunctionalInterface
  public interface Writer {
    void write(OutputStream out) throws IOException;
  }

  //  TODO: API is clumsy. Can we implement a streaming writer where the close method does the
  // atomic rename instead?
  /** Write to the destination file from {@code writer} as atomically as possible. */
  public static void saveToFile(Path destination, Writer writer) throws java.io.IOException {
    // Write to a temp file first, then make the final changes via a rename,
    // which can often be done atomically. This prevents issues such as overwriting a file with a
    // incomplete data, as could be the case if we hit an IO exception in the middle of writing the
    // file (especially relevant for folks using network drives!)
    String tempFilename = destination.toAbsolutePath() + ".temp";
    Path tempPath = Paths.get(tempFilename);

    try (FileOutputStream output = new FileOutputStream(tempPath.toFile())) {
      writer.write(output);
    }

    try {
      Files.move(tempPath, destination, REPLACE_EXISTING, ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      LOGGER.warning(
          "Cannot write "
              + destination
              + " atomically  ("
              + e.getMessage()
              + "); falling back to non-atomic write");
      Files.move(tempPath, destination, REPLACE_EXISTING);
    }
  }
}
