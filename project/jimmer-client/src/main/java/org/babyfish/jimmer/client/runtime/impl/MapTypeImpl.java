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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MapTypeImpl mapType = (MapTypeImpl) o;

        if (!keyType.equals(mapType.keyType)) return false;
        return valueType.equals(mapType.valueType);
    }

    @Override
    public int hashCode() {
        int result = keyType.hashCode();
        result = 31 * result + valueType.hashCode();
        return result;
    }

    @Override
    protected String toStringImpl(Set<Graph> stack) {
        return "map<" + string(keyType, stack) + ", " + string(valueType, stack) + ">";
    }
}
