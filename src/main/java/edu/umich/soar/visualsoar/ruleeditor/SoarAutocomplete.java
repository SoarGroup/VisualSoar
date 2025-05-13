package edu.umich.soar.visualsoar.ruleeditor;

import edu.umich.soar.visualsoar.datamap.DataMapMatcher;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.graph.EnumerationVertex;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import edu.umich.soar.visualsoar.parser.ParseException;
import edu.umich.soar.visualsoar.parser.SoarParser;
import edu.umich.soar.visualsoar.parser.SoarProduction;

import java.io.StringReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SoarAutocomplete {

  /**
   * Retrieves the strings associated with entries in the datamap with attributes that match the
   * user's current production.
   *
   * @param prodSoFar The content of the production so far
   * @param fullText editorPane.getText()
   * @param associatedNode the node the rule editor is associated with
   * @return a list of possible completions (could be empty)
   */
  public static List<String> getAttributeMatches(
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
  public static List<String> getValueMatches(
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
