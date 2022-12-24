package edu.umich.soar.visualsoar.datamap;

import edu.umich.soar.visualsoar.graph.SoarIdentifierVertex;
import edu.umich.soar.visualsoar.operatorwindow.OperatorNode;
import edu.umich.soar.visualsoar.parser.TriplesExtractor;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class provides some static methods to do the checking against the
 * datamap
 * @author Brad Jones */
 
public class DataMapChecker 
{
    // There is no need to instantiate this class
    private DataMapChecker() {}
    
    // Static Member Functions

    public static void check(SoarWorkingMemoryModel dataMap,
                             SoarIdentifierVertex startVertex,
                             TriplesExtractor triplesExtractor,
                             CheckerErrorHandler ceh) 
    {
        Map varMap = DataMapMatcher.matches(dataMap,
                                            startVertex,
                                            triplesExtractor,
                                            ceh);
        if(varMap != null) 
        {
            Set keySet = varMap.keySet();
            Iterator vars = keySet.iterator();
            while(vars.hasNext()) 
            {
                String varKey = (String)vars.next();
                Set value = (Set)varMap.get(varKey);
                if(value.isEmpty()) 
                ceh.variableNotMatched(varKey); 
            }
        }
    }


    /*
     *  This function is responsible for checking the datamap to see if the triples in the triplesExtractor are valid.
     *  In the triples are invalid, the datamap is adjusted to match the triples.
     */
    public static void complete(SoarWorkingMemoryModel dataMap,
                                SoarIdentifierVertex startVertex,
                                TriplesExtractor triplesExtractor,
                                CheckerErrorHandler ceh,
                                OperatorNode current) 
    {
        DataMapMatcher.complete(dataMap, startVertex, triplesExtractor, ceh, current);
    }//complete()
    
}

