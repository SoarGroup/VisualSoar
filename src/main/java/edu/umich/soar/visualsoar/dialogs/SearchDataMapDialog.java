package edu.umich.soar.visualsoar.dialogs;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.datamap.DataMapTree;
import edu.umich.soar.visualsoar.datamap.FakeTreeNode;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.graph.EnumerationVertex;
import edu.umich.soar.visualsoar.graph.FloatRangeVertex;
import edu.umich.soar.visualsoar.graph.IntegerRangeVertex;
import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.graph.StringVertex;
import edu.umich.soar.visualsoar.util.QueueAsLinkedList;
import edu.umich.soar.visualsoar.util.VSQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.*;

/**
 * Dialog which searches a datamap for requested edges.
 *
 * @author Brian Harleton
 * @see DataMapTree
 * @see DataMap
 */
public class SearchDataMapDialog extends JDialog {

  SearchDataMapFindPanel findPanel = new SearchDataMapFindPanel();

  SearchDataMapOptionsPanel optionsPanel = new SearchDataMapOptionsPanel();

  SearchDataMapButtonPanel buttonPanel = new SearchDataMapButtonPanel();

  private DataMapTree dmt = null;
  private SoarWorkingMemoryModel swmm = null;
  private FakeTreeNode rootOfSearch = null;
  private String lastSearch = "";

  // Search variables
  private VSQueue VSQueue;
  private boolean[] visitedVertices;
  private int numberOfVertices;

	/**
   *  Constructor for the SearchDataMapDialog
   *  @param  owner frame that owns this dialog window
   *  @param  tree  DataMapTree that is being searched
   *  @param  rootNode the FakeTreeNode in the datamap that is currently selected and
   *  from which the search will begin at.
	 */
	public SearchDataMapDialog(final Frame owner, DataMapTree tree, FakeTreeNode rootNode) {
		super(owner, "Search DataMap", false);

    dmt = tree;
    swmm = (MainFrame.getMainFrame()).getOperatorWindow().getDatamap();
    VSQueue = new QueueAsLinkedList();
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
    contentPane.add(buttonPanel, c);
    pack();
    getRootPane().setDefaultButton(buttonPanel.findNextButton);

    addWindowListener(new WindowAdapter() {
      public void windowOpened(WindowEvent we) {
        setLocationRelativeTo(owner);
        findPanel.requestFocus();
      }
    });

    buttonPanel.cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });

    buttonPanel.findNextButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Object[] theData = findPanel.getData();
        Boolean[] theOptions = optionsPanel.getData();

        String toFind = (String) theData[0];
        Boolean caseSensitive = (Boolean) theData[1];

        if(!toFind.equals(lastSearch)) {
          initializeSearch();
          lastSearch = toFind;
        }

        if(!caseSensitive.booleanValue()) {
          toFind = toFind.toLowerCase();
        }

        FakeTreeNode foundftn = null;
        boolean edgeNotFound = true;
        int children = 0;


        while((!VSQueue.isEmpty())  && edgeNotFound) {
          FakeTreeNode ftn = (FakeTreeNode) VSQueue.dequeue();
          int rootValue = ftn.getEnumeratingVertex().getValue();
          visitedVertices[rootValue] = true;
          children = ftn.getChildCount();

          // See if current FakeTreeNode's edge match desired edge
          if( ftn.getEdge() != null) {
            if( (ftn.getEdge()).getName().equals(toFind)  &&
                ((theOptions[0].booleanValue() && ftn.getEnumeratingVertex() instanceof SoarIdentifierVertex)
                    || (theOptions[1].booleanValue() && ftn.getEnumeratingVertex() instanceof EnumerationVertex)
                    || (theOptions[2].booleanValue() && ftn.getEnumeratingVertex() instanceof StringVertex)
                    || (theOptions[3].booleanValue() && ftn.getEnumeratingVertex() instanceof IntegerRangeVertex)
                    || (theOptions[4].booleanValue() && ftn.getEnumeratingVertex() instanceof FloatRangeVertex))   ) {
              edgeNotFound = false;
              foundftn = ftn;
            }
          }

          // Examine children of ftn
          if((children != 0) && edgeNotFound){
            for(int i = 0; i < children; i++) {
              FakeTreeNode childftn = ftn.getChildAt(i);
              int vertexValue = childftn.getEnumeratingVertex().getValue();
              if(! visitedVertices[vertexValue]) {
                visitedVertices[vertexValue] = true;
                VSQueue.enqueue(childftn);
              }   // if never visited vertex
              else {
                // Check this edge since it won't be added to the queue
                if( childftn.getEdge() != null) {
                  if( (childftn.getEdge()).getName().equals(toFind)  &&
                    ((theOptions[0].booleanValue() && childftn.getEnumeratingVertex() instanceof SoarIdentifierVertex)
                      || (theOptions[1].booleanValue() && childftn.getEnumeratingVertex() instanceof EnumerationVertex)
                      || (theOptions[2].booleanValue() && childftn.getEnumeratingVertex() instanceof StringVertex)
                      || (theOptions[3].booleanValue() && childftn.getEnumeratingVertex() instanceof IntegerRangeVertex)
                      || (theOptions[4].booleanValue() && childftn.getEnumeratingVertex() instanceof FloatRangeVertex))   ) {
                    edgeNotFound = false;
                    foundftn = childftn;
                  }
                }
              }    // end of else already visited this vertex
            }   // for checking all of ftn's children
          }   // if ftn has children
          else if((children != 0) && !edgeNotFound) {
            // still add children to queue for possible continued searching
            for(int i =0; i < children; i++) {
              FakeTreeNode childftn = ftn.getChildAt(i);
              int vertexValue = childftn.getEnumeratingVertex().getValue();
              if(! visitedVertices[vertexValue]) {
                visitedVertices[vertexValue] = true;
                VSQueue.enqueue(childftn);
              } // if never visited vertex, enqueue
            }
          } // end of if edge found, still add children to queue for continued searching
          
        }   // while queue is not empty, examine each vertex in it

        if(foundftn != null) {
          dmt.highlightEdge(foundftn);
       }
        else {
          JOptionPane.showMessageDialog(null, "No more instances found");
        }

        if(! buttonPanel.keepDialog.isSelected()) {
          dispose();
        }
      }
    });



  }

  /**
   *  Initializes the search queue for a search of the datamap
   *  called when a new search dialog is created or when the user changes the
   *  desired word
   */
  private void initializeSearch() {
    VSQueue = new QueueAsLinkedList();
    FakeTreeNode ftn = rootOfSearch;
    numberOfVertices = swmm.getNumberOfVertices();
    lastSearch = "";
    visitedVertices = new boolean[numberOfVertices];
    for(int i = 0; i < numberOfVertices; i++)
      visitedVertices[i] = false;
    VSQueue.enqueue(ftn);
  }

}     // end of SearchDataMapDialog class
