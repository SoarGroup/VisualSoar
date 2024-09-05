package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.graph.*;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;
import java.util.function.Function;
import java.util.stream.Stream;

import static edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryReader.readVertex;
import static edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryReader.readVertexSafe;
import static org.junit.jupiter.api.Assertions.*;

class SoarWorkingMemoryReaderTest {
  private <T> Iterable<T> getIterableFromIterator(Iterator<T> iterator) {
    return () -> iterator;
  }

  private static Stream<Arguments> provideVertexParsers() {
    Function<String, SoarVertex> oldParser =
        (String line) -> {
          StringReader reader = new StringReader(line);
          try {
            return readVertex(reader);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        };
    Function<String, SoarVertex> newParser =
        (String line) -> readVertexSafe(line, -1, new Vector<>());

    return Stream.of(
        Arguments.of(oldParser, "Old parser"), Arguments.of(newParser, "New (safe) parser"));
  }

  @Test
  public void parseFailsWithUnknownVertexType() {
    String line = "HELLO 87 foo";
    Vector<FeedbackListEntry> errors = new Vector<>();
    SoarVertex vertex = readVertexSafe(line, -1, errors);
    assertNull(vertex);
    assertEquals(1, errors.size());
    assertTrue(
        errors.get(0).getMessage().contains("datamap entry has invalid type"),
        "Incorrect message found: " + errors.get(0).getMessage());
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("provideVertexParsers")
  public void parseForeignVertex(Function<String, SoarVertex> parser, String parserName) {
    String line =
        "FOREIGN 99 my-foreign-dm ENUMERATION 87 5 tie conflict constraint-failure no-change |bye-bye \t birdie|";
    SoarVertex vertex = parser.apply(line);

    assertInstanceOf(ForeignVertex.class, vertex);
    ForeignVertex foreignVertex = (ForeignVertex) vertex;
    assertEquals(99, foreignVertex.getValue());
    assertEquals("my-foreign-dm", foreignVertex.getForeignDMName());

    assertInstanceOf(EnumerationVertex.class, foreignVertex.getCopyOfForeignSoarVertex());
    EnumerationVertex enumVertex = (EnumerationVertex) foreignVertex.getCopyOfForeignSoarVertex();
    assertEquals(87, enumVertex.getValue());
    assertIterableEquals(
        Arrays.asList("tie", "conflict", "constraint-failure", "no-change", "|bye-bye \t birdie|"),
        getIterableFromIterator(enumVertex.getEnumeration()));
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("provideVertexParsers")
  public void parseSoarIdVertex(Function<String, SoarVertex> parser, String parserName) {
    String line = "SOAR_ID 42";
    SoarVertex vertex = parser.apply(line);

    assertInstanceOf(SoarIdentifierVertex.class, vertex);
    SoarIdentifierVertex soarIdentifierVertex = (SoarIdentifierVertex) vertex;
    assertEquals(42, soarIdentifierVertex.getValue());
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("provideVertexParsers")
  public void parseEnumVertexNoSpaces(Function<String, SoarVertex> parser, String parserName) {
    String line = "ENUMERATION 87 4 tie conflict constraint-failure no-change";
    SoarVertex vertex = parser.apply(line);
    assertInstanceOf(EnumerationVertex.class, vertex);
    EnumerationVertex enumVertex = (EnumerationVertex) vertex;
    assertEquals(87, enumVertex.getValue());
    assertIterableEquals(
        Arrays.asList("tie", "conflict", "constraint-failure", "no-change"),
        getIterableFromIterator(enumVertex.getEnumeration()));
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("provideVertexParsers")
  public void parseEnumVertexWithSpaces(Function<String, SoarVertex> parser, String parserName) {
    // This was a malformed line that we found triggered exceptions. The parsed enum
    // values don't make a lot of sense, but we'll test the current behavior here for
    // documentation and to aid further iteration.
    String line = "ENUMERATION 334 4 |the |the |the |the  potato is cooked|";
    SoarVertex vertex = parser.apply(line);
    assertInstanceOf(EnumerationVertex.class, vertex);
    EnumerationVertex enumVertex = (EnumerationVertex) vertex;
    assertEquals(334, enumVertex.getValue());
    assertIterableEquals(
        // loses that last value!
        Arrays.asList("|the |", "the", "|the |", "the"),
        getIterableFromIterator(enumVertex.getEnumeration()));
  }

  // TODO: parseEnumVertexWithSpacesAndVerticalBars (tests escaping of vertical bars)

  @ParameterizedTest(name = "{1}")
  @MethodSource("provideVertexParsers")
  public void parseIntegerRangeVertex(Function<String, SoarVertex> parser, String parserName) {
    String line = "INTEGER_RANGE 42 -1 202";
    SoarVertex vertex = parser.apply(line);

    assertInstanceOf(IntegerRangeVertex.class, vertex);
    IntegerRangeVertex intRangeVertex = (IntegerRangeVertex) vertex;
    assertEquals(42, intRangeVertex.getValue());
    assertEquals(-1, intRangeVertex.getLow());
    assertEquals(202, intRangeVertex.getHigh());
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("provideVertexParsers")
  public void parseIntegerVertex(Function<String, SoarVertex> parser, String parserName) {
    String line = "INTEGER 35";
    SoarVertex vertex = parser.apply(line);

    assertInstanceOf(IntegerRangeVertex.class, vertex);
    IntegerRangeVertex intRangeVertex = (IntegerRangeVertex) vertex;
    assertEquals(35, intRangeVertex.getValue());
    assertEquals(Integer.MIN_VALUE, intRangeVertex.getLow());
    assertEquals(Integer.MAX_VALUE, intRangeVertex.getHigh());
  }

  // FLOAT_RANGE
  @ParameterizedTest(name = "{1}")
  @MethodSource("provideVertexParsers")
  public void parseFloatRangeVertex(Function<String, SoarVertex> parser, String parserName) {
    String line = "FLOAT_RANGE 42 -27.06 Infinity";
    SoarVertex vertex = parser.apply(line);

    assertInstanceOf(FloatRangeVertex.class, vertex);
    FloatRangeVertex floatRangeVertex = (FloatRangeVertex) vertex;
    assertEquals(42, floatRangeVertex.getValue());
    assertEquals(-27.06, floatRangeVertex.getLow(), 0.0001);
    assertEquals(Float.POSITIVE_INFINITY, floatRangeVertex.getHigh());
  }

  // FLOAT
  @ParameterizedTest(name = "{1}")
  @MethodSource("provideVertexParsers")
  public void parseFloatVertex(Function<String, SoarVertex> parser, String parserName) {
    String line = "FLOAT 76";
    SoarVertex vertex = parser.apply(line);

    assertInstanceOf(FloatRangeVertex.class, vertex);
    FloatRangeVertex floatRangeVertex = (FloatRangeVertex) vertex;
    assertEquals(76, floatRangeVertex.getValue());
    assertEquals(Float.NEGATIVE_INFINITY, floatRangeVertex.getLow());
    assertEquals(Float.POSITIVE_INFINITY, floatRangeVertex.getHigh());
  }

  // STRING
  @ParameterizedTest(name = "{1}")
  @MethodSource("provideVertexParsers")
  public void parseStringVertex(Function<String, SoarVertex> parser, String parserName) {
    String line = "STRING 32";
    SoarVertex vertex = parser.apply(line);

    assertInstanceOf(StringVertex.class, vertex);
    StringVertex stringVertex = (StringVertex) vertex;
    assertEquals(32, stringVertex.getValue());
  }
}
