package edu.umich.soar.visualsoar.util;

import java.util.LinkedList;

/**
 * This is basically just a wrapper class to make a Linked
 * list behave like a queue
 */

public class QueueAsLinkedList<E> implements VSQueue<E> {
    LinkedList<E> line = new LinkedList<>();

    public void enqueue(E o) {
        line.add(o);
    }

    public E dequeue() {
        return line.removeFirst();
    }

    public boolean isEmpty() {
        return line.isEmpty();
    }
}
