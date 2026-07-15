package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.ApiParameter;
import org.babyfish.jimmer.client.meta.TypeRef;

import java.io.IOException;

public class ApiParameterImpl<S> extends AstNode<S> implements ApiParameter {

    private final String name;

    private TypeRefImpl<S> type;

    private int originalIndex;

    ApiParameterImpl(S source, String name) {
        super(source);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TypeRef getType() {
        return type;
    }

    public void setType(TypeRefImpl<S> type) {
        this.type = type;
    }

    @Override
    public int getOriginalIndex() {
        return originalIndex;
    }

    public void setOriginalIndex(int originalIndex) {
        this.originalIndex = originalIndex;
    }

    @Override
    public void accept(AstNodeVisitor<S> visitor) {
        try {
            if (!visitor.visitAstNode(this)) {
                return;
            }
            type.accept(visitor);
        } finally {
            visitor.visitedAstNode(this);
        }
    }

    @Override
    public String toString() {
        return "ParameterImpl{" +
                "name='" + name + '\'' +
                ", type=" + type +
                '}';
    }

    }
