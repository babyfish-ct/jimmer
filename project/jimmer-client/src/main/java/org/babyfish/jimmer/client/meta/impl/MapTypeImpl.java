package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.MapType;
import org.babyfish.jimmer.client.meta.Type;
import org.babyfish.jimmer.client.meta.Visitor;

class MapTypeImpl implements MapType {

    private final Type keyType;

    private final Type valueType;

    MapTypeImpl(Type keyType, Type valueType) {
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
    public void accept(Visitor visitor) {
        if (visitor.isTypeVisitable(this)) {
            visitor.visitMapType(this);
            keyType.accept(visitor);
            valueType.accept(visitor);
        }
    }

    @Override
    public String toString() {
        return "Map<" + keyType + ", " + valueType + '>';
    }
}
