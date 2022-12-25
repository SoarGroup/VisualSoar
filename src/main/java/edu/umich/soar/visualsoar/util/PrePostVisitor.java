package edu.umich.soar.visualsoar.util;

/**
 * This class follows the visitor pattern
 * it is based on Object-Oriented Design patterns in C++, later converted to Java
 * We might want to visit a traversal in Pre or In Order a PrePostVisitor
 * allows this operation by letting a derived class support that operation
 *
 * @author Brad Jones
 */


public abstract class PrePostVisitor extends Visitor {

    // Over-ride this operation if you want a preorder traversal
    public void preVisit(Object o) {
    }

    // Over-ride this operation if you want an in-order traversal
    public void visit(Object o) {
    }

    // Over-ride this operation if you want a PostOrder traversal
    public void postVisit(Object o) {
    }
}
