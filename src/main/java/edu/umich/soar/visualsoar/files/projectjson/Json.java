package edu.umich.soar.visualsoar.files.projectjson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.umich.soar.visualsoar.files.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static com.fasterxml.jackson.core.StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION;
import static com.fasterxml.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION;
import static com.fasterxml.jackson.core.json.JsonReadFeature.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;
import static edu.umich.soar.visualsoar.files.Util.saveToFile;

public class Json {

  // standardize on one EOL for all platforms to avoid commit noise
  private static final String EOL = "\n";
  private static final JsonFactory JSON_FACTORY =
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
          .enable(STRICT_DUPLICATE_DETECTION)
          .enable(INCLUDE_SOURCE_IN_LOCATION)
          .build();
  private static final ObjectMapper JSON_OBJECT_MAPPER =
      JsonMapper.builder(JSON_FACTORY)
          .enable(INDENT_OUTPUT)
          .enable(FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(FAIL_ON_READING_DUP_TREE_KEY)
          .disable(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature())
          .enable(ORDER_MAP_ENTRIES_BY_KEYS)
          .enable(SORT_PROPERTIES_ALPHABETICALLY)
          .serializationInclusion(JsonInclude.Include.NON_NULL)
          .defaultPrettyPrinter(new CustomPrettyPrinter())
          .build();

  private static class CustomPrettyPrinter extends DefaultPrettyPrinter {
    public CustomPrettyPrinter() {
      super();
      this.indentObjectsWith(new DefaultIndenter("  ", EOL));
      this.indentArraysWith(new DefaultIndenter("  ", EOL));
    }

    @Override
    public DefaultPrettyPrinter createInstance() {
      return new CustomPrettyPrinter();
    }

    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator g) throws IOException {
      g.writeRaw(": ");
    }

    @Override
    public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException {
      super.writeEndObject(g, nrOfEntries);
      // place newline at end of file
      if (g.getOutputContext().getNestingDepth() == 1) {
        g.writeRaw(EOL);
      }
    }

    @Override
    public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException {
      super.writeEndArray(g, nrOfValues);
      // place newline at end of file
      if (g.getOutputContext().getNestingDepth() == 1) {
        g.writeRaw(EOL);
      }
    }
  }

  public static <T> T loadFromJson(Reader src, Class<T> clazz) throws IOException {
    return JSON_OBJECT_MAPPER.readValue(src, clazz);
  }

  private static class JsonWriter<T> implements Util.Writer {
    private final T toWrite;

    private JsonWriter(T toWrite) {
      this.toWrite = toWrite;
    }

    @Override
    public void write(OutputStream out) throws IOException {
      try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
        JSON_OBJECT_MAPPER.writerFor(toWrite.getClass()).writeValue(writer, toWrite);
      }
    }
  }

  public static <T> void writeJsonToFile(Path destination, T toWrite) throws IOException {
    saveToFile(destination, new JsonWriter<T>(toWrite));
  }
}
