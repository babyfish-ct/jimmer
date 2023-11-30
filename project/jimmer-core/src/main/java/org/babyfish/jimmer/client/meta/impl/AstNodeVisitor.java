package org.babyfish.jimmer.client.meta.impl;

public interface AstNodeVisitor<S> {

    void visitAstNode(AstNode<S> astNode);

    default void visitedAstNode(AstNode<S> astNode) {}
}
