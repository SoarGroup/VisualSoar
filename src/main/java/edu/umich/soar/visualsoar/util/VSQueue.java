package edu.umich.soar.visualsoar.util;

/**
 * This class is based on Object-Oriented Design patterns in C++, later converted to Java
 * This is an interface to a Queue
 * <p>
 * FIXME:  This is over-engineered.  Just use the implementing class.
 *
 * @author Brad Jones
 */

public interface VSQueue<E> {
    void enqueue(E o);

    E dequeue();

    boolean isEmpty();
}
