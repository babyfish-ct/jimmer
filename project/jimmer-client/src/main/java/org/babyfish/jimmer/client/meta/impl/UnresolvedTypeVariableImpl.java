package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.UnresolvedTypeVariable;
import org.babyfish.jimmer.client.meta.Visitor;

public class UnresolvedTypeVariableImpl implements UnresolvedTypeVariable {

    private final String name;

    public UnresolvedTypeVariableImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitUnresolvedTypeVariable(this);
    }

    @Override
    public String toString() {
        return "@generic:" + name;
    }
}
