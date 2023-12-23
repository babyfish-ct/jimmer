package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.runtime.NullableType;
import org.babyfish.jimmer.client.runtime.Type;

import java.util.Set;

public class NullableTypeImpl extends Graph implements NullableType {

    private final Type targetType;

    private NullableTypeImpl(Type targetType) {
        this.targetType = targetType;
    }

    @Override
    public Type getTargetType() {
        return targetType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NullableTypeImpl that = (NullableTypeImpl) o;

        return targetType.equals(that.targetType);
    }

    @Override
    public int hashCode() {
        return targetType.hashCode();
    }

    @Override
    protected String toStringImpl(Set<Graph> stack) {
        return string(targetType, stack) + '?';
    }

    public static NullableType of(Type type) {
        if (type instanceof NullableType) {
            return (NullableType) type;
        }
        return new NullableTypeImpl(type);
    }

    public static Type unwrap(Type type) {
        if (type instanceof NullableType) {
            return ((NullableType) type).getTargetType();
        }
        return type;
    }
}
