package edu.umich.soar.visualsoar.ruleeditor;

import edu.umich.soar.visualsoar.components.AutocompleteContext;
import edu.umich.soar.visualsoar.datamap.DataMapMatcher;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.graph.EnumerationVertex;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import edu.umich.soar.visualsoar.parser.ParseException;
import edu.umich.soar.visualsoar.parser.SoarParser;
import edu.umich.soar.visualsoar.parser.SoarProduction;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SoarAutocomplete {

  /**
   * Populate an auto-complete context for the document at the given position.
   *
   * @param periodMustBeMostSignificant
   * @param caretPos current caret position
   * @param document entire document text
   * @return the constructed context, or null if no heuristic construction was possible or there was
   *     a parse error
   */
  @Nullable
  public static AutocompleteContext getAutocompleteContext(
      boolean periodMustBeMostSignificant,
      int caretPos,
      String document,
      OperatorNode associatedNode) {
    int sp_pos = document.lastIndexOf("sp ", caretPos);
    if (sp_pos == -1) {
      return null;
    }
    String prodSoFar = document.substring(sp_pos, caretPos);
    int arrowPos = prodSoFar.indexOf("-->");
    String end;
    if (arrowPos == -1) {
      end = ") --> }";
    } else {
      end = " <$$$>)}";
    }
    int caret = prodSoFar.lastIndexOf("^");
    int period = prodSoFar.lastIndexOf(".");
    int space = prodSoFar.lastIndexOf(" ");

    try {
      AutocompleteContext autocompleteContext;
      // The most relevant is the caret
      if (!periodMustBeMostSignificant
              && (period == -1 && caret != -1 && space != -1 && caret > space)
          || (period != -1 && caret != -1 && space != -1 && period < caret && space < caret)) {
        String userType = prodSoFar.substring(caret + 1);
        prodSoFar = prodSoFar.substring(0, caret + 1) + "<$$>" + end;
        return SoarAutocomplete.attributeComplete(userType, prodSoFar, document, associatedNode);
      }
      // The most relevant is the period
      else if (period != -1 && caret != -1 && space != -1 && period > caret && period > space) {
        String userType = prodSoFar.substring(period + 1);
        prodSoFar = prodSoFar.substring(0, period + 1) + "<$$>" + end;
        return SoarAutocomplete.attributeComplete(userType, prodSoFar, document, associatedNode);
      }
      // The most relevant is the space
      else if (!periodMustBeMostSignificant
              && (period == -1 && caret != -1 && space != -1 && space > caret)
          || (period != -1 && caret != -1 && space != -1 && space > caret && space > period)) {
        String userType = prodSoFar.substring(space + 1);
        prodSoFar = prodSoFar.substring(0, space + 1) + "<$$>" + end;
        return SoarAutocomplete.valueComplete(userType, prodSoFar, document, associatedNode);
      }
      // Failure
      else {
        return null;
      }
    } catch (ParseException e) {
      return null;
    }
  }

  private static AutocompleteContext valueComplete(
      String userType, String prodSoFar, String fullText, OperatorNode associatedNode)
      throws ParseException {
    List<String> completeMatches = getValueMatches(prodSoFar, fullText, associatedNode);
    return new AutocompleteContext(userType, completeMatches);
  }

  private static AutocompleteContext attributeComplete(
      String userType, String prodSoFar, String fullText, OperatorNode associatedNode)
      throws ParseException {
    List<String> completeMatches = getAttributeMatches(prodSoFar, fullText, associatedNode);
    return new AutocompleteContext(userType, completeMatches);
  }

  /**
   * Retrieves the strings associated with entries in the datamap with attributes that match the
   * user's current production.
   *
   * @param prodSoFar The content of the production so far
   * @param fullText editorPane.getText()
   * @param associatedNode the node the rule editor is associated with
   * @return a list of possible completions (could be empty)
   */
  private static List<String> getAttributeMatches(
      String prodSoFar, String fullText, OperatorNode associatedNode) throws ParseException {
    // parse the code the user has written so far
    prodSoFar = makeStringValidForParser(prodSoFar, fullText);
    SoarParser soarParser = new SoarParser(new StringReader(prodSoFar));
    SoarProduction sp = soarParser.soarProduction();

    // Find all matching string via the datamap
    List<DataMapMatcher.Match> matches;
    SoarWorkingMemoryModel dataMap = MainFrame.getMainFrame().getOperatorWindow().getDatamap();
    SoarIdentifierVertex siv =
        ((OperatorNode) associatedNode.getParent()).getStateIdVertex(dataMap);
    if (siv != null) {
      matches = dataMap.matches(siv, sp, "<$$>");
    } else {
      matches = dataMap.matches(dataMap.getTopstate(), sp, "<$$>");
    }
    List<String> completeMatches = new LinkedList<>();
    for (DataMapMatcher.Match match : matches) {
      completeMatches.add(match.toString());
    }
    Collections.sort(completeMatches);
    return completeMatches;
  }

  /**
   * Retrieves the strings associated with entries in the datamap with values that match the user's
   * current production.
   *
   * @param prodSoFar
   * @param associatedNode the node the rule editor is associated with
   * @param fullText editorPane.getText()
   */

  // TODO: here we should also suggest a <variable> with the name of its matched attribute
  // TODO: It should not be suggested if the user has typed something that doesn't start with <
  private static List<String> getValueMatches(
      String prodSoFar, String fullText, OperatorNode associatedNode) throws ParseException {
    SoarWorkingMemoryModel dataMap = MainFrame.getMainFrame().getOperatorWindow().getDatamap();
    prodSoFar = makeStringValidForParser(prodSoFar, fullText);
    SoarParser soarParser = new SoarParser(new StringReader(prodSoFar));
    SoarProduction sp = soarParser.soarProduction();
    OperatorNode parent = (OperatorNode) associatedNode.getParent();
    List<DataMapMatcher.Match> matches;
    SoarIdentifierVertex siv = parent.getStateIdVertex(dataMap);
    if (siv != null) {
      matches = dataMap.matches(siv, sp, "<$$>");
    } else {
      matches = dataMap.matches(dataMap.getTopstate(), sp, "<$$>");
    }
    List<String> completeMatches = new LinkedList<>();
    for (DataMapMatcher.Match match : matches) {
      if (match.getVertex() instanceof EnumerationVertex) {
        EnumerationVertex ev = (EnumerationVertex) match.getVertex();
        Iterator<String> iter = ev.getEnumeration();
        while (iter.hasNext()) {
          String enumString = iter.next();
          completeMatches.add(enumString);
        }
      }
    }

    Collections.sort(completeMatches);
    return completeMatches;
  }

  /** Same as above but for a given string */
  private static String makeStringValidForParser(String prod, String fullText) {
    int pound = fullText.lastIndexOf("#");
    int nl = fullText.lastIndexOf("\n");
    if ((pound != -1) && (nl < pound)) {
      prod += "\n";
    }
    return prod;
  }
}
