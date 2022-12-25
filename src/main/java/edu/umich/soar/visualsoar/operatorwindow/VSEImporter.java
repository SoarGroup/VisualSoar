package edu.umich.soar.visualsoar.operatorwindow;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryAppender;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.util.ReaderUtils;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

class VSEImporter {
    public static final int FILE = 1;
    public static final int HLOPERATOR = 2;
    public static final int OPERATOR = 4;

    private VSEImporter() {
    }

    public static void read(Reader r, OperatorNode on, OperatorWindow operatorWindow,
                            SoarWorkingMemoryModel swmm, int acceptableImports) throws IOException, NumberFormatException {
        String versionCheck = ReaderUtils.getWord(r);
		if (!versionCheck.equals("VERSION")) {
			throw new IOException("Parse Exception");
		}
        int versionNumber = ReaderUtils.getInteger(r);
		if ((versionNumber != 1) && (versionNumber != 2)) {
			throw new IOException("Parse Exception");
		}
        String typeCheck = ReaderUtils.getWord(r);
		if (!typeCheck.equals("IMPORT_TYPE")) {
			throw new IOException("Parse Exception");
		}
        if ((ReaderUtils.getInteger(r) & acceptableImports) == 0) {
            JOptionPane.showMessageDialog(MainFrame.getMainFrame(), "Unacceptable Import Type",
                    "Could Not Import", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String layoutCheck = ReaderUtils.getWord(r);
		if (!layoutCheck.equals("LAYOUT")) {
			throw new IOException("Parse Exception");
		}

        int numberOfNodes = ReaderUtils.getInteger(r);
        int currentNode = 0;
        LinkedList<OperatorNode> hlOperators = new LinkedList<>();

        // This hash table has keys of ids and a pointer as a value
        // it is used for parent lookup
        Hashtable<Integer, OperatorNode> ht = new Hashtable<>();

        ht.put(-1, on);
        // Read in all the other nodes
        do {
            // again read in the tree specific stuff
            int parentId = ReaderUtils.getInteger(r);
            OperatorNode node;

            // get the parent
            OperatorNode parent = ht.get(parentId);

            String type = ReaderUtils.getWord(r);
            if (type.equals("HLOPERATOR")) {
                String name = ReaderUtils.getWord(r);
                node = operatorWindow.createSoarOperatorNode(name, name + ".soar", name, ReaderUtils.getInteger(r) + swmm.numberOfVertices());
                operatorWindow.addChild(parent, node);
                File file = new File(node.getFolderName());
                file.mkdir();
                hlOperators.add(node);
            } else if (type.equals("HLFOPERATOR")) {
                String name = ReaderUtils.getWord(r);
                node = operatorWindow.createFileOperatorNode(name, name + ".soar", name, ReaderUtils.getInteger(r) + swmm.numberOfVertices());
                operatorWindow.addChild(parent, node);
                File file = new File(node.getFolderName());
                file.mkdir();
                hlOperators.add(node);
            } else if (type.equals("HLIOPERATOR")) {
                String name = ReaderUtils.getWord(r);
                node = operatorWindow.createImpasseOperatorNode(name, name + ".soar", name, ReaderUtils.getInteger(r) + swmm.numberOfVertices());
                operatorWindow.addChild(parent, node);
                File file = new File(node.getFolderName());
                file.mkdir();
                hlOperators.add(node);
            } else if (type.equals("OPERATOR")) {
                String name = ReaderUtils.getWord(r);
                node = operatorWindow.createSoarOperatorNode(name, name + ".soar");
                operatorWindow.addChild(parent, node);
            } else if (type.equals("FOPERATOR")) {
                String name = ReaderUtils.getWord(r);
                node = operatorWindow.createFileOperatorNode(name, name + ".soar");
                operatorWindow.addChild(parent, node);
            } else if (type.equals("IOPERATOR")) {
                String name = ReaderUtils.getWord(r);
                node = operatorWindow.createImpasseOperatorNode(name, name + ".soar");
                operatorWindow.addChild(parent, node);
            } else if (type.equals("FILE")) {
				String name = ReaderUtils.getWord(r);
				if (currentNode == 0) {
					if (parent.creationConflict(new File(name + ".soar"))) {
						return;
					}
				}
				node = operatorWindow.createFileOperatorNode(name, name + ".soar");
				// Put in alphabetical order
				boolean found = false;
				for (int i = 0; i < parent.getChildCount() && !found; ++i) {
					String s = node.toString();
					if (s.compareTo(parent.getChildAt(i).toString()) <= 0) {
						found = true;
						if (s.compareTo(parent.getChildAt(i).toString()) == 0) {
							JOptionPane.showMessageDialog(MainFrame.getMainFrame(),
									"Node conflict for " + s,
									"Node Conflict",
									JOptionPane.ERROR_MESSAGE);
							return;
						} else {
							operatorWindow.addChild(parent, node);
						}
					}
				}
				if (!found) {
					operatorWindow.addChild(parent, node);
				}
			} else {
				throw new IOException("Parse Error");
			}
            // add that node to the hash table
            ht.put(currentNode, node);
        } while (numberOfNodes > ++currentNode);

        /////////////////////////////////////////////////
        // Rule Section
        /////////////////////////////////////////////////
        String rulesCheck = ReaderUtils.getWord(r);
		if (!rulesCheck.equals("RULES")) {
			throw new IOException("Parse Error");
		}
        String ruleFile = ReaderUtils.getWord(r);
        int currentLineNumber;
        while (ruleFile.equals("RULE_FILE")) {
            currentLineNumber = 0;
            OperatorNode node = ht.get(ReaderUtils.getInteger(r));
            FileWriter w = new FileWriter(node.getFileName());
            int numberOfLines = ReaderUtils.getInteger(r);
            while (currentLineNumber++ < numberOfLines) {
                w.write(ReaderUtils.getLine(r));
            }
            ruleFile = ReaderUtils.getWord(r);
            w.close();
        }

        /////////////////////////////////////////////////////
        // DataMap Section
        /////////////////////////////////////////////////////
        if (ruleFile.equals("NODATAMAP")) {
            r.close();
        } else if (ruleFile.equals("DATAMAP")) {
			SoarWorkingMemoryAppender.append(swmm, r);
			Iterator<OperatorNode> i = hlOperators.iterator();
			while (i.hasNext()) {
				SoarOperatorNode hlo = (SoarOperatorNode) i.next();
				hlo.restoreId(swmm);
			}
		} else {
			throw new IOException("Parse Error");
		}
    }
}
