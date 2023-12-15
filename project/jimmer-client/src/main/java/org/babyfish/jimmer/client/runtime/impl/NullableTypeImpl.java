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
    protected String toStringImpl(Set<Graph> stack) {
        return string(targetType, stack) + '?';
    }

    public static NullableType of(Type targetType) {
        if (targetType instanceof NullableType) {
            return (NullableType) targetType;
        }
        return new NullableTypeImpl(targetType);
    }
}
