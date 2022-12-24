package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.util.ReaderUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

public class SoarWorkingMemoryAppender {
	public static void append(SoarWorkingMemoryModel swmm,Reader fr) throws IOException, NumberFormatException {
		try {
			int prevNumber = swmm.numberOfVertices();
			int numberOfVertices = ReaderUtils.getInteger(fr);			
			for(int i = 0; i < numberOfVertices; ++i) {
				String type = ReaderUtils.getWord(fr);
				ReaderUtils.getInteger(fr);
				if (type.equals("SOAR_ID")) {
					swmm.createNewSoarId();
				}
				else if (type.equals("ENUMERATION")) {
					int enumerationSize = ReaderUtils.getInteger(fr);
					Vector<String> v = new Vector<>();
					for(int j = 0; j < enumerationSize; ++j) 
						v.add(ReaderUtils.getWord(fr));
					swmm.createNewEnumeration(v);
				}
				else if (type.equals("INTEGER_RANGE")) {
					swmm.createNewIntegerRange(ReaderUtils.getInteger(fr), ReaderUtils.getInteger(fr));
				}
				else if (type.equals("INTEGER")) {
					swmm.createNewInteger();
				}
				else if (type.equals("FLOAT_RANGE")) {
					swmm.createNewFloatRange(ReaderUtils.getFloat(fr), ReaderUtils.getFloat(fr));
				}
				else if (type.equals("FLOAT")) {
					swmm.createNewFloat();
				}
				else if (type.equals("STRING")) {
					swmm.createNewString();
				}
				else {
					System.err.println("Unknown type: please update SoarWorking Memory Reader constructor :" + type);
				}
								
			}
			int numberOfEdges = ReaderUtils.getInteger(fr);
			for(int j = 0; j < numberOfEdges; ++j) 
				swmm.addTriple(swmm.getVertexForId(ReaderUtils.getInteger(fr)+prevNumber),ReaderUtils.getWord(fr),swmm.getVertexForId(ReaderUtils.getInteger(fr)+prevNumber) );
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		catch(NumberFormatException nfe) {
			nfe.printStackTrace();
			throw nfe;
		}
	}
}
