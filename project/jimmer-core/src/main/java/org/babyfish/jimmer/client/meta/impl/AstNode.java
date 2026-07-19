package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class AstNode<S> {

    protected S source;

    AstNode(S source) {
        this.source = source;
    }

    AstNode() {}

    @JsonIgnore
    public S getSource() {
        return source;
    }

    public abstract void accept(AstNodeVisitor<S> visitor);
}
