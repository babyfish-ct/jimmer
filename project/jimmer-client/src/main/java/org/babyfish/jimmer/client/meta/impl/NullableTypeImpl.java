package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.NullableType;
import org.babyfish.jimmer.client.meta.Type;
import org.babyfish.jimmer.client.meta.Visitor;

class NullableTypeImpl implements NullableType {

    private final Type targetType;

    private NullableTypeImpl(Type targetType) {
        this.targetType = targetType;
    }

    @Override
    public Type getTargetType() {
        return targetType;
    }

    static NullableType of(Type type) {
        if (type instanceof NullableType) {
            return (NullableType) type;
        }
        return new NullableTypeImpl(type);
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor.isTypeVisitable(this)) {
            visitor.visitNullableType(this);
            targetType.accept(visitor);
        }
    }

    @Override
    public String toString() {
        return targetType.toString() + '?';
    }
}
