package edu.umich.soar.visualsoar.util;

/**
 * This is a wrapper class to make a iterator behave like an
 * Enumeration.
 * <p>
 * FIXME:  This class leads to awkward code.  Better to phase out and use
 * Collection classes with for-each loops. -:AMN:
 *
 * @author Brad Jones
 */
public class EnumerationIteratorWrapper implements java.util.Enumeration {
    java.util.Iterator i;

    public EnumerationIteratorWrapper(java.util.Iterator _i) {
        i = _i;
    }

    public boolean hasMoreElements() {
        return i.hasNext();
    }

    public Object nextElement() {
        return i.next();
    }
}
