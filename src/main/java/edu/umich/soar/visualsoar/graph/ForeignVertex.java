package edu.umich.soar.visualsoar.graph;

import java.io.IOException;
import java.io.Writer;

/**
 * class ForeignVertex
 *
 * represents a node that is linked from another project.  It can be used
 * in this project but not edited.
 *
 * @author Andrew Nuxoll
 * @version created Jan 2024
 */
public class ForeignVertex extends SoarVertex {

    //The .dm file that contains this vertex natively
    protected String foreignDM;

    //This is a copy of the original Vertex in the foreign file
    SoarVertex foreignSV;

    public ForeignVertex(int id, String foreignDM, SoarVertex foreignSv) {
        super(id);
        // normalize to forward slashes, which are portable between Windows/Posix
        this.foreignDM = foreignDM.replace('\\', '/');
        this.foreignSV = foreignSv;
    }

  public ForeignVertex(int id, String serializationId, String foreignDM, SoarVertex foreignSv) {
    super(id, serializationId);
    this.foreignDM = foreignDM;
    this.foreignSV = foreignSv;
  }

    @Override
    public boolean allowsEmanatingEdges() {
        return foreignSV.allowsEmanatingEdges();
    }

    @Override
    public boolean isValid(String s) {
        return foreignSV.isValid(s);
    }

    @Override
    public SoarVertex copy(int newId) {
        SoarVertex svCopy = this.foreignSV.copy(this.foreignSV.getValue());
        return new ForeignVertex(newId, this.foreignDM, svCopy);
    }

    @Override
    public void write(Writer w) throws IOException {
        w.write("FOREIGN " + this.getValue() + " " + this.foreignDM + " ");
        //The copied data is saved so the project can be used even if the original DM is missing
        foreignSV.write(w);
    }

    @Override
    public String typeName() { return "foreign " + this.foreignSV.typeName(); }

    @Override
    public String toString() {
        return foreignSV.toString();
    }

    /** the relative path to the foreign datamap file (.dm)
     * that this ForeignVertex object refers to */
    public String getForeignDMName() {
        return this.foreignDM;
    }

    /** the SoarVertex object in the foreign datamap
     * that this ForeignVertex refers to */
    public SoarVertex getForeignSoarVertex() {
        // TODO: maybe for some use-cases we should be taking a copy?
        return this.foreignSV;
    }

}//class ForeignVertex
