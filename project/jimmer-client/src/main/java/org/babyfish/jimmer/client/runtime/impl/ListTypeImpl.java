package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.runtime.ListType;
import org.babyfish.jimmer.client.runtime.Type;

import java.util.Set;

public class ListTypeImpl extends Graph implements ListType {

    private final Type elementType;

    public ListTypeImpl(Type elementType) {
        this.elementType = elementType;
    }

    @Override
    public Type getElementType() {
        return elementType;
    }

    @Override
    protected String toStringImpl(Set<Graph> stack) {
        return "list<" + string(elementType, stack) + '>';
    }
}
