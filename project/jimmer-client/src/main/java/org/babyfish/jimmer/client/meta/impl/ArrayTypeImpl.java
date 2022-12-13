package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.ArrayType;
import org.babyfish.jimmer.client.meta.Type;
import org.babyfish.jimmer.client.meta.Visitor;

class ArrayTypeImpl implements ArrayType {

    private final Type elementType;

    ArrayTypeImpl(Type elementType) {
        this.elementType = elementType;
    }

    @Override
    public Type getElementType() {
        return elementType;
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor.isTypeVisitable(this)) {
            visitor.visitArrayType(this);
            elementType.accept(visitor);
        }
    }

    @Override
    public String toString() {
        return "Array<" + elementType + '>';
    }
}
