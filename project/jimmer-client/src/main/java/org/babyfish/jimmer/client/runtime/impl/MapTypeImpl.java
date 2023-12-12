package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.runtime.MapType;
import org.babyfish.jimmer.client.runtime.Type;

import java.util.Set;

public class MapTypeImpl extends Graph implements MapType {

    private final Type keyType;

    private final Type valueType;

    public MapTypeImpl(Type keyType, Type valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public Type getKeyType() {
        return keyType;
    }

    @Override
    public Type getValueType() {
        return valueType;
    }

    @Override
    protected String toStringImpl(Set<Graph> stack) {
        return "map<" + string(keyType, stack) + ", " + string(valueType, stack) + ">";
    }
}
