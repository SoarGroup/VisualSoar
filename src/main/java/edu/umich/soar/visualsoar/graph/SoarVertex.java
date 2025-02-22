package edu.umich.soar.visualsoar.graph;

import edu.umich.soar.visualsoar.util.IdGenerator;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is the base class for all Soar Working memory
 * vertices.  The known subclasses are:
 * {@link SoarIdentifierVertex},
 * {@link IntegerRangeVertex},
 * {@link FloatRangeVertex},
 * {@link EnumerationVertex},
 * {@link StringVertex} (no longer used?),
 * {@link ForeignVertex}
 *
 * @author Brad Jones
 * Created: 05 Jun 2000
 */

public abstract class SoarVertex extends Vertex {
    private static final long serialVersionUID = 20221225L;

    //These are valid vertex types that can be used in .dm files
    public static final Set<String> VERTEX_TYPES = new HashSet<>(){{
      add("SOAR_ID");
      add("ENUMERATION");
      add("INTEGER_RANGE");
      add("INTEGER");
      add("FLOAT_RANGE");
      add("FLOAT");
      add("STRING");
      add("FOREIGN");
    }};

  // Used for uniquely identifying this node in a project file
  private final String serializationId;

///////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////

    /**
     * Constructs a SoarVertex with the given id (deriving serializationId from id)
     */
    public SoarVertex(int id) {
        super(id);
        serializationId = IdGenerator.getId();
    }

  /** Constructs a SoarVertex with the given IDs */
  public SoarVertex(int id, String serializationId) {
    super(id);
    this.serializationId = serializationId;
  }

///////////////////////////////////////////////
// Accessors
///////////////////////////////////////////////

    /**
     * This method is used to determine whether this node allows children
     *
     * @return whether this Vertex allows emanating edges
     */
    public abstract boolean allowsEmanatingEdges();

    /**
     * This method tells us whether the edit method will work
     *
     * @return whether this node is editable
     */
    public boolean isEditable() {
        return false;
    }

    /**
     * This method determines whether a given value is valid
     * for this particular node
     *
     * @param value the string we are checking the validity of
     * @return is the string a valid value
     */
    public abstract boolean isValid(String value);

    /**
     * Method returns a new copy of the same data, but with
     * a new id
     *
     * @param newId the new ID to use
     * @return the new vertex
     */
    public abstract SoarVertex copy(int newId);

    /**
     * returns the name of this vertex type as a string (used for reporting information to the user)
     */
    public abstract String typeName();

    public String getSerializationId() {
      return serializationId;
    }

///////////////////////////////////////////////
// Modifiers
///////////////////////////////////////////////

    /**
     * This method allows the user to edit the contents of this node
     */
    public boolean edit(java.awt.Frame owner) {
        JOptionPane.showMessageDialog(owner,
                "This element has no values to edit,\n use \"Rename Attribute...\" to edit the attribute name",
                "Invalid Edit", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    /**
     * This method writes a description of this node to the
     * stream pointed to by the writer
     *
     * @param w the stream where this node is described to
     * @throws java.io.IOException if there was an error writing to the stream
     */
    public abstract void write(java.io.Writer w) throws java.io.IOException;

    public boolean isUnknown() {
        return false;
    }

    /**
     * I added this because the re-import of linked datamap subtrees
     * was causing a crash in {@link Edge#mate(SoarVertex)}.  I don't know why
     * by the given parameter was a match but a copy.  It makes me nervous that
     * I've fixed it this way.
     *
     * I tried to fix it by calling reduce on the SWMM object before/after
     * deleting the old subtree but it didn't work. I think you could fix it
     * by saving to SWMM to disk and reloading it.  yuck. :(
     *
     * :AMN: Feb 2024
     */
    @Override
    public boolean equals(Object o) {
        if (! (o instanceof SoarVertex)) return false;
        return ((SoarVertex)o).number == this.number;
    }

}

