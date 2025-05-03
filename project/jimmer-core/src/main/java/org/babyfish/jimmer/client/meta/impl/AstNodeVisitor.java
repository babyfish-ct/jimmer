package org.babyfish.jimmer.client.meta.impl;

public interface AstNodeVisitor<S> {

    boolean visitAstNode(AstNode<S> astNode);

    default boolean visitAstNode(AstNode<S> astNode,boolean onlySaveCurrentModuleClass){
        return visitAstNode(astNode);
    }

    default void visitedAstNode(AstNode<S> astNode) {}
}
