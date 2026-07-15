package org.babyfish.jimmer.client.meta.impl;

public abstract class AstNode<S> {

    protected S source;

    AstNode(S source) {
        this.source = source;
    }

    AstNode() {}

    public S getSource() {
        return source;
    }

    public abstract void accept(AstNodeVisitor<S> visitor);
}
