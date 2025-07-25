package edu.umich.soar.visualsoar.dialogs.searchdm;

import edu.umich.soar.visualsoar.dialogs.DialogUtils;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.datamap.DataMapTree;
import edu.umich.soar.visualsoar.datamap.FakeTreeNode;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.graph.*;
import edu.umich.soar.visualsoar.util.QueueAsLinkedList;
import edu.umich.soar.visualsoar.util.VSQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Dialog which searches a datamap for requested edges.
 *
 * @author Brian Harleton
 * @see DataMapTree
 * @see DataMap
 */
public class SearchDataMapDialog extends JDialog {
  private static final long serialVersionUID = 20221225L;

  private final SearchDataMapFindPanel findPanel = new SearchDataMapFindPanel();

  private final SearchDataMapOptionsPanel optionsPanel = new SearchDataMapOptionsPanel();

  private final DataMapTree dmt;
  private final SoarWorkingMemoryModel swmm;
  private final FakeTreeNode rootOfSearch;
  private String lastSearch = "";

  // Search variables
  private VSQueue<FakeTreeNode> queue;
  private boolean[] visitedVertices;

  /**
   * Constructor for the SearchDataMapDialog
   *
   * @param owner frame that owns this dialog window
   * @param tree DataMapTree that is being searched
   * @param rootNode the FakeTreeNode in the datamap that is currently selected and from which the
   *     search will begin at.
   */
  public SearchDataMapDialog(final Frame owner, DataMapTree tree, FakeTreeNode rootNode) {
    super(owner, "Search Datamap (" + rootNode.toString() + ")", false);

    dmt = tree;
    swmm = (MainFrame.getMainFrame()).getOperatorWindow().getDatamap();
    queue = new QueueAsLinkedList<>();
    rootOfSearch = rootNode;
    initializeSearch();

    setResizable(false);
    Container contentPane = getContentPane();
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    contentPane.setLayout(gridbag);

    c.gridwidth = GridBagConstraints.REMAINDER;
    c.fill = GridBagConstraints.HORIZONTAL;

    contentPane.add(findPanel, c);
    contentPane.add(optionsPanel, c);
    SearchDataMapButtonPanel buttonPanel = new SearchDataMapButtonPanel();
    contentPane.add(buttonPanel, c);
    pack();
    getRootPane().setDefaultButton(buttonPanel.findNextButton);

    DialogUtils.setUpDialogFocus(this, owner, findPanel.findField);

    addWindowListener(
        new WindowAdapter() {
          public void windowOpened(WindowEvent we) {
            setLocationRelativeTo(owner);
            findPanel.requestFocus();
          }
        });

    buttonPanel.cancelButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            dispose();
          }
        });

    buttonPanel.findNextButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Object[] theData = findPanel.getData();
            Boolean[] theOptions = optionsPanel.getData();

            String toFind = (String) theData[0];
            Boolean caseSensitive = (Boolean) theData[1];

            if (!toFind.equals(lastSearch)) {
              initializeSearch();
              lastSearch = toFind;
            }

            if (!caseSensitive) {
              toFind = toFind.toLowerCase();
            }

            FakeTreeNode foundftn = null;
            boolean edgeNotFound = true;
            int children;

            while ((!queue.isEmpty()) && edgeNotFound) {
              FakeTreeNode ftn = queue.dequeue();
              int rootValue = ftn.getEnumeratingVertex().getValue();
              visitedVertices[rootValue] = true;
              children = ftn.getChildCount();

              // See if current FakeTreeNode's edge match desired edge
              if (ftn.getEdge() != null) {
                SoarVertex enumVert = ftn.getEnumeratingVertex();
                if (enumVert instanceof ForeignVertex) {
                  enumVert = ((ForeignVertex) enumVert).getForeignSoarVertex();
                }
                if ((ftn.getEdge()).getName().equals(toFind)
                    && ((theOptions[0] && enumVert instanceof SoarIdentifierVertex)
                        || (theOptions[1] && enumVert instanceof EnumerationVertex)
                        || (theOptions[2] && enumVert instanceof StringVertex)
                        || (theOptions[3] && enumVert instanceof IntegerRangeVertex)
                        || (theOptions[4] && enumVert instanceof FloatRangeVertex))) {
                  edgeNotFound = false;
                  foundftn = ftn;
                }
              }

              // Examine children of ftn
              if ((children != 0) && edgeNotFound) {
                for (int i = 0; i < children; i++) {
                  FakeTreeNode childftn = ftn.getChildAt(i);
                  int vertexValue = childftn.getEnumeratingVertex().getValue();
                  if (!visitedVertices[vertexValue]) {
                    visitedVertices[vertexValue] = true;
                    queue.enqueue(childftn);
                  } // if never visited vertex
                  else {
                    // Check this edge since it won't be added to the queue
                    if (childftn.getEdge() != null) {
                      SoarVertex childEnumVert = ftn.getEnumeratingVertex();
                      if (childEnumVert instanceof ForeignVertex) {
                        childEnumVert = ((ForeignVertex) childEnumVert).getForeignSoarVertex();
                      }
                      if ((childftn.getEdge()).getName().equals(toFind)
                          && ((theOptions[0] && childEnumVert instanceof SoarIdentifierVertex)
                              || (theOptions[1] && childEnumVert instanceof EnumerationVertex)
                              || (theOptions[2] && childEnumVert instanceof StringVertex)
                              || (theOptions[3] && childEnumVert instanceof IntegerRangeVertex)
                              || (theOptions[4] && childEnumVert instanceof FloatRangeVertex))) {
                        edgeNotFound = false;
                        foundftn = childftn;
                      }
                    }
                  } // end of else already visited this vertex
                } // for checking all of ftn's children
              } // if ftn has children
              else if (children != 0) {
                // still add children to queue for possible continued searching
                for (int i = 0; i < children; i++) {
                  FakeTreeNode childftn = ftn.getChildAt(i);
                  int vertexValue = childftn.getEnumeratingVertex().getValue();
                  if (!visitedVertices[vertexValue]) {
                    visitedVertices[vertexValue] = true;
                    queue.enqueue(childftn);
                  } // if never visited vertex, enqueue
                }
              } // end of if edge found, still add children to queue for continued searching
            } // while queue is not empty, examine each vertex in it

            if (foundftn != null) {
              dmt.highlightEdge(foundftn);
            } else {
              JOptionPane.showMessageDialog(MainFrame.getMainFrame(), "No more instances found");
            }
          }
        });
  }

  /**
   * Initializes the search queue for a search of the datamap called when a new search dialog is
   * created or when the user changes the desired word
   */
  private void initializeSearch() {
    queue = new QueueAsLinkedList<>();
    int numberOfVertices = swmm.getNumberOfVertices();
    lastSearch = "";
    visitedVertices = new boolean[numberOfVertices];
    for (int i = 0; i < numberOfVertices; i++) {
      visitedVertices[i] = false;
    }
    queue.enqueue(rootOfSearch);
  }
} // end of SearchDataMapDialog class
